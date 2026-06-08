import request from './request'
import type { ApiResponse, TokenResponse } from '@/types/api'

export function login(data: { username: string; password: string; mfaCode?: string }) {
  return request.post<any, ApiResponse<TokenResponse>>('/auth/login', data)
}

export function register(data: { username: string; email: string; password: string }) {
  return request.post<any, ApiResponse<TokenResponse>>('/auth/register', data)
}

export function refreshToken(refreshToken: string) {
  return request.post<any, ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken })
}

export function logout() {
  return request.post<any, ApiResponse<void>>('/auth/logout')
}

export function getMfaStatus() {
  return request.get<any, ApiResponse<{ enabled: boolean }>>('/auth/mfa/status')
}

export function setupMfa() {
  return request.post<any, ApiResponse<{ secret: string; qrUri: string }>>('/auth/mfa/setup')
}

export function enableMfa(code: string) {
  return request.post<any, ApiResponse<void>>('/auth/mfa/enable', { code })
}
