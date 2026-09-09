<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  article: { type: Object, required: true },
})

// 封面是使用者貼的外部網址，載不到時退回佔位框，不要留一個破圖
const coverFailed = ref(false)
watch(
  () => props.article.coverImage,
  () => {
    coverFailed.value = false
  },
)
</script>

<template>
  <RouterLink
    class="article-card"
    :to="{ name: 'forumDetail', params: { articleId: article.articleId } }"
  >
    <div class="body">
      <div class="head">
        <span class="category">{{ article.categoryName }}</span>
        <!-- 置頂標籤改成跟分類同一列：右上角現在是縮圖的位置 -->
        <span v-if="article.isPinned" class="pinned-badge">置頂</span>
      </div>
      <h3 class="title">{{ article.title }}</h3>
      <div class="meta">
        <span>{{ article.authorNickName }}</span>
        <span :title="`瀏覽數: ${article.viewCount}`">👁 {{ article.viewCount }}</span>
        <span :title="`讚數: ${article.likeCount}`">👍 {{ article.likeCount }}</span>
        <span>
          💬
          <span :title="`蓋樓數: ${article.floorCount}`">{{ article.floorCount }}</span
          >/<span :title="`留言數: ${article.commentCount}`">{{ article.commentCount }}</span>
        </span>
      </div>
    </div>

    <!-- 縮圖位置固定保留，沒有封面圖的卡片也對得齊 -->
    <div class="thumb">
      <img
        v-if="article.coverImage && !coverFailed"
        :src="article.coverImage"
        alt=""
        @error="coverFailed = true"
      />
      <svg
        v-else
        class="thumb-placeholder"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="3" y="3" width="18" height="18" rx="2"></rect>
        <circle cx="8.5" cy="8.5" r="1.5"></circle>
        <polyline points="21 15 16 10 5 21"></polyline>
      </svg>
    </div>
  </RouterLink>
</template>

<style scoped>
.article-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  text-decoration: none;
  color: inherit;
}

.article-card:hover {
  border-color: #2b77c5;
}

.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category {
  font-size: 12px;
  color: #2b77c5;
}

.pinned-badge {
  background-color: #e05a2b;
  color: #ffffff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.title {
  margin: 0;
  font-size: 18px;
  color: #1d324b;
  /* 標題截兩行，否則長標題會把卡片高度撐開，失去固定高度的意義 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: #888888;
}

/* 固定 120×120，圖片依原比例縮到框內置中；直向與橫向的留白對稱 */
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
}

.thumb img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.thumb-placeholder {
  width: 28px;
  height: 28px;
  color: #ccd3dc;
}
</style>
