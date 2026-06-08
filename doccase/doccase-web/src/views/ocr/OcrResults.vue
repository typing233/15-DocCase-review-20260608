<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">OCR识别结果</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card v-loading="loading">
      <template v-if="result">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="任务ID">{{ result.taskId }}</el-descriptions-item>
          <el-descriptions-item label="使用引擎">{{ result.engineUsed }}</el-descriptions-item>
          <el-descriptions-item label="置信度">{{ result.confidence ? (result.confidence * 100).toFixed(1) + '%' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理耗时">{{ result.processingTimeMs ? result.processingTimeMs + 'ms' : '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />
        <h3>识别文本</h3>
        <pre class="ocr-text">{{ result.fullText || '无识别内容' }}</pre>

        <template v-if="result.structuredData">
          <el-divider />
          <h3>结构化数据</h3>
          <pre class="structured-data">{{ JSON.stringify(result.structuredData, null, 2) }}</pre>
        </template>
      </template>
      <el-empty v-else-if="!loading" description="暂无结果" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOcrResult } from '@/api/ocr'
import type { OcrResult } from '@/types/ocr'

const route = useRoute()
const loading = ref(true)
const result = ref<OcrResult | null>(null)

onMounted(async () => {
  try {
    const res = await getOcrResult(Number(route.params.taskId))
    result.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.ocr-text, .structured-data {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 500px;
  overflow-y: auto;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.6;
}
</style>
