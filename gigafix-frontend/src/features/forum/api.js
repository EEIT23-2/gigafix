import axios from 'axios'

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

// 身分改由登入 cookie 帶：有登入的話後端會附上 isAuthor / likedByCurrentMember / bookmarkedByCurrentMember，
// 沒登入則一律當成匿名訪客
export function getArticle(articleId) {
  return http.get(`/api/articles/${articleId}`).then((res) => res.data)
}

// 最近瀏覽用的批次讀取。跟 getArticle 不同，這支「不會」讓瀏覽數 +1——
// 否則每渲染一次最近瀏覽列表，就會幫列表上的每一篇各灌一次
export function getRecentArticles(ids) {
  if (!ids || ids.length === 0) return Promise.resolve([])
  return http
    .get('/api/articles/recent', { params: { ids: ids.join(',') } })
    .then((res) => res.data)
}

// 會員自己的文章。包含草稿與自己蓋的樓層，由呼叫端自己分流
export function getMyArticles() {
  return http.get('/api/members/me/articles').then((res) => res.data)
}

export function createArticle(data) {
  return http.post('/api/members/me/articles', data).then((res) => res.data)
}

export function updateArticle(articleId, data) {
  return http.put(`/api/members/me/articles/${articleId}`, data).then((res) => res.data)
}

export function deleteArticle(articleId) {
  return http.delete(`/api/members/me/articles/${articleId}`)
}

// 捨棄草稿：真的刪列，跟上面那支軟刪除（改成下架）不同，後端只接受 DRAFT
export function deleteDraft(articleId) {
  return http.delete(`/api/members/me/drafts/${articleId}`)
}

// 離開頁面時的補存：sendBeacon 只能發 POST，但自動存檔走的是 PUT，
// 所以改用 fetch 的 keepalive——它同樣能在頁面卸載後把請求送完，且不限方法
export function flushArticleOnUnload(articleId, payload) {
  return fetch(`/api/members/me/articles/${articleId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    keepalive: true,
  })
}

export function updateArticleStatus(articleId, status) {
  return http
    .patch(`/api/members/me/articles/${articleId}/status`, { status })
    .then((res) => res.data)
}

// ---------------- 檢舉 ----------------

export function reportArticle(articleId, reason) {
  return http
    .post(`/api/members/me/articles/${articleId}/reports`, { reason })
    .then((res) => res.data)
}

export function reportComment(commentId, reason) {
  return http
    .post(`/api/members/me/comments/${commentId}/reports`, { reason })
    .then((res) => res.data)
}

// ---------------- 蓋樓（樓層） ----------------

export function getFloors(articleId) {
  return http.get(`/api/articles/${articleId}/floors`).then((res) => res.data)
}

export function createFloor(articleId, content) {
  return http
    .post(`/api/members/me/articles/${articleId}/floors`, { content })
    .then((res) => res.data)
}

// 編輯樓層只送內文；標題與分類由後端擁有，走的是樓層專用端點而不是 updateArticle
export function updateFloor(floorId, content) {
  return http.put(`/api/members/me/floors/${floorId}`, { content }).then((res) => res.data)
}

// ---------------- 留言 ----------------

export function getComments(articleId) {
  return http.get(`/api/articles/${articleId}/comments`).then((res) => res.data)
}

export function createComment(articleId, data) {
  return http
    .post(`/api/members/me/articles/${articleId}/comments`, data)
    .then((res) => res.data)
}

export function deleteComment(commentId) {
  return http.delete(`/api/members/me/comments/${commentId}`)
}

// ---------------- 讚 ----------------

export function likeArticle(articleId) {
  return http.post(`/api/members/me/articles/${articleId}/like`).then((res) => res.data)
}

export function unlikeArticle(articleId) {
  return http.delete(`/api/members/me/articles/${articleId}/like`)
}

export function hasLikedArticle(articleId) {
  return http.get(`/api/members/me/articles/${articleId}/like`).then((res) => res.data)
}

export function likeComment(commentId) {
  return http.post(`/api/members/me/comments/${commentId}/like`).then((res) => res.data)
}

export function unlikeComment(commentId) {
  return http.delete(`/api/members/me/comments/${commentId}/like`)
}

// ---------------- 收藏 ----------------

export function addBookmark(articleId) {
  return http.post(`/api/members/me/articles/${articleId}/bookmark`).then((res) => res.data)
}

export function removeBookmark(articleId) {
  return http.delete(`/api/members/me/articles/${articleId}/bookmark`)
}

export function getBookmarks() {
  return http.get('/api/members/me/bookmarks').then((res) => res.data)
}

export function hasBookmarked(articleId) {
  return http.get(`/api/members/me/articles/${articleId}/bookmark`).then((res) => res.data)
}
