<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getReportsForAdmin } from '../../adminApi'
import { REPORT_STATUS_MAP } from '../../adminStatusMaps'
import ReportAdminTable from '../../components/admin/ReportAdminTable.vue'

const router = useRouter()

const allReports = ref([])
const loading = ref(false)
const errorMessage = ref('')

const statusFilter = ref('')
const targetTypeFilter = ref('') // '', 'article', 'comment'
const keyword = ref('')
const page = ref(0)
const size = ref(10)

async function fetchReports() {
  loading.value = true
  errorMessage.value = ''
  try {
    allReports.value = await getReportsForAdmin()
    page.value = 0
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response
      ? `讀取失敗：HTTP ${error.response.status}`
      : '無法連線到後端伺服器'
  } finally {
    loading.value = false
  }
}

const filteredReports = computed(() => {
  return allReports.value.filter((report) => {
    if (statusFilter.value && report.status !== statusFilter.value) return false
    if (targetTypeFilter.value === 'article' && report.articleId == null) return false
    if (targetTypeFilter.value === 'comment' && report.commentId == null) return false
    if (keyword.value) {
      const kw = keyword.value.trim().toLowerCase()
      const haystack = [
        report.reason,
        report.reporterNickName,
        report.articleId != null ? String(report.articleId) : '',
        report.commentId != null ? String(report.commentId) : '',
      ]
        .join(' ')
        .toLowerCase()
      if (!haystack.includes(kw)) return false
    }
    return true
  })
})

const totalElements = computed(() => filteredReports.value.length)
const totalPages = computed(() => Math.ceil(totalElements.value / size.value) || 0)
const pagedReports = computed(() => {
  const start = page.value * size.value
  return filteredReports.value.slice(start, start + size.value)
})
const startItem = computed(() => (totalElements.value ? page.value * size.value + 1 : 0))
const endItem = computed(() => Math.min((page.value + 1) * size.value, totalElements.value))

function applyFilters() {
  page.value = 0
}

function changePage(targetPage) {
  if (targetPage >= 0 && targetPage < totalPages.value) {
    page.value = targetPage
  }
}

function goToDetail(report) {
  router.push({ name: 'admin-forum-report-detail', params: { reportId: report.reportId } })
}

onMounted(fetchReports)
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="mx-auto content-width">
      <header class="d-flex align-items-center gap-3 mb-4">
        <h1 class="fw-bold mb-0">論壇檢舉管理</h1>
        <span class="badge rounded-pill text-bg-light border">Total: {{ totalElements }}</span>
      </header>

      <section class="card mb-4">
        <div class="card-body d-flex flex-column flex-md-row flex-md-wrap gap-3">
          <input
            v-model.trim="keyword"
            class="form-control search-input"
            type="search"
            placeholder="搜尋原因/檢舉人/文章ID/留言ID..."
            @keyup.enter="applyFilters"
          />
          <select v-model="statusFilter" class="form-select filter-select" @change="applyFilters">
            <option value="">全部狀態</option>
            <option v-for="[value, meta] in Object.entries(REPORT_STATUS_MAP)" :key="value" :value="value">
              {{ meta.label }}
            </option>
          </select>
          <select v-model="targetTypeFilter" class="form-select filter-select" @change="applyFilters">
            <option value="">全部類型</option>
            <option value="article">檢舉文章</option>
            <option value="comment">檢舉留言</option>
          </select>
          <button class="btn btn-outline-primary" type="button" @click="applyFilters">搜尋</button>
          <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="fetchReports">
            重新整理
          </button>
        </div>
      </section>

      <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

      <section class="card overflow-hidden">
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <div class="mt-2">載入中...</div>
        </div>

        <ReportAdminTable v-else :reports="pagedReports" @view="goToDetail" />

        <footer
          class="card-footer bg-white d-flex flex-column flex-sm-row align-items-center justify-content-between gap-3 py-3"
        >
          <div class="text-secondary">
            顯示 {{ startItem }} 至 {{ endItem }} 筆，共 {{ totalElements }} 筆（第 {{ totalPages > 0 ? page + 1 : 0 }} / {{ totalPages }} 頁）
          </div>
          <nav aria-label="檢舉分頁">
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: page === 0 }">
                <button class="page-link" @click="changePage(page - 1)">上一頁</button>
              </li>
              <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
                <button class="page-link" @click="changePage(page + 1)">下一頁</button>
              </li>
            </ul>
          </nav>
        </footer>
      </section>
    </div>
  </main>
</template>

<style scoped>
.content-width {
  max-width: 1400px;
}
.search-input {
  flex: 1 1 280px;
}
.filter-select {
  max-width: 190px;
}
</style>
