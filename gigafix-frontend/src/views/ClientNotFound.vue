<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="not-found">
    <p class="code">404</p>
    <h1 class="title">找不到這個頁面</h1>
    <p class="hint">網址可能打錯了，或是頁面已經被移除。</p>
    <RouterLink to="/" class="home-link">
      <i class="bi bi-house-door"></i>
      回到首頁
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
  color: #2b77c5;
  margin: 0;
  line-height: 1;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #1d324b;
  margin: 16px 0 8px;
}

.hint {
  font-size: 17px;
  color: #666666;
  margin: 0 0 28px;
}

.home-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 8px;
  background-color: #2b77c5;
  color: #ffffff;
  font-size: 17px;
  font-weight: 600;
  text-decoration: none;
  transition: background-color 0.15s ease;
}

.home-link:hover {
  background-color: #235f9e;
  color: #ffffff;
}
</style>
