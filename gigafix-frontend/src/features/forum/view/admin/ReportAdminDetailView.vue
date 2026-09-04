<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getReportForAdmin,
  getArticleForAdmin,
  getCommentForAdmin,
  getArticleCommentsForAdmin,
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
import { sanitizeHtml } from '../../htmlContent'

const route = useRoute()
const router = useRouter()
const reportId = route.params.reportId

// 預設在被檢舉留言之前保留幾則當上下文，其餘收在「載入前面 N 則」後面
const CONTEXT_BEFORE = 2

const report = ref(null)
const article = ref(null)
const reportedComment = ref(null)
// 後台留言串：含已下架的留言，前台端點撈不到這些
const comments = ref([])

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const commentActionBusy = ref(false)
const reportSubmitting = ref(false)
const targetReportStatus = ref('')

const articleTargetStatus = ref('')
const articleStatusSubmitting = ref(false)

// 留言串顯示控制（全部在前端切，後端沒有分頁）
const showAllComments = ref(false)
const onlyShowReported = ref(false)
const commentsExpanded = ref(false)

const isCommentReport = computed(() => report.value?.commentId != null)

const canChangeArticleStatus = computed(
  () => !!article.value && ADMIN_ARTICLE_SOURCE_STATUSES.includes(article.value.status),
)

// 每種處理狀態對應一句說明，讓管理員知道現在這件檢舉處在什麼位置
const REPORT_STATUS_HINTS = {
  PENDING: { text: '尚未有人處分或駁回，這件檢舉會留在待處理佇列。', color: '#ffc107' },
  RESOLVED: {
    text: '已由管理員處理完畢，不再出現在待處理佇列。仍可再改回待處理或關閉。',
    color: '#198754',
  },
  CLOSED: { text: '已關閉、不再處理。仍可改回待處理重新審視。', color: '#6c757d' },
}
const reportStatusHint = computed(() => REPORT_STATUS_HINTS[report.value?.status] ?? null)

// 留言串統計：總則數與各狀態則數，都依後台完整清單計算（含已下架）
const commentStats = computed(() => ({
  total: comments.value.length,
  takenDown: comments.value.filter((c) => c.status === 'TAKEN_DOWN').length,
  hidden: comments.value.filter((c) => c.status === 'HIDDEN').length,
}))

const reportedIndex = computed(() =>
  reportedComment.value
    ? comments.value.findIndex((c) => c.commentId === reportedComment.value.commentId)
    : -1,
)

// 預設從「被檢舉留言往前 CONTEXT_BEFORE 則」開始顯示；找不到被檢舉留言時退回顯示全部
const windowStart = computed(() => {
  if (showAllComments.value || reportedIndex.value < 0) return 0
  return Math.max(0, reportedIndex.value - CONTEXT_BEFORE)
})

const visibleComments = computed(() => {
  if (onlyShowReported.value) {
    return reportedIndex.value >= 0 ? [comments.value[reportedIndex.value]] : []
  }
  return comments.value.slice(windowStart.value)
})

function formatDate(value) {
  return value?.slice(0, 19)?.replace('T', ' ') ?? '-'
}

function isReported(comment) {
  return reportedComment.value != null && comment.commentId === reportedComment.value.commentId
}

function describeError(error, fallbackAction) {
  if (error.response?.status === 404) return `找不到 ID 為 ${reportId} 的檢舉`
  const message = error.response?.data?.message
  if (message) return message
  return error.response ? `${fallbackAction}：HTTP ${error.response.status}` : '無法連線到後端伺服器'
}

