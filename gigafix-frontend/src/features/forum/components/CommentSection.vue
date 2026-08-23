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

const props = defineProps({
  articleId: { type: [Number, String], required: true },
  status: { type: String, default: null },
})

const MAX_LENGTH = 1000
const REPORT_MAX_LENGTH = 500

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
    const fieldError = error.response?.data?.errors?.[0]?.message
    reportErrorMessage.value = fieldError || '檢舉失敗，請稍後再試'
  } finally {
    reportSubmitting.value = false
  }
}
</script>

<template>
  <section class="comment-section">
    <h3>留言</h3>

    <p v-if="loadError" class="error">{{ loadError }}</p>
    <ul v-else class="comment-list">
      <li v-for="comment in comments" :key="comment.commentId" class="comment-item">
        <button
          v-if="comment.status === 'HIDDEN' && !revealed[comment.commentId]"
          type="button"
          class="hidden-mask"
          @click="revealComment(comment.commentId)"
        >
          此留言已被系統隱藏，點擊以顯示
        </button>
        <template v-else>
          <div class="comment-header">
            <span class="author">{{ comment.authorNickName }}</span>
            <span class="time">{{ new Date(comment.commentCreatedTime).toLocaleString() }}</span>
          </div>
          <p class="content">{{ comment.content }}</p>
          <div class="comment-actions">
            <button
              type="button"
              class="like"
              :class="{ liked: comment.likedByCurrentMember }"
              @click="handleLike(comment)"
            >
              👍 {{ comment.likeCount }}
            </button>
            <button
              v-if="comment.authorId === TEST_MEMBER_ID"
              type="button"
              class="delete"
              @click="handleDelete(comment.commentId)"
            >
              刪除
            </button>
            <button
              v-if="comment.authorId !== TEST_MEMBER_ID"
              type="button"
              class="report"
              @click="toggleReportForm(comment.commentId)"
            >
              檢舉
            </button>
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
              <button type="button" class="cancel" @click="toggleReportForm(comment.commentId)">取消</button>
              <button type="submit" :disabled="reportSubmitting">
                {{ reportSubmitting ? '送出中...' : '送出檢舉' }}
              </button>
            </div>
            <p v-if="reportErrorMessage" class="error">{{ reportErrorMessage }}</p>
          </form>
        </template>
      </li>
      <li v-if="comments.length === 0" class="empty">還沒有留言，來搶頭香吧！</li>
    </ul>

    <p v-if="reportSuccessMessage" class="report-success">{{ reportSuccessMessage }}</p>

    <p v-if="locked" class="locked-message">留言功能已關閉</p>
    <form v-else class="comment-form" @submit.prevent="handleSubmit">
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
        <button type="submit" :disabled="submitting">送出</button>
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </form>
  </section>
</template>

<style scoped>
.comment-section {
  margin-top: 12px;
  border-top: 1px solid #eaeaea;
  padding-top: 10px;
}

.comment-section h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.comment-form textarea,
.report-form textarea {
  padding: 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-family: inherit;
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

.comment-form button[type='submit'],
.report-form button[type='submit'] {
  padding: 4px 12px;
  font-size: 13px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.comment-form button[type='submit']:disabled,
.report-form button[type='submit']:disabled {
  opacity: 0.6;
  cursor: default;
}

.locked-message {
  margin-top: 12px;
  color: #999999;
  font-size: 13px;
}

.error {
  color: #c0392b;
  font-size: 13px;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.comment-item {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.hidden-mask {
  width: 100%;
  padding: 8px;
  background-color: #f3f3f3;
  border: 1px dashed #cccccc;
  border-radius: 4px;
  color: #888888;
  font-size: 13px;
  text-align: center;
  cursor: pointer;
}

.hidden-mask:hover {
  background-color: #ececec;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #888888;
}

.comment-header .author {
  font-weight: 600;
  color: #333333;
}

.content {
  margin: 6px 0;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  gap: 10px;
}

.comment-actions button {
  background: none;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  padding: 2px 10px;
  font-size: 12px;
  cursor: pointer;
}

.comment-actions .like.liked {
  border-color: #2b77c5;
  color: #2b77c5;
  background-color: #eaf2fb;
}

.comment-actions .delete {
  color: #c0392b;
  border-color: #f0c0c0;
}

.comment-actions .report {
  color: #a15c00;
  border-color: #e8d3a0;
}

.report-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 6px;
  padding: 8px;
  background-color: #fffaf0;
  border: 1px solid #e8d3a0;
  border-radius: 4px;
}

.report-form .cancel {
  padding: 4px 12px;
  font-size: 13px;
  background: none;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  cursor: pointer;
}

.report-success {
  margin: 8px 0 0;
  color: #1e7e34;
  font-size: 13px;
}

.empty {
  color: #999999;
  text-align: center;
  padding: 10px 0;
  font-size: 13px;
}
</style>
