<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getArticle,
  deleteArticle,
  likeArticle,
  unlikeArticle,
  addBookmark,
  removeBookmark,
  getFloors,
  createFloor,
  reportArticle,
  TEST_MEMBER_ID,
} from '../api'
import CommentSection from '../components/CommentSection.vue'
import MoreActionsMenu from '../components/MoreActionsMenu.vue'

const route = useRoute()
const router = useRouter()

// 對齊後端 CreateReportRequest 的 @Size(max = 250)，欄位是 NVARCHAR(250)，250 是字元數
const REPORT_MAX_LENGTH = 250

const article = ref(null)
const loading = ref(true)
const errorMessage = ref('')
// 讚/收藏等互動操作失敗的提示。跟 errorMessage 分開，因為 errorMessage 會蓋掉整頁
const interactionError = ref('')

const floors = ref([])
const floorContent = ref('')
const floorSubmitting = ref(false)
const floorErrorMessage = ref('')
const floorTextareaRef = ref(null)

// 首篇留言區預設展開；樓層的留言區預設收合，點該層的「留言 N」才展開
const commentsOpen = ref(true)
const expandedFloors = ref({})

// 一次只展開一份檢舉表單。null = 沒展開；有值 = 正在檢舉哪一篇
// （樓層本身也是一筆 article，所以主文章跟樓層可以共用同一組狀態與同一支 reportArticle API）
const reportingArticleId = ref(null)
const reportReason = ref('')
const reportSubmitting = ref(false)
const reportErrorMessage = ref('')
const reportSuccessMessage = ref('')
// 記住成功訊息屬於哪一篇，否則檢舉完某個樓層後訊息會顯示在錯的地方
const reportSuccessTargetId = ref(null)

const articleId = computed(() => route.params.articleId)
const isAuthor = computed(() => article.value?.authorId === TEST_MEMBER_ID)
// 只有根文章（不是樓層本身）才能被蓋樓
const canAddFloor = computed(() => !!article.value && article.value.parentArticleId == null)
const floorLocked = computed(
  () => article.value?.status === 'CLOSED' || article.value?.status === 'FORCE_CLOSED',
)

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

