<script setup>
import { onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useFetchMemberInfoStore } from '@/stores/member'

const router = useRouter()
const fetchMemberInfoStore = useFetchMemberInfoStore()

//進入會員中心時檢查是否已登入，沒登入就踢回首頁
onMounted(() => {
  if (!fetchMemberInfoStore.memberInfo) {
    router.push('/gigafix')
  }
})

//登出的非同步請求
const logout = async () => {
  await fetchMemberInfoStore.logoutMember()
  router.push('/gigafix')
}
</script>

<template>
  <aside class="member-sidebar">
    <div class="sidebar-header">
      <div class="avatar-circle">
        <i class="bi bi-person-fill"></i>
      </div>
      <p class="sidebar-title">會員中心</p>
    </div>

    <nav class="nav flex-column sidebar-nav">
        <RouterLink to="/gigafix/member-center/memberInfo" class="nav-link sidebar-link" exact-active-class="active">
            <i class="bi bi-house-door"></i>
            <span>會員資料</span>
        </RouterLink>

        <RouterLink to="/gigafix/member-center/profile" class="nav-link sidebar-link" active-class="active">
            <i class="bi bi-recycle"></i>
            <span>回收手機紀錄</span>
        </RouterLink>

        <RouterLink to="/gigafix/member-center/orders" class="nav-link sidebar-link" active-class="active">
            <i class="bi bi-box-seam"></i>
            <span>訂單查詢</span>
        </RouterLink>

        <RouterLink to="/gigafix/member-center/forum" class="nav-link sidebar-link" active-class="active">
            <i class="bi bi-chat-left-text"></i>
            <span>我的討論</span>
        </RouterLink>

        <RouterLink to="/gigafix/member-center/repair" class="nav-link sidebar-link" active-class="active">
            <i class="bi bi-tools"></i>
            <span>維修進度</span>
        </RouterLink>

        <hr class="sidebar-divider" />
        <button class="nav-link sidebar-link logout-link" type="button" @click="logout()">
            <i class="bi bi-box-arrow-left"></i>
            <span>登出</span>
        </button>
    </nav>
  </aside>
</template>

<style scoped>
.member-sidebar {
  width: 260px;
  min-height: 100%;
  background-color: #ffffff;
  border-left: 1px solid #eaeaea;
  padding: 28px 0;
  flex-shrink: 0;
  margin-left: auto; /* 讓父層是 flex 時，自動被推到最右邊 */
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 16px 24px;
  border-bottom: 1px solid #eaeaea;
  margin-bottom: 16px;
}

.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background-color: #eef4fb;
  color: #2b77c5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  font-size: 34px;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 700;
  color: #1d324b;
  margin: 0;
}

.sidebar-nav {
  padding: 0 14px;
  gap: 6px;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  color: #444444;
  font-size: 19px;
  font-weight: 500;
  text-decoration: none;
  background: none;
  border: none;
  text-align: left;
  width: 100%;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.sidebar-link i {
  font-size: 22px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.sidebar-link:hover {
  background-color: #f2f6fb;
  color: #2b77c5;
}

.sidebar-link.active {
  background-color: #eef4fb;
  color: #2b77c5;
  font-weight: 700;
}

.sidebar-divider {
  margin: 16px 6px;
  border-color: #eaeaea;
}

.logout-link {
  color: #c0392b;
}

.logout-link:hover {
  background-color: #fdecea;
  color: #c0392b;
}
</style>