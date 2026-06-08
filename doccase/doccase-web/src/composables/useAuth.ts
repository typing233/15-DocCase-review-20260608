import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()

  const isLoggedIn = computed(() => !!userStore.token)
  const currentUser = computed(() => userStore.userInfo)
  const roles = computed(() => userStore.userInfo?.roles || [])

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function isAdmin(): boolean {
    return hasRole('ADMIN')
  }

  async function logout() {
    userStore.logout()
    await router.push('/login')
  }

  return { isLoggedIn, currentUser, roles, hasRole, isAdmin, logout }
}
