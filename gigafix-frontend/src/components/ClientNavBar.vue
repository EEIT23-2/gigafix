<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useRouter } from 'vue-router'
import LoginRegisterModal from './LoginRegisterModal.vue';
import axios from 'axios';
import { useFetchMemberInfoStore } from '@/stores/member';
import { storeToRefs } from 'pinia'

//跟登入有關的變數宣告
const mail = ref('')
const password = ref('')
const loginErrorMsg =ref('')
const showloginModal = ref(false)

//取得Member資料
const fetchMemberInfoStore = useFetchMemberInfoStore()
const { memberInfo } = storeToRefs(fetchMemberInfoStore)
//==登入相關==
const openLoginModal = () => {
  password.value = null
  mail.value = null
  showloginModal.value = true
}
const login =async () => {
  try {
        const resp = await axios.post('/api/gigafix/members/login',{
            email: mail.value,
            password: password.value
        })
        showloginModal.value = false
        alert(`${resp.data.nickName}您好~登入成功！`)
    } catch (err) { //回傳4xx,5xx
        const message = err.response?.data?.message || '請稍後再試'
        alert(`登入失敗，原因: ${message}`)
    }finally{
        password.value = ''
    }
}

const router = useRouter()
</script>

<template>
  <header class="site-header-wrapper">
    <div class="top-announcement-bar">
      <span>加入會員就送可愛正彥寶寶一組</span>  
      <!-- 這個應該在layout或是抽成元件????? -->
    </div>

    <div class="site-header">
      <div class="header-inner">
        <!-- 右上方功能按鈕 -->
        <nav class="user-actions">
        
            <RouterLink class="action-item">
              <span class="icon-box">
                <i class="bi bi-search icon"></i>
              </span>
            </RouterLink>

            <!-- 用使用者是否登入決定要顯示某個標籤 -->
            <button v-if="!memberInfo" type="button" class="action-item" @click="openLoginModal()">
              <span class="icon-box"><i class="bi bi-person icon icon-person"></i></span>
              <span class="action-text">登入</span>
            </button>
            <!-- 小人icon，連結到member center path -->
            <RouterLink v-if="memberInfo" class="action-item" active-class="active" to="/gigafix/member-center">
              <span class="icon-box"><i class="bi bi-person-fill icon icon-person"></i></span>
              <span class="action-text">{{ memberInfo.nickName }}</span>
            </RouterLink>

            <RouterLink class="action-item"><!-- 購物車icon -->
              <span class="icon-box">
                <i class="bi bi-cart icon"></i>
              </span>
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
            <router-link class="nav-item" to="/gigafix/forum">Gigafix討論區</router-link>
            <router-link class="nav-item">關於Gigafix</router-link>
          </ul>
        </nav>
      </div>
    </div>
  </header>

  <!-- 登入的彈窗 -->
  <LoginRegisterModal v-model="showloginModal">
    <template #title>會員登入</template>
    <label class="form-label">Email</label>
    <input type="email" class="form-control mb-3" v-model="mail" :disabled="loginLoading">
    <label class="form-label">密碼</label>
    <input type="password" class="form-control" v-model="password" :disabled="loginLoading">

    <template #footer>
      <button class="btn btn-primary" @click="login()">送出</button>
    </template>

  </LoginRegisterModal>
</template>

<style scoped>
.top-announcement-bar {
  background-color: #1e3557;
  color: #ffffff;
  text-align: center;
  font-size: 15px;
  padding: 8px 0;
  letter-spacing: 0.5px;
}

.site-header {
  border-bottom: 1px solid #eaeaea;
  background-color: #ffffff;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 14px 20px 16px;
}

.user-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 22px;
  font-size: 15px;
  color: #666666;
  margin-bottom: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;              /* icon 跟文字之間的間距 */
  cursor: pointer;
  outline: none;
  text-decoration: none;
  color: #666666;
  background: none;      /* 蓋掉 <button> 原生底色 */
  border: none;           /* 蓋掉 <button> 原生框線 */
  padding: 0;              /* 蓋掉 <button> 原生內距 */
  font: inherit;           /* 讓 <button> 文字跟其他 nav 項目字型一致 */
}

.action-text {
  font-size: 15px;
  white-space: nowrap;
}

.action-item:focus,
.action-item:focus-visible {
  outline: none;
  box-shadow: none;
}

.action-item.active {
  background: none;
  border: none;
  box-shadow: none;
  color: #2b77c5;
}

/* 統一每個 icon 的視覺框大小，讓不同圖示對齊在同一個尺寸內 */
.icon-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
}

.icon-box .icon {
  font-size: 22px;
  line-height: 1;
}

/* 個別微調視覺重量較輕的圖示，用 transform 縮放而非改 font-size，避免影響對齊基準線 */
.icon-box .icon-person {
  transform: scale(1.25);
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
  gap: 14px;
  margin: 0;
  flex-shrink: 0;
}

.logo-mark {
  background-color: #2b77c5;
  color: #ffffff;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 900;
  font-style: italic;
}

.logo-title {
  font-size: 28px;
  font-weight: 800;
  color: #1d324b;
  line-height: 1;
}

.logo-title span {
  font-size: 19px;
  font-weight: 400;
  color: #7b94ad;
  margin-left: 4px;
}

.logo-sub {
  font-size: 13px;
  letter-spacing: 3px;
  color: #555555;
  text-align: left;
  margin-top: 5px;
}

/* 選單清單：透過 margin: 0 auto 自動推到中間 */
.nav-list {
  display: flex;
  align-items: center;
  justify-content: center;
  list-style: none;
  gap: 26px;
  padding: 0;
  margin: 0 auto;
  flex-wrap: wrap;
}

.nav-item {
  font-size: 22px;
  color: #444444;
  cursor: pointer;
  white-space: nowrap;
}

.nav-item:hover {
  color: #0b5cab;
}
</style>