import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTagTree, createTag, deleteTag, mergeTags } from '@/api/tag'
import type { TagDTO } from '@/types/tag'

export const useTagStore = defineStore('tag', () => {
  const tagTree = ref<TagDTO[]>([])
  const loading = ref(false)

  async function fetchTagTree() {
    loading.value = true
    try {
      const res = await getTagTree()
      tagTree.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function addTag(data: { name: string; parentId?: number; color?: string }) {
    const res = await createTag(data)
    await fetchTagTree()
    return res.data
  }

  async function removeTag(id: number) {
    await deleteTag(id)
    await fetchTagTree()
  }

  async function mergeTag(sourceId: number, targetId: number) {
    await mergeTags(sourceId, targetId)
    await fetchTagTree()
  }

  return { tagTree, loading, fetchTagTree, addTag, removeTag, mergeTag }
})
