package com.doccase.search.controller;

import com.doccase.common.domain.PageResult;
import com.doccase.common.response.ApiResponse;
import com.doccase.search.document.DocumentIndex;
import com.doccase.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ApiResponse<PageResult<DocumentIndex>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(searchService.search(keyword, tagIds, fileType, status,
                startDate, endDate, pageNum, pageSize));
    }

    @PostMapping("/index")
    public ApiResponse<Void> indexDocument(@RequestBody DocumentIndex document) {
        searchService.indexDocument(document);
        return ApiResponse.success();
    }

    @PutMapping("/index")
    public ApiResponse<Void> updateDocument(@RequestBody DocumentIndex document) {
        searchService.updateDocument(document);
        return ApiResponse.success();
    }

    @DeleteMapping("/index/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        searchService.deleteDocument(id);
        return ApiResponse.success();
    }
}
