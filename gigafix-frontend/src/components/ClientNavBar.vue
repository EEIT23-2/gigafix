<script>
import { RouterLink } from 'vue-router'
</script>

<template>
  <header class="site-header-wrapper">
    <div class="top-announcement-bar">
      <span>加入會員就送100元購物金｜全館購物滿 $ 599 免運</span>  
      <!-- 這個應該在layout或是抽成元件????? -->
    </div>

    <div class="site-header">
      <div class="header-inner">
        <!-- 右上方功能按鈕 -->
        <nav class="user-actions">
            <RouterLink class="action-item"> <!-- 放大鏡icon，可能可以寫搜尋商品功能 -->
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                
            </RouterLink>  <!-- 小人icon，連結到member path -->
            <RouterLink class="action-item" active-class="active" to="/gigafix/member"> <!-- 放大鏡icon -->
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
            </RouterLink>
            <RouterLink class="action-item"> <!-- 購物車icon -->
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
            </RouterLink>
        </nav>

        <!-- 導覽區 -->
        <nav class="main-nav">
          <div class="logo-container">
            <div class="logo-mark">G</div>
            <div class="logo-text">
              <div class="logo-title">Gigafix<span>機不可失</span></div>
              <div class="logo-sub">物美價廉您最好的選擇</div>
            </div>
          </div>

          <!-- 選單項目置中 -->
          <ul class="nav-list">
            <router-link class="nav-item" >最新活動 ▾</router-link>
            <router-link class="nav-item" >二手手機 ▾</router-link>
            <router-link class="nav-item" >維修手機 ▾</router-link>
            <router-link class="nav-item" :to="{ name: 'forumList' }">Gigafix討論區</router-link>
            <router-link class="nav-item">關於Gigafix</router-link>
          </ul>
        </nav>
      </div>
    </div>
  </header>
</template>

<style scoped>
.top-announcement-bar {
  background-color: #1e3557;
  color: #ffffff;
  text-align: center;
  font-size: 11px;
  padding: 5px 0;
  letter-spacing: 0.5px;
}

.site-header {
  border-bottom: 1px solid #eaeaea;
  background-color: #ffffff;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px 20px 12px;
}

.user-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  font-size: 11px;
  color: #666666;
  margin-bottom: 8px;
}

.action-item {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.action-item .icon {
  width: 15px;
  height: 15px;
}

/* 主導覽列容器 */
.main-nav {
  display: flex;
  align-items: center;
  position: relative;
}

/* Logo 靠左 */
.logo-container {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  flex-shrink: 0;
}

.logo-mark {
  background-color: #2b77c5;
  color: #ffffff;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 900;
  font-style: italic;
}

.logo-title {
  font-size: 20px;
  font-weight: 800;
  color: #1d324b;
  line-height: 1;
}

.logo-title span {
  font-size: 14px;
  font-weight: 400;
  color: #7b94ad;
  margin-left: 2px;
}

.logo-sub {
  font-size: 9px;
  letter-spacing: 3px;
  color: #555555;
  text-align: left;
  margin-top: 3px;
}

/* 選單清單：透過 margin: 0 auto 自動推到中間 */
.nav-list {
  display: flex;
  align-items: center;
  justify-content: center;
  list-style: none;
  gap: 18px;
  padding: 0;
  margin: 0 auto;
  flex-wrap: wrap;
}

.nav-item {
  font-size: 16px;
  color: #444444;
  cursor: pointer;
  white-space: nowrap;
}

.nav-item:hover {
  color: #0b5cab;
}
</style>