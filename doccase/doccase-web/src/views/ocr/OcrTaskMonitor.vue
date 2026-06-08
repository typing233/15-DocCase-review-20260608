<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">OCR任务监控</h2>
      <el-button @click="loadTasks">刷新</el-button>
    </div>

    <el-card>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column prop="id" label="任务ID" width="100" />
        <el-table-column prop="documentId" label="文档ID" width="100" />
        <el-table-column prop="engine" label="引擎" width="120">
          <template #default="{ row }">{{ row.engine || '自动' }}</template>
        </el-table-column>
        <el-table-column prop="language" label="语言" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="90" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="completedAt" label="完成时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 3"
              link type="primary" size="small"
              @click="$router.push(`/ocr/results/${row.id}`)"
            >查看结果</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        :total="total"
        :page-size="20"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadTasks"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listOcrTasks } from '@/api/ocr'
import type { OcrTask } from '@/types/ocr'

const loading = ref(false)
const tasks = ref<OcrTask[]>([])
const total = ref(0)
const pageNum = ref(1)

onMounted(loadTasks)

async function loadTasks() {
  loading.value = true
  try {
    const res = await listOcrTasks({ pageNum: pageNum.value, pageSize: 20 })
    tasks.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function statusText(status: number) {
  const map: Record<number, string> = { 0: '等待中', 1: '预处理', 2: '识别中', 3: '已完成', 4: '失败' }
  return map[status] || '未知'
}

function statusType(status: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'warning', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}
</script>
