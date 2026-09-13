<script setup>
import { RouterLink, RouterView } from "vue-router";

const priceMenuItems = [
  {
    label: "iPhone 收購價目表",
    icon: "bi-phone",
    to: { name: "recycle-iphone-prices" },
  },
  {
    label: "iPad 收購價目表",
    icon: "bi-tablet",
    to: { name: "recycle-ipad-prices" },
  },
  {
    label: "Apple Watch 收購價目表",
    icon: "bi-smartwatch",
    to: { name: "recycle-watch-prices" },
  },
];
</script>

<template>
  <div class="recycle-layout">
    <aside class="recycle-sidebar">
      <div class="sidebar-inner">
        <div class="sidebar-heading">
          <span class="eyebrow">RECYCLE</span>
          <h1>二手裝置回收</h1>
        </div>

        <nav class="recycle-menu" aria-label="二手裝置回收選單">
          <RouterLink
            :to="{ name: 'recycle-form' }"
            class="menu-item"
            active-class="active"
          >
            <i class="bi bi-recycle" aria-hidden="true"></i>
            <span>二手機回收申請</span>
            <i class="bi bi-chevron-right menu-arrow" aria-hidden="true"></i>
          </RouterLink>

          <RouterLink
            v-for="item in priceMenuItems"
            :key="item.label"
            :to="item.to"
            class="menu-item"
            active-class="active"
          >
            <i class="bi" :class="item.icon" aria-hidden="true"></i>
            <span>{{ item.label }}</span>
            <i class="bi bi-chevron-right menu-arrow" aria-hidden="true"></i>
          </RouterLink>
        </nav>
      </div>
    </aside>

    <section class="recycle-view">
      <RouterView />
    </section>
  </div>
</template>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;500;600;700&family=Noto+Sans+TC:wght@400;500;600;700&display=swap");

.recycle-layout {
  --ink: #1b1b1b;
  --muted: #635d5e;
  --soft: #f9f9f9;
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 48px;
  width: min(100%, 1480px);
  min-height: 100%;
  margin: 0 auto;
  padding: 64px 5rem 96px;
  color: var(--ink);
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
}

.recycle-sidebar {
  min-width: 0;
}

.sidebar-inner {
  position: sticky;
  top: 40px;
}

.sidebar-heading {
  margin-bottom: 28px;
}

.eyebrow {
  display: block;
  margin-bottom: 8px;
  color: #005ab7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.sidebar-heading h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.recycle-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  /* 提高回收導覽的字級與點擊高度，讓桌面版選項更容易閱讀。 */
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  padding: 14px 16px;
  border: 0;
  border-left: 2px solid transparent;
  border-radius: 0 10px 10px 0;
  color: var(--muted);
  background: transparent;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.35;
  text-decoration: none;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.menu-item:hover {
  color: var(--ink);
  background: #f3f3f3;
}

.menu-item.active {
  border-left-color: var(--ink);
  color: var(--ink);
  background: var(--soft);
}

.menu-item > i:first-child {
  font-size: 20px;
  text-align: center;
}

.menu-arrow {
  font-size: 12px;
}

.recycle-view {
  min-width: 0;
}

@media (max-width: 991.98px) {
  .recycle-layout {
    grid-template-columns: 210px minmax(0, 1fr);
    gap: 32px;
    padding: 56px 32px 80px;
  }
}

@media (max-width: 767.98px) {
  .recycle-layout {
    grid-template-columns: 150px minmax(0, 1fr);
    gap: 18px;
    padding: 40px 16px 64px;
  }

  .sidebar-heading h1 {
    font-size: 20px;
  }

  .menu-item {
    grid-template-columns: minmax(0, 1fr);
    gap: 4px;
    padding: 11px 10px;
    font-size: 14px;
  }

  .menu-item > i {
    display: none;
  }
}
</style>
