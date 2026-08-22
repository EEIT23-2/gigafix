<script setup>
import { ARTICLE_STATUS_MAP, statusBadgeClass, statusLabel } from '../../adminStatusMaps'

defineProps({
  articles: { type: Array, default: () => [] },
})

const emit = defineEmits(['view', 'toggle-pin'])

function formatDate(value) {
  return value?.slice(0, 19)?.replace('T', ' ') ?? '-'
}

function parentArticleLabel(article) {
  return article.parentArticleId != null ? `樓層（根文章 #${article.parentArticleId}）` : '—'
}
</script>

<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th>ID</th>
          <th>標題</th>
          <th>分類</th>
          <th>作者</th>
          <th>所屬文章</th>
          <th>狀態</th>
          <th>置頂</th>
          <th class="text-end">瀏覽/讚/留言</th>
          <th>建立時間</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="article in articles" :key="article.articleId">
          <td>{{ article.articleId }}</td>
          <td>
            <button class="article-title-button fw-semibold" type="button" @click="emit('view', article)">
              {{ article.title || '（無標題）' }}
            </button>
          </td>
          <td>{{ article.categoryName }}</td>
          <td>{{ article.authorNickName }}（#{{ article.authorId }}）</td>
          <td>{{ parentArticleLabel(article) }}</td>
          <td>
            <span class="badge" :class="statusBadgeClass(ARTICLE_STATUS_MAP, article.status)">
              {{ statusLabel(ARTICLE_STATUS_MAP, article.status) }}
            </span>
          </td>
          <td>
            <button
              class="btn btn-sm"
              :class="article.isPinned ? 'btn-warning' : 'btn-outline-secondary'"
              type="button"
              @click="emit('toggle-pin', article)"
            >
              {{ article.isPinned ? '📌 已置頂（點擊取消）' : '設為置頂' }}
            </button>
          </td>
          <td class="text-end font-monospace">
            {{ article.viewCount }} / {{ article.likeCount }} / {{ article.commentCount }}
          </td>
          <td>{{ formatDate(article.articleCreatedTime) }}</td>
        </tr>
        <tr v-if="articles.length === 0">
          <td colspan="9" class="text-center text-secondary py-5">沒有文章資料</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.article-title-button {
  display: block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0;
  color: #111827;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}
.article-title-button:hover {
  color: #0d6efd;
  text-decoration: underline;
}
th {
  white-space: nowrap;
  font-size: 0.8rem;
  letter-spacing: 0.03em;
}
td {
  font-size: 0.875rem;
}
</style>
