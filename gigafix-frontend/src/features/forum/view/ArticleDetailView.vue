<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getArticle,
  deleteArticle,
  likeArticle,
  unlikeArticle,
  hasLikedArticle,
  addBookmark,
  removeBookmark,
  hasBookmarked,
  getFloors,
  createFloor,
  TEST_MEMBER_ID,
} from '../api'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const router = useRouter()

const article = ref(null)
const liked = ref(false)
const bookmarked = ref(false)
const loading = ref(true)
const errorMessage = ref('')

const floors = ref([])
const floorContent = ref('')
const floorSubmitting = ref(false)
const floorErrorMessage = ref('')

const articleId = computed(() => route.params.articleId)
const isAuthor = computed(() => article.value?.authorId === TEST_MEMBER_ID)
// 只有根文章（不是樓層本身）才能被蓋樓
const canAddFloor = computed(() => !!article.value && article.value.parentArticleId == null)

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [articleData, likedStatus, bookmarkedStatus] = await Promise.all([
      getArticle(articleId.value),
      hasLikedArticle(articleId.value),
      hasBookmarked(articleId.value),
    ])
    article.value = articleData
    liked.value = likedStatus
    bookmarked.value = bookmarkedStatus
  } catch {
    errorMessage.value = '文章不存在、已被下架，或載入失敗'
  } finally {
    loading.value = false
  }
}

async function loadFloors() {
  try {
    floors.value = await getFloors(articleId.value)
  } catch {
    // 樓層載入失敗不影響文章本身的顯示，安靜失敗即可
  }
}

onMounted(() => {
  load()
  loadFloors()
})

async function toggleLike() {
  try {
    if (liked.value) {
      await unlikeArticle(articleId.value)
      liked.value = false
      article.value.likeCount -= 1
    } else {
      await likeArticle(articleId.value)
      liked.value = true
      article.value.likeCount += 1
    }
  } catch {
    // 本地狀態可能跟伺服器實際狀態不同步（例如上次操作沒真的送達），失敗時反轉一次貼近實際結果
    liked.value = !liked.value
  }
}

async function toggleBookmark() {
  try {
    if (bookmarked.value) {
      await removeBookmark(articleId.value)
      bookmarked.value = false
    } else {
      await addBookmark(articleId.value)
      bookmarked.value = true
    }
  } catch {
    bookmarked.value = !bookmarked.value
  }
}

async function handleCreateFloor() {
  if (!floorContent.value.trim()) return
  floorSubmitting.value = true
  floorErrorMessage.value = ''
  try {
    await createFloor(articleId.value, floorContent.value)
    floorContent.value = ''
    await loadFloors()
  } catch (error) {
    const fieldError = error.response?.data?.errors?.[0]?.message
    floorErrorMessage.value = fieldError || '蓋樓失敗，請確認文章目前是否可以蓋樓'
  } finally {
    floorSubmitting.value = false
  }
}

async function handleDelete() {
  if (!confirm('確定要刪除這篇文章嗎？')) return
  await deleteArticle(articleId.value)
  router.push({ name: 'forumList' })
}
</script>

<template>
  <div class="article-detail-view">
    <p v-if="loading">載入中...</p>
    <template v-else-if="article">
      <div class="header">
        <span class="category">{{ article.categoryName }}</span>
        <h1>{{ article.title }}</h1>
        <div class="meta">
          <span>{{ article.authorNickName }}</span>
          <span>{{ new Date(article.articleCreatedTime).toLocaleString() }}</span>
          <span>👁 {{ article.viewCount }}</span>
        </div>
      </div>

      <img v-if="article.coverImage" class="cover" :src="article.coverImage" alt="" />

      <div class="content" v-html="article.content"></div>

      <div class="actions">
        <button type="button" @click="toggleLike">👍 讚 {{ article.likeCount }}</button>
        <button type="button" @click="toggleBookmark">
          {{ bookmarked ? '★ 已收藏' : '☆ 收藏' }}
        </button>
        <template v-if="isAuthor">
          <RouterLink :to="{ name: 'forumEdit', params: { articleId } }">編輯</RouterLink>
          <button type="button" class="delete" @click="handleDelete">刪除</button>
        </template>
      </div>

      <CommentSection :article-id="articleId" />

      <div v-for="floor in floors" :key="floor.articleId" class="floor-block">
        <div class="floor-header">
          <span class="floor-badge">{{ floor.floorNumber }}樓</span>
          <span class="author">{{ floor.authorNickName }}</span>
          <span class="time">{{ new Date(floor.articleCreatedTime).toLocaleString() }}</span>
        </div>
        <p class="floor-content">{{ floor.content }}</p>
        <CommentSection :article-id="floor.articleId" />
      </div>

      <section v-if="canAddFloor" class="floor-form-section">
        <h3>蓋樓</h3>
        <form class="floor-form" @submit.prevent="handleCreateFloor">
          <textarea v-model="floorContent" rows="3" placeholder="回覆這篇文章（蓋樓）..." />
          <div class="form-footer">
            <button type="submit" :disabled="floorSubmitting">
              {{ floorSubmitting ? '送出中...' : '蓋樓' }}
            </button>
          </div>
          <p v-if="floorErrorMessage" class="error">{{ floorErrorMessage }}</p>
        </form>
      </section>
    </template>
    <p v-else class="empty">{{ errorMessage || '文章不存在或已被下架' }}</p>
  </div>
</template>

<style scoped>
.article-detail-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.category {
  font-size: 12px;
  color: #2b77c5;
}

h1 {
  margin: 8px 0;
  color: #1d324b;
}

.meta {
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: #888888;
  margin-bottom: 16px;
}

.cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 16px;
}

.content {
  line-height: 1.7;
  margin-bottom: 20px;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.actions button,
.actions a {
  padding: 6px 14px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background-color: #ffffff;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  font-size: 14px;
}

.actions .delete {
  color: #c0392b;
  border-color: #f0c0c0;
}

.empty {
  text-align: center;
  color: #999999;
  padding: 60px 0;
}

.floor-block {
  margin-top: 20px;
  padding: 14px 16px;
  background-color: #f8fafc;
  border: 1px solid #e5e9f0;
  border-radius: 6px;
}

.floor-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #888888;
  margin-bottom: 6px;
}

.floor-badge {
  padding: 2px 8px;
  background-color: #2b77c5;
  color: #ffffff;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.floor-header .author {
  font-weight: 600;
  color: #333333;
}

.floor-content {
  margin: 0 0 4px;
  white-space: pre-wrap;
  line-height: 1.6;
}

.floor-form-section {
  margin-top: 20px;
  border-top: 1px solid #eaeaea;
  padding-top: 12px;
}

.floor-form-section h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.floor-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.floor-form textarea {
  padding: 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-family: inherit;
  resize: vertical;
}

.floor-form .form-footer {
  display: flex;
  justify-content: flex-end;
}

.floor-form button[type='submit'] {
  padding: 6px 16px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.floor-form button[type='submit']:disabled {
  opacity: 0.6;
  cursor: default;
}

.error {
  color: #c0392b;
  font-size: 13px;
  margin: 0;
}
</style>
