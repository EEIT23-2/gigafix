import { defineStore } from 'pinia'

// 全站「登入 / 註冊 / 忘記密碼」彈窗的唯一狀態來源。
// 彈窗本體只有 ClientNavBar 有一份(它在 ClientLayout 底下的每一頁都掛著)，
// 其他模組(purchase、repair、forum...)要開窗就呼叫這裡的 action，不要各自再做一份表單。
export const useAuthModalStore = defineStore('authModal', {
  state: () => ({
    // 同一時間只會有一個視窗開著，用一個欄位表示就夠，共用的背景遮罩也好判斷
    active: null, // 'login' | 'register' | 'forgotPassword' | null
    // 登入成功後要導去的路徑；null 表示留在原頁
    redirect: null,
    // 關窗(含取消、點遮罩)時要不要留著 redirect。
    // 預設 false：點了購物車卻沒登入，之後在別頁登入時不該被莫名導去購物車。
    // 預約維修沿用它原本的行為，開窗時會傳 true，取消後再登入仍會被帶去預約表單。
    keepRedirectOnClose: false,
  }),
  actions: {
    // 模組的進入點用這個開窗，順便記住登入成功後要去哪
    open(target, redirect = null, keepRedirectOnClose = false) {
      this.redirect = redirect
      this.keepRedirectOnClose = keepRedirectOnClose
      this.active = target
    },
    // 三個視窗互切用這個：算同一個流程，redirect 要留著
    switchTo(target) {
      this.active = target
    },
    close() {
      this.active = null
      if (!this.keepRedirectOnClose) {
        this.redirect = null
      }
    },
    // 登入成功後取用並清掉，避免同一個路徑被導兩次
    consumeRedirect() {
      const redirect = this.redirect
      this.redirect = null
      this.keepRedirectOnClose = false
      return redirect
    },
  },
})
