<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">仪表盘</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>文档总数</template>
          <div class="stat-number">{{ stats.totalDocuments }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>标签总数</template>
          <div class="stat-number">{{ stats.totalTags }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>OCR任务</template>
          <div class="stat-number">{{ stats.ocrTasks }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>存储用量</template>
          <div class="stat-number">{{ stats.storageUsed }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>最近文档</template>
          <el-table :data="recentDocuments" stripe>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="fileType" label="类型" width="80" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>快速操作</template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/documents/upload')">上传文档</el-button>
            <el-button @click="$router.push('/ocr')">OCR识别</el-button>
            <el-button @click="$router.push('/tags')">管理标签</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryDocuments } from '@/api/document'

const stats = ref({
  totalDocuments: 0,
  totalTags: 0,
  ocrTasks: 0,
  storageUsed: '0 MB',
})

const recentDocuments = ref<any[]>([])

onMounted(async () => {
  try {
    const res = await queryDocuments({ pageNum: 1, pageSize: 5 })
    recentDocuments.value = res.data.records
    stats.value.totalDocuments = res.data.total
  } catch {
    // Dashboard loads gracefully even if API unavailable
  }
})
</script>

<style scoped>
.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
