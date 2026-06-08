<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">提交OCR识别</h2>
    </div>

    <el-card>
      <el-form :model="form" label-width="100px">
        <el-form-item label="文档ID">
          <el-input v-model.number="form.documentId" placeholder="输入要识别的文档ID" />
        </el-form-item>
        <el-form-item label="OCR引擎">
          <el-radio-group v-model="form.engine">
            <el-radio value="auto">自动选择</el-radio>
            <el-radio value="tesseract">Tesseract</el-radio>
            <el-radio value="paddleocr">PaddleOCR</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="识别语言">
          <el-select v-model="form.language" placeholder="选择语言">
            <el-option label="英文" value="eng" />
            <el-option label="中文简体" value="chi_sim" />
            <el-option label="中文繁体" value="chi_tra" />
            <el-option label="日文" value="jpn" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交识别任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="taskResult" style="margin-top: 16px">
      <template #header>任务已提交</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ taskResult.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">处理中</el-descriptions-item>
        <el-descriptions-item label="引擎">{{ taskResult.engine || '自动' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ taskResult.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <el-button type="primary" style="margin-top: 12px" @click="$router.push('/ocr/tasks')">查看任务列表</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { submitOcrTask } from '@/api/ocr'
import { ElMessage } from 'element-plus'
import type { OcrTask } from '@/types/ocr'

const submitting = ref(false)
const taskResult = ref<OcrTask | null>(null)

const form = reactive({
  documentId: undefined as number | undefined,
  engine: 'auto',
  language: 'chi_sim',
})

async function handleSubmit() {
  if (!form.documentId) {
    ElMessage.warning('请输入文档ID')
    return
  }
  submitting.value = true
  try {
    const res = await submitOcrTask({
      documentId: form.documentId,
      engine: form.engine,
      language: form.language,
    })
    taskResult.value = res.data
    ElMessage.success('OCR任务已提交')
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>
