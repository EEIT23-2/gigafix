<script setup>
import ClientNavBar from '@/components/ClientNavBar.vue';
import { RouterView } from 'vue-router';
</script>

<template>
  <div class="app-layout">
    <!-- 引入導覽列 -->
    <ClientNavBar />

    <!-- main 負責 flex: 1，會自動把 footer 往下推 -->
    <main class="main-content">
      <RouterView />
    </main>

    <!-- 共用頁尾 -->
    <footer class="site-footer">
      <p>© Gigafix 機不可失 - 歡迎聯絡我們以取得您最好的手機</p>
    </footer>
  </div>
</template>

<style scoped>
.app-layout {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans TC", sans-serif;
  color: #333333;
  background-color: #ffffff;
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1; /* 只要 RouterView 沒有內容或內容很少，會自動撐開空白區域 */
}

.layout-body {
  flex: 1;
}

.site-footer {
  background-color: #1a1a1a;
  color: #888888;
  text-align: center;
  padding: 16px 0;
  font-size: 12px;
}
</style>
