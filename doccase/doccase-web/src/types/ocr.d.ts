export interface OcrTask {
  id: number
  documentId: number
  userId: number
  engine: string | null
  sourceUrl: string
  fileType: string
  language: string
  status: number
  retryCount: number
  errorMessage: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}

export interface OcrResult {
  id: number
  taskId: number
  documentId: number
  engineUsed: string
  fullText: string | null
  confidence: number | null
  pageResults: any[] | null
  structuredData: any | null
  processingTimeMs: number | null
  createdAt: string
}
