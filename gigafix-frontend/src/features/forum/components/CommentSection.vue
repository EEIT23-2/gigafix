<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import {
  getComments,
  createComment,
  deleteComment,
  likeComment,
  unlikeComment,
  reportComment,
  TEST_MEMBER_ID,
} from '../api'
import MoreActionsMenu from './MoreActionsMenu.vue'

const props = defineProps({
  articleId: { type: [Number, String], required: true },
  status: { type: String, default: null },
})

const MAX_LENGTH = 1000
const REPORT_MAX_LENGTH = 250

const comments = ref([])
const newContent = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const loadError = ref('')
const textareaRef = ref(null)

// 系統隱藏的留言暫時被使用者點開過（只存在這次瀏覽，重新整理會恢復遮蔽）
const revealed = ref({})

// 檢舉表單：同時間只會展開一則留言的檢舉表單
const reportingCommentId = ref(null)
const reportReason = ref('')
const reportSubmitting = ref(false)
const reportErrorMessage = ref('')
const reportSuccessMessage = ref('')

const locked = computed(() => props.status === 'CLOSED' || props.status === 'FORCE_CLOSED')

// 沒有頭像欄位，用暱稱首字當頭像。用展開運算子取字，避免 emoji 之類的字元被切成半個
function initial(nickName) {
  return nickName ? [...nickName][0] : '?'
}

function formatDateTime(value) {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

// 防呆：不只依賴 textarea 的 maxlength，貼上超長文字時也要能立刻裁切、讓字數顯示反映出來
watch(newContent, (value) => {
  if (value.length > MAX_LENGTH) {
    newContent.value = value.slice(0, MAX_LENGTH)
  }
})

function autoResize(event) {
  const el = event.target
  el.style.height = 'auto'
  el.style.height = `${el.scrollHeight}px`
}

function resetTextareaHeight() {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

async function loadComments() {
  try {
    comments.value = await getComments(props.articleId)
    loadError.value = ''
  } catch {
    loadError.value = '留言載入失敗，請確認後端服務是否啟動'
  }
}

onMounted(loadComments)

function revealComment(commentId) {
  revealed.value[commentId] = true
}

async function handleSubmit() {
  if (!newContent.value.trim()) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await createComment(props.articleId, { content: newContent.value })
    newContent.value = ''
    resetTextareaHeight()
    await loadComments()
  } catch (error) {
    const fieldError = error.response?.data?.errors?.[0]?.message
    errorMessage.value = fieldError || '留言失敗，請確認文章目前是否可以留言'
  } finally {
    submitting.value = false
  }
}

async function handleDelete(commentId) {
  try {
    await deleteComment(commentId)
    await loadComments()
  } catch {
    errorMessage.value = '刪除留言失敗'
  }
}

async function handleLike(comment) {
  try {
    if (comment.likedByCurrentMember) {
      await unlikeComment(comment.commentId)
    } else {
      await likeComment(comment.commentId)
    }
    await loadComments()
  } catch {
    errorMessage.value = '操作失敗，請稍後再試'
  }
}

function toggleReportForm(commentId) {
  reportSuccessMessage.value = ''
  reportErrorMessage.value = ''
  reportingCommentId.value = reportingCommentId.value === commentId ? null : commentId
  reportReason.value = ''
}

async function handleReportSubmit(commentId) {
  if (!reportReason.value.trim()) return
  reportSubmitting.value = true
  reportErrorMessage.value = ''
  try {
    await reportComment(commentId, reportReason.value)
    reportingCommentId.value = null
    reportReason.value = ''
    reportSuccessMessage.value = '已送出檢舉'
  } catch (error) {
    // 兩種錯誤格式都要接：@Valid 失敗是 { errors: [...] }，
    // 後端商業規則（重複檢舉、檢舉自己的留言）走 ForumExceptionHandler，回的是 { errorCode, message }
    const data = error.response?.data
    reportErrorMessage.value =
      data?.errors?.[0]?.message || data?.message || '檢舉失敗，請稍後再試'
  } finally {
    reportSubmitting.value = false
  }
}
</script>

<template>
  <section class="comment-section">
    <p v-if="loadError" class="error">{{ loadError }}</p>
    <ul v-else class="comment-list">
      <li v-for="comment in comments" :key="comment.commentId" class="comment-item">
        <span class="avatar" :class="{ 'avatar-masked': comment.status === 'HIDDEN' && !revealed[comment.commentId] }">
          {{ comment.status === 'HIDDEN' && !revealed[comment.commentId] ? '?' : initial(comment.authorNickName) }}
        </span>
        <div class="comment-main">
          <div class="comment-header">
            <span class="author">{{ comment.authorNickName }}</span>
            <span class="time">{{ formatDateTime(comment.commentCreatedTime) }}</span>
          </div>
          <button
            v-if="comment.status === 'HIDDEN' && !revealed[comment.commentId]"
            type="button"
            class="hidden-mask"
            @click="revealComment(comment.commentId)"
          >
            此留言已被系統隱藏，點擊以顯示
          </button>
          <template v-else>
            <p class="content">{{ comment.content }}</p>
            <div class="comment-actions">
              <button
                type="button"
                class="chip"
                :class="{ active: comment.likedByCurrentMember }"
                @click="handleLike(comment)"
              >
                <i class="bi bi-hand-thumbs-up"></i>{{ comment.likeCount }}
              </button>
              <MoreActionsMenu>
                <template #default="{ close }">
                  <!-- 留言沒有「編輯」：後端只有新增/刪除/改狀態，沒有更新留言內容的端點 -->
                  <button
                    v-if="comment.authorId === TEST_MEMBER_ID"
                    type="button"
                    class="danger"
                    @click="close(); handleDelete(comment.commentId)"
                  >
                    刪除
                  </button>
                  <button v-else type="button" @click="close(); toggleReportForm(comment.commentId)">
                    檢舉
                  </button>
                </template>
              </MoreActionsMenu>
            </div>
            <form
              v-if="reportingCommentId === comment.commentId"
              class="report-form"
              @submit.prevent="handleReportSubmit(comment.commentId)"
            >
              <textarea
                v-model="reportReason"
                rows="1"
                :maxlength="REPORT_MAX_LENGTH"
                placeholder="請輸入檢舉原因..."
                @input="autoResize"
              />
              <div class="form-footer">
                <span class="char-count">{{ reportReason.length }}/{{ REPORT_MAX_LENGTH }}</span>
                <button type="button" class="cancel" @click="toggleReportForm(comment.commentId)">取消</button>
                <button type="submit" :disabled="reportSubmitting">
                  {{ reportSubmitting ? '送出中...' : '送出檢舉' }}
                </button>
              </div>
              <p v-if="reportErrorMessage" class="error">{{ reportErrorMessage }}</p>
            </form>
          </template>
        </div>
      </li>
      <li v-if="comments.length === 0" class="empty">還沒有留言，來搶頭香吧！</li>
    </ul>

    <p v-if="reportSuccessMessage" class="report-success">{{ reportSuccessMessage }}</p>

    <p v-if="locked" class="locked-message">
      <i class="bi bi-lock"></i>留言功能已關閉
    </p>
    <!-- 撰寫框放在留言列表下方 -->
    <form v-else class="comment-form" @submit.prevent="handleSubmit">
      <span class="avatar avatar-me">我</span>
      <div class="comment-form-main">
        <textarea
          ref="textareaRef"
          v-model="newContent"
          rows="1"
          :maxlength="MAX_LENGTH"
          placeholder="寫下你的留言..."
          @input="autoResize"
        />
        <div class="form-footer">
          <span class="char-count">{{ newContent.length }}/{{ MAX_LENGTH }}</span>
          <button type="submit" class="submit-btn" :disabled="submitting">送出</button>
        </div>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      </div>
    </form>
  </section>
</template>

<style scoped>
/* 這個元件現在活在文章卡片內，外框由卡片負責，所以不再自己畫上邊界 */
.comment-section {
  display: flex;
  flex-direction: column;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #eef1f5;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background-color: #e5e9f0;
  color: #1d324b;
  font-size: 13px;
  font-weight: 700;
}

.avatar-masked {
  background-color: #f0f0f0;
  color: #adb5bd;
}

.avatar-me {
  background-color: #2b77c5;
  color: #ffffff;
}

.comment-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #888888;
}

