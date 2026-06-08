<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">上传文档</h2>
    </div>

    <el-card>
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持PDF、Word、Excel、图片等格式，单文件最大500MB</div>
        </template>
      </el-upload>

      <el-form v-if="selectedFile" :model="form" label-width="80px" style="margin-top: 20px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="文档标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="文档描述" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple placeholder="选择标签">
            <el-option v-for="tag in flatTags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="uploadProgress > 0">
          <el-progress :percentage="uploadProgress" :status="uploadStatus" />
          <span v-if="uploadStatus === 'success'">上传完成</span>
          <span v-else-if="uploading">正在上传分片 {{ currentChunk }}/{{ totalChunks }}...</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="startUpload">
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { initChunkUpload, uploadChunk, mergeChunks } from '@/api/document'
import { getTagTree } from '@/api/tag'
import { ElMessage } from 'element-plus'
import SparkMD5 from 'spark-md5'
import type { TagDTO } from '@/types/tag'

const router = useRouter()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref<'' | 'success' | 'exception'>('')
const currentChunk = ref(0)
const totalChunks = ref(0)
const tags = ref<TagDTO[]>([])

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

const form = reactive({
  title: '',
  description: '',
  tagIds: [] as number[],
})

const flatTags = computed(() => {
  const flat: TagDTO[] = []
  function flatten(nodes: TagDTO[]) {
    for (const node of nodes) {
      flat.push(node)
      if (node.children) flatten(node.children)
    }
  }
  flatten(tags.value)
  return flat
})

onMounted(async () => {
  try {
    const res = await getTagTree()
    tags.value = res.data
  } catch {}
})

function handleFileChange(file: any) {
  selectedFile.value = file.raw
  if (!form.title) {
    form.title = file.raw.name.replace(/\.[^/.]+$/, '')
  }
}

async function computeFileHash(file: File): Promise<string> {
  return new Promise((resolve) => {
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    reader.onload = (e) => {
      spark.append(e.target!.result as ArrayBuffer)
      resolve(spark.end())
    }
    reader.readAsArrayBuffer(file)
  })
}

async function startUpload() {
  if (!selectedFile.value) return

  uploading.value = true
  uploadStatus.value = ''
  const file = selectedFile.value

  try {
    const fileHash = await computeFileHash(file)
    const initRes = await initChunkUpload({
      fileName: file.name,
      totalSize: file.size,
      chunkSize: CHUNK_SIZE,
      fileHash,
    })

    if (initRes.data.instantUpload) {
      ElMessage.success('秒传成功！')
      uploadProgress.value = 100
      uploadStatus.value = 'success'
      return
    }

    const { uploadId, totalChunks: total } = initRes.data
    totalChunks.value = total

    for (let i = 0; i < total; i++) {
      currentChunk.value = i + 1
      const start = i * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      const chunk = file.slice(start, end)
      await uploadChunk(uploadId, i, chunk)
      uploadProgress.value = Math.round(((i + 1) / total) * 100)
    }

    await mergeChunks(uploadId)
    uploadStatus.value = 'success'
    ElMessage.success('上传成功！')
    setTimeout(() => router.push('/documents'), 1500)
  } catch (e: any) {
    uploadStatus.value = 'exception'
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>
