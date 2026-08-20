<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle, createArticle, updateArticle } from '../api'
import CategorySelect from '../components/CategorySelect.vue'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => route.params.articleId)
const isEdit = computed(() => !!articleId.value)

const form = ref({
  categoryId: null,
  title: '',
  content: '',
  coverImage: '',
})
const submitting = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  if (!isEdit.value) return
  try {
    const article = await getArticle(articleId.value)
    form.value = {
      categoryId: article.categoryId,
      title: article.title,
      content: article.content,
      coverImage: article.coverImage ?? '',
    }
  } catch {
    errorMessage.value = '文章資料載入失敗，請確認文章是否存在'
  }
})

async function handleSubmit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    const payload = { ...form.value, coverImage: form.value.coverImage || null }
    if (isEdit.value) {
      await updateArticle(articleId.value, payload)
      router.push({ name: 'forumDetail', params: { articleId: articleId.value } })
    } else {
      const created = await createArticle(payload)
      router.push({ name: 'forumDetail', params: { articleId: created.articleId } })
    }
  } catch {
    errorMessage.value = '儲存失敗，請確認欄位是否都已正確填寫'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="article-form-view">
    <h1>{{ isEdit ? '編輯文章' : '發表文章' }}</h1>
    <form @submit.prevent="handleSubmit">
      <label>
        分類
        <CategorySelect v-model="form.categoryId" :include-all-option="false" />
      </label>
      <label>
        標題
        <input v-model="form.title" type="text" maxlength="255" required />
      </label>
      <label>
        封面圖網址（選填）
        <input v-model="form.coverImage" type="text" placeholder="https://..." />
      </label>
      <label>
        內文
        <textarea v-model="form.content" rows="10" required />
      </label>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <button type="submit" :disabled="submitting">{{ isEdit ? '儲存變更' : '發布文章' }}</button>
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

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
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

button[type='submit'] {
  align-self: flex-start;
  padding: 8px 20px;
  background-color: #2b77c5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

button[type='submit']:disabled {
  opacity: 0.6;
  cursor: default;
}

.error {
  color: #c0392b;
  font-size: 13px;
}
</style>
