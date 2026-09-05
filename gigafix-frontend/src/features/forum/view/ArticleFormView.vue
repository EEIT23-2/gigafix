<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import {
  getArticle,
  createArticle,
  updateArticle,
  updateArticleStatus,
  deleteDraft,
  flushArticleOnUnload,
} from '../api'
import CategorySelect from '../components/CategorySelect.vue'
import RichTextEditor from '../components/RichTextEditor.vue'
import { isHtmlEmpty } from '../htmlContent'

const route = useRoute()
const router = useRouter()

const AUTOSAVE_DELAY_MS = 1500
const TITLE_MAX = 255

const articleId = computed(() => route.params.articleId)
const isEdit = computed(() => !!articleId.value)

const form = ref({
  categoryId: null,
  title: '',
  content: '',
  coverImage: '',
})

// 編輯模式：從抓到的文章狀態取得；建立模式：第一次自動存檔成功後設為 DRAFT
const currentStatus = ref(null)
// 建立模式專用：第一次自動存檔（建立草稿）後拿到的文章 id
const draftArticleId = ref(null)

const effectiveArticleId = computed(() => (isEdit.value ? articleId.value : draftArticleId.value))
// 建立模式永遠走草稿流程；編輯模式只有原本就是草稿才走
const isDraftFlow = computed(() => (isEdit.value ? currentStatus.value === 'DRAFT' : true))
// 內文是 TipTap 產生的 HTML，空編輯器的輸出是 <p></p>——不能用 .trim() 判斷是否為空
const contentEmpty = computed(() => isHtmlEmpty(form.value.content))
const canPublish = computed(
  () => !!form.value.categoryId && form.value.title.trim() !== '' && !contentEmpty.value,
)
// 發布鈕停用時給使用者看的原因（不講 <p></p> 或 trim 這種實作細節）
const publishBlockedReason = computed(() => {
  if (canPublish.value) return ''
  const missing = []
  if (!form.value.title.trim()) missing.push('標題')
  if (contentEmpty.value) missing.push('內文')
  if (!form.value.categoryId) missing.push('分類')
  return `還差${missing.join('、')}才能發布`
})

const coverPreviewFailed = ref(false)
// 只把看起來像網址的值送去預覽，避免使用者才打兩個字就閃一次破圖
const coverPreviewUrl = computed(() => {
  const url = form.value.coverImage.trim()
  return /^https?:\/\/\S+$/i.test(url) ? url : ''
})
watch(coverPreviewUrl, () => {
  coverPreviewFailed.value = false
})

const loading = ref(false)
const submitting = ref(false)
const publishing = ref(false)
const discarding = ref(false)
const autosaving = ref(false)
const autosavedAt = ref('')
const errorMessage = ref('')

// 非響應式狀態：debounce 計時器與「還不算使用者編輯」的抑制旗標
let debounceTimer = null
let suppressAutosave = true

function buildPayload() {
  return { ...form.value, coverImage: form.value.coverImage || null }
}

function scheduleAutosave() {
  if (suppressAutosave || !isDraftFlow.value) return
  // 標題與內文都還空白時不排程，避免分類自動預選單獨觸發建立空白草稿。
  // 內文同樣要用 isHtmlEmpty——編輯器一掛載就會產出 <p></p>，用 .trim() 會誤判成「已經有內容」
  if (!form.value.title.trim() && contentEmpty.value) return

  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(runAutosave, AUTOSAVE_DELAY_MS)
}

watch(form, scheduleAutosave, { deep: true })

async function runAutosave() {
  debounceTimer = null
  autosaving.value = true
  try {
    const payload = buildPayload()
    if (!isEdit.value && !draftArticleId.value) {
      // 這篇文章的第一次存檔：用 POST 建立草稿
      const created = await createArticle({ ...payload, status: 'DRAFT' })
      draftArticleId.value = created.articleId
      currentStatus.value = 'DRAFT'
    } else {
      await updateArticle(effectiveArticleId.value, payload)
    }
    autosavedAt.value = new Date().toLocaleTimeString('zh-TW', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
  } catch (error) {
    console.error(error)
    // 自動存檔失敗不打斷編輯，下一次編輯會重新排程並重試
  } finally {
    autosaving.value = false
  }
}

async function flushPendingAutosave() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
    debounceTimer = null
    await runAutosave()
  }
}

