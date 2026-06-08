export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PaginatedResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  mfaRequired: boolean
}
