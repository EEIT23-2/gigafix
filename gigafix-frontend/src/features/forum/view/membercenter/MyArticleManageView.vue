<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle, updateArticleStatus } from '../../api'
import { ARTICLE_STATUS_MAP, statusLabel, statusBadgeClass } from '../../adminStatusMaps'
import { sanitizeHtml } from '../../htmlContent'
import { normalizeTab, backToMemberForum, memberForumOrigin } from '../../utils/memberForumNav'

const route = useRoute()
const router = useRouter()

// 後端 EDIT_BLOCKED_STATUSES：這些狀態下不能再編輯
const EDIT_BLOCKED_STATUSES = ['CLOSED', 'FORCE_CLOSED', 'TAKEN_DOWN']
// 會員只能在自己造成的狀態之間切換；被管理員處分過的（FORCE_*）與已下架的都動不了
const MEMBER_OWNED_STATUSES = ['PUBLISHED', 'HIDDEN', 'CLOSED']

const articleId = computed(() => route.params.articleId)
// 這頁夾在列表與編輯／公開頁中間，要把來源分頁一路接力下去，
// 否則從「我的草稿」進來、返回時會落到「我的文章」
const fromTab = computed(() => normalizeTab(route.query.tab))
const article = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const working = ref(false)
const confirmingTakeDown = ref(false)

const isFloor = computed(() => article.value?.parentArticleId != null)
const canEdit = computed(() => !!article.value && !EDIT_BLOCKED_STATUSES.includes(article.value.status))
const canChangeStatus = computed(
  () => !!article.value && MEMBER_OWNED_STATUSES.includes(article.value.status),
)
const isClosed = computed(() => article.value?.status === 'CLOSED')
const isHidden = computed(() => article.value?.status === 'HIDDEN')
const isTakenDown = computed(() => article.value?.status === 'TAKEN_DOWN')

