import request from '@/api/request'

export async function downloadFile(url: string, fileName?: string) {
  const response = await request.get(url, { responseType: 'blob' })
  const blob = new Blob([response.data])
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName || extractFileName(response) || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

function extractFileName(response: any): string | null {
  const disposition = response.headers?.['content-disposition']
  if (!disposition) return null
  const match = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^;"'\s]+)/)
  return match ? decodeURIComponent(match[1]) : null
}
