import { ref, reactive } from 'vue'

export interface PaginationOptions {
  pageSize?: number
  immediate?: boolean
}

export function usePagination<T>(
  fetchFn: (params: { pageNum: number; pageSize: number }) => Promise<{ records: T[]; total: number }>,
  options: PaginationOptions = {}
) {
  const pageSize = options.pageSize || 20
  const loading = ref(false)
  const data = ref<T[]>([]) as { value: T[] }
  const pagination = reactive({
    pageNum: 1,
    pageSize,
    total: 0,
  })

  async function load(pageNum?: number) {
    if (pageNum !== undefined) pagination.pageNum = pageNum
    loading.value = true
    try {
      const result = await fetchFn({ pageNum: pagination.pageNum, pageSize: pagination.pageSize })
      data.value = result.records
      pagination.total = result.total
    } finally {
      loading.value = false
    }
  }

  function reset() {
    pagination.pageNum = 1
    return load()
  }

  if (options.immediate !== false) {
    load()
  }

  return { data, loading, pagination, load, reset }
}
