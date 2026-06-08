export interface UserVO {
  id: number
  username: string
  email: string
  phone: string | null
  avatarUrl: string | null
  status: number
  roles: string[]
  createdAt: string
  updatedAt: string
}
