import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NProgress from 'nprogress'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/Dashboard.vue'),
        },
        {
          path: 'documents',
          name: 'DocumentList',
          component: () => import('@/views/document/DocumentList.vue'),
        },
        {
          path: 'documents/upload',
          name: 'DocumentUpload',
          component: () => import('@/views/document/DocumentUpload.vue'),
        },
        {
          path: 'documents/:id',
          name: 'DocumentDetail',
          component: () => import('@/views/document/DocumentDetail.vue'),
        },
        {
          path: 'tags',
          name: 'TagManagement',
          component: () => import('@/views/tag/TagManagement.vue'),
        },
        {
          path: 'ocr',
          name: 'OcrSubmit',
          component: () => import('@/views/ocr/OcrSubmit.vue'),
        },
        {
          path: 'ocr/tasks',
          name: 'OcrTasks',
          component: () => import('@/views/ocr/OcrTaskMonitor.vue'),
        },
        {
          path: 'ocr/results/:taskId',
          name: 'OcrResults',
          component: () => import('@/views/ocr/OcrResults.vue'),
        },
        {
          path: 'admin/users',
          name: 'UserManagement',
          component: () => import('@/views/admin/UserManagement.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/audit',
          name: 'AuditLog',
          component: () => import('@/views/admin/AuditLog.vue'),
          meta: { roles: ['ADMIN'] },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  NProgress.start()
  const authStore = useAuthStore()

  if (to.meta.public) {
    next()
    return
  }

  if (!authStore.isAuthenticated) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
