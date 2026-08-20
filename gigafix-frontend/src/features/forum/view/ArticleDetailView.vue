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

const articleId = computed(() => route.params.articleId)
const isAuthor = computed(() => article.value?.authorId === TEST_MEMBER_ID)

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

onMounted(load)

async function toggleLike() {
  try {
    if (liked.value) {
      await unlikeArticle(articleId.value)
      liked.value = false
    } else {
      await likeArticle(articleId.value)
      liked.value = true
    }
    await load()
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
</style>