// 分頁被關掉／重新整理時，debounce 還沒到期的那 1.5 秒編輯不能就這樣消失。
// 這裡只能同步發出請求，所以走 keepalive 的 fetch，不能 await
function handleBeforeUnload() {
  if (!debounceTimer || !effectiveArticleId.value) return
  clearTimeout(debounceTimer)
  debounceTimer = null
  flushArticleOnUnload(effectiveArticleId.value, buildPayload())
}

// 站內換頁（router）走得到 async，可以好好等存檔完成再離開
onBeforeRouteLeave(async () => {
  if (isDraftFlow.value) await flushPendingAutosave()
})

// 修正原生 <select> 初次渲染不觸發 change 的落差，同時讓「建立草稿需要 categoryId」這件事自動成立
function handleCategoriesLoaded(categories) {
  if (!isEdit.value && form.value.categoryId == null && categories.length > 0) {
    form.value.categoryId = categories[0].categoryId
  }
}

async function handlePublish() {
  if (!canPublish.value) return
  publishing.value = true
  errorMessage.value = ''
  try {
    await flushPendingAutosave()
    if (!effectiveArticleId.value) await runAutosave()
    await updateArticleStatus(effectiveArticleId.value, 'PUBLISHED')
    router.push({ name: 'forumDetail', params: { articleId: effectiveArticleId.value } })
  } catch {
    errorMessage.value = '發布失敗，請確認欄位是否都已正確填寫'
  } finally {
    publishing.value = false
  }
}

async function handleDiscardDraft() {
  if (!confirm('捨棄後這篇草稿會被永久刪除，確定嗎？')) return
  // 還沒存過任何一次的話，資料庫裡根本沒有這篇，直接離開就好
  if (!effectiveArticleId.value) {
    router.push({ name: 'forumList' })
    return
  }
  discarding.value = true
  errorMessage.value = ''
  try {
    // 先取消待處理的自動存檔，否則刪掉之後那個計時器會再把它寫回來
    if (debounceTimer) {
      clearTimeout(debounceTimer)
      debounceTimer = null
    }
    await deleteDraft(effectiveArticleId.value)
    suppressAutosave = true
    router.push({ name: 'forumList' })
  } catch (error) {
    const data = error.response?.data
    errorMessage.value = data?.message || '捨棄草稿失敗，請稍後再試'
  } finally {
    discarding.value = false
  }
}

// 編輯已發布（非草稿）文章時的既有流程：維持不動，不接自動存檔
async function handleSubmit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    await updateArticle(articleId.value, buildPayload())
    router.push({ name: 'forumDetail', params: { articleId: articleId.value } })
  } catch {
    errorMessage.value = '儲存失敗，請確認欄位是否都已正確填寫'
  } finally {
    submitting.value = false
  }
}

