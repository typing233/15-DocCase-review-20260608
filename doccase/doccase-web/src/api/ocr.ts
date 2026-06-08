import request from './request'
import type { ApiResponse, PaginatedResult } from '@/types/api'
import type { OcrTask, OcrResult } from '@/types/ocr'

export function submitOcrTask(data: { documentId: number; engine?: string; language?: string }) {
  return request.post<any, ApiResponse<OcrTask>>('/ocr/tasks', data)
}

export function getOcrTask(id: number) {
  return request.get<any, ApiResponse<OcrTask>>(`/ocr/tasks/${id}`)
}

export function listOcrTasks(params: { pageNum?: number; pageSize?: number; status?: number }) {
  return request.get<any, ApiResponse<PaginatedResult<OcrTask>>>('/ocr/tasks', { params })
}

export function getOcrResult(taskId: number) {
  return request.get<any, ApiResponse<OcrResult>>(`/ocr/tasks/${taskId}/result`)
}
