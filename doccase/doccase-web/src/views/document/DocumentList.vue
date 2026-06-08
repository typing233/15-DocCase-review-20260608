<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">文档管理</h2>
      <el-button type="primary" @click="$router.push('/documents/upload')">
        <el-icon><Upload /></el-icon>上传文档
      </el-button>
    </div>

    <!-- Search & Filters -->
    <el-card style="margin-bottom: 16px">
      <el-form :inline="true" :model="queryForm">
        <el-form-item>
          <el-input v-model="queryForm.keyword" placeholder="搜索文档..." clearable @keyup.enter="loadDocuments" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="queryForm.fileType" placeholder="文件类型" clearable>
            <el-option label="PDF" value="pdf" />
            <el-option label="Word" value="docx" />
            <el-option label="Excel" value="xlsx" />
            <el-option label="图片" value="png" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="queryForm.status" placeholder="状态" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="已归档" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDocuments">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Document Table -->
    <el-card>
      <el-table :data="documents" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/documents/${row.id}`" class="doc-link">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" width="180" />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="OCR状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ocrStatusType(row.ocrStatus)" size="small">{{ ocrStatusText(row.ocrStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/documents/${row.id}`)">详情</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadDocuments"
        @size-change="loadDocuments"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { queryDocuments, deleteDocument } from '@/api/document'
import { ElMessage } from 'element-plus'
import type { DocumentDTO } from '@/types/document'

const route = useRoute()
const loading = ref(false)
const documents = ref<DocumentDTO[]>([])
const total = ref(0)

const queryForm = reactive({
  keyword: (route.query.keyword as string) || '',
  fileType: undefined as string | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20,
})

onMounted(loadDocuments)

async function loadDocuments() {
  loading.value = true
  try {
    const res = await queryDocuments(queryForm)
    documents.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.keyword = ''
  queryForm.fileType = undefined
  queryForm.status = undefined
  queryForm.pageNum = 1
  loadDocuments()
}

async function handleDelete(id: number) {
  await deleteDocument(id)
  ElMessage.success('删除成功')
  loadDocuments()
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function ocrStatusText(status: number) {
  const map: Record<number, string> = { 0: '未识别', 1: '处理中', 2: '已完成', 3: '失败' }
  return map[status] || '未知'
}

function ocrStatusType(status: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}
</script>

<style scoped>
.doc-link {
  color: #409eff;
  text-decoration: none;
}
.doc-link:hover {
  text-decoration: underline;
}
</style>
