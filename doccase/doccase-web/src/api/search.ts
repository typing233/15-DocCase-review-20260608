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
  return request.get<any, ApiResponse<PaginatedResult<DocumentDTO>>>('/search', { params })
}

export function hybridSearch(params: {
  keyword?: string
  semanticQuery?: string
  tagIds?: number[]
  fileType?: string
  status?: number
  startDate?: string
  endDate?: string
  tenantId?: string
  pageNum?: number
  pageSize?: number
  keywordWeight?: number
}) {
  return request.post<any, ApiResponse<PaginatedResult<DocumentDTO>>>('/search/hybrid', params)
}

export function semanticSearch(query: string, pageNum = 1, pageSize = 20) {
  return request.post<any, ApiResponse<PaginatedResult<DocumentDTO>>>('/search/semantic', null, {
    params: { query, pageNum, pageSize }
  })
}

export function getIndexStatus() {
  return request.get<any, ApiResponse<any>>('/search/admin/index/status')
}

export function triggerReindex(params: {
  sourceIndex: string
  targetIndex: string
  batchSize?: number
  switchAliasOnComplete?: boolean
}) {
  return request.post<any, ApiResponse<void>>('/search/admin/index/reindex', params)
}

