<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getReportForAdmin,
  getArticleForAdmin,
  getCommentForAdmin,
  getArticleComments,
  updateArticleStatus,
  updateCommentStatus,
  updateReportStatus,
} from '../../adminApi'
import {
  ADMIN_ARTICLE_SOURCE_STATUSES,
  ADMIN_ARTICLE_STATUS_OPTIONS,
  ARTICLE_STATUS_MAP,
  COMMENT_STATUS_MAP,
  REPORT_STATUS_MAP,
  statusBadgeClass,
  statusLabel,
} from '../../adminStatusMaps'

const route = useRoute()
const router = useRouter()
const reportId = route.params.reportId

const report = ref(null)
const article = ref(null)
const reportedComment = ref(null)
const inlineComments = ref([])

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const commentActionBusy = ref(false)
const reportSubmitting = ref(false)
const targetReportStatus = ref('')

const articleTargetStatus = ref('')
const articleTargetPinned = ref(false)
const articleStatusSubmitting = ref(false)

const isCommentReport = computed(() => report.value?.commentId != null)

const canChangeArticleStatus = computed(
  () => !!article.value && ADMIN_ARTICLE_SOURCE_STATUSES.includes(article.value.status),
)

const highlightedCommentVisibleInline = computed(
  () =>
    reportedComment.value != null &&
    inlineComments.value.some((c) => c.commentId === reportedComment.value.commentId),
)

function formatDate(value) {
  return value?.slice(0, 19)?.replace('T', ' ') ?? '-'
}

async function fetchAll() {
  loading.value = true
  errorMessage.value = ''
  article.value = null
  reportedComment.value = null
  inlineComments.value = []

  try {
    report.value = await getReportForAdmin(reportId)

    if (report.value.articleId != null) {
      article.value = await getArticleForAdmin(report.value.articleId)
    } else if (report.value.commentId != null) {
      reportedComment.value = await getCommentForAdmin(report.value.commentId)
      article.value = await getArticleForAdmin(reportedComment.value.articleId)
      inlineComments.value = await getArticleComments(reportedComment.value.articleId)
    }

    if (article.value) {
      articleTargetPinned.value = article.value.isPinned ?? false
    }
  } catch (error) {
    console.error(error)
    errorMessage.value =
      error.response?.status === 404
        ? `找不到 ID 為 ${reportId} 的檢舉`
        : error.response
          ? `讀取檢舉失敗：HTTP ${error.response.status}`
          : '無法連線到後端伺服器'
  } finally {
    loading.value = false
  }
}

async function refreshCommentAndList() {
  if (!reportedComment.value) return
  reportedComment.value = await getCommentForAdmin(reportedComment.value.commentId)
  inlineComments.value = await getArticleComments(reportedComment.value.articleId)
}

async function handleCommentStatusAction(status) {
  commentActionBusy.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await updateCommentStatus(reportedComment.value.commentId, status)
    await refreshCommentAndList()
    successMessage.value = '留言狀態已更新'
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response
      ? `留言狀態更新失敗：HTTP ${error.response.status}`
      : '無法連線到後端伺服器'
  } finally {
    commentActionBusy.value = false
  }
}

async function handleArticleStatusSubmit() {
  if (!articleTargetStatus.value) return
  articleStatusSubmitting.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    article.value = await updateArticleStatus(article.value.articleId, {
      status: articleTargetStatus.value,
      isPinned: articleTargetPinned.value,
    })
    articleTargetStatus.value = ''
    successMessage.value = '文章狀態已更新'
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response
      ? `文章狀態更新失敗：HTTP ${error.response.status}`
      : '無法連線到後端伺服器'
  } finally {
    articleStatusSubmitting.value = false
  }
}

async function handleReportStatusSubmit() {
  if (!targetReportStatus.value) return
  reportSubmitting.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    report.value = await updateReportStatus(reportId, targetReportStatus.value)
    targetReportStatus.value = ''
    successMessage.value = '檢舉狀態已更新'
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response
      ? `檢舉狀態更新失敗：HTTP ${error.response.status}`
      : '無法連線到後端伺服器'
  } finally {
    reportSubmitting.value = false
  }
}

function goBack() {
  router.push({ name: 'admin-forum-reports' })
}

function goToArticleDetail() {
  router.push({ name: 'admin-forum-article-detail', params: { articleId: article.value.articleId } })
}

onMounted(fetchAll)
</script>

