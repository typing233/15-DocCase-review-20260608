import request from './request'
import type { ApiResponse } from '@/types/api'
import type { TagDTO } from '@/types/tag'

export function getTagTree() {
  return request.get<any, ApiResponse<TagDTO[]>>('/tags/tree')
}

export function createTag(data: { name: string; parentId?: number; color?: string; icon?: string }) {
  return request.post<any, ApiResponse<TagDTO>>('/tags', data)
}

export function updateTag(id: number, data: Partial<TagDTO>) {
  return request.put<any, ApiResponse<TagDTO>>(`/tags/${id}`, data)
}

export function deleteTag(id: number) {
  return request.delete<any, ApiResponse<void>>(`/tags/${id}`)
}

export function mergeTags(sourceTagId: number, targetTagId: number) {
  return request.post<any, ApiResponse<void>>('/tags/merge', { sourceTagId, targetTagId })
}

export function importTags(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ApiResponse<void>>('/tags/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function exportTags() {
  return request.get<any, Blob>('/tags/export', { responseType: 'blob' })
}

export function batchTag(documentIds: number[], tagIds: number[], action: 'ADD' | 'REMOVE') {
  return request.post<any, ApiResponse<void>>('/tags/batch/tag', { documentIds, tagIds, action })
}

export function batchMove(tagIds: number[], targetParentId: number | null) {
  return request.post<any, ApiResponse<void>>('/tags/batch/move', { tagIds, targetParentId })
}

export function batchDelete(tagIds: number[]) {
  return request.post<any, ApiResponse<void>>('/tags/batch/delete', { tagIds })
}

export function getInheritedDocuments(tagId: number) {
  return request.get<any, ApiResponse<number[]>>(`/tags/inherited/${tagId}/documents`)
}

