import { defineStore } from 'pinia'

// forum 局部共用的登入視窗狀態：只給 forum 自己的元件用，跟 ClientNavBar/cart/repair 無關。
// afterLoginRedirect 記住使用者原本要去的路徑，登入成功後自動導過去；沒登入就關窗，留在原頁。
export const useForumLoginModalStore = defineStore('forumLoginModal', {
  state: () => ({
    show: false,
    afterLoginRedirect: null,
  }),
  actions: {
    open(redirectPath) {
      this.afterLoginRedirect = redirectPath
      this.show = true
    },
    close() {
      this.show = false
    },
  },
})
