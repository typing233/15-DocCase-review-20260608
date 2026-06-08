<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">审计日志</h2>
    </div>

    <el-card>
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="action" label="操作" width="150" />
        <el-table-column prop="resourceType" label="资源类型" width="120" />
        <el-table-column prop="resourceId" label="资源ID" width="100" />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="detail" label="详情" min-width="200">
          <template #default="{ row }">
            <span>{{ row.detail ? JSON.stringify(row.detail).substring(0, 50) : '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        :total="total"
        :page-size="20"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadLogs"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const loading = ref(false)
const logs = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)

onMounted(loadLogs)

async function loadLogs() {
  loading.value = true
  try {
    const res: any = await request.get('/auth/audit-logs', { params: { pageNum: pageNum.value, pageSize: 20 } })
    logs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}
</script>
