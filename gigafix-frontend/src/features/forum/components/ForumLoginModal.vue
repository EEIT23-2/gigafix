<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useFetchMemberInfoStore } from '@/stores/member'
import { useForumLoginModalStore } from '../store/loginModal'
import LoginRegisterModal from '@/components/LoginRegisterModal.vue'

// forum 自己的登入視窗：只做登入，不含註冊／忘記密碼——那兩個既有的入口都在 ClientNavBar 上，
// 使用者隨時可以從那裡走，這裡只是讓 forum 內「沒登入就先請登入」的動作能原地完成
const router = useRouter()
const fetchMemberInfoStore = useFetchMemberInfoStore()
const loginModalStore = useForumLoginModalStore()

const mail = ref('')
const password = ref('')
const loginErrorMsg = ref('')
const loading = ref(false)

function checkLoginError() {
  if (!mail.value) {
    loginErrorMsg.value = '請輸入Email'
  } else if (!password.value) {
    loginErrorMsg.value = '請輸入密碼'
  } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(password.value)) {
    loginErrorMsg.value = '密碼需至少8碼，並包含大小寫英文字母及數字'
  } else {
    loginErrorMsg.value = ''
  }
}

function resetForm() {
  mail.value = ''
  password.value = ''
  loginErrorMsg.value = '請輸入Email'
}

// 開窗時重置表單，避免看到上一次輸入到一半的殘留內容
function handleModelUpdate(value) {
  if (value) {
    resetForm()
  } else {
    loginModalStore.close()
  }
}

async function login() {
  loading.value = true
  try {
    const resp = await axios.post('/api/gigafix/login', {
      email: mail.value,
      password: password.value,
    })
    await fetchMemberInfoStore.fetchMember(true)
    loginModalStore.close()
    alert(`${resp.data.nickName}您好~登入成功！`)
    // 登入成功後直接導去原本要做的事，不用使用者自己再點一次
    if (loginModalStore.afterLoginRedirect) {
      router.push(loginModalStore.afterLoginRedirect)
      loginModalStore.afterLoginRedirect = null
    }
  } catch (err) {
    const message = err.response?.data?.message || '請稍後再試'
    alert(`登入失敗，原因: ${message}`)
  } finally {
    password.value = ''
    loading.value = false
  }
}
</script>

<template>
  <LoginRegisterModal :model-value="loginModalStore.show" @update:model-value="handleModelUpdate">
    <template #title>會員登入</template>
    <label class="form-label">Email</label>
    <input
      type="email"
      class="form-control mb-3"
      v-model="mail"
      :disabled="loading"
      placeholder="請輸入Email"
      @input="checkLoginError()"
    />
    <label class="form-label">密碼</label>
    <input
      type="password"
      class="form-control"
      v-model="password"
      :disabled="loading"
      placeholder="請輸入密碼"
      @input="checkLoginError()"
    />
    <p v-if="loginErrorMsg" class="text-danger form-error-msg">
      {{ loginErrorMsg }}
    </p>

    <template #footer>
      <span v-if="loginErrorMsg" class="btn btn-primary disabled">請輸入正確資訊</span>
      <button v-else class="btn btn-primary" :disabled="loading" @click="login()">
        {{ loading ? '登入中...' : '送出' }}
      </button>
    </template>
  </LoginRegisterModal>
</template>
