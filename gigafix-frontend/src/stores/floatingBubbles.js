import { defineStore } from 'pinia'

// 全站右下角的浮動氣泡（客服、購物車...）共用的登記機制。
// 每顆氣泡元件在自己「目前顯示中」的狀態改變時呼叫 register/unregister，
// 這裡依登記的排序權重（order，越小越靠近畫面右下角原點）算出每顆氣泡目前該疊在第幾層，
// 這樣不管當下實際有幾顆氣泡同時顯示，都會自動疊好、不重疊，也不會留下多餘的空隙。
// 實際的像素間距留給各元件自己的 CSS 處理（因為手機版/桌面版按鈕大小不同），這裡只提供「第幾層」。
export const useFloatingBubblesStore = defineStore('floatingBubbles', {
  state: () => ({
    bubbles: [], // [{ id, order }]
  }),
  actions: {
    register(id, order) {
      this.bubbles = this.bubbles.filter((b) => b.id !== id)
      this.bubbles.push({ id, order })
    },
    unregister(id) {
      this.bubbles = this.bubbles.filter((b) => b.id !== id)
    },
  },
  getters: {
    // 用法：floatingBubbles.stackIndex(id) -> 0, 1, 2...；沒登記的話回傳 0（退回最下面那一層）
    stackIndex: (state) => (id) => {
      const sorted = [...state.bubbles].sort((a, b) => a.order - b.order)
      const index = sorted.findIndex((b) => b.id === id)
      return index === -1 ? 0 : index
    },
  },
})
