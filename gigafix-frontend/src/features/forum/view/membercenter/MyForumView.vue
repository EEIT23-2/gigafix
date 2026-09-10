<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useFetchMemberInfoStore } from '@/stores/member'
import {
  getMyArticles,
  getBookmarks,
  getRecentArticles,
  updateArticleStatus,
  deleteDraft,
  removeBookmark,
} from '../../api'
import { ARTICLE_STATUS_MAP, statusLabel, statusBadgeClass } from '../../adminStatusMaps'
import { readRecentViewed, clearRecentViewed } from '../../utils/recentViewed'
import { normalizeTab, memberForumOrigin } from '../../utils/memberForumNav'

const route = useRoute()
const router = useRouter()
const { memberInfo } = storeToRefs(useFetchMemberInfoStore())

// 後端 EDIT_BLOCKED_STATUSES：這些狀態下不能再編輯
const EDIT_BLOCKED_STATUSES = ['CLOSED', 'FORCE_CLOSED', 'TAKEN_DOWN']
// 會員只能在自己造成的狀態之間切換，被管理員處分過的（FORCE_*）動不了
const MEMBER_OWNED_STATUSES = ['PUBLISHED', 'HIDDEN', 'CLOSED']

// 分頁狀態放網址：離開這頁再回來（看文章、編輯、瀏覽器上一頁）才停得回原本的分頁
const activeTab = computed(() => normalizeTab(route.query.tab))
const loading = ref(false)
const errorMessage = ref('')
// 破壞性動作的同列二次確認，跟 CategoryAdminPanel 刪分類同一種模式
const confirmingId = ref(null)

const myArticles = ref([])
const bookmarks = ref([])
const recent = ref([])

// 我的文章專用的前端篩選
const categoryFilter = ref('all')
const sortBy = ref('latest')

// getMyArticles 一次回傳全部（含草稿與自己蓋的樓層），在前端切成兩個分頁
const articles = computed(() => myArticles.value.filter((a) => a.status !== 'DRAFT'))
const drafts = computed(() => myArticles.value.filter((a) => a.status === 'DRAFT'))

const categoryOptions = computed(() => {
  const names = []
  for (const a of articles.value) {
    if (a.categoryName && !names.includes(a.categoryName)) names.push(a.categoryName)
  }
  return names
})

const visibleArticles = computed(() => {
  let list = articles.value
  if (categoryFilter.value !== 'all') {
    list = list.filter((a) => a.categoryName === categoryFilter.value)
  }
  // 對齊後端 searchOrderByPopularity 的排序鍵：讚數 → 留言數 → 瀏覽數（不是瀏覽數優先）
  return [...list].sort((a, b) => {
    if (sortBy.value === 'popular') {
      return (
        (b.likeCount ?? 0) - (a.likeCount ?? 0) ||
        (b.commentCount ?? 0) - (a.commentCount ?? 0) ||
        (b.viewCount ?? 0) - (a.viewCount ?? 0)
      )
    }
    return (b.articleCreatedTime ?? '').localeCompare(a.articleCreatedTime ?? '')
  })
})

const tabs = computed(() => [
  { id: 'articles', label: '我的文章', count: articles.value.length },
  { id: 'drafts', label: '我的草稿', count: drafts.value.length },
  { id: 'bookmarks', label: '我的收藏', count: bookmarks.value.length },
  { id: 'activity', label: '活動', count: recent.value.length },
])

function formatDate(value) {
  return value ? value.slice(0, 10).replace(/-/g, '/') : ''
}

function formatDateTime(value) {
  return value ? value.slice(0, 16).replace('T', ' ').replace(/-/g, '/') : ''
}

function isFloor(article) {
  return article.parentArticleId != null
}

function canEdit(article) {
  return !EDIT_BLOCKED_STATUSES.includes(article.status)
}

function canChangeStatus(article) {
  return MEMBER_OWNED_STATUSES.includes(article.status)
}

