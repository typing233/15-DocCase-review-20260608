package com.doccase.search.controller;

import com.doccase.common.response.ApiResponse;
import com.doccase.search.dto.IndexStatus;
import com.doccase.search.dto.ReindexRequest;
import com.doccase.search.service.IndexManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search/admin/index")
@RequiredArgsConstructor
public class IndexAdminController {

    private final IndexManagementService indexManagementService;

    @PostMapping("/reindex")
    public ApiResponse<Void> reindex(@RequestBody ReindexRequest request) {
        indexManagementService.reindex(request);
        return ApiResponse.success("重建索引任务已启动", null);
    }

    @PostMapping("/switch")
    public ApiResponse<Void> switchAlias(@RequestParam String aliasName, @RequestParam String targetIndex) {
        indexManagementService.switchAlias(aliasName, targetIndex);
        return ApiResponse.success("别名切换成功", null);
    }

    @GetMapping("/status")
    public ApiResponse<IndexStatus> getStatus() {
        return ApiResponse.success(indexManagementService.getStatus());
    }

    @PostMapping("/cancel-reindex")
    public ApiResponse<Void> cancelReindex() {
        indexManagementService.cancelReindex();
        return ApiResponse.success("重建索引已取消", null);
    }

    @PostMapping("/create")
    public ApiResponse<Void> createIndex(@RequestParam String indexName, @RequestBody String mappingJson) {
        indexManagementService.createIndexWithMapping(indexName, mappingJson);
        return ApiResponse.success("索引创建成功", null);
    }
}
