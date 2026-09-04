<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle, createArticle, updateArticle, updateArticleStatus } from '../api'
import CategorySelect from '../components/CategorySelect.vue'
import RichTextEditor from '../components/RichTextEditor.vue'
import { isHtmlEmpty } from '../htmlContent'

const route = useRoute()
const router = useRouter()

const AUTOSAVE_DELAY_MS = 1500

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
const canPublish = computed(
  () => !!form.value.categoryId && form.value.title.trim() !== '' && !isHtmlEmpty(form.value.content),
)

const loading = ref(false)
const submitting = ref(false)
const publishing = ref(false)
const autosaving = ref(false)
const autosaveMessage = ref('')
const errorMessage = ref('')

// 非響應式狀態：debounce 計時器與「還不算使用者編輯」的抑制旗標
let debounceTimer = null
let suppressAutosave = true

function scheduleAutosave() {
  // 任何新的編輯都先清掉上一次的「已自動存檔」提示
  autosaveMessage.value = ''

  if (suppressAutosave || !isDraftFlow.value) return
  // 標題與內文都還空白時不排程，避免分類自動預選單獨觸發建立空白草稿。
  // 內文同樣要用 isHtmlEmpty——編輯器一掛載就會產出 <p></p>，用 .trim() 會誤判成「已經有內容」
  if (!form.value.title.trim() && isHtmlEmpty(form.value.content)) return

  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(runAutosave, AUTOSAVE_DELAY_MS)
}

watch(form, scheduleAutosave, { deep: true })

async function runAutosave() {
  autosaving.value = true
  try {
    const payload = { ...form.value, coverImage: form.value.coverImage || null }
    if (!isEdit.value && !draftArticleId.value) {
      // 這篇文章的第一次存檔：用 POST 建立草稿
      const created = await createArticle({ ...payload, status: 'DRAFT' })
      draftArticleId.value = created.articleId
      currentStatus.value = 'DRAFT'
    } else {
      await updateArticle(effectiveArticleId.value, payload)
    }
    autosaveMessage.value = '已自動存檔'
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

// 編輯已發布（非草稿）文章時的既有流程：維持不動，不接自動存檔
async function handleSubmit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    const payload = { ...form.value, coverImage: form.value.coverImage || null }
    await updateArticle(articleId.value, payload)
    router.push({ name: 'forumDetail', params: { articleId: articleId.value } })
  } catch {
    errorMessage.value = '儲存失敗，請確認欄位是否都已正確填寫'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
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
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<template>
  <div class="article-form-view">
    <h1>{{ isEdit ? '編輯文章' : '發表文章' }}</h1>
    <form @submit.prevent="handleSubmit">
      <label>
        分類
        <CategorySelect
          v-model="form.categoryId"
          :include-all-option="false"
          @categories-loaded="handleCategoriesLoaded"
        />
      </label>
      <label>
        標題
        <input v-model="form.title" type="text" maxlength="255" />
      </label>
      <label>
        封面圖網址（選填）
        <input v-model="form.coverImage" type="text" placeholder="https://..." />
      </label>
      <!-- 這裡刻意不用 <label>：label 會把點擊轉發給內部第一個可標記控制項，
           而編輯器工具列的按鈕就在裡面，會變成「點編輯區＝按到工具列第一顆鈕」 -->
      <div class="field">
        <span class="field-label">內文</span>
        <RichTextEditor v-model="form.content" />
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <template v-if="isDraftFlow">
        <p v-if="autosaving" class="autosave-status">存檔中...</p>
        <div v-if="autosaveMessage" class="autosave-banner" role="alert">
          {{ autosaveMessage }}
          <button class="dismiss-btn" type="button" aria-label="關閉" @click="autosaveMessage = ''"></button>
        </div>
        <button type="button" :disabled="!canPublish || publishing" @click="handlePublish">
          {{ publishing ? '發布中...' : '發布' }}
        </button>
      </template>
      <template v-else>
        <button type="submit" :disabled="submitting">{{ submitting ? '儲存中...' : '儲存變更' }}</button>
      </template>
    </form>
  </div>
</template>

<style scoped>
.article-form-view {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}

form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

label,
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #555555;
}

/* 純標示用，不是 <label>，不會有轉發點擊的行為 */
.field-label {
  font-size: 14px;
  color: #555555;
}

input,
textarea {
  padding: 8px 10px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-family: inherit;
  font-size: 14px;
}

textarea {
  resize: vertical;
}

button[type='submit'],
button[type='button'] {
  align-self: flex-start;
  padding: 8px 20px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

button[type='submit']:disabled,
button[type='button']:disabled {
  opacity: 0.6;
  cursor: default;
}

.error {
  color: #c0392b;
  font-size: 13px;
}

.autosave-status {
  margin: 0;
  font-size: 13px;
  color: #888888;
}

.autosave-banner {
  padding: 8px 36px 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  position: relative;
  background-color: #e6f4ea;
  color: #1e7e34;
  border: 1px solid #b7dfc0;
}

.dismiss-btn {
  position: absolute;
  top: 6px;
  right: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  color: inherit;
  opacity: 0.6;
}

.dismiss-btn::before {
  content: '×';
}

.dismiss-btn:hover {
  opacity: 1;
}
</style>
