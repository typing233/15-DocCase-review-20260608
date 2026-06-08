package com.doccase.email.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doccase.common.domain.PageResult;
import com.doccase.common.exception.BizException;
import com.doccase.common.response.ApiResponse;
import com.doccase.email.domain.entity.EmailAccount;
import com.doccase.email.domain.entity.EmailArchiveRecord;
import com.doccase.email.domain.entity.EmailAuditLog;
import com.doccase.email.domain.vo.EmailAccountCreateRequest;
import com.doccase.email.mapper.EmailAccountMapper;
import com.doccase.email.mapper.EmailArchiveRecordMapper;
import com.doccase.email.mapper.EmailAuditLogMapper;
import com.doccase.email.service.EmailPollingService;
import com.doccase.email.service.impl.EmailPollingServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailAccountController {

    private final EmailAccountMapper accountMapper;
    private final EmailArchiveRecordMapper archiveRecordMapper;
    private final EmailAuditLogMapper auditLogMapper;
    private final EmailPollingService emailPollingService;
    private final EmailPollingServiceImpl pollingServiceImpl;

    @PostMapping("/accounts")
    public ApiResponse<EmailAccount> createAccount(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid EmailAccountCreateRequest request) {
        EmailAccount account = new EmailAccount();
        account.setUserId(userId);
        account.setTenantId(tenantId);
        account.setEmailAddress(request.getEmailAddress());
        account.setImapHost(request.getImapHost());
        account.setImapPort(request.getImapPort());
        account.setUseSsl(request.getUseSsl() != null ? request.getUseSsl() : true);
        account.setUsername(request.getUsername());
        account.setPasswordEncrypted(pollingServiceImpl.encryptPassword(request.getPassword()));
        account.setFolderFilter(request.getFolderFilter() != null ? request.getFolderFilter() : "INBOX");
        account.setAttachmentFilter(request.getAttachmentFilter());
        account.setPollIntervalMinutes(request.getPollIntervalMinutes() != null ? request.getPollIntervalMinutes() : 15);
        account.setIsEnabled(true);
        account.setStatus(1);
        account.setIsDeleted(0);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        return ApiResponse.success(account);
    }

    @GetMapping("/accounts")
    public ApiResponse<List<EmailAccount>> listAccounts(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        LambdaQueryWrapper<EmailAccount> query = new LambdaQueryWrapper<>();
        query.eq(EmailAccount::getUserId, userId);
        return ApiResponse.success(accountMapper.selectList(query));
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<EmailAccount> updateAccount(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestBody EmailAccountCreateRequest request) {
        EmailAccount account = accountMapper.selectById(id);
        if (account == null) throw new BizException("Account not found");
        if (!account.getUserId().equals(userId)) throw new BizException("Access denied");

        if (request.getImapHost() != null) account.setImapHost(request.getImapHost());
        if (request.getImapPort() != null) account.setImapPort(request.getImapPort());
        if (request.getUseSsl() != null) account.setUseSsl(request.getUseSsl());
        if (request.getUsername() != null) account.setUsername(request.getUsername());
        if (request.getPassword() != null) account.setPasswordEncrypted(pollingServiceImpl.encryptPassword(request.getPassword()));
        if (request.getFolderFilter() != null) account.setFolderFilter(request.getFolderFilter());
        if (request.getAttachmentFilter() != null) account.setAttachmentFilter(request.getAttachmentFilter());
        if (request.getPollIntervalMinutes() != null) account.setPollIntervalMinutes(request.getPollIntervalMinutes());
        account.setUpdatedAt(LocalDateTime.now());

        accountMapper.updateById(account);
        return ApiResponse.success(account);
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        EmailAccount account = accountMapper.selectById(id);
        if (account == null) throw new BizException("Account not found");
        if (!account.getUserId().equals(userId)) throw new BizException("Access denied");

        account.setIsDeleted(1);
        account.setDeletedAt(LocalDateTime.now());
        accountMapper.updateById(account);
        return ApiResponse.success();
    }

    @PostMapping("/accounts/{id}/poll")
    public ApiResponse<Void> triggerPoll(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        EmailAccount account = accountMapper.selectById(id);
        if (account == null) throw new BizException("Account not found");
        if (!account.getUserId().equals(userId)) throw new BizException("Access denied");

        emailPollingService.pollAccount(account);
        return ApiResponse.success();
    }

    @GetMapping("/accounts/{id}/records")
    public ApiResponse<PageResult<EmailArchiveRecord>> listRecords(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        EmailAccount account = accountMapper.selectById(id);
        if (account == null) throw new BizException("Account not found");
        if (!account.getUserId().equals(userId)) throw new BizException("Access denied");

        LambdaQueryWrapper<EmailArchiveRecord> query = new LambdaQueryWrapper<>();
        query.eq(EmailArchiveRecord::getAccountId, id)
                .orderByDesc(EmailArchiveRecord::getCreatedAt);
        Page<EmailArchiveRecord> page = archiveRecordMapper.selectPage(new Page<>(pageNum, pageSize), query);

        return ApiResponse.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @PostMapping("/records/{id}/retry")
    public ApiResponse<Void> retryRecord(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        EmailArchiveRecord record = archiveRecordMapper.selectById(id);
        if (record == null) throw new BizException("Record not found");

        EmailAccount account = accountMapper.selectById(record.getAccountId());
        if (account == null || !account.getUserId().equals(userId)) throw new BizException("Access denied");

        record.setStatus(0);
        record.setRetryCount(record.getRetryCount() + 1);
        record.setErrorMessage(null);
        record.setUpdatedAt(LocalDateTime.now());
        archiveRecordMapper.updateById(record);

        // Trigger actual reprocessing
        emailPollingService.retryRecord(record);

        return ApiResponse.success();
    }

    @GetMapping("/accounts/{id}/audit")
    public ApiResponse<PageResult<EmailAuditLog>> getAuditLog(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize) {
        EmailAccount account = accountMapper.selectById(id);
        if (account == null) throw new BizException("Account not found");
        if (!account.getUserId().equals(userId)) throw new BizException("Access denied");

        LambdaQueryWrapper<EmailAuditLog> query = new LambdaQueryWrapper<>();
        query.eq(EmailAuditLog::getAccountId, id)
                .orderByDesc(EmailAuditLog::getCreatedAt);
        Page<EmailAuditLog> page = auditLogMapper.selectPage(new Page<>(pageNum, pageSize), query);

        return ApiResponse.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }
}