// 停用編輯時要講清楚原因，不然使用者只看到一顆按不動的按鈕
const editBlockedReason = computed(() => {
  if (canEdit.value) return ''
  if (isTakenDown.value) return '文章已下架，不能再編輯或調整狀態。要恢復請聯絡管理員。'
  if (isClosed.value) return '留言關閉期間無法編輯內文（與後端規則一致）。重新開放留言後就能繼續編輯。'
  return '目前狀態不允許編輯。'
})

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    article.value = await getArticle(articleId.value)
  } catch (error) {
    console.error(error)
    errorMessage.value =
      error.response?.status === 401 ? '登入已過期，請重新登入' : '文章不存在或載入失敗'
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function changeStatus(target) {
  working.value = true
  errorMessage.value = ''
  try {
    // 以後端回傳的狀態為準，而不是前端推算的值
    const updated = await updateArticleStatus(articleId.value, target)
    article.value.status = updated.status
    confirmingTakeDown.value = false
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response?.data?.message || '狀態變更失敗'
  } finally {
    working.value = false
  }
}

function formatDateTime(value) {
  return value ? value.slice(0, 16).replace('T', ' ').replace(/-/g, '/') : ''
}

function goBack() {
  router.push(backToMemberForum(fromTab.value))
}

function goToEdit() {
  router.push({
    name: 'member-forum-edit',
    params: { articleId: articleId.value },
    query: { tab: fromTab.value },
  })
}

function goToPublic() {
  // 樓層沒有自己的公開頁，要看要回到它所屬的討論串
  const target = isFloor.value ? article.value.parentArticleId : articleId.value
  router.push({
    name: 'forumDetail',
    params: { articleId: target },
    query: memberForumOrigin(fromTab.value),
  })
}
</script>

<template>
  <div>
    <button class="back-link" type="button" @click="goBack">
      <i class="bi bi-arrow-left me-1"></i>返回我的討論
    </button>

    <div v-if="errorMessage" class="alert alert-danger mt-3">{{ errorMessage }}</div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
      <div class="mt-2">載入中...</div>
    </div>

    <template v-else-if="article">
      <!-- 文章資訊 -->
      <section class="panel">
        <div class="d-flex align-items-start gap-4">
          <div class="flex-grow-1 min-w-0">
            <div class="d-flex align-items-center gap-2">
              <span class="category">{{ article.categoryName }}</span>
              <span v-if="isFloor" class="tag-floor">樓層</span>
              <span class="badge" :class="statusBadgeClass(ARTICLE_STATUS_MAP, article.status)">
                {{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}
              </span>
            </div>
            <h2 class="panel-title">{{ article.title }}</h2>
            <div class="times">
              <span>{{ formatDateTime(article.articleCreatedTime) }} 發表</span>
              <span v-if="article.articleEditedTime">
                {{ formatDateTime(article.articleEditedTime) }} 最後編輯
              </span>
            </div>
            <div class="stats">
              <span><i class="bi bi-eye"></i>{{ article.viewCount }} 次瀏覽</span>
              <span><i class="bi bi-hand-thumbs-up"></i>{{ article.likeCount }} 個讚</span>
              <span><i class="bi bi-chat"></i>{{ article.commentCount }} 則留言</span>
            </div>
          </div>

          <div class="cover">
            <img v-if="article.coverImage" :src="article.coverImage" alt="" />
            <i v-else class="bi bi-image"></i>
          </div>
        </div>
      </section>

      <!-- 動作區 -->
      <section class="panel">
        <div class="d-flex flex-wrap align-items-center gap-2">
          <button class="btn btn-outline-primary" type="button" :disabled="!canEdit" @click="goToEdit">
            <i class="bi bi-pencil-square me-1"></i>{{ isFloor ? '編輯內文' : '編輯文章' }}
          </button>

          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="!canChangeStatus || working"
            @click="changeStatus(isHidden ? 'PUBLISHED' : 'HIDDEN')"
          >
            <i class="bi bi-eye-slash me-1"></i>{{ isHidden ? '取消隱藏' : '隱藏' }}
          </button>

          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="!canChangeStatus || working"
            @click="changeStatus(isClosed ? 'PUBLISHED' : 'CLOSED')"
          >
            <i class="bi bi-lock me-1"></i>{{ isClosed ? '重新開放留言' : '關閉留言' }}
          </button>

          <button
            class="btn btn-outline-danger"
            type="button"
            :disabled="!canChangeStatus || working"
            @click="confirmingTakeDown = true"
          >
            <i class="bi bi-archive me-1"></i>下架
          </button>

          <button class="btn btn-link ms-auto" type="button" @click="goToPublic">
            {{ isFloor ? '查看所屬討論串' : '查看公開頁面' }}
          </button>
        </div>

        <p v-if="!canEdit" class="hint">{{ editBlockedReason }}</p>

        <!-- 下架是單向的：會員自己改不回來，而且 getMyArticles 會濾掉 TAKEN_DOWN，
             文章會直接從列表消失，所以一定要在按下去之前講清楚 -->
        <div v-if="confirmingTakeDown" class="alert alert-danger mt-3">
          <div class="d-flex align-items-start gap-2">
            <i class="bi bi-exclamation-triangle-fill mt-1"></i>
            <div>
              <p class="mb-1 fw-semibold">確定要下架{{ isFloor ? '這個樓層' : '這篇文章' }}嗎？</p>
              <p class="mb-0 small">
                下架後會從討論區消失，留言也一併隱藏，而且<strong>不會再出現在你的「我的文章」列表</strong>。
                你無法自己改回其他狀態，需要聯絡管理員才能恢復。
              </p>
            </div>
          </div>
          <div class="d-flex gap-2 mt-3">
            <button
              class="btn btn-sm btn-danger"
              type="button"
              :disabled="working"
              @click="changeStatus('TAKEN_DOWN')"
            >
              {{ working ? '處理中...' : '確定下架' }}
            </button>
            <button class="btn btn-sm btn-outline-secondary" type="button" @click="confirmingTakeDown = false">
              取消
            </button>
          </div>
        </div>
      </section>

      <!-- 內文 -->
      <section class="panel">
        <h3 class="section-label">內文</h3>
        <div class="content" v-html="sanitizeHtml(article.content)"></div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.back-link {
  padding: 0;
  border: 0;
  background: none;
  color: #2b77c5;
  font-size: 14px;
  cursor: pointer;
}

.back-link:hover {
  color: #1f5b99;
}

.panel {
  margin-top: 16px;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 24px;
  background-color: #ffffff;
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

.panel-title {
  margin: 8px 0 0;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.35;
  color: #1d324b;
}

.times {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #888888;
}

.stats {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 14px;
  color: #555555;
}

.stats i {
  margin-right: 6px;
}

.cover {
  flex-shrink: 0;
  width: 140px;
  height: 140px;
  border-radius: 6px;
  background-color: #f6f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #ccd3dc;
  font-size: 32px;
}

.cover img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.hint {
  margin: 14px 0 0;
  font-size: 13px;
  color: #888888;
}

.section-label {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1d324b;
}

.content {
  font-size: 15px;
  line-height: 1.85;
  color: #444444;
}

.content :deep(img) {
  max-width: 100%;
}
</style>
