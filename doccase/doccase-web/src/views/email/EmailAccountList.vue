<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">邮件归档管理</h2>
      <el-button type="primary" @click="openCreateDialog">添加邮箱账户</el-button>
    </div>

    <!-- Account List -->
    <el-card style="margin-bottom: 16px">
      <el-table :data="accounts" v-loading="loading" stripe>
        <el-table-column prop="emailAddress" label="邮箱地址" min-width="200" />
        <el-table-column prop="imapHost" label="IMAP服务器" width="160" />
        <el-table-column prop="imapPort" label="端口" width="60" />
        <el-table-column label="SSL" width="60">
          <template #default="{ row }">
            <el-tag :type="row.useSsl ? 'success' : 'info'" size="small">
              {{ row.useSsl ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="accountStatusType(row.status)" size="small">
              {{ accountStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pollIntervalMinutes" label="轮询间隔(分)" width="110" />
        <el-table-column prop="lastPollAt" label="上次轮询" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePoll(row as any as EmailAccount)">立即轮询</el-button>
            <el-button link size="small" @click="viewRecords(row as any as EmailAccount)">归档记录</el-button>
            <el-button link size="small" @click="viewAudit(row as any as EmailAccount)">审计日志</el-button>
            <el-popconfirm title="确定删除此账户？" @confirm="handleDeleteAccount(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Archive Records Panel -->
    <el-card v-if="selectedAccount" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>归档记录 - {{ selectedAccount.emailAddress }}</span>
          <el-button size="small" @click="selectedAccount = null">关闭</el-button>
        </div>
      </template>
      <el-table :data="records" v-loading="recordsLoading" stripe size="small">
        <el-table-column prop="fromAddress" label="发件人" width="180" show-overflow-tooltip />
        <el-table-column prop="subject" label="主题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="attachmentFileName" label="附件名" width="180" show-overflow-tooltip />
        <el-table-column label="大小" width="80">
          <template #default="{ row }">{{ formatSize(row.attachmentSize) }}</template>
        </el-table-column>
        <el-table-column label="加密" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.isEncrypted" type="warning" size="small">是</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="recordStatusType(row.status)" size="small">
              {{ recordStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receivedAt" label="接收时间" width="170" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 3"
              link type="primary" size="small"
              @click="handleRetry(row as any as EmailArchiveRecord)"
            >重试</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="recordPageNum"
        :page-size="10"
        :total="recordTotal"
        layout="total, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadRecords"
      />
    </el-card>

    <!-- Audit Log Panel -->
    <el-card v-if="auditAccount">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>审计日志 - {{ auditAccount.emailAddress }}</span>
          <el-button size="small" @click="auditAccount = null">关闭</el-button>
        </div>
      </template>
      <el-table :data="auditLogs" v-loading="auditLoading" stripe size="small">
        <el-table-column prop="action" label="操作" width="140" />
        <el-table-column prop="messageId" label="消息ID" width="200" show-overflow-tooltip />
        <el-table-column prop="attachmentName" label="附件名" width="180" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="250" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>
      <el-pagination
        v-model:current-page="auditPageNum"
        :page-size="20"
        :total="auditTotal"
        layout="total, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadAuditLogs"
      />
    </el-card>

    <!-- Create Account Dialog -->
    <el-dialog v-model="createDialogVisible" title="添加邮箱账户" width="520px">
      <el-form :model="accountForm" label-width="110px">
        <el-form-item label="邮箱地址" required>
          <el-input v-model="accountForm.emailAddress" placeholder="user@example.com" />
        </el-form-item>
        <el-form-item label="IMAP服务器" required>
          <el-input v-model="accountForm.imapHost" placeholder="imap.example.com" />
        </el-form-item>
        <el-form-item label="端口" required>
          <el-input-number v-model="accountForm.imapPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="SSL">
          <el-switch v-model="accountForm.useSsl" />
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input v-model="accountForm.username" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="accountForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="文件夹过滤">
          <el-input v-model="accountForm.folderFilter" placeholder="INBOX（默认）" />
        </el-form-item>
        <el-form-item label="附件类型过滤">
          <el-input v-model="accountForm.attachmentFilter" placeholder="pdf,docx,xlsx（留空不过滤）" />
        </el-form-item>
        <el-form-item label="轮询间隔(分)">
          <el-input-number v-model="accountForm.pollIntervalMinutes" :min="1" :max="1440" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAccount">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  listAccounts, createAccount, deleteAccount,
  triggerPoll, listRecords, retryRecord, getAuditLog
} from '@/api/email'
import { ElMessage } from 'element-plus'
import type { EmailAccount, EmailArchiveRecord, EmailAuditLog } from '@/types/email'

const loading = ref(false)
const accounts = ref<EmailAccount[]>([])

const createDialogVisible = ref(false)
const accountForm = reactive({
  emailAddress: '',
  imapHost: '',
  imapPort: 993,
  useSsl: true,
  username: '',
  password: '',
  folderFilter: 'INBOX',
  attachmentFilter: '',
  pollIntervalMinutes: 5,
})

const selectedAccount = ref<EmailAccount | null>(null)
const records = ref<EmailArchiveRecord[]>([])
const recordsLoading = ref(false)
const recordPageNum = ref(1)
const recordTotal = ref(0)

const auditAccount = ref<EmailAccount | null>(null)
const auditLogs = ref<EmailAuditLog[]>([])
const auditLoading = ref(false)
const auditPageNum = ref(1)
const auditTotal = ref(0)

onMounted(loadAccounts)

async function loadAccounts() {
  loading.value = true
  try {
    const res = await listAccounts()
    accounts.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  accountForm.emailAddress = ''
  accountForm.imapHost = ''
  accountForm.imapPort = 993
  accountForm.useSsl = true
  accountForm.username = ''
  accountForm.password = ''
  accountForm.folderFilter = 'INBOX'
  accountForm.attachmentFilter = ''
  accountForm.pollIntervalMinutes = 5
  createDialogVisible.value = true
}

async function handleCreateAccount() {
  if (!accountForm.emailAddress || !accountForm.imapHost || !accountForm.username || !accountForm.password) {
    ElMessage.warning('请填写必填字段')
    return
  }
  await createAccount({
    ...accountForm,
    attachmentFilter: accountForm.attachmentFilter || undefined,
  })
  ElMessage.success('创建成功')
  createDialogVisible.value = false
  loadAccounts()
}

async function handleDeleteAccount(id: number) {
  await deleteAccount(id)
  ElMessage.success('删除成功')
  loadAccounts()
}

async function handlePoll(account: EmailAccount) {
  await triggerPoll(account.id)
  ElMessage.success('轮询已触发')
}

async function viewRecords(account: EmailAccount) {
  selectedAccount.value = account
  recordPageNum.value = 1
  loadRecords()
}

async function loadRecords() {
  if (!selectedAccount.value) return
  recordsLoading.value = true
  try {
    const res = await listRecords(selectedAccount.value.id, {
      pageNum: recordPageNum.value, pageSize: 10
    })
    records.value = res.data.records
    recordTotal.value = res.data.total
  } finally {
    recordsLoading.value = false
  }
}

async function handleRetry(record: EmailArchiveRecord) {
  await retryRecord(record.id)
  ElMessage.success('已提交重试')
  loadRecords()
}

async function viewAudit(account: EmailAccount) {
  auditAccount.value = account
  auditPageNum.value = 1
  loadAuditLogs()
}

async function loadAuditLogs() {
  if (!auditAccount.value) return
  auditLoading.value = true
  try {
    const res = await getAuditLog(auditAccount.value.id, {
      pageNum: auditPageNum.value, pageSize: 20
    })
    auditLogs.value = res.data.records
    auditTotal.value = res.data.total
  } finally {
    auditLoading.value = false
  }
}

function accountStatusText(status: number) {
  const map: Record<number, string> = { 0: '未连接', 1: '正常', 2: '错误' }
  return map[status] || '未知'
}

function accountStatusType(status: number): 'success' | 'info' | 'danger' {
  const map: Record<number, 'success' | 'info' | 'danger'> = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

function recordStatusText(status: number) {
  const map: Record<number, string> = { 0: '待处理', 1: '已归档', 2: '已跳过', 3: '失败' }
  return map[status] || '未知'
}

function recordStatusType(status: number): 'info' | 'success' | 'warning' | 'danger' {
  const map: Record<number, 'info' | 'success' | 'warning' | 'danger'> = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return map[status] || 'info'
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>
