<script setup>
import { ref, onMounted } from 'vue'

// 檢舉表單：文章、樓層、留言三處共用的輸入介面。
// 只負責「讓使用者打原因」這件事；送出時把原因交給父層，
// 登入檢查、要呼叫 reportArticle 還是 reportComment、成功訊息放哪都由父層決定。
// 父層用 v-if 控制展開，所以每次展開都是新的實例，原因不會殘留到下一次。
const props = defineProps({
  submitting: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
  // 對齊後端 CreateReportRequest 的 @Size(max = 250)，欄位是 NVARCHAR(250)，250 是字元數
  maxLength: { type: Number, default: 250 },
})

const emit = defineEmits(['submit', 'cancel'])

const reason = ref('')
const rootRef = ref(null)
const textareaRef = ref(null)

function autoResize(event) {
  const el = event.target
  // 先歸零才量得到內容真正需要的高度
  el.style.height = 'auto'
  // scrollHeight 不含 border，box-sizing: border-box 下直接套用會每次少掉 border 那幾 px
  const borderHeight = el.offsetHeight - el.clientHeight
  el.style.height = `${el.scrollHeight + borderHeight}px`
}

function handleSubmit() {
  if (!reason.value.trim() || props.submitting) return
  emit('submit', reason.value)
}

// 檢舉鈕在卡片上方的「更多選項」裡，表單卻長在內文或動作列之後，
// 文章本文一長，點了檢舉根本看不到表單出現在哪。展開時直接把畫面帶過去並把游標放進輸入框。
// 先 focus 再捲動：focus 預設會讓瀏覽器瞬間跳到元素位置，蓋掉 smooth 動畫，所以要 preventScroll
onMounted(() => {
  textareaRef.value?.focus({ preventScroll: true })
  rootRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
})
</script>

<template>
  <form ref="rootRef" class="report-form" @submit.prevent="handleSubmit">
    <textarea
      ref="textareaRef"
      v-model="reason"
      rows="1"
      :maxlength="maxLength"
      placeholder="請輸入檢舉原因..."
      @input="autoResize"
    />
    <div class="form-footer">
      <span class="char-count">{{ reason.length }}/{{ maxLength }}</span>
      <button type="button" class="cancel" @click="emit('cancel')">取消</button>
      <button type="submit" class="submit" :disabled="submitting">
        {{ submitting ? '送出中...' : '送出檢舉' }}
      </button>
    </div>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </form>
</template>

<style scoped>
/* 外距刻意不寫在這裡：文章卡片、樓層、留言三處的留白各不相同，由父層對 .report-form 設定
   （父層 scoped 樣式選得到子元件的根節點，所以不需要 :deep） */
.report-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background-color: #fffaf0;
  border: 1px solid #e8d3a0;
  border-radius: 0.375rem;
}

textarea {
  padding: 10px 12px;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  font-family: inherit;
  font-size: 14px;
  resize: none;
  overflow: hidden;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

/* footer 是 flex-end + gap，字數靠 margin-right:auto 吃掉剩餘空間推到最左邊 */
.char-count {
  margin-right: auto;
  font-size: 14px;
  color: #999999;
}

.cancel {
  padding: 4px 12px;
  font-size: 14px;
  background: none;
  border: 1px solid #d0d0d0;
  border-radius: 0.375rem;
  cursor: pointer;
}

.submit {
  padding: 4px 12px;
  font-size: 14px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 0.375rem;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
  cursor: default;
}

.error {
  margin: 0;
  color: #c0392b;
  font-size: 14px;
}
</style>
