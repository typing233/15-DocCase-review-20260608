import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, refreshToken as apiRefresh, logout as apiLogout } from '@/api/auth'
import type { TokenResponse } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const username = ref(localStorage.getItem('username') || '')
  const userId = ref(localStorage.getItem('userId') || '')

  const isAuthenticated = computed(() => !!accessToken.value)

  async function login(data: { username: string; password: string; mfaCode?: string }) {
    const res = await apiLogin(data)
    const token = res.data
    if (token.mfaRequired) {
      return { mfaRequired: true }
    }
    setTokens(token)
    return { mfaRequired: false }
  }

  async function refresh() {
    const res = await apiRefresh(refreshToken.value)
    setTokens(res.data)
  }

  function setTokens(token: TokenResponse) {
    accessToken.value = token.accessToken
    refreshToken.value = token.refreshToken
    localStorage.setItem('accessToken', token.accessToken)
    localStorage.setItem('refreshToken', token.refreshToken)
  }

  function logout() {
    apiLogout().catch(() => {})
    accessToken.value = ''
    refreshToken.value = ''
    username.value = ''
    userId.value = ''
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
  }

  return { accessToken, refreshToken, username, userId, isAuthenticated, login, refresh, logout }
})
