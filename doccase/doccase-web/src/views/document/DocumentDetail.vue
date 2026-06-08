<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">文档详情</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card v-loading="loading">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="标题">{{ doc.title }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ doc.fileName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ doc.fileType }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ formatSize(doc.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="版本">v{{ doc.currentVersion }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag>{{ doc.status === 1 ? '正常' : '已归档' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="存储类型">{{ doc.storageType }}</el-descriptions-item>
        <el-descriptions-item label="Hash">{{ doc.fileHash }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ doc.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ doc.updatedAt }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ doc.description || '无' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h3>OCR识别结果</h3>
      <div v-if="doc.ocrStatus === 2" class="ocr-text">
        <pre>{{ doc.ocrText }}</pre>
      </div>
      <el-tag v-else-if="doc.ocrStatus === 1" type="warning">OCR处理中...</el-tag>
      <el-tag v-else-if="doc.ocrStatus === 3" type="danger">OCR识别失败</el-tag>
      <p v-else>未进行OCR识别</p>

      <el-divider />

      <h3>动态元数据</h3>
      <pre v-if="doc.metadata">{{ JSON.stringify(doc.metadata, null, 2) }}</pre>
      <p v-else>无元数据</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDocument } from '@/api/document'
import type { DocumentDTO } from '@/types/document'

const route = useRoute()
const loading = ref(true)
const doc = ref<DocumentDTO>({} as DocumentDTO)

onMounted(async () => {
  try {
    const res = await getDocument(Number(route.params.id))
    doc.value = res.data
  } finally {
    loading.value = false
  }
})

function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.ocr-text pre {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
}
</style>
