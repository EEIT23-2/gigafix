<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticlesForAdmin, updateArticlePin } from '../../adminApi'
import { ARTICLE_STATUS_MAP } from '../../adminStatusMaps'
import CategorySelect from '../../components/CategorySelect.vue'
import ArticleAdminTable from '../../components/admin/ArticleAdminTable.vue'
import CategoryAdminPanel from '../../components/admin/CategoryAdminPanel.vue'

const router = useRouter()

// 分頁切換：文章管理 / 分類管理
// 兩個分頁刻意用 v-if 而不是 v-show——CategorySelect 只在自己 onMounted 抓一次分類、沒有快取也沒有 store，
// 靠重新掛載才能在分類異動後自動拿到最新的下拉選項，不需要額外寫同步邏輯
const activeTab = ref('articles')

const keyword = ref('')
const status = ref('')
const categoryId = ref(null)
const authorId = ref('')
const page = ref(0)
const size = ref(10)

const articles = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const statusOptions = Object.entries(ARTICLE_STATUS_MAP)

const middlePages = computed(() => {
  if (totalPages.value <= 2) return []
  if (totalPages.value <= 7) {
    return Array.from({ length: totalPages.value - 2 }, (_, index) => index + 1)
  }
  if (page.value <= 3) return [1, 2, 3, 4]
  if (page.value >= totalPages.value - 4) {
    return Array.from({ length: 4 }, (_, index) => totalPages.value - 5 + index)
  }
  return [page.value - 1, page.value, page.value + 1]
})
const showLeadingEllipsis = computed(() => totalPages.value > 7 && page.value > 3)
const showTrailingEllipsis = computed(() => totalPages.value > 7 && page.value < totalPages.value - 4)
const startItem = computed(() => (totalElements.value ? page.value * size.value + 1 : 0))
const endItem = computed(() => Math.min((page.value + 1) * size.value, totalElements.value))

function buildParams(targetPage) {
  return {
    page: targetPage,
    size: size.value,
    ...(status.value && { status: status.value }),
    ...(categoryId.value != null && { categoryId: categoryId.value }),
    ...(keyword.value && { keyword: keyword.value }),
    ...(authorId.value && Number(authorId.value) > 0 && { authorId: authorId.value }),
  }
}

async function fetchArticles(targetPage = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getArticlesForAdmin(buildParams(targetPage))
    articles.value = data.content ?? []
    page.value = data.number ?? 0
    totalElements.value = data.totalElements ?? 0
    totalPages.value = data.totalPages ?? 0
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response
      ? `讀取失敗：HTTP ${error.response.status}`
      : '無法連線到後端伺服器'
  } finally {
    loading.value = false
  }
}

function changePage(targetPage) {
  if (targetPage >= 0 && targetPage < totalPages.value && targetPage !== page.value) {
    fetchArticles(targetPage)
  }
}

function goToDetail(article) {
  router.push({ name: 'admin-forum-article-detail', params: { articleId: article.articleId } })
}

async function handleTogglePin(article) {
  errorMessage.value = ''
  try {
    const newPinned = !article.isPinned
    await updateArticlePin(article.articleId, newPinned)
    article.isPinned = newPinned
    successMessage.value = newPinned ? '已設為置頂' : '已取消置頂'
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response
      ? `操作失敗：HTTP ${error.response.status}`
      : '無法連線到後端伺服器'
  }
}