function goToArticle() {
  router.push({ name: 'forumDetail', params: { articleId: articleId.value } })
}

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  if (isEdit.value) {
    loading.value = true
    try {
      const article = await getArticle(articleId.value)
      form.value = {
        categoryId: article.categoryId,
        title: article.title,
        content: article.content,
        coverImage: article.coverImage ?? '',
      }
      currentStatus.value = article.status
    } catch {
      errorMessage.value = '文章資料載入失敗，請確認文章是否存在'
    } finally {
      loading.value = false
    }
  }
  // 等這次掛載造成的表單賦值處理完，才開始把後續變動視為使用者編輯
  await nextTick()
  suppressAutosave = false
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<template>
  <main class="article-form-page">
    <div class="form-shell">
      <!-- 標題列：模式一眼可辨 -->
      <header class="page-head">
        <span class="mode-label">{{ isDraftFlow ? '草稿流程' : '已發布' }}</span>
        <h1 class="page-title">{{ isEdit ? '編輯文章' : '發表文章' }}</h1>
      </header>

      <!-- 草稿：狀態與自動存檔放在視線內，不再壓在頁尾 -->
      <div v-if="isDraftFlow" class="status-bar">
        <span class="pill pill-draft">草稿</span>
        <span class="status-text">尚未公開，只有你看得到</span>
        <span v-if="autosaving" class="status-note">存檔中...</span>
        <span v-else-if="autosavedAt" class="status-saved">
          <i class="bi bi-check-lg"></i>已自動存檔 {{ autosavedAt }}
        </span>
      </div>

      <!-- 已發布：藍色橫幅，明確區隔於草稿流程 -->
      <div v-else class="status-bar status-bar-live">
        <span class="pill pill-live">發布中</span>
        <span class="status-text">這篇文章目前公開，儲存後改動會立即生效</span>
        <button type="button" class="link-btn" @click="goToArticle">查看公開頁面</button>
        <span class="status-note w-100">已發布的文章不會自動存檔，改完要按「儲存變更」</span>
      </div>

      <form class="form-card" @submit.prevent="handleSubmit">
        <!-- 標題：放最大，它是文章的門面 -->
        <div class="field">
          <div class="field-head">
            <label class="field-label" for="article-title">標題</label>
            <span class="required">必填</span>
            <span class="counter">{{ form.title.length }} / {{ TITLE_MAX }}</span>
          </div>
          <input
            id="article-title"
            v-model="form.title"
            class="title-input"
            type="text"
            :maxlength="TITLE_MAX"
            placeholder="為這篇文章下一個標題"
          />
        </div>

        <div class="field field-narrow">
          <div class="field-head">
            <span class="field-label">分類</span>
            <span class="required">必填</span>
          </div>
          <CategorySelect
            v-model="form.categoryId"
            :include-all-option="false"
            @categories-loaded="handleCategoriesLoaded"
          />
        </div>

        <!-- 這裡刻意不用 <label>：label 會把點擊轉發給內部第一個可標記控制項，
             而編輯器工具列的按鈕就在裡面，會變成「點編輯區＝按到工具列第一顆鈕」 -->
        <div class="field">
          <div class="field-head">
            <span class="field-label">內文</span>
            <span class="required">必填</span>
          </div>
          <RichTextEditor v-model="form.content" />
        </div>

        <!-- 封面圖：網址 ＋ 即時預覽，貼錯立刻看得出來 -->
        <div class="field">
          <div class="field-head">
            <label class="field-label" for="article-cover">封面圖</label>
            <span class="optional">選填 · 貼圖片網址</span>
          </div>
          <div class="cover-row">
            <input
              id="article-cover"
              v-model="form.coverImage"
              class="cover-input"
              type="url"
              placeholder="https://..."
            />
            <div class="cover-preview">
              <img
                v-if="coverPreviewUrl && !coverPreviewFailed"
                :src="coverPreviewUrl"
                alt="封面預覽"
                @error="coverPreviewFailed = true"
              />
              <span v-else-if="coverPreviewFailed" class="cover-hint cover-hint-error">圖片載入失敗</span>
              <span v-else class="cover-hint">尚未設定</span>
              <button
                v-if="form.coverImage"
                type="button"
                class="cover-clear"
                aria-label="清除封面圖"
                @click="form.coverImage = ''"
              >
                ×
              </button>
            </div>
          </div>
        </div>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      </form>

      <!-- 動作列 -->
      <div class="action-bar">
        <template v-if="isDraftFlow">
          <button type="button" class="discard-btn" :disabled="discarding" @click="handleDiscardDraft">
            {{ discarding ? '捨棄中...' : '捨棄草稿' }}
          </button>
          <div class="action-right">
            <span v-if="publishBlockedReason" class="blocked-reason">{{ publishBlockedReason }}</span>
            <span v-else class="action-note">發布後所有人都看得到</span>
            <button
              type="button"
              class="primary-btn"
              :disabled="!canPublish || publishing"
              @click="handlePublish"
            >
              {{ publishing ? '發布中...' : '發布' }}
            </button>
          </div>
        </template>
        <template v-else>
          <button type="button" class="discard-btn" @click="goToArticle">取消，返回文章</button>
          <div class="action-right">
            <button type="button" class="primary-btn" :disabled="submitting" @click="handleSubmit">
              {{ submitting ? '儲存中...' : '儲存變更' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </main>
</template>

<style scoped>
.article-form-page {
  background: #f6f8fa;
  padding: 24px 20px 40px;
  min-height: 100vh;
}

.form-shell {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ── 標題列 ── */
.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 4px;
  border-bottom: 2px solid #2b77c5;
}

.mode-label {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2b77c5;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1d324b;
}

/* ── 狀態列 ── */
.status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 18px;
  background: #ffffff;
  border: 1px solid #e5e9f0;
  border-radius: 8px;
}

.status-bar-live {
  background: #eaf2fb;
  border-color: #b8d4ef;
}

.pill {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.pill-draft {
  background: #eef1f5;
  color: #6c757d;
}

.pill-live {
  background: #1e7e34;
  color: #ffffff;
}

.status-text {
  font-size: 13px;
  color: #888888;
}

.status-bar-live .status-text {
  color: #1f5fa8;
}

.status-note {
  font-size: 12px;
  color: #adb5bd;
}

.status-bar-live .status-note {
  color: #5a86b3;
}

.status-saved {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1e7e34;
}

.status-bar .status-note:not(.w-100) {
  margin-left: auto;
}

.w-100 {
  width: 100%;
}

.link-btn {
  padding: 0;
  border: 0;
  background: none;
  color: #2b77c5;
  font-size: 13px;
  cursor: pointer;
}

.link-btn:hover {
  text-decoration: underline;
}

/* ── 表單卡 ── */
.form-card {
  background: #ffffff;
  border: 1px solid #e5e9f0;
  border-radius: 8px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-narrow {
  max-width: 280px;
}

.field-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #555555;
}

.required {
  font-size: 12px;
  color: #c0392b;
}

.optional {
  font-size: 12px;
  color: #adb5bd;
}

.counter {
  margin-left: auto;
  font-size: 12px;
  color: #adb5bd;
}

/* 標題是文章門面，字級明顯大於其他欄位 */
.title-input {
  padding: 12px 14px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-family: inherit;
  font-size: 20px;
  font-weight: 600;
  color: #1d324b;
}

.title-input::placeholder {
  font-weight: 400;
  color: #adb5bd;
}

.title-input:focus,
.cover-input:focus {
  outline: none;
  border-color: #2b77c5;
}

/* ── 封面圖 ── */
.cover-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.cover-input {
  flex: 1;
  min-width: 0;
  padding: 9px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-family: inherit;
  font-size: 14px;
  color: #555555;
}

.cover-preview {
  position: relative;
  flex-shrink: 0;
  width: 132px;
  height: 78px;
  border: 1px solid #e5e9f0;
  border-radius: 6px;
  background: #f6f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-hint {
  font-size: 11px;
  color: #6c757d;
}

.cover-hint-error {
  color: #c0392b;
}

.cover-clear {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid #d0d0d0;
  background: #ffffff;
  color: #888888;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-clear:hover {
  color: #c0392b;
  border-color: #c0392b;
}

/* ── 動作列 ── */
.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 20px;
  background: #ffffff;
  border: 1px solid #e5e9f0;
  border-radius: 8px;
}

.action-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-note {
  font-size: 12px;
  color: #adb5bd;
}

.blocked-reason {
  font-size: 12px;
  color: #a15c00;
}

.discard-btn {
  padding: 0;
  border: 0;
  background: none;
  font-size: 14px;
  color: #6c757d;
  cursor: pointer;
}

.discard-btn:hover:not(:disabled) {
  color: #c0392b;
}

.discard-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.primary-btn {
  padding: 9px 24px;
  border: none;
  border-radius: 6px;
  background: #2b77c5;
  color: #ffffff;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.error {
  margin: 0;
  color: #c0392b;
  font-size: 13px;
}
</style>
