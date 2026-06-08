import { ref, computed } from 'vue'
import SparkMD5 from 'spark-md5'
import request from '@/api/request'

export interface UploadOptions {
  chunkSize?: number
  onProgress?: (percent: number) => void
  onSuccess?: (doc: any) => void
  onError?: (err: Error) => void
}

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

export function useUpload(options: UploadOptions = {}) {
  const chunkSize = options.chunkSize || DEFAULT_CHUNK_SIZE
  const uploading = ref(false)
  const progress = ref(0)
  const currentFile = ref<File | null>(null)
  const aborted = ref(false)

  const canAbort = computed(() => uploading.value)

  async function computeHash(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const spark = new SparkMD5.ArrayBuffer()
      const reader = new FileReader()
      const chunks = Math.ceil(file.size / chunkSize)
      let current = 0

      function loadNext() {
        const start = current * chunkSize
        const end = Math.min(start + chunkSize, file.size)
        reader.readAsArrayBuffer(file.slice(start, end))
      }

      reader.onload = (e) => {
        spark.append(e.target!.result as ArrayBuffer)
        current++
        if (current < chunks) {
          loadNext()
        } else {
          resolve(spark.end())
        }
      }
      reader.onerror = () => reject(new Error('Failed to read file'))
      loadNext()
    })
  }

  async function checkInstantUpload(hash: string, fileName: string): Promise<any | null> {
    const res: any = await request.post('/documents/upload/instant', { hash, fileName })
    if (res.data?.existed) {
      return res.data.document
    }
    return null
  }

  async function upload(file: File) {
    currentFile.value = file
    uploading.value = true
    progress.value = 0
    aborted.value = false

    try {
      const hash = await computeHash(file)
      progress.value = 5

      const instantResult = await checkInstantUpload(hash, file.name)
      if (instantResult) {
        progress.value = 100
        options.onProgress?.(100)
        options.onSuccess?.(instantResult)
        return instantResult
      }

      const totalChunks = Math.ceil(file.size / chunkSize)

      const initRes: any = await request.post('/documents/upload/init', {
        fileName: file.name,
        fileSize: file.size,
        hash,
        totalChunks,
      })
      const uploadId = initRes.data.uploadId

      for (let i = 0; i < totalChunks; i++) {
        if (aborted.value) throw new Error('Upload aborted')

        const start = i * chunkSize
        const end = Math.min(start + chunkSize, file.size)
        const chunk = file.slice(start, end)

        const formData = new FormData()
        formData.append('file', chunk)
        formData.append('uploadId', uploadId)
        formData.append('chunkIndex', String(i))

        await request.post('/documents/upload/chunk', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })

        const percent = Math.round(5 + ((i + 1) / totalChunks) * 90)
        progress.value = percent
        options.onProgress?.(percent)
      }

      const mergeRes: any = await request.post('/documents/upload/merge', { uploadId })
      progress.value = 100
      options.onProgress?.(100)
      options.onSuccess?.(mergeRes.data)
      return mergeRes.data
    } catch (err: any) {
      options.onError?.(err)
      throw err
    } finally {
      uploading.value = false
    }
  }

  function abort() {
    aborted.value = true
  }

  return { upload, abort, uploading, progress, canAbort, currentFile }
}
