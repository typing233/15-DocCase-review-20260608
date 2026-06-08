export interface DocumentDTO {
  id: number
  userId: number
  title: string
  description: string
  fileName: string
  fileSize: number
  fileType: string
  mimeType: string
  fileHash: string
  storageType: string
  storagePath: string
  thumbnailPath: string | null
  currentVersion: number
  status: number
  tagIds: number[] | null
  metadata: Record<string, any> | null
  ocrStatus: number
  ocrText: string | null
  createdAt: string
  updatedAt: string
}

export interface DocumentQueryRequest {
  keyword?: string
  tagIds?: number[]
  fileType?: string
  status?: number
  userId?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
  orderBy?: string
  asc?: boolean
}

export interface DocumentVersion {
  id: number
  documentId: number
  versionNumber: number
  fileHash: string
  fileSize: number
  changeNote: string
  createdBy: number
  createdAt: string
}
