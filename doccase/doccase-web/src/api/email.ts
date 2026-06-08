import request from './request'
import type { ApiResponse, PaginatedResult } from '@/types/api'
import type { EmailAccount, EmailArchiveRecord, EmailAuditLog } from '@/types/email'

export function listAccounts() {
  return request.get<any, ApiResponse<EmailAccount[]>>('/email/accounts')
}

export function createAccount(data: {
  emailAddress: string
  imapHost: string
  imapPort: number
  useSsl?: boolean
  username: string
  password: string
  folderFilter?: string
  attachmentFilter?: string
  pollIntervalMinutes?: number
}) {
  return request.post<any, ApiResponse<EmailAccount>>('/email/accounts', data)
}

export function updateAccount(id: number, data: Record<string, any>) {
  return request.put<any, ApiResponse<EmailAccount>>(`/email/accounts/${id}`, data)
}

export function deleteAccount(id: number) {
  return request.delete<any, ApiResponse<void>>(`/email/accounts/${id}`)
}

export function triggerPoll(id: number) {
  return request.post<any, ApiResponse<void>>(`/email/accounts/${id}/poll`)
}

export function listRecords(accountId: number, params?: { pageNum?: number; pageSize?: number }) {
  return request.get<any, ApiResponse<PaginatedResult<EmailArchiveRecord>>>(
    `/email/accounts/${accountId}/records`, { params }
  )
}

export function retryRecord(recordId: number) {
  return request.post<any, ApiResponse<void>>(`/email/records/${recordId}/retry`)
}

export function getAuditLog(accountId: number, params?: { pageNum?: number; pageSize?: number }) {
  return request.get<any, ApiResponse<PaginatedResult<EmailAuditLog>>>(
    `/email/accounts/${accountId}/audit`, { params }
  )
}
