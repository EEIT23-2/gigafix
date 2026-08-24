<script setup>
defineProps({
  article: { type: Object, required: true },
})
</script>

<template>
  <RouterLink
    class="article-card"
    :to="{ name: 'forumDetail', params: { articleId: article.articleId } }"
  >
    <div v-if="article.isPinned" class="pinned-badge">置頂</div>
    <img v-if="article.coverImage" class="cover" :src="article.coverImage" alt="" />
    <div class="body">
      <span class="category">{{ article.categoryName }}</span>
      <h3 class="title">{{ article.title }}</h3>
      <div class="meta">
        <span>{{ article.authorNickName }}</span>
        <span>👁 {{ article.viewCount }}</span>
        <span>👍 {{ article.likeCount }}</span>
        <span>
          <span :title="`[蓋樓數: ${article.floorCount}]`">🧱{{ article.floorCount }}</span
          >/<span :title="`[留言數: ${article.commentCount}]`">💬{{ article.commentCount }}</span>
        </span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.article-card {
  display: block;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  text-decoration: none;
  color: inherit;
  position: relative;
}

.article-card:hover {
  border-color: #2b77c5;
}

.pinned-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background-color: #e05a2b;
  color: #ffffff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.cover {
  width: 100%;
  max-height: 160px;
  object-fit: cover;
  border-radius: 6px;
  margin-bottom: 10px;
}

.category {
  font-size: 12px;
  color: #2b77c5;
}

.title {
  margin: 6px 0;
  font-size: 18px;
  color: #1d324b;
}

.meta {
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: #888888;
}
</style>
