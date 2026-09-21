import { defineStore } from 'pinia'

// 單純的一次性訊號：forum 的登入視窗按下「註冊 / 忘記密碼」時，
// 請 ClientNavBar 打開它既有的那兩個視窗(那兩份表單含 reCAPTCHA 與 OTP 倒數，只有 navbar 有，不重做一份)。
// 登入本身不走這裡，navbar 與 forum 各自維持自己的登入表單。
export const useAuthModalStore = defineStore('authModal', {
  state: () => ({
    request: null, // 'register' | 'forgotPassword' | null
  }),
  actions: {
    requestRegister() {
      this.request = 'register'
    },
    requestForgotPassword() {
      this.request = 'forgotPassword'
    },
    clear() {
      this.request = null
    },
  },
})
