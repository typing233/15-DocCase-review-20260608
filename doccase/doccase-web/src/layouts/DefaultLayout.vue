<template>
  <el-container class="layout-container">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '240px'" class="sidebar">
      <div class="logo">
        <h1 v-show="!appStore.sidebarCollapsed">DocCase</h1>
        <span v-show="appStore.sidebarCollapsed">DC</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.sidebarCollapsed"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/documents">
          <el-icon><Document /></el-icon>
          <template #title>文档管理</template>
        </el-menu-item>
        <el-menu-item index="/documents/upload">
          <el-icon><Upload /></el-icon>
          <template #title>上传文档</template>
        </el-menu-item>
        <el-menu-item index="/tags">
          <el-icon><PriceTag /></el-icon>
          <template #title>标签管理</template>
        </el-menu-item>
        <el-sub-menu index="/ocr">
          <template #title>
            <el-icon><Camera /></el-icon>
            <span>OCR识别</span>
          </template>
          <el-menu-item index="/ocr">提交识别</el-menu-item>
          <el-menu-item index="/ocr/tasks">任务监控</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/audit">审计日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-input
            v-model="searchKeyword"
            placeholder="全局搜索文档..."
            style="width: 300px; margin-left: 16px"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32">{{ authStore.username?.[0]?.toUpperCase() }}</el-avatar>
              <span class="username">{{ authStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const searchKeyword = ref('')

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/documents', query: { keyword: searchKeyword.value } })
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.sidebar {
  background: #304156;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 20px;
    font-weight: bold;
    border-bottom: 1px solid #3d4a5a;
  }

  .sidebar-menu {
    border-right: none;
    background: #304156;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: #bfcbd9;

      &:hover {
        background-color: #263445;
      }
    }

    :deep(.el-menu-item.is-active) {
      color: #409eff;
      background-color: #263445;
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  height: 60px;

  .header-left {
    display: flex;
    align-items: center;
  }

  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
    }

    .username {
      font-size: 14px;
    }
  }
}

.main-content {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
