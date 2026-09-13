<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="not-found">
    <p class="code">404</p>
    <h1 class="title">找不到這個後台頁面</h1>
    <p class="hint">網址可能打錯了，或是頁面已經被移除。</p>
    <RouterLink to="/admin" class="home-link">
      回到後台首頁
    </RouterLink>
  </section>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 24px;
}

.code {
  font-size: 96px;
  font-weight: 800;
  color: #4e73df;
  margin: 0;
  line-height: 1;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #5a5c69;
  margin: 16px 0 8px;
}

.hint {
  font-size: 15px;
  color: #858796;
  margin: 0 0 28px;
}

.home-link {
  display: inline-flex;
  align-items: center;
  padding: 10px 24px;
  border-radius: 6px;
  background-color: #4e73df;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  text-decoration: none;
  transition: background-color 0.15s ease;
}

.home-link:hover {
  background-color: #3a5cc4;
  color: #ffffff;
}
</style>