async function loadAll() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [mine, marks] = await Promise.all([getMyArticles(), getBookmarks()])
    myArticles.value = mine
    bookmarks.value = marks
    // 最近瀏覽的順序只有 localStorage 知道，後端不保證回傳順序，所以拿回來後自己重排
    const ids = readRecentViewed()
    const fetched = await getRecentArticles(ids)
    recent.value = ids
      .map((id) => fetched.find((a) => a.articleId === id))
      .filter((a) => a != null)
  } catch (error) {
    console.error(error)
    errorMessage.value =
      error.response?.status === 401
        ? '登入已過期，請重新登入'
        : '載入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

function goToManage(article) {
  router.push({
    name: 'member-forum-detail',
    params: { articleId: article.articleId },
    query: { tab: activeTab.value },
  })
}

// 去前台文章頁要留下來源，那一頁的返回鍵才知道要走回哪個分頁
function goToPublic(articleId) {
  router.push({
    name: 'forumDetail',
    params: { articleId },
    query: memberForumOrigin(activeTab.value),
  })
}

function goToEdit(articleId) {
  router.push({
    name: 'member-forum-edit',
    params: { articleId },
    query: { tab: activeTab.value },
  })
}

async function handleToggleHidden(article) {
  errorMessage.value = ''
  const target = article.status === 'HIDDEN' ? 'PUBLISHED' : 'HIDDEN'
  try {
    const updated = await updateArticleStatus(article.articleId, target)
    // 以後端回傳的狀態為準，而不是前端推算的值
    article.status = updated.status
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response?.data?.message || '狀態變更失敗'
  }
}

async function handleDiscardDraft(draft) {
  errorMessage.value = ''
  try {
    await deleteDraft(draft.articleId)
    myArticles.value = myArticles.value.filter((a) => a.articleId !== draft.articleId)
    confirmingId.value = null
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response?.data?.message || '捨棄草稿失敗'
  }
}

async function handleRemoveBookmark(bookmark) {
  errorMessage.value = ''
  try {
    await removeBookmark(bookmark.article.articleId)
    bookmarks.value = bookmarks.value.filter((b) => b.bookmarkId !== bookmark.bookmarkId)
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response?.data?.message || '取消收藏失敗'
  }
}

function handleClearRecent() {
  clearRecentViewed()
  recent.value = []
}

function switchTab(id) {
  // 用 replace 不是 push：切四個分頁不該在瀏覽器歷史裡塞四筆，上一頁應該直接離開這一頁
  router.replace({ query: { ...route.query, tab: id } })
}

// switchTab 不再直接改狀態了，換分頁時要清掉的東西改用 watch 處理
watch(activeTab, () => {
  confirmingId.value = null
  errorMessage.value = ''
})
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between gap-3 mb-4">
      <h2 class="mb-0">我的討論</h2>
      <RouterLink v-if="memberInfo" class="btn btn-primary" :to="{ name: 'member-forum-create' }">
        <i class="bi bi-plus-lg me-1"></i>發表文章
      </RouterLink>
    </div>

    <!-- 會員中心本來就靠 MemberCenterNavBar 擋未登入，這裡只是重新整理時的保險 -->
    <p v-if="!memberInfo" class="alert alert-warning">請先登入會員才能查看我的討論。</p>

    <template v-else>
      <ul class="nav nav-tabs mb-4">
        <li v-for="tab in tabs" :key="tab.id" class="nav-item">
          <button
            class="nav-link"
            :class="{ active: activeTab === tab.id }"
            type="button"
            @click="switchTab(tab.id)"
          >
            {{ tab.label }}
            <span class="badge rounded-pill text-bg-light border ms-1">{{ tab.count }}</span>
          </button>
        </li>
      </ul>

      <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <div class="mt-2">載入中...</div>
      </div>

      <template v-else>
        <!-- ─────────── 我的文章 ─────────── -->
        <template v-if="activeTab === 'articles'">
          <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
            <select v-model="categoryFilter" class="form-select filter-select">
              <option value="all">全部分類</option>
              <option v-for="name in categoryOptions" :key="name" :value="name">{{ name }}</option>
            </select>
            <select v-model="sortBy" class="form-select filter-select">
              <option value="latest">最新</option>
              <option value="popular">熱門</option>
            </select>
            <span class="text-secondary small">共 {{ visibleArticles.length }} 篇</span>
          </div>

          <div
            v-for="article in visibleArticles"
            :key="article.articleId"
            class="row-card"
            @click="goToManage(article)"
          >
            <div class="row-body">
              <div class="row-head">
                <span class="category">{{ article.categoryName }}</span>
                <span v-if="isFloor(article)" class="tag-floor">樓層</span>
                <span class="badge" :class="statusBadgeClass(ARTICLE_STATUS_MAP, article.status)">
                  {{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}
                </span>
              </div>
              <h3 class="row-title">{{ article.title }}</h3>
              <div class="row-foot">
                <div class="meta">
                  <span>{{ formatDate(article.articleCreatedTime) }} 發表</span>
                  <span><i class="bi bi-eye"></i>{{ article.viewCount }}</span>
                  <span><i class="bi bi-hand-thumbs-up"></i>{{ article.likeCount }}</span>
                  <span><i class="bi bi-chat"></i>{{ article.commentCount }}</span>
                </div>
                <div class="actions" @click.stop>
                  <button
                    class="act"
                    type="button"
                    :disabled="!canEdit(article)"
                    :title="canEdit(article) ? '編輯' : '目前狀態不允許編輯'"
                    @click="goToEdit(article.articleId)"
                  >
                    編輯
                  </button>
                  <button
                    class="act"
                    type="button"
                    :disabled="!canChangeStatus(article)"
                    :title="canChangeStatus(article) ? '切換隱藏狀態' : '管理員處分中，無法自行變更'"
                    @click="handleToggleHidden(article)"
                  >
                    {{ article.status === 'HIDDEN' ? '取消隱藏' : '隱藏' }}
                  </button>
                </div>
              </div>
            </div>
            <div class="thumb">
              <img v-if="article.coverImage" :src="article.coverImage" alt="" />
              <i v-else class="bi bi-image"></i>
            </div>
          </div>

          <p v-if="visibleArticles.length === 0" class="empty">
            {{ categoryFilter === 'all' ? '你還沒有發表過文章' : '這個分類底下沒有你的文章' }}
          </p>
        </template>

        <!-- ─────────── 我的草稿 ─────────── -->
        <template v-else-if="activeTab === 'drafts'">
          <div
            v-for="draft in drafts"
            :key="draft.articleId"
            class="row-card"
            @click="goToManage(draft)"
          >
            <div class="row-body">
              <div class="row-head">
                <span class="category">{{ draft.categoryName }}</span>
              </div>
              <h3 class="row-title">{{ draft.title || '（尚未填標題）' }}</h3>
              <div class="row-foot">
                <div class="meta">
                  <span>{{ formatDateTime(draft.articleUpdatedTime) }} 最後編輯</span>
                </div>
                <div class="actions" @click.stop>
                  <template v-if="confirmingId === draft.articleId">
                    <span class="confirm-text">捨棄後無法復原</span>
                    <button class="act act-danger-solid" type="button" @click="handleDiscardDraft(draft)">
                      確定
                    </button>
                    <button class="act" type="button" @click="confirmingId = null">取消</button>
                  </template>
                  <template v-else>
                    <button class="act" type="button" @click="goToEdit(draft.articleId)">繼續編輯</button>
                    <button class="act act-danger" type="button" @click="confirmingId = draft.articleId">
                      捨棄
                    </button>
                  </template>
                </div>
              </div>
            </div>
            <div class="thumb">
              <img v-if="draft.coverImage" :src="draft.coverImage" alt="" />
              <i v-else class="bi bi-image"></i>
            </div>
          </div>

          <p v-if="drafts.length === 0" class="empty">目前沒有未完成的草稿</p>
        </template>

        <!-- ─────────── 我的收藏 ─────────── -->
        <template v-else-if="activeTab === 'bookmarks'">
          <div
            v-for="bookmark in bookmarks"
            :key="bookmark.bookmarkId"
            class="row-card"
            @click="bookmark.article.visible && goToPublic(bookmark.article.articleId)"
          >
            <div class="row-body">
              <div class="row-head">
                <span class="category">{{ bookmark.article.categoryName }}</span>
              </div>
              <!-- 收藏後文章可能被作者隱藏或被管理員下架，後端會遮蔽標題並帶回說明 -->
              <h3 v-if="bookmark.article.visible" class="row-title">{{ bookmark.article.title }}</h3>
              <h3 v-else class="row-title row-title-masked">
                {{ bookmark.article.visibilityMessage }}
              </h3>
              <div class="row-foot">
                <div class="meta">
                  <span>{{ bookmark.article.authorNickName }}</span>
                  <span>{{ formatDate(bookmark.bookmarkCreatedTime) }} 收藏</span>
                  <template v-if="bookmark.article.visible">
                    <span><i class="bi bi-eye"></i>{{ bookmark.article.viewCount }}</span>
                    <span><i class="bi bi-hand-thumbs-up"></i>{{ bookmark.article.likeCount }}</span>
                    <span><i class="bi bi-chat"></i>{{ bookmark.article.commentCount }}</span>
                  </template>
                </div>
                <div class="actions" @click.stop>
                  <button class="act" type="button" @click="handleRemoveBookmark(bookmark)">
                    取消收藏
                  </button>
                </div>
              </div>
            </div>
            <div class="thumb">
              <img v-if="bookmark.article.coverImage" :src="bookmark.article.coverImage" alt="" />
              <i v-else class="bi bi-image"></i>
            </div>
          </div>

          <p v-if="bookmarks.length === 0" class="empty">還沒有收藏任何文章</p>
        </template>

        <!-- ─────────── 活動：最近瀏覽 ─────────── -->
        <template v-else>
          <section class="recent-card">
            <header class="recent-head">
              <div class="d-flex align-items-baseline gap-2">
                <h3 class="recent-title">最近瀏覽</h3>
                <span class="text-secondary small">只保留最近 10 篇，記錄在這台裝置上</span>
              </div>
              <button class="act" type="button" @click="handleClearRecent">清除紀錄</button>
            </header>

            <div
              v-for="article in recent"
              :key="article.articleId"
              class="recent-row"
              @click="goToPublic(article.articleId)"
            >
              <span class="recent-category">{{ article.categoryName }}</span>
              <span class="recent-name">{{ article.title }}</span>
              <span class="recent-author">{{ article.authorNickName }}</span>
            </div>

            <p v-if="recent.length === 0" class="empty mb-0">還沒有瀏覽紀錄</p>
          </section>
        </template>
      </template>
    </template>
  </div>
</template>

<style scoped>
.filter-select {
  max-width: 160px;
}

/* 卡片沿用前台 ArticleCard 的量法：固定高度、右側 120×120 縮圖槽 */
.row-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  background-color: #ffffff;
  cursor: pointer;
}

.row-card:hover {
  border-color: #2b77c5;
}

.row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.row-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category {
  font-size: 12px;
  color: #2b77c5;
}

.tag-floor {
  padding: 2px 8px;
  border-radius: 4px;
  background-color: #eef4fb;
  color: #2b77c5;
  font-size: 11px;
}

.row-title {
  margin: 6px 0 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: #1d324b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.row-title-masked {
  color: #888888;
  font-weight: 500;
  font-style: italic;
}

/* meta 與行內動作共用一列，卡片高度才不會被動作鈕撐開 */
.row-foot {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #888888;
  min-width: 0;
}

.meta i {
  margin-right: 4px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.act {
  padding: 4px 10px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background-color: #ffffff;
  color: #555555;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
}

.act:hover:not(:disabled) {
  border-color: #2b77c5;
  background-color: #f2f6fb;
  color: #2b77c5;
}

.act:disabled {
  border-color: #dee2e6;
  color: #adb5bd;
  cursor: not-allowed;
}

.act-danger {
  border-color: #e2b6b1;
  color: #c0392b;
}

.act-danger:hover:not(:disabled) {
  border-color: #c0392b;
  background-color: #fdecea;
  color: #c0392b;
}

.act-danger-solid {
  border-color: #c0392b;
  background-color: #c0392b;
  color: #ffffff;
}

.act-danger-solid:hover:not(:disabled) {
  border-color: #a93226;
  background-color: #a93226;
  color: #ffffff;
}

.confirm-text {
  font-size: 13px;
  color: #c0392b;
  white-space: nowrap;
}

.thumb {
  flex-shrink: 0;
  width: 120px;
  height: 120px;
  border-radius: 6px;
  background-color: #f6f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #ccd3dc;
  font-size: 28px;
}

.thumb img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.empty {
  border: 1px dashed #dee2e6;
  border-radius: 8px;
  padding: 48px 24px;
  text-align: center;
  color: #888888;
}

/* 活動是 10 筆的快照，列刻意做得比上面窄 */
.recent-card {
  border: 1px solid #eaeaea;
  border-radius: 8px;
  background-color: #ffffff;
  overflow: hidden;
}

.recent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #eaeaea;
}

.recent-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d324b;
}

.recent-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border-bottom: 1px solid #f1f3f5;
  cursor: pointer;
}

.recent-row:last-child {
  border-bottom: 0;
}

.recent-row:hover {
  background-color: #f8fafc;
}

.recent-category {
  flex-shrink: 0;
  width: 76px;
  font-size: 12px;
  color: #2b77c5;
}

.recent-name {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  color: #1d324b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-author {
  flex-shrink: 0;
  font-size: 13px;
  color: #888888;
}
</style>