function autoResize(event) {
  const el = event.target
  // 先歸零才量得到內容真正需要的高度；height:auto 時 rows 屬性會決定最小高度，
  // 所以蓋樓框（rows=3）打第一個字時不會被縮成一行
  el.style.height = 'auto'
  // scrollHeight 不含 border，box-sizing: border-box 下直接套用會每次少掉 border 那幾 px
  const borderHeight = el.offsetHeight - el.clientHeight
  el.style.height = `${el.scrollHeight + borderHeight}px`
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    // 讚/收藏狀態已經跟著文章一起回來（likedByCurrentMember / bookmarkedByCurrentMember），
    // 不用再另外打 hasLikedArticle / hasBookmarked
    article.value = await getArticle(articleId.value)
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

function toggleFloorComments(floorId) {
  expandedFloors.value[floorId] = !expandedFloors.value[floorId]
}

// 讚/收藏失敗時的狀態校正：
// 後端在「已經按過讚了」「尚未按讚」「已經收藏過了」這幾種情況會回 409/404，
// 代表前端記錄的狀態跟資料庫不同步，這時反轉本地狀態剛好會校正回正確值。
// 但如果是斷線或 500，反轉只會把原本正確的畫面弄錯，所以只在這兩種狀態碼才校正。
function isStateDesyncError(error) {
  const status = error.response?.status
  return status === 409 || status === 404
}

// 樓層本身就是一篇 article，讚/收藏走的是同一組 API，只是帶該層自己的 articleId
async function toggleLikeOn(target) {
  interactionError.value = ''
  const wasLiked = target.likedByCurrentMember
  try {
    if (wasLiked) {
      await unlikeArticle(target.articleId)
      target.likedByCurrentMember = false
      target.likeCount -= 1
    } else {
      await likeArticle(target.articleId)
      target.likedByCurrentMember = true
      target.likeCount += 1
    }
  } catch (error) {
    if (isStateDesyncError(error)) {
      // 資料庫其實是相反的狀態，校正本地值與計數
      target.likedByCurrentMember = !wasLiked
      target.likeCount += wasLiked ? -1 : 1
    } else {
      interactionError.value = error.response
        ? `操作失敗：HTTP ${error.response.status}`
        : '無法連線到後端伺服器'
    }
  }
}

async function toggleBookmarkOn(target) {
  interactionError.value = ''
  const wasBookmarked = target.bookmarkedByCurrentMember
  try {
    if (wasBookmarked) {
      await removeBookmark(target.articleId)
      target.bookmarkedByCurrentMember = false
    } else {
      await addBookmark(target.articleId)
      target.bookmarkedByCurrentMember = true
    }
  } catch (error) {
    if (isStateDesyncError(error)) {
      target.bookmarkedByCurrentMember = !wasBookmarked
    } else {
      interactionError.value = error.response
        ? `操作失敗：HTTP ${error.response.status}`
        : '無法連線到後端伺服器'
    }
  }
}

async function handleCreateFloor() {
  if (!floorContent.value.trim()) return
  floorSubmitting.value = true
  floorErrorMessage.value = ''
  try {
    await createFloor(articleId.value, floorContent.value)
    floorContent.value = ''
    if (floorTextareaRef.value) floorTextareaRef.value.style.height = 'auto'
    await loadFloors()
  } catch (error) {
    const fieldError = error.response?.data?.errors?.[0]?.message
    floorErrorMessage.value = fieldError || '蓋樓失敗，請確認文章目前是否可以蓋樓'
  } finally {
    floorSubmitting.value = false
  }
}

function toggleReportForm(targetId) {
  reportSuccessMessage.value = ''
  reportSuccessTargetId.value = null
  reportErrorMessage.value = ''
  reportReason.value = ''
  reportingArticleId.value = reportingArticleId.value === targetId ? null : targetId
}

async function handleReportSubmit(targetId) {
  if (!reportReason.value.trim()) return
  reportSubmitting.value = true
  reportErrorMessage.value = ''
  try {
    await reportArticle(targetId, reportReason.value)
    reportingArticleId.value = null
    reportReason.value = ''
    reportSuccessMessage.value = '已送出檢舉'
    reportSuccessTargetId.value = targetId
  } catch (error) {
    // 兩種錯誤格式都要接：@Valid 失敗是 { errors: [...] }，
    // 後端商業規則（重複檢舉、檢舉自己的文章）走 ForumExceptionHandler，回的是 { errorCode, message }
    const data = error.response?.data
    reportErrorMessage.value =
      data?.errors?.[0]?.message || data?.message || '檢舉失敗，請稍後再試'
  } finally {
    reportSubmitting.value = false
  }
}

async function handleDelete() {
  if (!confirm('確定要刪除這篇文章嗎？')) return
  await deleteArticle(articleId.value)
  router.push({ name: 'forumList' })
}

// 樓層本身就是一篇 article，刪除走的是同一支軟刪除 API（狀態改成下架）
async function handleDeleteFloor(floorId) {
  if (!confirm('確定要刪除這個樓層嗎？')) return
  try {
    await deleteArticle(floorId)
    await loadFloors()
  } catch {
    floorErrorMessage.value = '刪除樓層失敗'
  }
}
</script>

<template>
  <main class="article-detail-page">
    <div class="page-shell mx-auto">
      <p v-if="loading" class="state-message">載入中...</p>

      <template v-else-if="article">
        <!-- 文章被遮蔽（隱藏/下架）：整頁只顯示提示，不顯示標題與內文 -->
        <section v-if="!article.visible" class="card masked-card">
          <i class="bi bi-eye-slash masked-icon"></i>
          <p class="masked-text">{{ article.visibilityMessage }}</p>
          <RouterLink :to="{ name: 'forumList' }" class="btn btn-outline-secondary btn-sm">
            返回討論區
          </RouterLink>
        </section>

        <template v-else>
          <RouterLink :to="{ name: 'forumList' }" class="back-link">
            <i class="bi bi-chevron-left"></i>返回討論區
          </RouterLink>

          <!-- 讚/收藏操作失敗的提示。不能併進 errorMessage，那個會把整頁換成錯誤畫面 -->
          <div
            v-if="interactionError"
            class="alert alert-warning alert-dismissible mt-3 mb-0"
            role="alert"
          >
            {{ interactionError }}
            <button
              class="btn-close"
              type="button"
              aria-label="關閉提示"
              @click="interactionError = ''"
            ></button>
          </div>

          <div class="row g-4">
            <!-- ──────── 主欄 ──────── -->
            <div class="col-lg-8">
              <!-- 首篇group：文章與它的留言在同一張卡片內，和下方樓層明確區隔 -->
              <section class="card op-card">
                <div class="card-body op-body">
                  <div class="badge-row">
                    <span class="tag tag-category">{{ article.categoryName }}</span>
                    <span v-if="article.isPinned" class="tag tag-pinned">
                      <i class="bi bi-pin-angle-fill"></i>置頂
                    </span>
                  </div>

                  <h1 class="op-title">{{ article.title }}</h1>

                  <div class="meta-row">
                    <span class="meta-author">
                      <span class="avatar avatar-sm">{{ initial(article.authorNickName) }}</span>
                      <span class="author-name">{{ article.authorNickName }}</span>
                    </span>
                    <span class="meta-item">
                      <i class="bi bi-clock"></i>{{ formatDateTime(article.articleCreatedTime) }}
                    </span>
                    <span v-if="article.articleUpdatedTime" class="meta-item">
                      <i class="bi bi-pencil"></i>{{ formatDateTime(article.articleUpdatedTime) }} 編輯
                    </span>
                    <span class="meta-item">
                      <i class="bi bi-eye"></i>{{ article.viewCount }}
                    </span>
                  </div>

                  <img v-if="article.coverImage" class="cover" :src="article.coverImage" alt="" />

                  <div class="op-content" v-html="article.content"></div>
                </div>

                <!-- 文章操作列 -->
                <div class="op-actions">
                  <button
                    type="button"
                    class="chip chip-lg"
                    :class="{ active: article.likedByCurrentMember }"
                    @click="toggleLikeOn(article)"
                  >
                    <i class="bi bi-hand-thumbs-up"></i>讚 {{ article.likeCount }}
                  </button>
                  <button
                    type="button"
                    class="chip chip-lg"
                    :class="{ active: article.bookmarkedByCurrentMember }"
                    @click="toggleBookmarkOn(article)"
                  >
                    <i :class="article.bookmarkedByCurrentMember ? 'bi bi-bookmark-fill' : 'bi bi-bookmark'"></i>
                    {{ article.bookmarkedByCurrentMember ? '已收藏' : '收藏' }}
                  </button>
                  <div class="ms-auto">
                    <MoreActionsMenu>
                      <template #default="{ close }">
                        <template v-if="isAuthor">
                          <RouterLink :to="{ name: 'forumEdit', params: { articleId } }">編輯</RouterLink>
                          <button type="button" class="danger" @click="close(); handleDelete()">
                            刪除
                          </button>
                        </template>
                        <button
                          v-else
                          type="button"
                          @click="close(); toggleReportForm(article.articleId)"
                        >
                          檢舉
                        </button>
                      </template>
                    </MoreActionsMenu>
                  </div>
                </div>

                <form
                  v-if="reportingArticleId === article.articleId"
                  class="report-form"
                  @submit.prevent="handleReportSubmit(article.articleId)"
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
                    <button type="button" class="cancel" @click="toggleReportForm(article.articleId)">
                      取消
                    </button>
                    <button type="submit" :disabled="reportSubmitting">
                      {{ reportSubmitting ? '送出中...' : '送出' }}
                    </button>
                  </div>
                  <p v-if="reportErrorMessage" class="error">{{ reportErrorMessage }}</p>
                </form>
                <p v-if="reportSuccessTargetId === article.articleId" class="report-success">
                  {{ reportSuccessMessage }}
                </p>

                <!-- 首篇留言group：同一張卡片內，用淡底色標示是附屬於本文的留言 -->
                <div class="comments-group">
                  <button type="button" class="comments-toggle" @click="commentsOpen = !commentsOpen">
                    <i class="bi bi-chat"></i>
                    <span>留言<span class="count">({{ article.commentCount }})</span></span>
                    <i class="bi ms-auto" :class="commentsOpen ? 'bi-chevron-down' : 'bi-chevron-right'"></i>
                  </button>
                  <CommentSection
                    v-if="commentsOpen"
                    :article-id="articleId"
                    :status="article.status"
                  />
                </div>
              </section>

              <!-- ──────── 樓層區：各自獨立卡片 ──────── -->
              <section
                v-for="floor in floors"
                :key="floor.articleId"
                class="card floor-card"
              >
                <div class="floor-head">
                  <span class="floor-badge">{{ floor.floorNumber }}樓</span>
                  <span class="author-name">{{ floor.authorNickName }}</span>
                  <span class="floor-time">{{ formatDateTime(floor.articleCreatedTime) }}</span>
                  <div class="ms-auto">
                    <MoreActionsMenu>
                      <template #default="{ close }">
                        <!-- 樓層沒有「編輯」：標題由後端自動生成、分類繼承樓主，套文章編輯頁語意不對 -->
                        <button
                          v-if="floor.authorId === TEST_MEMBER_ID"
                          type="button"
                          class="danger"
                          @click="close(); handleDeleteFloor(floor.articleId)"
                        >
                          刪除
                        </button>
                        <button v-else type="button" @click="close(); toggleReportForm(floor.articleId)">
                          檢舉
                        </button>
                      </template>
                    </MoreActionsMenu>
                  </div>
                </div>

                <div class="floor-body">
                  <!-- 被隱藏/下架的樓層：後端已把 content 遮蔽成 null，改顯示遮蔽訊息，
                       底下的留言區也一併不顯示（樓層都收回了，留言不該還看得到） -->
                  <p v-if="!floor.visible" class="floor-mask">{{ floor.visibilityMessage }}</p>
                  <template v-else>
                    <p class="floor-content">{{ floor.content }}</p>

                    <div class="floor-actions">
                      <button
                        type="button"
                        class="chip"
                        :class="{ active: floor.likedByCurrentMember }"
                        @click="toggleLikeOn(floor)"
                      >
                        <i class="bi bi-hand-thumbs-up"></i>{{ floor.likeCount }}
                      </button>
                      <button
                        type="button"
                        class="chip"
                        :class="{ active: floor.bookmarkedByCurrentMember }"
                        @click="toggleBookmarkOn(floor)"
                      >
                        <i :class="floor.bookmarkedByCurrentMember ? 'bi bi-bookmark-fill' : 'bi bi-bookmark'"></i>
                        {{ floor.bookmarkedByCurrentMember ? '已收藏' : '收藏' }}
                      </button>
                      <button
                        type="button"
                        class="chip"
                        :class="{ active: expandedFloors[floor.articleId] }"
                        @click="toggleFloorComments(floor.articleId)"
                      >
                        <i class="bi bi-chat"></i>留言 {{ floor.commentCount }}
                      </button>
                    </div>

                    <form
                      v-if="reportingArticleId === floor.articleId"
                      class="report-form"
                      @submit.prevent="handleReportSubmit(floor.articleId)"
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
                        <button type="button" class="cancel" @click="toggleReportForm(floor.articleId)">
                          取消
                        </button>
                        <button type="submit" :disabled="reportSubmitting">
                          {{ reportSubmitting ? '送出中...' : '送出' }}
                        </button>
                      </div>
                      <p v-if="reportErrorMessage" class="error">{{ reportErrorMessage }}</p>
                    </form>
                    <p v-if="reportSuccessTargetId === floor.articleId" class="report-success">
                      {{ reportSuccessMessage }}
                    </p>
                  </template>
                </div>

                <div v-if="floor.visible && expandedFloors[floor.articleId]" class="floor-comments">
                  <CommentSection :article-id="floor.articleId" :status="floor.status" />
                </div>
              </section>

              <!-- 蓋樓輸入 -->
              <section v-if="canAddFloor" class="card floor-composer">
                <p v-if="floorLocked" class="locked-message">
                  <i class="bi bi-lock"></i>蓋樓功能已關閉
                </p>
                <form v-else class="floor-form" @submit.prevent="handleCreateFloor">
                  <span class="avatar avatar-md">{{ initial('我') }}</span>
                  <div class="floor-form-main">
                    <textarea
                      ref="floorTextareaRef"
                      v-model="floorContent"
                      rows="3"
                      placeholder="回覆這篇文章（蓋樓）..."
                      @input="autoResize"
                    />
                    <div class="form-footer">
                      <button type="submit" class="submit-btn" :disabled="floorSubmitting">
                        {{ floorSubmitting ? '送出中...' : '送出' }}
                      </button>
                    </div>
                    <p v-if="floorErrorMessage" class="error">{{ floorErrorMessage }}</p>
                  </div>
                </form>
              </section>
            </div>

            <!-- ──────── 側欄 ──────── -->
            <div class="col-lg-4">
              <section class="card author-card">
                <span class="avatar avatar-lg">{{ initial(article.authorNickName) }}</span>
                <span class="author-card-name">{{ article.authorNickName }}</span>
                <!-- TODO: 後端還沒有「作者文章數」端點，也還沒有作者文章列表頁可以連，
                     等這兩件事補上再把篇數與連結接上，先不顯示假資料 -->
              </section>
            </div>
          </div>
        </template>
      </template>

      <p v-else class="state-message">{{ errorMessage || '文章不存在或已被下架' }}</p>
    </div>
  </main>
</template>

<style scoped>
/* ── 版面骨架：比照 ProductDetailView 的 page-shell，寬度對齊 ClientNavBar 的 1200px header ── */
.article-detail-page {
  background-color: #ffffff;
  padding: 24px 20px 40px;
}

.page-shell {
  max-width: 1200px;
}

.state-message {
  text-align: center;
  color: #999999;
  padding: 60px 0;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #6c757d;
  text-decoration: none;
}

.back-link:hover {
  color: #2b77c5;
}

/* ── 共用：卡片、頭像、chip ── */
.card {
  background-color: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.175);
  border-radius: 0.75rem;
  overflow: hidden;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 50%;
  background-color: #e5e9f0;
  color: #1d324b;
  font-weight: 700;
}

