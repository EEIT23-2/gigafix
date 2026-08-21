<script setup>
import { ref, onMounted, watch } from 'vue'
import { getArticles } from '../api'
import ArticleCard from '../components/ArticleCard.vue'
import CategorySelect from '../components/CategorySelect.vue'

const articles = ref([])
const totalPages = ref(0)
const page = ref(0)
const size = 10
const categoryId = ref(null)
const keyword = ref('')
const sort = ref('latest')
const loading = ref(true)
const errorMessage = ref('')

async function loadArticles() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getArticles({
      categoryId: categoryId.value,
      keyword: keyword.value || undefined,
      sort: sort.value,
      page: page.value,
      size,
    })
    articles.value = result.content
    totalPages.value = result.totalPages
  } catch {
    errorMessage.value = '文章列表載入失敗，請確認後端服務是否啟動'
  } finally {
    loading.value = false
  }
}

onMounted(loadArticles)

watch([categoryId, sort], () => {
  page.value = 0
  loadArticles()
})

function handleSearch() {
  page.value = 0
  loadArticles()
}

function changePage(delta) {
  const next = page.value + delta
  if (next < 0 || next >= totalPages.value) return
  page.value = next
  loadArticles()
}
</script>

<template>
  <div class="article-list-view">
    <div class="toolbar">
      <CategorySelect v-model="categoryId" />
      <select v-model="sort">
        <option value="latest">最新</option>
        <option value="popular">熱門</option>
      </select>
      <input
        v-model="keyword"
        type="text"
        placeholder="搜尋文章標題或內容"
        @keyup.enter="handleSearch"
      />
      <button type="button" @click="handleSearch">搜尋</button>
      <RouterLink class="new-article-link" :to="{ name: 'forumCreate' }">發表文章</RouterLink>
    </div>

    <p v-if="loading">載入中...</p>
    <p v-else-if="errorMessage" class="error">{{ errorMessage }}</p>
    <template v-else>
      <ArticleCard v-for="article in articles" :key="article.articleId" :article="article" />
      <p v-if="articles.length === 0" class="empty">目前沒有符合條件的文章</p>
    </template>

    <div v-if="totalPages > 1" class="pagination">
      <button type="button" :disabled="page === 0" @click="changePage(-1)">上一頁</button>
      <span>{{ page + 1 }} / {{ totalPages }}</span>
      <button type="button" :disabled="page >= totalPages - 1" @click="changePage(1)">下一頁</button>
    </div>
  </div>
</template>

<style scoped>
.article-list-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar input {
  flex: 1;
  min-width: 160px;
  padding: 6px 10px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
}

.toolbar button {
  padding: 6px 14px;
  border: 1px solid #2b77c5;
  background-color: #ffffff;
  color: #2b77c5;
  border-radius: 4px;
  cursor: pointer;
}

.new-article-link {
  margin-left: auto;
  padding: 6px 14px;
  background-color: #2b77c5;
  color: #ffffff;
  border-radius: 4px;
  text-decoration: none;
  font-size: 14px;
}

.empty {
  text-align: center;
  color: #999999;
  padding: 40px 0;
}

.error {
  text-align: center;
  color: #c0392b;
  padding: 40px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-top: 20px;
}

.pagination button {
  padding: 4px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background-color: #ffffff;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: default;
}
</style>
