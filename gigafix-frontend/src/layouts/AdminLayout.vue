<script setup>
import AdminNavBar from '@/components/AdminNavBar.vue';
//抓使用者資料的import
import { useFetchAdminInfoStore } from '@/stores/admin';
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

//抓管理員資料
const fetchAdminInfoStore = useFetchAdminInfoStore()
const { adminInfo } = storeToRefs(fetchAdminInfoStore)
//登出後強制轉跳頁面需要使用router
const router = useRouter()

//登出的非同步請求
const logout = async () =>{
  await fetchAdminInfoStore.logoutAdmin()
  router.push('/adminLogin')
}

</script>

<template>
  <div class="admin-container">
    <!-- 側邊欄 (Sidebar) -->
    <aside class="sidebar">
      <div class="sidebar-top">
        <div class="logo">
          <span class="logo-icon">😊</span>
          <span class="logo-text">Gigafix ADMIN</span>
        </div>

        <!-- 抽出後的導覽選單元件 -->
        <AdminNavBar />
      </div>

      <div class="sidebar-bottom">
        <button class="collapse-btn">◀</button>
      </div>
    </aside>

    <!-- 右側主內容區 -->
    <div class="main-wrapper">
      <!-- 頂部導航列 -->
      <header class="topbar">
        <!-- 右上角使用者名稱 -->
        <div class="user-section">
          <span class="username">
            <span v-if="adminInfo">
              美好的一天✨✨~ {{ adminInfo.adminName }}
            </span>
            |
            <button type="button" class="btn-logout" @click="logout()">
              登出
            </button>
          </span>
        </div>
      </header>

      <!-- 主要內容掛載區 -->
      <main class="content-body">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 基礎佈局 */
.admin-container {
  display: flex;
  height: 100vh;
  background-color: #f8f9fc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #5a5c69;
  overflow: hidden;
}

/* 側邊欄 */
.sidebar {
  width: 224px;
  background-color: #4e73df;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex-shrink: 0;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  text-decoration: none;
  color: #ffffff;
}

.logo-icon {
  font-size: 20px;
}

.logo-text {
  font-weight: 800;
  font-size: 16px;
  letter-spacing: 0.05em;
}

.sidebar-bottom {
  padding: 16px;
  display: flex;
  justify-content: center;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.2);
  border: none;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

/* 主內容區 */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 頂部導航 */
.topbar {
  height: 64px;
  background-color: #ffffff;
  border-bottom: 1px solid #e3e6f0;
  display: flex;
  align-items: center;
  /* 頂部列現在只剩右上角的使用者區塊，直接靠右對齊 */
  justify-content: flex-end;
  padding: 0 24px;
  flex-shrink: 0;
}

/* 使用者名稱區塊 */
.user-section {
  display: flex;
  align-items: center;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #858796;
}

/* 內容滾動區（子路由入口） */
.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
/*登出按鈕*/
.btn-logout {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  font-size: 0.875rem; /* 等同 btn-sm */
  font-weight: 700;
  line-height: 1;
  color: #dc3545; /* Bootstrap danger 紅色 */
  background-color: #f8d7da; /* Bootstrap danger-subtle 淺粉紅 */
  border: none;
  border-radius: 6px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
}
</style>