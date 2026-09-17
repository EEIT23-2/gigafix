import axios from 'axios'

// 後台專用的 forum API：不帶 memberId（admin 端點本身不吃 memberId），
// 走跟 forum/api.js 一樣的 /api proxy，開發時同源不會觸發 CORS
// 網址一律收在 /api/admin/forum 底下：SecurityConfig 對 /api/admin/forum/** 這條規則
// 要求 ROLE_FORUM_ADMIN，路徑少了 forum 這段就會掉到 catch-all 變成總/副管理員才能打
// 直接沿用全域的axios實例(不用axios.create()另開一個)，這樣stores/admin.js裝的
// session過期攔截器才會對這裡的請求也生效，否則axios.create()產生的是獨立實例，不會繼承全域攔截器
const http = axios

// ---------------- 文章（後台） ----------------

export function getArticlesForAdmin(params = {}) {
  return http.get('/api/admin/forum/articles', { params }).then((res) => res.data)
}

export function getArticleForAdmin(articleId) {
  return http.get(`/api/admin/forum/articles/${articleId}`).then((res) => res.data)
}

export function updateArticleStatus(articleId, { status, isPinned } = {}) {
  return http
    .patch(`/api/admin/forum/articles/${articleId}/status`, { status, isPinned })
    .then((res) => res.data)
}

export function updateArticlePin(articleId, isPinned) {
  return http
    .patch(`/api/admin/forum/articles/${articleId}/pin`, { isPinned })
    .then((res) => res.data)
}

// ---------------- 留言（後台） ----------------

export function getCommentForAdmin(commentId) {
  return http.get(`/api/admin/forum/comments/${commentId}`).then((res) => res.data)
}

export function updateCommentStatus(commentId, status) {
  return http
    .patch(`/api/admin/forum/comments/${commentId}/status`, { status })
    .then((res) => res.data)
}

// 後台專用的留言串：不過濾狀態，已下架（TAKEN_DOWN）的留言也會回傳供稽核。
// 公開端點 /api/articles/{id}/comments 會濾掉下架留言，後台不能用
export function getArticleCommentsForAdmin(articleId) {
  return http.get(`/api/admin/forum/articles/${articleId}/comments`).then((res) => res.data)
}

// ---------------- 檢舉（後台） ----------------

export function getReportsForAdmin(status) {
  return http
    .get('/api/admin/forum/reports', { params: status ? { status } : {} })
    .then((res) => res.data)
}

export function getReportForAdmin(reportId) {
  return http.get(`/api/admin/forum/reports/${reportId}`).then((res) => res.data)
}

export function updateReportStatus(reportId, status) {
  return http.patch(`/api/admin/forum/reports/${reportId}/status`, { status }).then((res) => res.data)
}

// ---------------- 分類（後台） ----------------
// 讀取沿用公開的 GET /api/categories（見 api.js 的 getCategories），
// 寫入端點才在 /api/admin/forum 底下，與其他後台端點一致

export function createCategory(name) {
  return http.post('/api/admin/forum/categories', { name }).then((res) => res.data)
}

export function updateCategory(categoryId, name) {
  return http.put(`/api/admin/forum/categories/${categoryId}`, { name }).then((res) => res.data)
}

export function deleteCategory(categoryId) {
  return http.delete(`/api/admin/forum/categories/${categoryId}`)
}

// ---------------- 展示資料（後台） ----------------

export function seedForumData() {
  return http.post('/api/admin/forum/seed').then((res) => res.data)
}
