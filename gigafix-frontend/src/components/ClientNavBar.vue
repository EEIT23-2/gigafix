<script setup>
import { ref, computed } from 'vue'
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

//跟註冊有關的變數宣告
const regPassword = ref('')
const regRealName = ref('')
const regNickName = ref('')
const regEmail = ref('')
const regPhone = ref('')
const regAddress = ref('')
const regGender = ref('')
const showRegisterModal = ref(false)

//登入視窗跟註冊視窗共用同一層背景遮罩，只要其中一個開著就要顯示，
//這樣互切的時候背景遮罩不會重新觸發淡入淡出，只有裡面的視窗內容在交叉淡出/淡入，感覺才會絲滑
const anyModalOpen = computed(() => showloginModal.value || showRegisterModal.value)
const closeAllModals = () => {
  showloginModal.value = false
  showRegisterModal.value = false
}

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
        await fetchMemberInfoStore.fetchMember(true) //登入成功後強制重抓一次會員資料，讓畫面上的icon等能即時切換
        showloginModal.value = false
        alert(`${resp.data.nickName}您好~登入成功！`)
    } catch (err) { //回傳4xx,5xx
        const message = err.response?.data?.message || '請稍後再試'
        alert(`登入失敗，原因: ${message}`)
    }finally{
        password.value = ''
  }
}

//==註冊相關==
const openRegisterModal = () => {
  regPassword.value = ''
  regRealName.value = ''
  regNickName.value = ''
  regEmail.value = ''
  regPhone.value = ''
  regAddress.value = ''
  regGender.value = ''
  showloginModal.value = false //關閉登入視窗，改開註冊視窗
  showRegisterModal.value = true
}
const register = async () => {
  try {
        const resp = await axios.post('/api/gigafix/members/register',{
            password: regPassword.value,
            realName: regRealName.value,
            nickName: regNickName.value,
            email: regEmail.value,
            phone: regPhone.value,
            address: regAddress.value,
            gender: regGender.value
        })
        await fetchMemberInfoStore.fetchMember(true) //註冊成功後端會自動簽發JWT等同自動登入，強制重抓一次會員資料讓畫面同步
        showRegisterModal.value = false
        alert(`${resp.data.nickName}註冊成功！`)
    } catch (err) { //回傳4xx,5xx
        const message = err.response?.data?.message || '請稍後再試'
        alert(`註冊失敗，原因: ${message}`)
    }finally{
        regPassword.value = ''
        //註冊成功後，後端會直接簽發JWT讓使用者保持登入狀態(等同自動登入)
        //所以這裡不需要再把showloginModal打開讓使用者重新輸入帳密登入一次
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

  <!-- 登入/註冊共用的背景遮罩，只跟著「是否有任一視窗開著」淡入淡出，
       兩個視窗互切時不會重新觸發，才不會有疊加變深或不夠絲滑的問題 -->
  <Teleport to="body">
    <Transition name="backdrop-fade">
      <div v-if="anyModalOpen" class="modal-backdrop fade show" @click="closeAllModals()"></div>
    </Transition>
  </Teleport>

  <!-- 登入的彈窗 -->
  <LoginRegisterModal v-model="showloginModal" :showBackdrop="false">
    <template #title>會員登入</template>
    <label class="form-label">Email</label>
    <input type="email" class="form-control mb-3" v-model="mail" :disabled="loginLoading">
    <label class="form-label">密碼</label>
    <input type="password" class="form-control" v-model="password" :disabled="loginLoading">

    <template #footer>
      <button class="btn btn-secondary" @click="openRegisterModal()">註冊</button>
      <button class="btn btn-primary" @click="login()">送出</button>
    </template>

  </LoginRegisterModal>

  <!-- 註冊的彈窗 -->
  <LoginRegisterModal v-model="showRegisterModal" :showBackdrop="false">
    <template #title>會員註冊</template>
    <label class="form-label">Email</label>
    <input type="email" class="form-control mb-3" v-model="regEmail">
    <label class="form-label">密碼</label>
    <input type="password" class="form-control mb-3" v-model="regPassword">
    <label class="form-label">真實姓名</label>
    <input type="text" class="form-control mb-3" v-model="regRealName" maxlength="40">
    <label class="form-label">暱稱</label>
    <input type="text" class="form-control mb-3" v-model="regNickName" maxlength="40">
    <label class="form-label">手機號碼</label>
    <input type="text" class="form-control mb-3" v-model="regPhone" placeholder="09xxxxxxxx">
    <label class="form-label">地址</label>
    <input type="text" class="form-control mb-3" v-model="regAddress">
    <label class="form-label d-block">性別</label>
    <div class="gender-group" role="group" aria-label="性別">
      <input type="radio" class="btn-check" id="regGenderMale" value="MALE" v-model="regGender" autocomplete="off">
      <label class="gender-circle" for="regGenderMale">男</label>

      <input type="radio" class="btn-check" id="regGenderFemale" value="FEMALE" v-model="regGender" autocomplete="off">
      <label class="gender-circle" for="regGenderFemale">女</label>
    </div>

    <template #footer>
      <button class="btn btn-primary" @click="register()">送出</button>
    </template>

  </LoginRegisterModal>
</template>

<style scoped>
/* 登入/註冊視窗內的表單文字整體放大一點
   這幾個class只有在這兩個彈窗裡用到，scoped不會影響到其他元件 */
.form-label {
  font-size: 1.05rem;
  margin-bottom: 0.4rem;
}

.form-control {
  font-size: 1.05rem;
  padding: 0.55rem 0.75rem;
}

.btn {
  font-size: 1.05rem;
}

/* 性別：把文字圈在圓形按鈕裡，選取時填色比視窗header的深藍淺很多 */
.gender-group {
  display: flex;
  gap: 0.75rem;
}

.gender-circle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  border: 2px solid #2b77c5;
  color: #2b77c5;
  font-size: 1.05rem;
  font-weight: 600;
  background-color: #ffffff;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.gender-circle:hover {
  background-color: #eef4fb;
}

.btn-check:checked + .gender-circle {
  background-color: #bcd9f2; /* 選取時的淺藍，比header的深藍(#1e3557)淺很多 */
  border-color: #2b77c5;
  color: #1e3557;
}

.backdrop-fade-enter-active,
.backdrop-fade-leave-active {
  transition: opacity 0.25s ease;
}

.backdrop-fade-enter-from,
.backdrop-fade-leave-to {
  opacity: 0;
}

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