.comment-header .author {
  font-weight: 600;
  color: #333333;
}

.content {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #333333;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  background-color: #ffffff;
  color: #6c757d;
  font-size: 12px;
  cursor: pointer;
}

.chip:hover {
  border-color: #2b77c5;
  color: #2b77c5;
}

.chip.active {
  border-color: #2b77c5;
  background-color: #eaf2fb;
  color: #2b77c5;
  font-weight: 600;
}

.hidden-mask {
  width: 100%;
  padding: 12px;
  background-color: #f3f3f3;
  border: 1px dashed #cccccc;
  border-radius: 0.375rem;
  color: #888888;
  font-size: 13px;
  text-align: center;
  cursor: pointer;
}

.hidden-mask:hover {
  background-color: #ececec;
}

.empty {
  padding: 20px 24px;
  border-top: 1px solid #eef1f5;
  color: #999999;
  text-align: center;
  font-size: 13px;
}

/* ── 撰寫框：列表下方 ── */
.comment-form {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #eef1f5;
}

.comment-form-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.comment-form textarea,
.report-form textarea {
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
  justify-content: space-between;
  align-items: center;
}

.char-count {
  font-size: 12px;
  color: #999999;
}

.submit-btn {
  padding: 7px 18px;
  border: 0;
  border-radius: 0.375rem;
  background-color: #2b77c5;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.locked-message {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 16px 24px;
  border-top: 1px solid #eef1f5;
  color: #6c757d;
  font-size: 13px;
}

/* ── 檢舉表單 ── */
.report-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 6px;
  padding: 8px;
  background-color: #fffaf0;
  border: 1px solid #e8d3a0;
  border-radius: 0.375rem;
}

/* .form-footer 是留言表單與檢舉表單共用的 space-between；檢舉表單多了字數會變成三個子元素，
   space-between 會把「取消」推到正中間，所以這裡改成靠右排列，字數用 margin-right:auto 推到左邊 */
.report-form .form-footer {
  justify-content: flex-end;
  gap: 8px;
}

.report-form .char-count {
  margin-right: auto;
}

.report-form .cancel {
  padding: 4px 12px;
  font-size: 13px;
  background: none;
  border: 1px solid #d0d0d0;
  border-radius: 0.375rem;
  cursor: pointer;
}

.report-form button[type='submit'] {
  padding: 4px 12px;
  font-size: 13px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 0.375rem;
  cursor: pointer;
}

.report-form button[type='submit']:disabled {
  opacity: 0.6;
  cursor: default;
}

.report-success {
  margin: 0;
  padding: 8px 24px 0;
  color: #1e7e34;
  font-size: 13px;
}

.error {
  margin: 0;
  color: #c0392b;
  font-size: 13px;
}
</style>
