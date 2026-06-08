import request from './request'
import type { ApiResponse, PaginatedResult } from '@/types/api'
import type { DocumentDTO, DocumentQueryRequest } from '@/types/document'

export function createDocument(formData: FormData) {
  return request.post<any, ApiResponse<DocumentDTO>>('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getDocument(id: number) {
  return request.get<any, ApiResponse<DocumentDTO>>(`/documents/${id}`)
}

export function queryDocuments(params: DocumentQueryRequest) {
  return request.post<any, ApiResponse<PaginatedResult<DocumentDTO>>>('/documents/query', params)
}

export function updateDocument(id: number, data: Record<string, any>) {
  return request.patch<any, ApiResponse<DocumentDTO>>(`/documents/${id}`, data)
}

export function deleteDocument(id: number) {
  return request.delete<any, ApiResponse<void>>(`/documents/${id}`)
}

export function restoreVersion(id: number, version: number) {
  return request.post<any, ApiResponse<void>>(`/documents/${id}/restore/${version}`)
}

// Chunked upload
export function initChunkUpload(data: { fileName: string; totalSize: number; chunkSize: number; fileHash?: string }) {
  return request.post<any, ApiResponse<{ uploadId: string; totalChunks: number; instantUpload: boolean }>>('/upload/init', data)
}

export function uploadChunk(uploadId: string, chunkIndex: number, chunk: Blob) {
  const formData = new FormData()
  formData.append('chunk', chunk)
  return request.post<any, ApiResponse<{ chunkIndex: number; uploaded: number; total: number; completed: boolean }>>(
    `/upload/chunk?uploadId=${uploadId}&chunkIndex=${chunkIndex}`, formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

export function mergeChunks(uploadId: string) {
  return request.post<any, ApiResponse<{ storagePath: string; fileSize: number }>>(`/upload/merge?uploadId=${uploadId}`)
}

export function checkInstantUpload(fileHash: string) {
  return request.get<any, ApiResponse<{ exists: boolean; documentId?: number }>>(`/upload/check?fileHash=${fileHash}`)
}

export function getUploadedChunks(uploadId: string) {
  return request.get<any, ApiResponse<number[]>>(`/upload/chunks?uploadId=${uploadId}`)
}
