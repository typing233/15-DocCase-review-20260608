import request from './request'
import type { ApiResponse, PaginatedResult } from '@/types/api'
import type { AutoTagRule, RuleDryRunResult } from '@/types/rule'

export function listRules(params?: { triggerEvent?: string; pageNum?: number; pageSize?: number }) {
  return request.get<any, ApiResponse<PaginatedResult<AutoTagRule>>>('/rules', { params })
}

export function createRule(data: {
  name: string
  description?: string
  priority?: number
  conditionTree: string
  actions: string
  triggerEvent?: string
  rolloutPercentage?: number
}) {
  return request.post<any, ApiResponse<AutoTagRule>>('/rules', data)
}

export function updateRule(id: number, data: {
  name: string
  description?: string
  priority?: number
  conditionTree: string
  actions: string
  triggerEvent?: string
  rolloutPercentage?: number
}) {
  return request.put<any, ApiResponse<AutoTagRule>>(`/rules/${id}`, data)
}

export function deleteRule(id: number) {
  return request.delete<any, ApiResponse<void>>(`/rules/${id}`)
}

export function enableRule(id: number, enabled: boolean) {
  return request.put<any, ApiResponse<void>>(`/rules/${id}/enable`, null, { params: { enabled } })
}

export function updateRollout(id: number, percentage: number) {
  return request.put<any, ApiResponse<void>>(`/rules/${id}/rollout`, null, { params: { percentage } })
}

export function rollbackRule(id: number, version: number) {
  return request.post<any, ApiResponse<void>>(`/rules/${id}/rollback/${version}`)
}

export function dryRunRule(id: number, event: Record<string, any>) {
  return request.post<any, ApiResponse<RuleDryRunResult>>(`/rules/${id}/dry-run`, event)
}

export function reloadRules() {
  return request.post<any, ApiResponse<void>>('/rules/reload')
}
