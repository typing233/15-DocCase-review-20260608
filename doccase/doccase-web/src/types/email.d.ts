export interface EmailAccount {
  id: number
  userId: number
  tenantId: string
  emailAddress: string
  imapHost: string
  imapPort: number
  useSsl: boolean
  username: string
  folderFilter: string
  attachmentFilter: string | null
  pollIntervalMinutes: number
  lastPollAt: string | null
  isEnabled: boolean
  status: number
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface EmailArchiveRecord {
  id: number
  accountId: number
  tenantId: string
  messageId: string
  messageUid: number
  fromAddress: string | null
  subject: string | null
  receivedAt: string | null
  attachmentFileName: string
  attachmentHash: string
  attachmentSize: number
  attachmentMimeType: string | null
  isEncrypted: boolean
  decryptionAttempted: boolean
  documentId: number | null
  status: number
  skipReason: string | null
  errorMessage: string | null
  retryCount: number
  createdAt: string
  updatedAt: string
}

export interface EmailAuditLog {
  id: number
  accountId: number
  action: string
  messageId: string | null
  attachmentName: string | null
  detail: string | null
  createdAt: string
}
