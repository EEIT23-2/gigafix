import axios from 'axios'

// TODO: 後端還沒有真的 JWT 驗證，先寫死測試用的 memberId，串接登入模組後要改成從登入狀態取得
export const TEST_MEMBER_ID = 1

// 開發時走 vite.config.js 的 /api proxy（同源，不會觸發 CORS），不要寫死絕對網址
// 正式環境如果前後端分開部署，屆時再視情況改用環境變數指定真正的後端網址
const http = axios.create()

// ---------------- 分類 ----------------

export function getCategories() {
  return http.get('/api/categories').then((res) => res.data)
}

// ---------------- 文章 ----------------

export function getArticles({ categoryId, keyword, sort = 'latest', page = 0, size = 10 } = {}) {
  return http
    .get('/api/articles', { params: { categoryId, keyword, sort, page, size } })
    .then((res) => res.data)
}

export function getArticle(articleId) {
  return http.get(`/api/articles/${articleId}`).then((res) => res.data)
}

export function createArticle(data) {
  return http.post(`/api/members/${TEST_MEMBER_ID}/articles`, data).then((res) => res.data)
}

export function updateArticle(articleId, data) {
  return http
    .put(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}`, data)
    .then((res) => res.data)
}

export function deleteArticle(articleId) {
  return http.delete(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}`)
}

export function updateArticleStatus(articleId, status) {
  return http
    .patch(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/status`, { status })
    .then((res) => res.data)
}

// ---------------- 蓋樓（樓層） ----------------

export function getFloors(articleId) {
  return http
    .get(`/api/articles/${articleId}/floors`, { params: { memberId: TEST_MEMBER_ID } })
    .then((res) => res.data)
}

export function createFloor(articleId, content) {
  return http
    .post(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/floors`, { content })
    .then((res) => res.data)
}

// ---------------- 留言 ----------------

export function getComments(articleId) {
  return http
    .get(`/api/articles/${articleId}/comments`, { params: { memberId: TEST_MEMBER_ID } })
    .then((res) => res.data)
}

export function createComment(articleId, data) {
  return http
    .post(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/comments`, data)
    .then((res) => res.data)
}

export function deleteComment(commentId) {
  return http.delete(`/api/members/${TEST_MEMBER_ID}/comments/${commentId}`)
}

// ---------------- 讚 ----------------

export function likeArticle(articleId) {
  return http
    .post(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/like`)
    .then((res) => res.data)
}

export function unlikeArticle(articleId) {
  return http.delete(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/like`)
}

export function hasLikedArticle(articleId) {
  return http
    .get(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/like`)
    .then((res) => res.data)
}

export function likeComment(commentId) {
  return http
    .post(`/api/members/${TEST_MEMBER_ID}/comments/${commentId}/like`)
    .then((res) => res.data)
}

export function unlikeComment(commentId) {
  return http.delete(`/api/members/${TEST_MEMBER_ID}/comments/${commentId}/like`)
}

// ---------------- 收藏 ----------------

export function addBookmark(articleId) {
  return http
    .post(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/bookmark`)
    .then((res) => res.data)
}

export function removeBookmark(articleId) {
  return http.delete(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/bookmark`)
}

export function getBookmarks() {
  return http.get(`/api/members/${TEST_MEMBER_ID}/bookmarks`).then((res) => res.data)
}

export function hasBookmarked(articleId) {
  return http
    .get(`/api/members/${TEST_MEMBER_ID}/articles/${articleId}/bookmark`)
    .then((res) => res.data)
}
