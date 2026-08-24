import axios from 'axios'

// 後台專用的 forum API：不帶 memberId（admin 端點本身不吃 memberId），
// 走跟 forum/api.js 一樣的 /api proxy，開發時同源不會觸發 CORS
const http = axios.create()

// ---------------- 文章（後台） ----------------

export function getArticlesForAdmin(params = {}) {
  return http.get('/api/admin/articles', { params }).then((res) => res.data)
}

export function getArticleForAdmin(articleId) {
  return http.get(`/api/admin/articles/${articleId}`).then((res) => res.data)
}

export function updateArticleStatus(articleId, { status, isPinned } = {}) {
  return http
    .patch(`/api/admin/articles/${articleId}/status`, { status, isPinned })
    .then((res) => res.data)
}

export function updateArticlePin(articleId, isPinned) {
  return http
    .patch(`/api/admin/articles/${articleId}/pin`, { isPinned })
    .then((res) => res.data)
}

// ---------------- 留言（後台） ----------------

export function getCommentForAdmin(commentId) {
  return http.get(`/api/admin/comments/${commentId}`).then((res) => res.data)
}

export function updateCommentStatus(commentId, status) {
  return http
    .patch(`/api/admin/comments/${commentId}/status`, { status })
    .then((res) => res.data)
}

// 既有公開端點，後台沒有登入身份可代入 memberId，likedByCurrentMember 用不到
export function getArticleComments(articleId) {
  return http.get(`/api/articles/${articleId}/comments`).then((res) => res.data)
}

// ---------------- 檢舉（後台） ----------------

export function getReportsForAdmin(status) {
  return http
    .get('/api/admin/reports', { params: status ? { status } : {} })
    .then((res) => res.data)
}

export function getReportForAdmin(reportId) {
  return http.get(`/api/admin/reports/${reportId}`).then((res) => res.data)
}

// 注意：這支不在 /api/admin 底下，是既有的路徑不一致，照現況使用
export function updateReportStatus(reportId, status) {
  return http.patch(`/api/reports/${reportId}/status`, { status }).then((res) => res.data)
}
