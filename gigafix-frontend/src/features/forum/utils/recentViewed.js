// 最近瀏覽：只存在使用者自己這台裝置的 localStorage，只記文章 id。
//
// 為什麼不存標題等內容：文章之後可能被改標題、被作者隱藏、被管理員下架。
// 存快照的話這些都不會反映出來，等於把已經不該看到的內容留在畫面上。
// 這裡只留 id，實際顯示的資料每次都跟後端要（api.js 的 getRecentArticles）。
//
// 為什麼不存後端：瀏覽這件事本身沒有留下任何紀錄，要做成跨裝置就得建一張表、
// 而且每看一篇文章都要多寫一次。10 筆的清單不值得這個成本。

const STORAGE_KEY = 'forum.recentViewed'
const MAX_ITEMS = 10

// localStorage 在無痕視窗、瀏覽器設定封鎖站台資料時，連讀取都會直接丟例外，
// 所以每一個進出點都要包起來——這個功能壞掉不該讓整個頁面跟著壞
function readRaw() {
  try {
    const parsed = JSON.parse(window.localStorage.getItem(STORAGE_KEY))
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writeRaw(ids) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ids))
  } catch {
    // 寫不進去就算了，最近瀏覽不是關鍵功能
  }
}

// 回傳最近瀏覽的文章 id，最近的排在最前面
export function readRecentViewed() {
  return readRaw().filter((id) => Number.isInteger(id))
}

// 記錄一次瀏覽。呼叫端要自己確認是根文章——樓層不記
export function pushRecentViewed(articleId) {
  const id = Number(articleId)
  if (!Number.isInteger(id)) return

  // 重看同一篇要移到最前面，而不是多出一筆，所以先濾掉舊的再插到頭部
  const next = [id, ...readRecentViewed().filter((x) => x !== id)].slice(0, MAX_ITEMS)
  writeRaw(next)
}

export function clearRecentViewed() {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    // 同上
  }
}
