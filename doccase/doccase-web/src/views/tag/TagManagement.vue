<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">标签管理</h2>
      <div>
        <el-button type="primary" @click="showCreateDialog = true">新建标签</el-button>
        <el-button @click="handleExport">导出</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>标签树</template>
          <el-tree
            :data="tagStore.tagTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span :style="{ color: data.color || '#333' }">{{ node.label }}</span>
                <span class="node-actions">
                  <el-tag size="small" type="info">{{ data.documentCount }}</el-tag>
                  <el-button link size="small" @click.stop="editTag(data)">编辑</el-button>
                  <el-popconfirm title="确定删除此标签？" @confirm="handleDelete(data.id)">
                    <template #reference>
                      <el-button link size="small" type="danger" @click.stop>删除</el-button>
                    </template>
                  </el-popconfirm>
                </span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>标签合并</template>
          <el-form :model="mergeForm" label-width="80px">
            <el-form-item label="源标签">
              <el-select v-model="mergeForm.sourceTagId" placeholder="选择要合并的标签" filterable>
                <el-option v-for="tag in flatTags" :key="tag.id" :label="tag.name" :value="tag.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="目标标签">
              <el-select v-model="mergeForm.targetTagId" placeholder="合并到..." filterable>
                <el-option v-for="tag in flatTags" :key="tag.id" :label="tag.name" :value="tag.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" @click="handleMerge">合并标签</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建标签" width="400px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item label="父标签">
          <el-select v-model="createForm.parentId" placeholder="无（顶级标签）" clearable filterable>
            <el-option v-for="tag in flatTags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="createForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useTagStore } from '@/stores/tag'
import { exportTags } from '@/api/tag'
import { ElMessage } from 'element-plus'
import type { TagDTO } from '@/types/tag'

const tagStore = useTagStore()
const showCreateDialog = ref(false)

const createForm = reactive({ name: '', parentId: undefined as number | undefined, color: '' })
const mergeForm = reactive({ sourceTagId: undefined as number | undefined, targetTagId: undefined as number | undefined })

const flatTags = computed(() => {
  const flat: TagDTO[] = []
  function flatten(nodes: TagDTO[]) {
    for (const node of nodes) {
      flat.push(node)
      if (node.children) flatten(node.children)
    }
  }
  flatten(tagStore.tagTree)
  return flat
})

onMounted(() => tagStore.fetchTagTree())

function handleNodeClick(data: TagDTO) {
  // Could show tag details
}

async function handleCreate() {
  if (!createForm.name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  await tagStore.addTag(createForm)
  ElMessage.success('创建成功')
  showCreateDialog.value = false
  createForm.name = ''
  createForm.parentId = undefined
  createForm.color = ''
}

async function handleDelete(id: number) {
  await tagStore.removeTag(id)
  ElMessage.success('删除成功')
}

async function handleMerge() {
  if (!mergeForm.sourceTagId || !mergeForm.targetTagId) {
    ElMessage.warning('请选择源标签和目标标签')
    return
  }
  if (mergeForm.sourceTagId === mergeForm.targetTagId) {
    ElMessage.warning('不能合并相同的标签')
    return
  }
  await tagStore.mergeTag(mergeForm.sourceTagId, mergeForm.targetTagId)
  ElMessage.success('合并成功')
  mergeForm.sourceTagId = undefined
  mergeForm.targetTagId = undefined
}

function editTag(_data: TagDTO) {
  // Open edit dialog
}

async function handleExport() {
  const data = await exportTags()
  const url = URL.createObjectURL(data as any)
  const a = document.createElement('a')
  a.href = url
  a.download = 'tags-export.json'
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}

.node-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}
</style>
