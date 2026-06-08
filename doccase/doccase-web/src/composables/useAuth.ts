import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function useAuth() {
  const router = useRouter()
  const authStore = useAuthStore()

  const isLoggedIn = computed(() => authStore.isAuthenticated)
  const username = computed(() => authStore.username)

  async function logout() {
    authStore.logout()
    await router.push('/login')
  }

  return { isLoggedIn, username, logout }
}
