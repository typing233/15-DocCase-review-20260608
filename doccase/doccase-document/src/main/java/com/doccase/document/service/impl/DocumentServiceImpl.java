package com.doccase.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doccase.common.constant.MqConstants;
import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.DocumentDTO;
import com.doccase.common.enums.ResponseCode;
import com.doccase.common.enums.StorageType;
import com.doccase.common.exception.BizException;
import com.doccase.common.util.FileUtil;
import com.doccase.common.util.HashUtil;
import com.doccase.document.domain.entity.Document;
import com.doccase.document.domain.entity.DocumentVersion;
import com.doccase.document.domain.vo.DocumentCreateRequest;
import com.doccase.document.domain.vo.DocumentQueryRequest;
import com.doccase.document.mapper.DocumentMapper;
import com.doccase.document.mapper.DocumentVersionMapper;
import com.doccase.document.service.DocumentService;
import com.doccase.document.storage.StorageFactory;
import com.doccase.document.storage.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper versionMapper;
    private final StorageFactory storageFactory;
    private final RabbitTemplate rabbitTemplate;

    @Value("${doccase.storage.type:minio}")
    private String storageType;

    @Override
    @Transactional
    public DocumentDTO createDocument(Long userId, DocumentCreateRequest request, MultipartFile file) {
        try {
            String ext = FileUtil.getExtension(file.getOriginalFilename());
            String objectKey = FileUtil.generateObjectKey("documents/" + userId, file.getOriginalFilename());
            String hash = HashUtil.sha256(file.getInputStream());

            // Check for duplicate (instant upload)
            Document existing = documentMapper.selectOne(
                    new LambdaQueryWrapper<Document>()
                            .eq(Document::getFileHash, hash)
                            .eq(Document::getIsDeleted, 0)
                            .last("LIMIT 1"));
            if (existing != null) {
                objectKey = existing.getStoragePath();
            } else {
                StorageStrategy strategy = storageFactory.getStrategy(storageType);
                strategy.upload(file.getInputStream(), objectKey, file.getContentType(), file.getSize());
            }

            Document doc = new Document();
            doc.setUserId(userId);
            doc.setTitle(request.getTitle());
            doc.setDescription(request.getDescription());
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setFileType(ext);
            doc.setMimeType(file.getContentType());
            doc.setFileHash(hash);
            doc.setStorageType(storageType);
            doc.setStoragePath(objectKey);
            doc.setCurrentVersion(1);
            doc.setStatus(1);
            doc.setTagIds(request.getTagIds());
            doc.setMetadata(request.getMetadata());
            doc.setOcrStatus(0);
            doc.setIsDeleted(0);
            documentMapper.insert(doc);

            // Save version 1
            DocumentVersion version = new DocumentVersion();
            version.setDocumentId(doc.getId());
            version.setVersionNumber(1);
            version.setFileHash(hash);
            version.setStoragePath(objectKey);
            version.setFileSize(file.getSize());
            version.setChangeNote("初始版本");
            version.setCreatedBy(userId);
            version.setCreatedAt(LocalDateTime.now());
            versionMapper.insert(version);

            // Publish event
            DocumentDTO dto = toDTO(doc);
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DOCUMENT, MqConstants.RK_DOCUMENT_CREATED, dto);

            return dto;
        } catch (Exception e) {
            throw new BizException(ResponseCode.FILE_UPLOAD_FAILED, e.getMessage());
        }
    }

    @Override
    public DocumentDTO getDocument(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BizException(ResponseCode.FILE_NOT_FOUND);
        }
        return toDTO(doc);
    }

    @Override
    public PageResult<DocumentDTO> queryDocuments(DocumentQueryRequest request) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getIsDeleted, 0);

        if (request.getUserId() != null) wrapper.eq(Document::getUserId, request.getUserId());
        if (request.getFileType() != null) wrapper.eq(Document::getFileType, request.getFileType());
        if (request.getStatus() != null) wrapper.eq(Document::getStatus, request.getStatus());
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(Document::getTitle, request.getKeyword())
                    .or().like(Document::getDescription, request.getKeyword())
                    .or().like(Document::getFileName, request.getKeyword()));
        }

        if (Boolean.TRUE.equals(request.getAsc())) {
            wrapper.orderByAsc(Document::getCreatedAt);
        } else {
            wrapper.orderByDesc(Document::getCreatedAt);
        }

        Page<Document> page = documentMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        List<DocumentDTO> records = page.getRecords().stream().map(this::toDTO).toList();
        return PageResult.of(records, page.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    @Transactional
    public DocumentDTO updateDocument(Long id, Long userId, Map<String, Object> updates) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) throw new BizException(ResponseCode.FILE_NOT_FOUND);

        if (updates.containsKey("title")) doc.setTitle((String) updates.get("title"));
        if (updates.containsKey("description")) doc.setDescription((String) updates.get("description"));
        if (updates.containsKey("status")) doc.setStatus((Integer) updates.get("status"));
        if (updates.containsKey("metadata")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) updates.get("metadata");
            doc.setMetadata(meta);
        }

        documentMapper.updateById(doc);

        DocumentDTO dto = toDTO(doc);
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DOCUMENT, MqConstants.RK_DOCUMENT_UPDATED, dto);
        return dto;
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, Long userId) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) throw new BizException(ResponseCode.FILE_NOT_FOUND);

        doc.setIsDeleted(1);
        doc.setDeletedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        DocumentDTO dto = toDTO(doc);
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DOCUMENT, MqConstants.RK_DOCUMENT_DELETED, dto);
    }

    @Override
    @Transactional
    public void restoreVersion(Long documentId, Integer versionNumber, Long userId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) throw new BizException(ResponseCode.FILE_NOT_FOUND);

        DocumentVersion version = versionMapper.selectOne(
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, documentId)
                        .eq(DocumentVersion::getVersionNumber, versionNumber));
        if (version == null) throw new BizException(ResponseCode.NOT_FOUND, "版本不存在");

        doc.setFileHash(version.getFileHash());
        doc.setStoragePath(version.getStoragePath());
        doc.setFileSize(version.getFileSize());
        doc.setCurrentVersion(versionNumber);
        documentMapper.updateById(doc);
    }

    private DocumentDTO toDTO(Document doc) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(doc.getId());
        dto.setUserId(doc.getUserId());
        dto.setTitle(doc.getTitle());
        dto.setDescription(doc.getDescription());
        dto.setFileName(doc.getFileName());
        dto.setFileSize(doc.getFileSize());
        dto.setFileType(doc.getFileType());
        dto.setMimeType(doc.getMimeType());
        dto.setFileHash(doc.getFileHash());
        dto.setStorageType(doc.getStorageType());
        dto.setStoragePath(doc.getStoragePath());
        dto.setThumbnailPath(doc.getThumbnailPath());
        dto.setCurrentVersion(doc.getCurrentVersion());
        dto.setStatus(doc.getStatus());
        dto.setTagIds(doc.getTagIds());
        dto.setMetadata(doc.getMetadata());
        dto.setOcrStatus(doc.getOcrStatus());
        dto.setOcrText(doc.getOcrText());
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setUpdatedAt(doc.getUpdatedAt());
        return dto;
    }
}
