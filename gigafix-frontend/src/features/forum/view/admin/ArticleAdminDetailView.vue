<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleForAdmin, updateArticleStatus } from '../../adminApi'
import {
  ADMIN_ARTICLE_SOURCE_STATUSES,
  ADMIN_ARTICLE_STATUS_OPTIONS,
  ARTICLE_STATUS_MAP,
  statusBadgeClass,
  statusLabel,
} from '../../adminStatusMaps'

const route = useRoute()
const router = useRouter()
const articleId = route.params.articleId

const article = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const targetStatus = ref('')
const targetPinned = ref(false)
const submitting = ref(false)

const canChangeStatus = computed(() => !!article.value && ADMIN_ARTICLE_SOURCE_STATUSES.includes(article.value.status))

async function fetchArticle() {
  loading.value = true
  errorMessage.value = ''
  try {
    article.value = await getArticleForAdmin(articleId)
    targetPinned.value = article.value.isPinned ?? false
  } catch (error) {
    console.error(error)
    errorMessage.value =
      error.response?.status === 404
        ? `找不到 ID 為 ${articleId} 的文章`
        : error.response
          ? `讀取文章失敗：HTTP ${error.response.status}`
          : '無法連線到後端伺服器'
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  return value?.slice(0, 19)?.replace('T', ' ') ?? '-'
}

async function handleStatusSubmit() {
  if (!targetStatus.value) return
  submitting.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await updateArticleStatus(articleId, { status: targetStatus.value, isPinned: targetPinned.value })
    successMessage.value = '狀態已更新'
    targetStatus.value = ''
    await fetchArticle()
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response
      ? `狀態更新失敗：HTTP ${error.response.status}`
      : '無法連線到後端伺服器'
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push({ name: 'admin-forum-articles' })
}

onMounted(fetchArticle)
</script>

<template>
  <main class="container-fluid px-3 px-md-4 py-4">
    <div class="page-shell mx-auto">
      <header class="mb-4">
        <h1 class="h2 fw-bold mb-1">文章詳情（後台管理）</h1>
        <p class="text-secondary mb-0">查看文章完整內容，並變更狀態或置頂設定。</p>
      </header>

      <div v-if="successMessage" class="alert alert-success alert-dismissible" role="alert">
        {{ successMessage }}
        <button class="btn-close" type="button" aria-label="關閉成功訊息" @click="successMessage = ''"></button>
      </div>

      <div v-if="loading" class="card">
        <div class="card-body text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <div class="text-secondary mt-3">正在讀取文章資料...</div>
        </div>
      </div>

      <div v-else-if="errorMessage && !article" class="alert alert-danger" role="alert">
        <div class="d-flex align-items-center justify-content-between gap-3">
          <span>{{ errorMessage }}</span>
          <button class="btn btn-sm btn-outline-danger" type="button" @click="fetchArticle">重新讀取</button>
        </div>
      </div>

      <template v-else-if="article">
        <section class="card mb-4">
          <div class="card-body p-4">
            <div class="d-flex align-items-center gap-2 mb-2">
              <span class="text-secondary small">文章 ID：{{ article.articleId }}</span>
              <span class="badge" :class="statusBadgeClass(ARTICLE_STATUS_MAP, article.status)">
                {{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}
              </span>
              <span v-if="article.isPinned" class="badge text-bg-warning">📌 已置頂</span>
            </div>
            <h2 class="h3 fw-bold mb-3">{{ article.title || '（無標題）' }}</h2>

            <dl class="row detail-list mb-4">
              <dt class="col-sm-3">分類</dt>
              <dd class="col-sm-9">{{ article.categoryName }}</dd>
              <dt class="col-sm-3">作者</dt>
              <dd class="col-sm-9">{{ article.authorNickName }}（會員 #{{ article.authorId }}）</dd>
              <dt class="col-sm-3">瀏覽 / 讚 / 留言</dt>
              <dd class="col-sm-9">{{ article.viewCount }} / {{ article.likeCount }} / {{ article.commentCount }}</dd>
              <dt class="col-sm-3">建立時間</dt>
              <dd class="col-sm-9">{{ formatDate(article.articleCreatedTime) }}</dd>
              <dt class="col-sm-3">最後修改</dt>
              <dd class="col-sm-9">{{ formatDate(article.articleUpdatedTime) }}</dd>
            </dl>

            <img v-if="article.coverImage" class="cover mb-3" :src="article.coverImage" alt="" />
            <div class="content">{{ article.content }}</div>
          </div>
        </section>

        <section class="card mb-4">
          <div class="card-body p-4">
            <h3 class="h5 fw-bold mb-3">變更狀態</h3>
            <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
            <p v-if="!canChangeStatus" class="text-secondary mb-0">
              此文章目前狀態為「{{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}」，管理員無法變更狀態（僅能變更發布／關閉／強制隱藏／強制關閉狀態的文章）。
            </p>
            <form
              v-else
              class="d-flex flex-column flex-md-row align-items-md-end gap-3"
              @submit.prevent="handleStatusSubmit"
            >
              <div>
                <label class="form-label small text-secondary" for="target-status">要變更的狀態</label>
                <select id="target-status" v-model="targetStatus" class="form-select">
                  <option value="">請選擇要變更的狀態</option>
                  <option v-for="value in ADMIN_ARTICLE_STATUS_OPTIONS" :key="value" :value="value">
                    {{ statusLabel(ARTICLE_STATUS_MAP, value) }}
                  </option>
                </select>
              </div>
              <div class="form-check">
                <input id="target-pinned" v-model="targetPinned" class="form-check-input" type="checkbox" />
                <label class="form-check-label" for="target-pinned">置頂</label>
              </div>
              <button class="btn btn-primary" type="submit" :disabled="!targetStatus || submitting">
                {{ submitting ? '更新中...' : '送出變更' }}
              </button>
            </form>
          </div>
        </section>

        <div class="d-flex justify-content-end">
          <button class="btn btn-outline-secondary" type="button" @click="goBack">回到文章列表</button>
        </div>
      </template>
    </div>
  </main>
</template>

<style scoped>
.page-shell {
  max-width: 1000px;
}
.cover {
  max-width: 100%;
  max-height: 320px;
  border-radius: 8px;
}
.content {
  line-height: 1.7;
  white-space: pre-wrap;
}
.detail-list dt,
.detail-list dd {
  padding-top: 0.6rem;
  padding-bottom: 0.6rem;
  border-bottom: 1px solid #e8ebf0;
}
.detail-list dt {
  color: #64748b;
}
</style>
