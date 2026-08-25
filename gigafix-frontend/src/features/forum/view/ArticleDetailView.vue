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
const liked = ref(false)
const bookmarked = ref(false)
const loading = ref(true)
const errorMessage = ref('')

const floors = ref([])
const floorContent = ref('')
const floorSubmitting = ref(false)
const floorErrorMessage = ref('')
const floorTextareaRef = ref(null)

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
    const fieldError = error.response?.data?.errors?.[0]?.message
    reportErrorMessage.value = fieldError || '檢舉失敗，請稍後再試'
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
  <div class="article-detail-view">
    <p v-if="loading">載入中...</p>
    <template v-else-if="article">
      <p v-if="!article.visible" class="empty">{{ article.visibilityMessage }}</p>
      <template v-else>
        <div class="header">
          <span class="category">{{ article.categoryName }}</span>
          <h1>{{ article.title }}</h1>
          <div class="meta">
            <span>{{ article.authorNickName }}</span>
            <span>{{ new Date(article.articleCreatedTime).toLocaleString() }}</span>
            <span :title="`瀏覽數: ${article.viewCount}`">👁 {{ article.viewCount }}</span>
            <span>
              💬
              <span :title="`蓋樓數: ${article.floorCount}`">{{ article.floorCount }}</span
              >/<span :title="`留言數: ${article.commentCount}`">{{ article.commentCount }}</span>
            </span>
          </div>
        </div>

        <img v-if="article.coverImage" class="cover" :src="article.coverImage" alt="" />

        <div class="content" v-html="article.content"></div>

        <div class="actions">
          <button type="button" @click="toggleLike">👍 讚 {{ article.likeCount }}</button>
          <button type="button" @click="toggleBookmark">
            {{ bookmarked ? '★ 已收藏' : '☆ 收藏' }}
          </button>
          <MoreActionsMenu>
            <template #default="{ close }">
              <template v-if="isAuthor">
                <RouterLink :to="{ name: 'forumEdit', params: { articleId } }">編輯</RouterLink>
                <button type="button" class="danger" @click="close(); handleDelete()">刪除</button>
              </template>
              <button v-else type="button" @click="close(); toggleReportForm(article.articleId)">
                檢舉
              </button>
            </template>
          </MoreActionsMenu>
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
            <button type="button" class="cancel" @click="toggleReportForm(article.articleId)">取消</button>
            <button type="submit" :disabled="reportSubmitting">
              {{ reportSubmitting ? '送出中...' : '送出' }}
            </button>
          </div>
          <p v-if="reportErrorMessage" class="error">{{ reportErrorMessage }}</p>
        </form>
        <p v-if="reportSuccessTargetId === article.articleId" class="report-success">
          {{ reportSuccessMessage }}
        </p>

        <CommentSection :article-id="articleId" :status="article.status" />

        <div v-for="floor in floors" :key="floor.articleId" class="floor-block">
          <div class="floor-header">
            <span class="floor-badge">{{ floor.floorNumber }}樓</span>
            <span class="author">{{ floor.authorNickName }}</span>
            <span class="time">{{ new Date(floor.articleCreatedTime).toLocaleString() }}</span>
          </div>

          <!-- 被隱藏/下架的樓層：後端已把 content 遮蔽成 null，改顯示遮蔽訊息，
               底下的留言區也一併不顯示（樓層都收回了，留言不該還看得到） -->
          <p v-if="!floor.visible" class="floor-mask">{{ floor.visibilityMessage }}</p>
          <template v-else>
            <p class="floor-content">{{ floor.content }}</p>

            <div class="floor-actions">
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

            <CommentSection :article-id="floor.articleId" :status="floor.status" />
          </template>
        </div>

        <section v-if="canAddFloor" class="floor-form-section">
          <p v-if="floorLocked" class="locked-message">蓋樓功能已關閉</p>
          <form v-else class="floor-form" @submit.prevent="handleCreateFloor">
            <textarea
              ref="floorTextareaRef"
              v-model="floorContent"
              rows="3"
              placeholder="回覆這篇文章（蓋樓）..."
              @input="autoResize"
            />
            <div class="form-footer">
              <button type="submit" :disabled="floorSubmitting">
                {{ floorSubmitting ? '送出中...' : '送出' }}
              </button>
            </div>
            <p v-if="floorErrorMessage" class="error">{{ floorErrorMessage }}</p>
          </form>
        </section>
      </template>
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

/* 用子代選擇器，避免這條規則透過 slot 蓋掉 ⋮ 選單內的選項樣式 */
.actions > button,
.actions > a {
  padding: 6px 14px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background-color: #ffffff;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  font-size: 14px;
}

.empty {
  text-align: center;
  color: #999999;
  padding: 60px 0;
}

.report-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
  padding: 8px;
  background-color: #fffaf0;
  border: 1px solid #e8d3a0;
  border-radius: 4px;
}

.report-form textarea {
  padding: 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-family: inherit;
  resize: none;
  overflow: hidden;
}

.report-form .form-footer {
  display: flex;
  justify-content: flex-end;
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
  border-radius: 4px;
  cursor: pointer;
}

.report-form button[type='submit'] {
  padding: 4px 12px;
  font-size: 13px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.report-form button[type='submit']:disabled {
  opacity: 0.6;
  cursor: default;
}

.report-success {
  margin: 8px 0 0;
  color: #1e7e34;
  font-size: 13px;
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

/* 被隱藏/下架的樓層，沿用留言遮蔽的視覺語彙（灰底虛線） */
.floor-mask {
  margin: 0;
  padding: 8px;
  background-color: #f3f3f3;
  border: 1px dashed #cccccc;
  border-radius: 4px;
  color: #888888;
  font-size: 13px;
  text-align: center;
}

.floor-actions {
  display: flex;
  justify-content: flex-end;
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
  resize: none;
  overflow: hidden;
}

.floor-form .form-footer {
  display: flex;
  justify-content: flex-end;
}

.floor-form button[type='submit'] {
  padding: 4px 12px;
  font-size: 13px;
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

.locked-message {
  color: #999999;
  font-size: 13px;
}

.error {
  color: #c0392b;
  font-size: 13px;
  margin: 0;
}
</style>
