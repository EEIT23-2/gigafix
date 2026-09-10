// 會員中心「我的討論」的分頁與返回規則。
//
// 分頁狀態放在網址的 ?tab= 而不是元件的 ref，因為使用者會離開這一頁（去看文章、去編輯）
// 再回來，狀態必須跟著網址走才回得到原本那個分頁——瀏覽器上一頁也才會落在正確的地方。
//
// 離開會員中心去前台文章頁時另外帶 ?from=member-forum，前台的返回鍵據此改文字與目的地。

export const MEMBER_FORUM_TABS = ['articles', 'drafts', 'bookmarks', 'activity']

export const TAB_LABELS = {
  articles: '我的文章',
  drafts: '我的草稿',
  bookmarks: '我的收藏',
  activity: '活動',
}

const DEFAULT_TAB = 'articles'

// 網址是使用者可以隨手亂打的，收斂成合法值再用
export function normalizeTab(value) {
  return MEMBER_FORUM_TABS.includes(value) ? value : DEFAULT_TAB
}

// 回「我的討論」時要停在哪個分頁
export function backToMemberForum(tab) {
  return { name: 'member-forum', query: { tab: normalizeTab(tab) } }
}

// 從會員中心點進前台文章頁時要帶的查詢參數
export function memberForumOrigin(tab) {
  return { from: 'member-forum', tab: normalizeTab(tab) }
}

export function fromMemberForum(route) {
  return route.query.from === 'member-forum'
}