<template>
  <main class="container-fluid px-3 px-md-4 py-4">
    <div class="page-shell mx-auto">
      <header class="mb-4">
        <h1 class="h2 fw-bold mb-1">檢舉詳情</h1>
        <p class="text-secondary mb-0">查看檢舉內容，追蹤被檢舉的文章／留言，並處理檢舉狀態。</p>
      </header>

      <div v-if="successMessage" class="alert alert-success alert-dismissible" role="alert">
        {{ successMessage }}
        <button class="btn-close" type="button" aria-label="關閉成功訊息" @click="successMessage = ''"></button>
      </div>
      <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

      <div v-if="loading" class="card">
        <div class="card-body text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <div class="text-secondary mt-3">正在讀取檢舉資料...</div>
        </div>
      </div>

      <template v-else-if="report">
        <section class="card mb-4">
          <div class="card-body p-4">
            <div class="d-flex align-items-center gap-2 mb-2">
              <span class="text-secondary small">檢舉 ID：{{ report.reportId }}</span>
              <span class="badge" :class="statusBadgeClass(REPORT_STATUS_MAP, report.status)">
                {{ statusLabel(REPORT_STATUS_MAP, report.status) }}
              </span>
            </div>
            <dl class="row detail-list mb-0">
              <dt class="col-sm-3">檢舉類型</dt>
              <dd class="col-sm-9">{{ isCommentReport ? '留言' : '文章' }}</dd>
              <dt class="col-sm-3">檢舉人</dt>
              <dd class="col-sm-9">{{ report.reporterNickName }}（會員 #{{ report.reporterId }}）</dd>
              <dt class="col-sm-3">檢舉原因</dt>
              <dd class="col-sm-9">{{ report.reason }}</dd>
              <dt class="col-sm-3">檢舉時間</dt>
              <dd class="col-sm-9">{{ formatDate(report.reportCreatedTime) }}</dd>
            </dl>
          </div>
        </section>

        <section v-if="article" class="card mb-4">
          <div class="card-body p-4">
            <div class="d-flex align-items-center justify-content-between mb-3">
              <h3 class="h5 fw-bold mb-0">
                {{ isCommentReport ? '被檢舉留言所屬的文章' : '被檢舉的文章' }}
              </h3>
              <button class="btn btn-sm btn-outline-primary" type="button" @click="goToArticleDetail">
                查看完整文章詳情
              </button>
            </div>
            <div class="text-secondary small mb-1">文章 ID：{{ article.articleId }}</div>
            <span class="badge mb-2" :class="statusBadgeClass(ARTICLE_STATUS_MAP, article.status)">
              {{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}
            </span>
            <h4 class="h5 fw-semibold mb-2 mt-2">{{ article.title || '（無標題）' }}</h4>
            <div v-if="!isCommentReport" class="article-content mb-3">{{ article.content }}</div>

            <hr />
            <h5 class="h6 fw-bold mb-2">變更文章狀態</h5>
            <p v-if="!canChangeArticleStatus" class="text-secondary small mb-0">
              此文章目前狀態為「{{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}」，管理員無法變更狀態。
            </p>
            <form
              v-else
              class="d-flex flex-wrap align-items-end gap-3"
              @submit.prevent="handleArticleStatusSubmit"
            >
              <div>
                <label class="form-label small text-secondary" for="article-target-status">要變更的狀態</label>
                <select id="article-target-status" v-model="articleTargetStatus" class="form-select">
                  <option value="">請選擇要變更的狀態</option>
                  <option v-for="value in ADMIN_ARTICLE_STATUS_OPTIONS" :key="value" :value="value">
                    {{ statusLabel(ARTICLE_STATUS_MAP, value) }}
                  </option>
                </select>
              </div>
              <div class="form-check">
                <input
                  id="article-target-pinned"
                  v-model="articleTargetPinned"
                  class="form-check-input"
                  type="checkbox"
                />
                <label class="form-check-label" for="article-target-pinned">置頂</label>
              </div>
              <button
                class="btn btn-sm btn-primary"
                type="submit"
                :disabled="!articleTargetStatus || articleStatusSubmitting"
              >
                {{ articleStatusSubmitting ? '更新中...' : '送出變更' }}
              </button>
            </form>
          </div>
        </section>

        <section v-if="isCommentReport && reportedComment" class="card mb-4">
          <div class="card-body p-4">
            <h3 class="h5 fw-bold mb-3">被檢舉的留言</h3>

            <div class="reported-comment-card mb-3">
              <div class="d-flex align-items-center gap-2 mb-1">
                <span class="fw-semibold">{{ reportedComment.authorNickName }}（#{{ reportedComment.authorId }}）</span>
                <span class="badge" :class="statusBadgeClass(COMMENT_STATUS_MAP, reportedComment.status)">
                  {{ statusLabel(COMMENT_STATUS_MAP, reportedComment.status) }}
                </span>
              </div>
              <div class="comment-content">{{ reportedComment.content }}</div>
            </div>

            <div class="d-flex flex-wrap gap-2 mb-4">
              <button
                class="btn btn-sm btn-outline-warning"
                type="button"
                :disabled="commentActionBusy || reportedComment.status === 'HIDDEN'"
                @click="handleCommentStatusAction('HIDDEN')"
              >
                隱藏留言
              </button>
              <button
                class="btn btn-sm btn-outline-danger"
                type="button"
                :disabled="commentActionBusy || reportedComment.status === 'TAKEN_DOWN'"
                @click="handleCommentStatusAction('TAKEN_DOWN')"
              >
                下架留言
              </button>
              <button
                class="btn btn-sm btn-outline-success"
                type="button"
                :disabled="commentActionBusy || reportedComment.status === 'VISIBLE'"
                @click="handleCommentStatusAction('VISIBLE')"
              >
                恢復可見
              </button>
            </div>

            <h4 class="h6 fw-bold mb-2">所屬文章的其他留言</h4>
            <p v-if="!highlightedCommentVisibleInline" class="text-secondary small mb-2">
              此留言目前已下架，不會顯示在下方列表中。
            </p>
            <ul class="list-unstyled comment-list mb-0">
              <li
                v-for="comment in inlineComments"
                :key="comment.commentId"
                class="comment-item"
                :class="{ highlighted: comment.commentId === reportedComment.commentId }"
              >
                <div class="d-flex align-items-center gap-2 mb-1">
                  <span class="fw-semibold">{{ comment.authorNickName }}（#{{ comment.authorId }}）</span>
                  <span class="badge" :class="statusBadgeClass(COMMENT_STATUS_MAP, comment.status)">
                    {{ statusLabel(COMMENT_STATUS_MAP, comment.status) }}
                  </span>
                  <span v-if="comment.commentId === reportedComment.commentId" class="badge text-bg-danger">
                    被檢舉的留言
                  </span>
                </div>
                <div class="comment-content">{{ comment.content }}</div>
              </li>
              <li v-if="inlineComments.length === 0" class="text-secondary py-3">這篇文章目前沒有其他留言</li>
            </ul>
          </div>
        </section>

        <section class="card mb-4">
          <div class="card-body p-4">
            <h3 class="h5 fw-bold mb-3">變更檢舉處理狀態</h3>
            <form class="d-flex align-items-end gap-3" @submit.prevent="handleReportStatusSubmit">
              <div>
                <label class="form-label small text-secondary" for="target-report-status">處理狀態</label>
                <select id="target-report-status" v-model="targetReportStatus" class="form-select">
                  <option value="">請選擇要變更的狀態</option>
                  <option v-for="[value, meta] in Object.entries(REPORT_STATUS_MAP)" :key="value" :value="value">
                    {{ meta.label }}
                  </option>
                </select>
              </div>
              <button class="btn btn-primary" type="submit" :disabled="!targetReportStatus || reportSubmitting">
                {{ reportSubmitting ? '更新中...' : '送出變更' }}
              </button>
            </form>
          </div>
        </section>

        <div class="d-flex justify-content-end">
          <button class="btn btn-outline-secondary" type="button" @click="goBack">回到檢舉列表</button>
        </div>
      </template>
    </div>
  </main>
</template>

<style scoped>
.page-shell {
  max-width: 1000px;
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
.article-content {
  line-height: 1.7;
  white-space: pre-wrap;
}
.reported-comment-card {
  border: 1px solid #e8ebf0;
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px 16px;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.comment-item {
  border: 1px solid #e8ebf0;
  border-radius: 8px;
  padding: 10px 14px;
}
.comment-item.highlighted {
  border-color: #dc3545;
  background: #fff5f5;
}
.comment-content {
  white-space: pre-wrap;
  font-size: 0.9rem;
}
</style>