.avatar-sm {
  width: 26px;
  height: 26px;
  font-size: 12px;
}

.avatar-md {
  width: 34px;
  height: 34px;
  font-size: 13px;
  background-color: #2b77c5;
  color: #ffffff;
}

.avatar-lg {
  width: 60px;
  height: 60px;
  font-size: 24px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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

.chip-lg {
  padding: 8px 16px;
  font-size: 14px;
  color: #495057;
}

/* ── 遮蔽狀態 ── */
.masked-card {
  padding: 48px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.masked-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #f1f3f5;
  color: #adb5bd;
  font-size: 24px;
}

.masked-text {
  margin: 0;
  font-size: 15px;
  color: #6c757d;
}

/* ── 首篇group ── */
.op-card {
  margin-bottom: 24px;
}

.op-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.35em 0.65em;
  border-radius: 50rem;
  font-size: 12px;
  font-weight: 600;
}

.tag-category {
  background-color: #eaf2fb;
  color: #2b77c5;
}

.tag-pinned {
  background-color: #fdefe9;
  color: #e05a2b;
}

.op-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.35;
  color: #1d324b;
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eaeaea;
  font-size: 13px;
  color: #888888;
}

.meta-author {
  display: flex;
  align-items: center;
  gap: 7px;
}

.author-name {
  font-weight: 600;
  color: #333333;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.cover {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  border-radius: 0.5rem;
}

