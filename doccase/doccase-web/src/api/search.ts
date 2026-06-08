import request from './request'
import type { ApiResponse, PaginatedResult } from '@/types/api'
import type { DocumentDTO } from '@/types/document'

export function search(params: {
  keyword: string
  tagIds?: number[]
  fileType?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.post<any, ApiResponse<PaginatedResult<DocumentDTO>>>('/search', params)
}
