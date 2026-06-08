<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">高级搜索</h2>
    </div>

    <el-card style="margin-bottom: 16px">
      <el-form :model="searchForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="搜索模式">
              <el-radio-group v-model="searchForm.mode" @change="handleModeChange">
                <el-radio-button value="keyword">关键词</el-radio-button>
                <el-radio-button value="semantic">语义</el-radio-button>
                <el-radio-button value="hybrid">混合</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="searchForm.mode === 'semantic' ? '语义查询' : '关键词'">
              <el-input
                v-model="searchForm.keyword"
                :placeholder="searchForm.mode === 'semantic' ? '输入自然语言描述...' : '输入关键词...'"
                clearable
                @keyup.enter="doSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="文件类型">
              <el-select v-model="searchForm.fileType" placeholder="全部" clearable>
                <el-option label="PDF" value="pdf" />
                <el-option label="Word" value="docx" />
                <el-option label="Excel" value="xlsx" />
                <el-option label="图片" value="png" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="searchForm.mode === 'hybrid'">
            <el-form-item label="关键词权重">
              <el-slider v-model="searchForm.keywordWeight" :min="0" :max="1" :step="0.1" show-input />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item>
              <el-button type="primary" @click="doSearch">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="results" v-loading="loading" stripe>
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
        <el-table-column prop="createdAt" label="创建时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="searchForm.pageNum"
        v-model:page-size="searchForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="doSearch"
        @size-change="doSearch"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { search, hybridSearch, semanticSearch } from '@/api/search'
import { ElMessage } from 'element-plus'
import type { DocumentDTO } from '@/types/document'

const loading = ref(false)
const results = ref<DocumentDTO[]>([])
const total = ref(0)

const searchForm = reactive({
  mode: 'keyword' as 'keyword' | 'semantic' | 'hybrid',
  keyword: '',
  fileType: undefined as string | undefined,
  dateRange: null as [string, string] | null,
  keywordWeight: 0.7,
  pageNum: 1,
  pageSize: 20,
})

function handleModeChange() {
  results.value = []
  total.value = 0
}

async function doSearch() {
  if (!searchForm.keyword.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  loading.value = true
  try {
    const startDate = searchForm.dateRange?.[0]
    const endDate = searchForm.dateRange?.[1]

    if (searchForm.mode === 'keyword') {
      const res = await search({
        keyword: searchForm.keyword,
        fileType: searchForm.fileType,
        startDate,
        endDate,
        pageNum: searchForm.pageNum,
        pageSize: searchForm.pageSize,
      })
      results.value = res.data.records
      total.value = res.data.total
    } else if (searchForm.mode === 'semantic') {
      const res = await semanticSearch(searchForm.keyword, searchForm.pageNum, searchForm.pageSize)
      results.value = res.data.records
      total.value = res.data.total
    } else {
      const res = await hybridSearch({
        keyword: searchForm.keyword,
        semanticQuery: searchForm.keyword,
        fileType: searchForm.fileType,
        startDate,
        endDate,
        keywordWeight: searchForm.keywordWeight,
        pageNum: searchForm.pageNum,
        pageSize: searchForm.pageSize,
      })
      results.value = res.data.records
      total.value = res.data.total
    }
  } catch (e: any) {
    ElMessage.error(e.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.fileType = undefined
  searchForm.dateRange = null
  searchForm.keywordWeight = 0.7
  searchForm.pageNum = 1
  results.value = []
  total.value = 0
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
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
