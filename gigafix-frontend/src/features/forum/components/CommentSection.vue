<script setup>
import { ref, watch, onMounted } from 'vue'
import {
  getComments,
  createComment,
  deleteComment,
  likeComment,
  unlikeComment,
  TEST_MEMBER_ID,
} from '../api'

const props = defineProps({
  articleId: { type: [Number, String], required: true },
})

const MAX_LENGTH = 1000

const comments = ref([])
const newContent = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const loadError = ref('')

// 防呆：不只依賴 textarea 的 maxlength，貼上超長文字時也要能立刻裁切、讓字數顯示反映出來
watch(newContent, (value) => {
  if (value.length > MAX_LENGTH) {
    newContent.value = value.slice(0, MAX_LENGTH)
  }
})

async function loadComments() {
  try {
    comments.value = await getComments(props.articleId)
    loadError.value = ''
  } catch {
    loadError.value = '留言載入失敗，請確認後端服務是否啟動'
  }
}

onMounted(loadComments)

async function handleSubmit() {
  if (!newContent.value.trim()) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await createComment(props.articleId, { content: newContent.value })
    newContent.value = ''
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
</script>

<template>
  <section class="comment-section">
    <h3>留言</h3>

    <form class="comment-form" @submit.prevent="handleSubmit">
      <textarea
        v-model="newContent"
        rows="3"
        :maxlength="MAX_LENGTH"
        placeholder="寫下你的留言..."
      />
      <div class="form-footer">
        <span class="char-count">{{ newContent.length }}/{{ MAX_LENGTH }}</span>
        <button type="submit" :disabled="submitting">送出留言</button>
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </form>

    <p v-if="loadError" class="error">{{ loadError }}</p>
    <ul v-else class="comment-list">
      <li v-for="comment in comments" :key="comment.commentId" class="comment-item">
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
        </div>
      </li>
      <li v-if="comments.length === 0" class="empty">還沒有留言，來搶頭香吧！</li>
    </ul>
  </section>
</template>

<style scoped>
.comment-section {
  margin-top: 24px;
  border-top: 1px solid #eaeaea;
  padding-top: 16px;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.comment-form textarea {
  padding: 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-family: inherit;
  resize: vertical;
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

.comment-form button[type='submit'] {
  padding: 6px 16px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.comment-form button[type='submit']:disabled {
  opacity: 0.6;
  cursor: default;
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
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
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

.empty {
  color: #999999;
  text-align: center;
  padding: 20px 0;
}
</style>
