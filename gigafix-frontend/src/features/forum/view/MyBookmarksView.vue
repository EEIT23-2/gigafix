<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { getBookmarks } from '../api'
import ArticleCard from '../components/ArticleCard.vue'
import { useFetchMemberInfoStore } from '@/stores/member'

const { memberInfo } = storeToRefs(useFetchMemberInfoStore())

const bookmarks = ref([])
const loading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  // 沒登入的話畫面交給模板的 v-else 內嵌提示處理，這裡不用做任何跳轉或 alert
  if (!memberInfo.value) {
    loading.value = false
    return
  }
  try {
    bookmarks.value = await getBookmarks()
  } catch {
    errorMessage.value = '收藏列表載入失敗，請確認後端服務是否啟動'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="my-bookmarks-view">
    <h1>我的收藏</h1>
    <p v-if="!memberInfo" class="alert alert-warning">請先登入會員才能查看收藏。</p>
    <template v-else>
      <p v-if="loading">載入中...</p>
      <p v-else-if="errorMessage" class="error">{{ errorMessage }}</p>
      <template v-else>
        <ArticleCard v-for="bookmark in bookmarks" :key="bookmark.bookmarkId" :article="bookmark.article" />
        <p v-if="bookmarks.length === 0" class="empty">還沒有收藏任何文章</p>
      </template>
    </template>
  </div>
</template>

<style scoped>
.my-bookmarks-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.empty {
  text-align: center;
  color: #999999;
  padding: 40px 0;
}

.error {
  text-align: center;
  color: #c0392b;
  padding: 40px 0;
}
</style>
