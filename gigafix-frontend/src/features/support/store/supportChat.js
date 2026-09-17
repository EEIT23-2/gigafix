import { defineStore } from 'pinia'
import { postChatMessage } from '../api'

// AI 客服浮動視窗的狀態。messages 純前端記憶體暫存，不寫進資料庫，重新整理就清空——
// 對應「不存對話紀錄」的決策，跟 forum 的 loginModal store 一樣走極簡的 UI 狀態模式
let nextMessageId = 1

export const useSupportChatStore = defineStore('supportChat', {
  state: () => ({
    open: false,
    messages: [], // [{ id, role: 'user' | 'assistant', content }]
    sending: false,
    // 'unauthorized' | 'rateLimited' | 'network' | null
    error: null,
  }),
  actions: {
    openPanel() {
      this.open = true
    },
    closePanel() {
      this.open = false
    },
    async sendMessage(text) {
      const trimmed = text.trim()
      if (!trimmed || this.sending) return

      this.error = null
      this.messages.push({ id: nextMessageId++, role: 'user', content: trimmed })
      this.sending = true

      // 只送 role/content 給後端，且用送出「這則使用者訊息」之前的歷史紀錄當上下文
      const history = this.messages
        .slice(0, -1)
        .map((m) => ({ role: m.role, content: m.content }))

      try {
        const { reply } = await postChatMessage(trimmed, history)
        this.messages.push({ id: nextMessageId++, role: 'assistant', content: reply })
      } catch (err) {
        if (err.response?.status === 401) {
          this.error = 'unauthorized'
        } else if (err.response?.status === 429) {
          this.error = 'rateLimited'
        } else {
          this.error = 'network'
        }
      } finally {
        this.sending = false
      }
    },
  },
})
