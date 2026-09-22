<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useFetchMemberInfoStore } from '@/stores/member'
import { useAuthModalStore } from '@/stores/authModal'
import { useForumLoginModalStore } from '../store/loginModal'
import LoginRegisterModal from '@/components/LoginRegisterModal.vue'

// forum 自己的登入視窗：登入表單是這裡自己的一份，註冊／忘記密碼則透過 authModal store
// 請 ClientNavBar 打開它既有的視窗（那兩份表單含 reCAPTCHA 與 OTP 倒數，只有 navbar 有，不重做一份）
const router = useRouter()
const fetchMemberInfoStore = useFetchMemberInfoStore()
const loginModalStore = useForumLoginModalStore()
const authModalStore = useAuthModalStore()

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

// 比照 ClientNavBar：輸入時就把空白濾掉，避免貼上帳號時帶進前後空白
// 用 :value 綁定而不是 v-model——濾過的值如果跟 ref 現值相同，v-model 不會把畫面上的內容同步回去
function onMailInput(event) {
  mail.value = event.target.value.replace(/\s/g, '')
  event.target.value = mail.value
  checkLoginError()
}

function onPasswordInput(event) {
  password.value = event.target.value.replace(/\s/g, '')
  event.target.value = password.value
  checkLoginError()
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

// 登入成功後的共同收尾：關窗、報喜、把使用者送去原本要做的事
function finishLogin(nickName) {
  loginModalStore.close()
  alert(`${nickName}您好~登入成功！`)
  if (loginModalStore.afterLoginRedirect) {
    router.push(loginModalStore.afterLoginRedirect)
    loginModalStore.afterLoginRedirect = null
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
    finishLogin(resp.data.nickName)
  } catch (err) {
    const message = err.response?.data?.message || '請稍後再試'
    alert(`登入失敗，原因: ${message}`)
  } finally {
    password.value = ''
    loading.value = false
  }
}

// <GoogleLogin> 登入成功後會帶著 Google 發的 id_token 回呼這裡，交給後端驗證
async function handleGoogleCredential(response) {
  loading.value = true
  try {
    const resp = await axios.post('/api/gigafix/login/google', {
      idToken: response.credential,
    })
    await fetchMemberInfoStore.fetchMember(true)
    finishLogin(resp.data.nickName)
  } catch (err) {
    const message = err.response?.data?.message || '請稍後再試'
    alert(`登入失敗，原因: ${message}`)
  } finally {
    loading.value = false
  }
}

// 註冊／忘記密碼的表單只有 ClientNavBar 有，這裡先關掉自己的視窗再請它代開
function goToRegister() {
  loginModalStore.close()
  authModalStore.requestRegister()
}

function goToForgotPassword() {
  loginModalStore.close()
  authModalStore.requestForgotPassword()
}
</script>

<template>
  <LoginRegisterModal :model-value="loginModalStore.show" @update:model-value="handleModelUpdate">
    <template #title>會員登入</template>
    <label class="form-label">Email</label>
    <input
      type="email"
      class="form-control mb-3"
      :value="mail"
      :disabled="loading"
      placeholder="請輸入Email"
      @input="onMailInput"
    />
    <label class="form-label">密碼</label>
    <input
      type="password"
      class="form-control"
      :value="password"
      :disabled="loading"
      placeholder="請輸入密碼"
      @input="onPasswordInput"
    />
    <p v-if="loginErrorMsg" class="text-danger form-error-msg">
      {{ loginErrorMsg }}
    </p>

    <template #footer>
      <button
        class="btn btn-link forgot-password-link"
        :disabled="loading"
        @click="goToForgotPassword()"
      >
        忘記密碼？
      </button>
      <button class="btn btn-secondary" :disabled="loading" @click="goToRegister()">
        註冊
      </button>
      <span v-if="loginErrorMsg" class="btn btn-primary disabled">請輸入正確資訊</span>
      <button v-else class="btn btn-primary" :disabled="loading" @click="login()">
        {{ loading ? '登入中...' : '送出' }}
      </button>

      <div class="w-100 d-flex align-items-center gap-2 my-2">
        <hr class="flex-grow-1 m-0" />
        <span class="text-muted small">或</span>
        <hr class="flex-grow-1 m-0" />
      </div>
      <!-- 使用第三方登入的按鈕 -->
      <div class="w-100 d-flex justify-content-center">
        <GoogleLogin :callback="handleGoogleCredential" />
      </div>
    </template>
  </LoginRegisterModal>
</template>

<style scoped>
/* ClientNavBar 的 <style> 是 scoped，那邊的樣式套不到這裡，
   所以 footer 連結與錯誤訊息的樣式要在這裡各留一份，視覺才會跟導覽列開出來的登入視窗一致 */

/* 「忘記密碼？」用margin-right:auto推到footer最左邊，跟其他送出/註冊鈕分開 */
.forgot-password-link {
  margin-right: auto;
  padding: 0;
  color: #2b77c5;
  font-weight: 500;
  text-decoration: none;
}

.forgot-password-link:hover {
  text-decoration: underline;
}

/* 錯誤訊息放在modal-body最底部，但故意讓它「貼著下面的分隔線」而不是貼著上面的輸入框 */
.form-error-msg {
  margin: 1.25rem 0 -1.25rem;
  font-size: 1.05rem;
}
</style>
