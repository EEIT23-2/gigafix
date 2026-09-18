<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { storeToRefs } from 'pinia'
import { useFetchMemberInfoStore } from '@/stores/member'
import { useFloatingBubblesStore } from '@/stores/floatingBubbles'
import { useSupportChatStore } from '../store/supportChat'

const { memberInfo } = storeToRefs(useFetchMemberInfoStore())
const store = useSupportChatStore()

const draft = ref('')
const messageListRef = ref(null)
const composerInputRef = ref(null)

// 跟 ProductCartDrawer 共用同一份浮動氣泡登記機制，避免兩顆按鈕疊在同一個位置——
// order 比購物車（10）大，維持「客服疊在購物車上面」的既有安排
const BUBBLE_ID = 'supportChat'
const BUBBLE_ORDER = 20
const floatingBubbles = useFloatingBubblesStore()
const isVisible = computed(() => !!memberInfo.value && !store.open)
const stackIndex = computed(() => floatingBubbles.stackIndex(BUBBLE_ID))

watch(
  isVisible,
  (visible) => {
    if (visible) {
      floatingBubbles.register(BUBBLE_ID, BUBBLE_ORDER)
    } else {
      floatingBubbles.unregister(BUBBLE_ID)
    }
  },
  { immediate: true },
)

const ERROR_MESSAGES = {
  unauthorized: '你的登入狀態已逾時，請重新登入後再繼續對話。',
  rateLimited: '訊息發送太頻繁，請稍等一下再試。',
  network: '客服服務暫時連不上，請稍後再試一次。',
}

// AI 回覆裡用中括號標記的站內頁面名稱 -> 實際路由。白名單機制：只有完全比對到這裡的文字才會變成可點擊連結，
// 其他中括號文字（包含模型可能亂寫或被誘導寫出的內容）一律當純文字顯示，不會被拿去當連結目標——
// 連結目的地永遠來自這個寫死的白名單，不會相信模型輸出的任何網址/路徑本身，避免被提示注入導去不該去的地方
const INTERNAL_LINKS = {
  '維修手機>預約維修': { name: 'repair-appointment' },
  '認證二手手機販售': { name: 'mall-list' },
  '二手機收購/回收': { name: 'recycle-form' },
}

// 真人客服信箱，跟 SupportSystemPrompt 裡寫給模型的地址是同一組——
// 只認這個寫死的字串，不是「任何看起來像信箱的文字」都轉連結，理由跟站內連結白名單一樣：
// 不相信模型輸出裡可能出現的任意信箱地址，只轉我們自己知道、確實是真人客服的這一個
const SUPPORT_EMAIL = 'gigafix.demo@gmail.com'

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

// 把一則訊息文字拆成「純文字」「白名單內的站內連結」「客服信箱」交錯的片段，給模板用 v-for 渲染
function parseMessageSegments(content) {
  const segments = []
  const pattern = new RegExp(`\\[([^[\\]]+)\\]|(${escapeRegExp(SUPPORT_EMAIL)})`, 'g')
  let lastIndex = 0
  let match

  while ((match = pattern.exec(content)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ type: 'text', text: content.slice(lastIndex, match.index) })
    }
    if (match[1] !== undefined) {
      const label = match[1]
      const to = INTERNAL_LINKS[label]
      segments.push(to ? { type: 'link', text: label, to } : { type: 'text', text: match[0] })
    } else {
      segments.push({ type: 'email', text: SUPPORT_EMAIL })
    }
    lastIndex = pattern.lastIndex
  }

  if (lastIndex < content.length) {
    segments.push({ type: 'text', text: content.slice(lastIndex) })
  }
  return segments
}

// 面板打開時鎖住背景捲動，關閉時解鎖，跟 ProductCartDrawer 同一種寫法
watch(
  () => store.open,
  (open) => {
    document.body.style.overflow = open ? 'hidden' : ''
  },
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  floatingBubbles.unregister(BUBBLE_ID)
})

// 有新訊息時自動捲到底部，讓使用者不用自己往下滑
watch(
  () => store.messages.length,
  async () => {
    await nextTickScroll()
  },
)

