<template>
  <div class="login-container">
    <div class="login-card">
      <h2>DocCase 登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item v-if="showMfa" prop="mfaCode">
          <el-input v-model="form.mfaCode" placeholder="MFA验证码" prefix-icon="Key" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" native-type="submit">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <router-link to="/register">没有账号？去注册</router-link>
      </div>
      <el-divider>第三方登录</el-divider>
      <div class="oauth-buttons">
        <el-button @click="oauthLogin('github')">GitHub</el-button>
        <el-button @click="oauthLogin('google')">Google</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const showMfa = ref(false)

const form = reactive({
  username: '',
  password: '',
  mfaCode: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  loading.value = true
  try {
    const result = await authStore.login(form)
    if (result.mfaRequired) {
      showMfa.value = true
      ElMessage.info('请输入MFA验证码')
    } else {
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/dashboard'
      router.push(redirect)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function oauthLogin(provider: string) {
  window.location.href = `/api/auth/oauth/${provider}`
}
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);

  h2 {
    text-align: center;
    margin-bottom: 30px;
    color: #303133;
  }
}

.login-footer {
  text-align: center;
  margin-top: 16px;
}

.oauth-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
