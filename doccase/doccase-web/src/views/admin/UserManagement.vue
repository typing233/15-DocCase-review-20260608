<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
    </div>

    <el-card>
      <el-form :inline="true" style="margin-bottom: 16px">
        <el-form-item>
          <el-input v-model="keyword" placeholder="搜索用户..." clearable @keyup.enter="loadUsers" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 4px">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        :total="total"
        :page-size="20"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadUsers"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const loading = ref(false)
const users = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const keyword = ref('')

onMounted(loadUsers)

async function loadUsers() {
  loading.value = true
  try {
    const res: any = await request.get('/users', { params: { pageNum: pageNum.value, pageSize: 20, keyword: keyword.value } })
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
</script>