async function nextTickScroll() {
  await new Promise((resolve) => requestAnimationFrame(resolve))
  const el = messageListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function handleSend() {
  const text = draft.value
  draft.value = ''
  await store.sendMessage(text)
  // 送出期間 textarea 會被 disabled 蓋掉，瀏覽器會強制讓它失焦；
  // 送出結束、disabled 解除後要手動把游標接回去，不然使用者每次都要自己重新點輸入框
  await nextTick()
  composerInputRef.value?.focus()
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="trigger-fade">
      <button
        v-if="memberInfo && !store.open"
        class="chat-floating-button"
        :style="{ '--stack-index': stackIndex }"
        type="button"
        aria-label="打開 AI 客服"
        @click="store.openPanel()"
      >
        <i class="bi bi-chat-dots-fill" aria-hidden="true"></i>
      </button>
    </Transition>

    <Transition name="drawer-fade">
      <div v-if="store.open" class="chat-drawer-layer" role="presentation">
        <button
          class="drawer-backdrop"
          type="button"
          aria-label="關閉 AI 客服"
          @click="store.closePanel()"
        ></button>

        <aside class="chat-drawer" role="dialog" aria-modal="true" aria-labelledby="support-chat-title">
          <header class="drawer-header">
            <div>
              <span class="drawer-eyebrow">GIGAFIX SUPPORT</span>
              <h2 id="support-chat-title">AI 智能客服</h2>
            </div>
            <button class="close-button" type="button" aria-label="關閉" @click="store.closePanel()">
              <i class="bi bi-x-lg" aria-hidden="true"></i>
            </button>
          </header>

          <div ref="messageListRef" class="message-list">
            <div v-if="store.messages.length === 0" class="empty-state">
              <i class="bi bi-robot" aria-hidden="true"></i>
              <p>你好，我是 Gigafix AI 客服，有什麼可以幫你的嗎？</p>
            </div>

            <div
              v-for="message in store.messages"
              :key="message.id"
              class="message-row"
              :class="message.role"
            >
              <div class="message-bubble">
                <template v-if="message.role === 'assistant'">
                  <template v-for="(segment, index) in parseMessageSegments(message.content)" :key="index">
                    <RouterLink
                      v-if="segment.type === 'link'"
                      :to="segment.to"
                      class="message-link"
                      @click="store.closePanel()"
                    >{{ segment.text }}</RouterLink>
                    <a
                      v-else-if="segment.type === 'email'"
                      :href="`mailto:${segment.text}`"
                      class="message-link"
                    >{{ segment.text }}</a>
                    <template v-else>{{ segment.text }}</template>
                  </template>
                </template>
                <template v-else>{{ message.content }}</template>
              </div>
            </div>

            <div v-if="store.sending" class="message-row assistant">
              <div class="message-bubble message-pending">思考中...</div>
            </div>
          </div>

          <p v-if="store.error" class="error-banner">
            <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
            {{ ERROR_MESSAGES[store.error] }}
          </p>

          <form class="composer" @submit.prevent="handleSend">
            <textarea
              ref="composerInputRef"
              v-model="draft"
              class="composer-input"
              rows="1"
              placeholder="輸入你的問題..."
              :disabled="store.sending"
              @keydown="handleKeydown"
            ></textarea>
            <button
              class="composer-send"
              type="submit"
              :disabled="store.sending || !draft.trim()"
              aria-label="送出"
            >
              <i class="bi bi-send-fill" aria-hidden="true"></i>
            </button>
          </form>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.chat-floating-button {
  position: fixed;
  right: 28px;
  /* --stack-index 由 floatingBubbles store 決定，跟其他浮動氣泡（例如購物車）疊在一起時不重疊 */
  bottom: calc(28px + var(--stack-index, 0) * 70px);
  z-index: 1060;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  color: #ffffff;
  background: #2b77c5;
  box-shadow: 0 12px 28px rgba(43, 119, 197, 0.4);
  font-size: 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.chat-floating-button:hover,
.chat-floating-button:focus-visible {
  transform: translateY(-3px);
  box-shadow: 0 16px 34px rgba(43, 119, 197, 0.48);
  outline: none;
}

.chat-drawer-layer {
  position: fixed;
  inset: 0;
  z-index: 1080;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  border: 0;
  background: rgba(20, 24, 30, 0.32);
  cursor: pointer;
}

.chat-drawer {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  width: min(400px, 92vw);
  height: 100%;
  color: #1d324b;
  background: #ffffff;
  box-shadow: -24px 0 70px rgba(0, 0, 0, 0.18);
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 20px 18px;
  border-bottom: 1px solid #eef1f5;
}

.drawer-eyebrow {
  display: block;
  margin-bottom: 4px;
  color: #2b77c5;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.drawer-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #e3e6f0;
  border-radius: 50%;
  color: #5a5c69;
  background: #ffffff;
  cursor: pointer;
}

.close-button:hover,
.close-button:focus-visible {
  color: #ffffff;
  background: #2b77c5;
  border-color: #2b77c5;
  outline: none;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #6c757d;
  text-align: center;
}

.empty-state i {
  margin-bottom: 10px;
  font-size: 32px;
  color: #2b77c5;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.message-row {
  display: flex;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-row.user .message-bubble {
  color: #ffffff;
  background: #2b77c5;
  border-bottom-right-radius: 4px;
}

.message-row.assistant .message-bubble {
  color: #1d324b;
  background: #eef1f5;
  border-bottom-left-radius: 4px;
}

.message-pending {
  color: #888888;
  font-style: italic;
}

.message-link {
  color: #2b77c5;
  font-weight: 600;
  text-decoration: underline;
}

.message-link:hover,
.message-link:focus-visible {
  color: #1e3557;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 10px 20px;
  background: #fff5f5;
  border-top: 1px solid #f5c2c7;
  color: #c0392b;
  font-size: 13px;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid #eef1f5;
}

.composer-input {
  flex: 1;
  min-height: 40px;
  max-height: 120px;
  padding: 9px 12px;
  border: 1px solid #dee2e6;
  border-radius: 10px;
  font-family: inherit;
  font-size: 14px;
  resize: none;
}

.composer-input:disabled {
  background: #f6f8fa;
}

.composer-send {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border: none;
  border-radius: 10px;
  color: #ffffff;
  background: #2b77c5;
  font-size: 16px;
  cursor: pointer;
}

.composer-send:disabled {
  background: #adb5bd;
  cursor: default;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.22s ease;
}

.drawer-fade-enter-active .chat-drawer,
.drawer-fade-leave-active .chat-drawer {
  transition: transform 0.28s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-fade-enter-from .chat-drawer,
.drawer-fade-leave-to .chat-drawer {
  transform: translateX(100%);
}

.trigger-fade-enter-active,
.trigger-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.trigger-fade-enter-from,
.trigger-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
