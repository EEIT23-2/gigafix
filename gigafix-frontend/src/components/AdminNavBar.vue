<script>
import { RouterLink } from "vue-router";
</script>

<template>
  <nav class="nav-menu">
    <RouterLink to="/admin/manager" class="nav-item">
      <span class="icon">📊</span>
      <span>管理員資訊</span>
    </RouterLink>

    <div class="nav-heading">商品訂單相關</div>
    <RouterLink class="nav-item" to="/admin/products">
      <span class="nav-title"><span class="icon">⚙️</span> 商品管理</span>
      <span class="arrow">›</span>
    </RouterLink>
    <RouterLink class="nav-item" to="">
      <span class="nav-title"><span class="icon">🔧</span> 訂單管理</span>
      <span class="arrow">›</span>
    </RouterLink>

    <div class="nav-heading">ADDONS</div>
    <RouterLink class="nav-item" to="">
      <span class="nav-title"><span class="icon">📁</span> 維修單</span>
      <span class="arrow">›</span>
    </RouterLink>
    <RouterLink class="nav-item" to="">
      <span class="nav-title"><span class="icon">📁</span> 論壇</span>
      <span class="arrow">›</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.nav-menu {
  padding: 12px;
}

.nav-heading {
  font-size: 11px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.4);
  padding: 16px 12px 6px 12px;
  letter-spacing: 0.08em;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 13px;
  border-radius: 6px;
  transition:
    background-color 0.2s,
    color 0.2s;
}

.nav-item:hover,
.nav-item.router-link-active {
  background-color: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  font-weight: 600;
}

.nav-item .icon {
  margin-right: 8px;
}

.nav-title {
  display: flex;
  align-items: center;
}

.arrow {
  font-size: 14px;
  opacity: 0.6;
}
</style>