async function fetchAll() {
  loading.value = true
  errorMessage.value = ''
  article.value = null
  reportedComment.value = null
  comments.value = []

  try {
    report.value = await getReportForAdmin(reportId)

    if (report.value.articleId != null) {
      // 檢舉文章：文章與留言串沒有相依關係，可以併發
      const [articleData, commentData] = await Promise.all([
        getArticleForAdmin(report.value.articleId),
        getArticleCommentsForAdmin(report.value.articleId),
      ])
      article.value = articleData
      comments.value = commentData
    } else if (report.value.commentId != null) {
      // 檢舉留言：要先拿到留言才知道它屬於哪篇文章，後兩支才能併發
      reportedComment.value = await getCommentForAdmin(report.value.commentId)
      const [articleData, commentData] = await Promise.all([
        getArticleForAdmin(reportedComment.value.articleId),
        getArticleCommentsForAdmin(reportedComment.value.articleId),
      ])
      article.value = articleData
      comments.value = commentData
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = describeError(error, '讀取檢舉失敗')
  } finally {
    loading.value = false
  }
}

// 處分留言後：留言本身、整串、以及文章的留言數都會變，三個都要重抓
async function refreshAfterCommentAction() {
  if (!reportedComment.value) return
  const [commentData, listData, articleData] = await Promise.all([
    getCommentForAdmin(reportedComment.value.commentId),
    getArticleCommentsForAdmin(reportedComment.value.articleId),
    getArticleForAdmin(reportedComment.value.articleId),
  ])
  reportedComment.value = commentData
  comments.value = listData
  article.value = articleData
}

async function handleCommentStatusAction(status) {
  commentActionBusy.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await updateCommentStatus(reportedComment.value.commentId, status)
    await refreshAfterCommentAction()
    successMessage.value = '留言狀態已更新'
  } catch (error) {
    console.error(error)
    errorMessage.value = describeError(error, '留言狀態更新失敗')
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
    // 刻意不帶 isPinned：後端只在該欄位非 null 時才覆寫置頂，不帶就等於維持文章原本的置頂設定
    article.value = await updateArticleStatus(article.value.articleId, {
      status: articleTargetStatus.value,
    })
    articleTargetStatus.value = ''
    successMessage.value = '文章狀態已更新'
  } catch (error) {
    console.error(error)
    errorMessage.value = describeError(error, '文章狀態更新失敗')
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
    errorMessage.value = describeError(error, '檢舉狀態更新失敗')
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
  <main class="report-detail-page">
    <div class="page-shell mx-auto">
      <header class="mb-3">
        <button type="button" class="back-link" @click="goBack">← 論壇檢舉管理</button>
        <h1 class="page-title">檢舉詳情</h1>
      </header>

      <div v-if="successMessage" class="alert alert-success alert-dismissible" role="alert">
        {{ successMessage }}
        <button class="btn-close" type="button" aria-label="關閉成功訊息" @click="successMessage = ''"></button>
      </div>
      <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

      <div v-if="loading" class="panel">
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <div class="text-secondary mt-3">正在讀取檢舉資料...</div>
        </div>
      </div>

      <div v-else-if="report" class="report-grid">
        <!-- ═══════════ 左欄：檢舉資訊與審核動作，捲動時固定 ═══════════ -->
        <div class="side-column">
          <section class="panel">
            <div class="d-flex align-items-center gap-2 flex-wrap mb-3">
              <span class="panel-heading">檢舉 #{{ report.reportId }}</span>
              <span class="badge" :class="statusBadgeClass(REPORT_STATUS_MAP, report.status)">
                {{ statusLabel(REPORT_STATUS_MAP, report.status) }}
              </span>
            </div>

            <p
              v-if="reportStatusHint"
              class="status-hint"
              :style="{ borderLeftColor: reportStatusHint.color }"
            >
              {{ reportStatusHint.text }}
            </p>

            <div class="field-list">
              <div class="field">
                <span class="field-label">檢舉類型</span>
                <span class="field-value">{{ isCommentReport ? '留言' : '文章' }}</span>
              </div>
              <div class="field">
                <span class="field-label">檢舉人</span>
                <span class="field-value">{{ report.reporterNickName }}（會員 #{{ report.reporterId }}）</span>
              </div>
              <div class="field">
                <span class="field-label">檢舉原因</span>
                <span class="field-value reason">{{ report.reason }}</span>
              </div>
              <div class="field">
                <span class="field-label">檢舉時間</span>
                <span class="field-value">{{ formatDate(report.reportCreatedTime) }}</span>
              </div>
            </div>
          </section>

          <section class="panel">
            <span class="panel-heading d-block mb-3">審核動作</span>

            <!-- 處分被檢舉的留言（僅檢舉留言時出現） -->
            <template v-if="isCommentReport && reportedComment">
              <div class="action-block">
                <div class="d-flex align-items-center gap-2">
                  <span class="action-title">處分被檢舉的留言</span>
                  <span class="text-muted-sm">#{{ reportedComment.commentId }}</span>
                </div>
                <div class="text-muted-sm">
                  目前狀態：
                  <span class="badge" :class="statusBadgeClass(COMMENT_STATUS_MAP, reportedComment.status)">
                    {{ statusLabel(COMMENT_STATUS_MAP, reportedComment.status) }}
                  </span>
                </div>
                <div class="d-flex flex-wrap gap-2">
                  <button
                    class="btn btn-sm btn-outline-warning"
                    type="button"
                    :disabled="commentActionBusy || reportedComment.status === 'HIDDEN'"
                    @click="handleCommentStatusAction('HIDDEN')"
                  >
                    隱藏
                  </button>
                  <button
                    class="btn btn-sm btn-outline-danger"
                    type="button"
                    :disabled="commentActionBusy || reportedComment.status === 'TAKEN_DOWN'"
                    @click="handleCommentStatusAction('TAKEN_DOWN')"
                  >
                    下架
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
              </div>
            </template>

            <!-- 處分文章：只在「檢舉文章」時出現。檢舉留言時不提供連帶處分文章的入口，
                 要處分文章請從右欄的「開啟文章管理頁」進去 -->
            <div v-if="!isCommentReport && article" class="action-block">
              <div class="d-flex align-items-center gap-2">
                <span class="action-title">處分被檢舉的文章</span>
                <span class="text-muted-sm">#{{ article.articleId }}</span>
              </div>
              <div class="text-muted-sm">
                目前狀態：
                <span class="badge" :class="statusBadgeClass(ARTICLE_STATUS_MAP, article.status)">
                  {{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}
                </span>
              </div>

              <p v-if="!canChangeArticleStatus" class="text-muted-sm mb-0">
                此文章目前狀態為「{{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}」，管理員無法變更狀態。
              </p>
              <form v-else class="d-flex flex-column gap-2" @submit.prevent="handleArticleStatusSubmit">
                <select v-model="articleTargetStatus" class="form-select form-select-sm" aria-label="要變更的文章狀態">
                  <option value="">不變更文章狀態</option>
                  <option v-for="value in ADMIN_ARTICLE_STATUS_OPTIONS" :key="value" :value="value">
                    {{ statusLabel(ARTICLE_STATUS_MAP, value) }}
                  </option>
                </select>
                <button
                  class="btn btn-sm btn-outline-primary align-self-start"
                  type="submit"
                  :disabled="!articleTargetStatus || articleStatusSubmitting"
                >
                  {{ articleStatusSubmitting ? '更新中...' : '送出變更' }}
                </button>
              </form>
            </div>

            <!-- 上方處分區（留言或文章）有渲染時才需要分隔線，避免資料載入失敗時出現孤立的線 -->
            <hr v-if="isCommentReport ? !!reportedComment : !!article" class="action-divider" />

            <!-- 結案 -->
            <div class="action-block">
              <span class="action-title">變更處理狀態</span>
              <div class="text-muted-sm">
                目前：
                <span class="badge" :class="statusBadgeClass(REPORT_STATUS_MAP, report.status)">
                  {{ statusLabel(REPORT_STATUS_MAP, report.status) }}
                </span>
              </div>
              <form class="d-flex flex-column gap-2" @submit.prevent="handleReportStatusSubmit">
                <select v-model="targetReportStatus" class="form-select form-select-sm" aria-label="要變更的處理狀態">
                  <option value="">請選擇要變更的狀態</option>
                  <option v-for="[value, meta] in Object.entries(REPORT_STATUS_MAP)" :key="value" :value="value">
                    {{ meta.label }}
                  </option>
                </select>
                <button
                  class="btn btn-primary btn-sm align-self-start"
                  type="submit"
                  :disabled="!targetReportStatus || reportSubmitting"
                >
                  {{ reportSubmitting ? '更新中...' : '送出變更' }}
                </button>
              </form>
            </div>
          </section>
        </div>

        <!-- ═══════════ 右欄：證據 ═══════════ -->
        <section v-if="article" class="panel evidence">
          <!-- 檢舉文章時，頂部標明文章本身就是被檢舉的對象 -->
          <div v-if="!isCommentReport" class="reported-banner">
            <i class="bi bi-flag-fill"></i>
            <span>被檢舉的文章</span>
          </div>

          <div class="evidence-article">
            <div class="d-flex align-items-center gap-2 flex-wrap">
              <span v-if="isCommentReport" class="section-label">留言所屬的文章</span>
              <span class="badge" :class="statusBadgeClass(ARTICLE_STATUS_MAP, article.status)">
                {{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}
              </span>
              <button type="button" class="ms-auto link-button" @click="goToArticleDetail">
                開啟文章管理頁
              </button>
            </div>

            <h2 class="article-title">{{ article.title || '（無標題）' }}</h2>

            <div class="article-meta">
              文章 #{{ article.articleId }} · {{ article.authorNickName }}（#{{ article.authorId }}） ·
              {{ article.categoryName }} · {{ formatDate(article.articleCreatedTime) }}
            </div>

            <div v-if="!isCommentReport" class="article-stats">
              <span>瀏覽 {{ article.viewCount }}</span>
              <span>讚 {{ article.likeCount }}</span>
              <span>留言 {{ commentStats.total }}</span>
              <span>樓層 {{ article.floorCount }}</span>
            </div>

            <!-- 檢舉留言時文章內文一樣要顯示，那是判斷留言是否違規的上下文 -->
            <div class="article-content" v-html="sanitizeHtml(article.content)"></div>
          </div>

          <!-- 檢舉文章：留言串預設收合 -->
          <template v-if="!isCommentReport">
            <div v-if="!commentsExpanded" class="collapsed-comments">
              <span>這篇文章有 {{ commentStats.total }} 則留言</span>
              <span class="text-faint-sm">檢舉的是文章本身，留言預設收合</span>
              <button type="button" class="btn btn-sm btn-outline-secondary ms-auto" @click="commentsExpanded = true">
                展開留言串
              </button>
            </div>
            <template v-else>
              <div class="thread-toolbar">
                <span class="section-label">留言串</span>
                <span>共 {{ commentStats.total }} 則</span>
                <span v-if="commentStats.takenDown || commentStats.hidden" class="text-muted-sm">
                  （{{ commentStats.takenDown }} 則已下架、{{ commentStats.hidden }} 則已隱藏）
                </span>
                <button type="button" class="btn btn-sm btn-outline-secondary ms-auto" @click="commentsExpanded = false">
                  收合
                </button>
              </div>
              <ul class="thread">
                <li v-for="comment in comments" :key="comment.commentId" class="thread-item">
                  <div class="thread-head">
                    <span class="thread-author">{{ comment.authorNickName }}（#{{ comment.authorId }}）</span>
                    <span
                      v-if="comment.status !== 'VISIBLE'"
                      class="badge"
                      :class="statusBadgeClass(COMMENT_STATUS_MAP, comment.status)"
                    >
                      {{ statusLabel(COMMENT_STATUS_MAP, comment.status) }}
                    </span>
                    <span class="text-muted-sm">留言 #{{ comment.commentId }} · {{ formatDate(comment.commentCreatedTime) }}</span>
                  </div>
                  <div class="thread-content">{{ comment.content }}</div>
                </li>
                <li v-if="comments.length === 0" class="thread-empty">這篇文章目前沒有留言</li>
              </ul>
            </template>
          </template>

          <!-- 檢舉留言：留言串展開，並定位到被檢舉的那則 -->
          <template v-else>
            <div class="thread-toolbar">
              <span class="section-label">留言串</span>
              <span>共 {{ commentStats.total }} 則</span>
              <span v-if="commentStats.takenDown || commentStats.hidden" class="text-muted-sm">
                （{{ commentStats.takenDown }} 則已下架、{{ commentStats.hidden }} 則已隱藏）
              </span>
              <button
                type="button"
                class="btn btn-sm ms-auto"
                :class="onlyShowReported ? 'btn-secondary' : 'btn-outline-secondary'"
                @click="onlyShowReported = !onlyShowReported"
              >
                只看被檢舉的
              </button>
            </div>

            <ul class="thread">
              <li v-if="!onlyShowReported && windowStart > 0" class="thread-more">
                <button type="button" class="btn btn-sm btn-outline-secondary" @click="showAllComments = true">
                  載入前面 {{ windowStart }} 則留言
                </button>
              </li>

              <li
                v-for="comment in visibleComments"
                :key="comment.commentId"
                class="thread-item"
                :class="{ reported: isReported(comment) }"
              >
                <div class="thread-head">
                  <span v-if="isReported(comment)" class="badge text-bg-danger">被檢舉</span>
                  <span class="thread-author">{{ comment.authorNickName }}（#{{ comment.authorId }}）</span>
                  <span
                    v-if="isReported(comment) || comment.status !== 'VISIBLE'"
                    class="badge"
                    :class="statusBadgeClass(COMMENT_STATUS_MAP, comment.status)"
                  >
                    {{ statusLabel(COMMENT_STATUS_MAP, comment.status) }}
                  </span>
                  <span class="text-muted-sm">留言 #{{ comment.commentId }} · {{ formatDate(comment.commentCreatedTime) }}</span>
                </div>
                <div class="thread-content">{{ comment.content }}</div>
              </li>

              <li v-if="comments.length === 0" class="thread-empty">這篇文章目前沒有留言</li>
            </ul>
          </template>
        </section>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ── 版面 ── */
.report-detail-page {
  min-height: 100vh;
  background: #f8f9fc;
  padding: 24px;
  color: #5a5c69;
}

.page-shell {
  max-width: 1400px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #3a3b45;
}

.back-link {
  padding: 0;
  border: 0;
  background: none;
  color: #0d6efd;
  font-size: 13px;
  cursor: pointer;
}

.back-link:hover {
  text-decoration: underline;
}

.report-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

/* AdminLayout 的 .content-body 是捲動容器，sticky 以它為基準 */
.side-column {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 991.98px) {
  .report-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .side-column {
    position: static;
  }
}

/* ── 卡片 ── */
.panel {
  background: #ffffff;
  border: 1px solid #e3e6f0;
  border-radius: 6px;
  padding: 18px;
}

.panel-heading {
  font-size: 17px;
  font-weight: 700;
  color: #3a3b45;
}

.status-hint {
  margin: 0 0 14px;
  padding: 8px 10px;
  background: #f8f9fc;
  border-left: 3px solid #b7b9cc;
  border-radius: 0 4px 4px 0;
  font-size: 12px;
  line-height: 1.6;
  color: #858796;
}

/* ── 檢舉欄位 ── */
.field-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.field-label {
  font-size: 12px;
  color: #858796;
}

.field-value {
  font-size: 14px;
  color: #3a3b45;
}

.field-value.reason {
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* ── 審核動作 ── */
.action-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-title {
  font-size: 12px;
  font-weight: 600;
  color: #5a5c69;
}

.action-divider {
  margin: 16px 0;
  border: 0;
  border-top: 1px solid #e8ebf0;
  opacity: 1;
}

.text-muted-sm {
  font-size: 12px;
  color: #858796;
}

.text-faint-sm {
  font-size: 11px;
  color: #b7b9cc;
}

/* ── 右欄證據 ── */
.evidence {
  padding: 0;
  overflow: hidden;
}

.reported-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #fff5f5;
  border-bottom: 1px solid #f5c2c7;
  font-size: 13px;
  font-weight: 700;
  color: #dc3545;
}

.evidence-article {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-bottom: 1px solid #e8ebf0;
}

.section-label {
  font-size: 11px;
  letter-spacing: 0.06em;
  font-weight: 700;
  color: #858796;
}

.link-button {
  padding: 0;
  border: 0;
  background: none;
  color: #0d6efd;
  font-size: 13px;
  cursor: pointer;
}

.link-button:hover {
  text-decoration: underline;
}

.article-title {
  margin: 2px 0 0;
  font-size: 20px;
  font-weight: 700;
  color: #3a3b45;
}

.article-meta {
  font-size: 12px;
  color: #858796;
}

.article-stats {
  display: flex;
  gap: 14px;
  padding-bottom: 4px;
  border-bottom: 1px solid #e8ebf0;
  font-size: 12px;
  color: #858796;
}

.article-content {
  margin-top: 4px;
  font-size: 15px;
  line-height: 1.8;
  color: #5a5c69;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* ── 留言串 ── */
.collapsed-comments {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 16px 20px;
  padding: 12px 14px;
  background: #f8f9fc;
  border: 1px dashed #d1d3e2;
  border-radius: 6px;
  font-size: 13px;
}

.thread-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e8ebf0;
  font-size: 13px;
}

.thread {
  list-style: none;
  margin: 0;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.thread-more {
  align-self: center;
}

.thread-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  border: 1px solid #e8ebf0;
  border-radius: 6px;
}

.thread-item.reported {
  padding: 14px;
  border: 2px solid #dc3545;
  background: #fff5f5;
}

.thread-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.thread-author {
  font-size: 13px;
  font-weight: 600;
  color: #5a5c69;
}

.thread-item.reported .thread-author {
  color: #3a3b45;
}

.thread-content {
  font-size: 14px;
  line-height: 1.7;
  color: #5a5c69;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.thread-item.reported .thread-content {
  font-size: 15px;
  color: #3a3b45;
}

.thread-empty {
  padding: 12px 0;
  text-align: center;
  font-size: 13px;
  color: #858796;
}
</style>