onMounted(() => fetchArticles(0))
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="mx-auto content-width">
      <header class="d-flex align-items-center gap-3 mb-3">
        <h1 class="fw-bold mb-0">論壇內容管理</h1>
        <span v-if="activeTab === 'articles'" class="badge rounded-pill text-bg-light border">
          Total: {{ totalElements }}
        </span>
      </header>

      <ul class="nav nav-tabs mb-4">
        <li class="nav-item">
          <button
            class="nav-link"
            :class="{ active: activeTab === 'articles' }"
            type="button"
            @click="activeTab = 'articles'"
          >
            文章管理
          </button>
        </li>
        <li class="nav-item">
          <button
            class="nav-link"
            :class="{ active: activeTab === 'categories' }"
            type="button"
            @click="activeTab = 'categories'"
          >
            分類管理
          </button>
        </li>
      </ul>

      <CategoryAdminPanel v-if="activeTab === 'categories'" />

      <template v-else>
        <section class="card mb-4">
          <div class="card-body d-flex flex-column flex-md-row flex-md-wrap gap-3">
            <input
              v-model.trim="keyword"
              class="form-control search-input"
              type="search"
              placeholder="搜尋標題/內文關鍵字..."
              @keyup.enter="fetchArticles(0)"
            />
            <select v-model="status" class="form-select filter-select" @change="fetchArticles(0)">
              <option value="">全部狀態</option>
              <option v-for="[value, meta] in statusOptions" :key="value" :value="value">
                {{ meta.label }}
              </option>
            </select>
            <CategorySelect
              v-model="categoryId"
              :include-all-option="true"
              @update:model-value="fetchArticles(0)"
            />
            <input
              v-model.trim="authorId"
              class="form-control filter-select"
              type="number"
              min="1"
              placeholder="作者會員 ID"
              @keyup.enter="fetchArticles(0)"
            />
            <button class="btn btn-outline-primary" type="button" @click="fetchArticles(0)">搜尋</button>
          </div>
        </section>

        <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
        <div v-if="successMessage" class="alert alert-success alert-dismissible" role="alert">
          {{ successMessage }}
          <button class="btn-close" type="button" aria-label="關閉成功訊息" @click="successMessage = ''"></button>
        </div>

        <section class="card overflow-hidden">
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <div class="mt-2">載入中...</div>
          </div>

          <ArticleAdminTable v-else :articles="articles" @view="goToDetail" @toggle-pin="handleTogglePin" />

          <footer
            class="card-footer bg-white d-flex flex-column flex-sm-row align-items-center justify-content-between gap-3 py-3"
          >
            <div class="text-secondary d-flex flex-column flex-md-row gap-md-3 text-center text-md-start">
              <span>顯示 {{ startItem }} 至 {{ endItem }} 筆，共 {{ totalElements }} 筆</span>
              <span>第 {{ totalPages > 0 ? page + 1 : 0 }} / {{ totalPages }} 頁</span>
            </div>
            <nav aria-label="文章分頁">
              <ul class="pagination pagination-sm mb-0">
                <li class="page-item" :class="{ disabled: page === 0 }">
                  <button class="page-link" @click="changePage(0)">最前頁</button>
                </li>
                <li class="page-item" :class="{ disabled: page === 0 }">
                  <button class="page-link" @click="changePage(page - 1)">上一頁</button>
                </li>
                <li v-if="totalPages > 0" class="page-item" :class="{ active: page === 0 }">
                  <button class="page-link" @click="changePage(0)">1</button>
                </li>
                <li v-if="showLeadingEllipsis" class="page-item disabled" aria-hidden="true">
                  <span class="page-link">…</span>
                </li>
                <li
                  v-for="pageNumber in middlePages"
                  :key="pageNumber"
                  class="page-item"
                  :class="{ active: pageNumber === page }"
                >
                  <button class="page-link" @click="changePage(pageNumber)">{{ pageNumber + 1 }}</button>
                </li>
                <li v-if="showTrailingEllipsis" class="page-item disabled" aria-hidden="true">
                  <span class="page-link">…</span>
                </li>
                <li v-if="totalPages > 1" class="page-item" :class="{ active: page === totalPages - 1 }">
                  <button class="page-link" @click="changePage(totalPages - 1)">{{ totalPages }}</button>
                </li>
                <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
                  <button class="page-link" @click="changePage(page + 1)">下一頁</button>
                </li>
                <li class="page-item" :class="{ disabled: totalPages === 0 || page >= totalPages - 1 }">
                  <button class="page-link" @click="changePage(totalPages - 1)">最後頁</button>
                </li>
              </ul>
            </nav>
          </footer>
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.content-width {
  max-width: 1600px;
}
.search-input {
  flex: 1 1 260px;
}
.filter-select {
  max-width: 190px;
}
</style>
