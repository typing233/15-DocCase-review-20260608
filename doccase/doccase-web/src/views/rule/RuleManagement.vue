<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">自动标签规则</h2>
      <el-button type="primary" @click="openCreateDialog">新建规则</el-button>
    </div>

    <el-card>
      <el-table :data="rules" v-loading="loading" stripe>
        <el-table-column prop="name" label="规则名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="triggerEvent" label="触发事件" width="160" />
        <el-table-column prop="priority" label="优先级" width="80" sortable />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isEnabled"
              @change="(val: boolean) => handleToggleEnable(row as any as AutoTagRule, val)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column label="灰度" width="160">
          <template #default="{ row }">
            <el-slider
              :model-value="row.rolloutPercentage"
              :min="0" :max="100" :step="5"
              size="small"
              @change="(val: number) => handleRolloutChange(row as any as AutoTagRule, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="执行/错误" width="100">
          <template #default="{ row }">
            <span>{{ row.executionCount }}</span> /
            <span :class="{ 'text-danger': row.errorCount > 0 }">{{ row.errorCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="60" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row as any as AutoTagRule)">编辑</el-button>
            <el-button link size="small" @click="handleDryRun(row as any as AutoTagRule)">试运行</el-button>
            <el-button link size="small" @click="handleRollback(row as any as AutoTagRule)" :disabled="row.version <= 1">回滚</el-button>
            <el-popconfirm title="确定删除此规则？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadRules"
        @size-change="loadRules"
      />
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingRule ? '编辑规则' : '新建规则'" width="680px" destroy-on-close>
      <el-form :model="ruleForm" label-width="100px">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" placeholder="输入规则名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="ruleForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="触发事件">
          <el-select v-model="ruleForm.triggerEvent">
            <el-option label="文档创建" value="DOCUMENT_CREATED" />
            <el-option label="文档更新" value="DOCUMENT_UPDATED" />
            <el-option label="OCR完成" value="OCR_COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="ruleForm.priority" :min="0" :max="1000" />
        </el-form-item>
        <el-form-item label="灰度比例%">
          <el-slider v-model="ruleForm.rolloutPercentage" :min="0" :max="100" :step="5" show-input />
        </el-form-item>
        <el-form-item label="条件树(JSON)">
          <el-input
            v-model="ruleForm.conditionTree"
            type="textarea"
            :rows="6"
            placeholder='{"operator":"AND","conditions":[{"field":"fileType","fieldOperator":"EQUALS","value":"pdf"}]}'
          />
        </el-form-item>
        <el-form-item label="动作(JSON)">
          <el-input
            v-model="ruleForm.actions"
            type="textarea"
            :rows="3"
            placeholder='[{"type":"ADD_TAG","tagId":1}]'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Dry Run Results Dialog -->
    <el-dialog v-model="dryRunVisible" title="试运行结果" width="700px">
      <el-descriptions :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="规则名称">{{ dryRunResult?.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="匹配数">
          {{ dryRunResult?.matchedDocuments }} / {{ dryRunResult?.totalDocuments }}
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="dryRunResult?.matches || []" max-height="400" stripe>
        <el-table-column prop="documentId" label="文档ID" width="80" />
        <el-table-column prop="documentTitle" label="文档标题" min-width="200" />
        <el-table-column label="匹配" width="60">
          <template #default="{ row }">
            <el-tag :type="row.matched ? 'success' : 'info'" size="small">
              {{ row.matched ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="待执行动作" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="(action, idx) in row.actionsToExecute" :key="idx" size="small" style="margin-right: 4px">
              {{ action }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  listRules, createRule, updateRule, deleteRule,
  enableRule, updateRollout, rollbackRule, dryRunRule
} from '@/api/rule'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AutoTagRule, RuleDryRunResult } from '@/types/rule'

const loading = ref(false)
const rules = ref<AutoTagRule[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const dialogVisible = ref(false)
const editingRule = ref<AutoTagRule | null>(null)
const ruleForm = reactive({
  name: '',
  description: '',
  triggerEvent: 'DOCUMENT_CREATED',
  priority: 0,
  rolloutPercentage: 100,
  conditionTree: '',
  actions: '',
})

const dryRunVisible = ref(false)
const dryRunResult = ref<RuleDryRunResult | null>(null)

onMounted(loadRules)

async function loadRules() {
  loading.value = true
  try {
    const res = await listRules({ pageNum: pageNum.value, pageSize: pageSize.value })
    rules.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingRule.value = null
  ruleForm.name = ''
  ruleForm.description = ''
  ruleForm.triggerEvent = 'DOCUMENT_CREATED'
  ruleForm.priority = 0
  ruleForm.rolloutPercentage = 100
  ruleForm.conditionTree = ''
  ruleForm.actions = ''
  dialogVisible.value = true
}

function openEditDialog(rule: AutoTagRule) {
  editingRule.value = rule
  ruleForm.name = rule.name
  ruleForm.description = rule.description || ''
  ruleForm.triggerEvent = rule.triggerEvent
  ruleForm.priority = rule.priority
  ruleForm.rolloutPercentage = rule.rolloutPercentage
  ruleForm.conditionTree = rule.conditionTree
  ruleForm.actions = rule.actions
  dialogVisible.value = true
}

async function handleSave() {
  if (!ruleForm.name) {
    ElMessage.warning('请输入规则名称')
    return
  }
  if (!ruleForm.conditionTree || !ruleForm.actions) {
    ElMessage.warning('条件树和动作不能为空')
    return
  }
  try {
    JSON.parse(ruleForm.conditionTree)
    JSON.parse(ruleForm.actions)
  } catch {
    ElMessage.error('条件树或动作JSON格式不正确')
    return
  }

  if (editingRule.value) {
    await updateRule(editingRule.value.id, { ...ruleForm })
    ElMessage.success('更新成功')
  } else {
    await createRule({ ...ruleForm })
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  loadRules()
}

async function handleDelete(id: number) {
  await deleteRule(id)
  ElMessage.success('删除成功')
  loadRules()
}

async function handleToggleEnable(rule: AutoTagRule, enabled: boolean) {
  await enableRule(rule.id, enabled)
  rule.isEnabled = enabled
  ElMessage.success(enabled ? '已启用' : '已禁用')
}

async function handleRolloutChange(rule: AutoTagRule, percentage: number) {
  await updateRollout(rule.id, percentage)
  rule.rolloutPercentage = percentage
}

async function handleDryRun(rule: AutoTagRule) {
  try {
    const res = await dryRunRule(rule.id, {})
    dryRunResult.value = res.data
    dryRunVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '试运行失败')
  }
}

async function handleRollback(rule: AutoTagRule) {
  const targetVersion = rule.version - 1
  await ElMessageBox.confirm(`确定回滚到版本 ${targetVersion}？`, '回滚确认')
  await rollbackRule(rule.id, targetVersion)
  ElMessage.success('回滚成功')
  loadRules()
}
</script>

<style scoped>
.text-danger {
  color: #f56c6c;
}
</style>