.op-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333333;
  white-space: pre-wrap;
}

.op-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid #eaeaea;
}

/* ── 首篇留言group：淡底色標示附屬於本文 ── */
.comments-group {
  background-color: #fbfcfd;
  border-top: 1px solid #eaeaea;
}

.comments-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 14px 24px;
  border: 0;
  background: none;
  font-size: 16px;
  font-weight: 600;
  color: #1d324b;
  text-align: left;
  cursor: pointer;
}

.comments-toggle:hover {
  background-color: #f2f6fb;
}

.comments-toggle .count {
  color: #888888;
  font-weight: 400;
}

/* ── 樓層 ── */
.floor-card {
  margin-bottom: 16px;
}

.floor-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e5e9f0;
  font-size: 12px;
  color: #888888;
}

.floor-head .author-name {
  font-size: 13px;
}

.floor-badge {
  padding: 3px 11px;
  border-radius: 50rem;
  background-color: #2b77c5;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
}

.floor-body {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.floor-content {
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: #333333;
  white-space: pre-wrap;
}

/* 被隱藏/下架的樓層，沿用留言遮蔽的視覺語彙（灰底虛線） */
.floor-mask {
  margin: 0;
  padding: 12px;
  background-color: #f3f3f3;
  border: 1px dashed #cccccc;
  border-radius: 0.375rem;
  color: #888888;
  font-size: 13px;
  text-align: center;
}

.floor-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.floor-comments {
  padding: 0 20px 8px;
  border-top: 1px solid #f0f0f0;
  background-color: #fbfcfd;
}

/* ── 蓋樓輸入 ── */
.floor-composer {
  padding: 20px;
}

.floor-form {
  display: flex;
  gap: 12px;
}

.floor-form-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.floor-form textarea {
  padding: 12px;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  font-family: inherit;
  font-size: 14px;
  resize: none;
  overflow: hidden;
}

.floor-form .form-footer {
  display: flex;
  justify-content: flex-end;
}

.submit-btn {
  padding: 8px 20px;
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
  padding: 16px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 0.375rem;
  color: #6c757d;
  font-size: 13px;
}

/* ── 側欄作者卡 ── */
.author-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.author-card-name {
  font-size: 16px;
  font-weight: 600;
  color: #1d324b;
}

/* ── 檢舉表單（沿用既有視覺語彙） ── */
.report-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0 24px 16px;
  padding: 8px;
  background-color: #fffaf0;
  border: 1px solid #e8d3a0;
  border-radius: 0.375rem;
}

.floor-body .report-form {
  margin: 0;
}

.report-form textarea {
  padding: 8px;
  border: 1px solid #d0d0d0;
  border-radius: 0.375rem;
  font-family: inherit;
  resize: none;
  overflow: hidden;
}

.report-form .form-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.char-count {
  font-size: 12px;
  color: #999999;
}

/* footer 已經是 flex-end + gap，字數靠 margin-right:auto 吃掉剩餘空間推到最左邊 */
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
  margin: 0 24px 16px;
  color: #1e7e34;
  font-size: 13px;
}

.floor-body .report-success {
  margin: 0;
}

.error {
  margin: 0;
  color: #c0392b;
  font-size: 13px;
}
</style>
