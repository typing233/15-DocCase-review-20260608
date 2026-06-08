export interface TagDTO {
  id: number
  name: string
  parentId: number | null
  path: string
  level: number
  sortOrder: number
  color: string | null
  icon: string | null
  documentCount: number
  createdAt: string
  children?: TagDTO[]
}
