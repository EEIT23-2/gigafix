<script setup>
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { storeToRefs } from 'pinia'
import { useFetchMemberInfoStore } from '@/stores/member'
import BaseModal from '../component/BaseModal.vue'

const fetchMemberInfoStore = useFetchMemberInfoStore()
const { memberInfo, loading } = storeToRefs(fetchMemberInfoStore)

//有 fetched 快取擋著，這裡呼叫不會造成重複打API，純粹是進頁面時確保資料是新的
onMounted(() => {
  fetchMemberInfoStore.fetchMember()
})

const genderLabelMap = {
  MALE: '男',
  FEMALE: '女',
}
const genderLabel = (gender) => genderLabelMap[gender] ?? '未提供'

//==修改個人資料彈窗相關==
const showEditModal = ref(false)
const editNickName = ref('')
const editPhone = ref('')
const editAddress = ref('')
const editGender = ref('')
const editSubmitting = ref(false)

const openEditModal = () => {
  editNickName.value = memberInfo.value.nickName
  editPhone.value = memberInfo.value.phone
  editAddress.value = memberInfo.value.address
  editGender.value = memberInfo.value.gender
  showEditModal.value = true
}

const submitEdit = async () => {
  editSubmitting.value = true
  try {
    await axios.patch('/api/gigafix/members/me', {
      nickName: editNickName.value,
      phone: editPhone.value,
      address: editAddress.value,
      gender: editGender.value
    })
    await fetchMemberInfoStore.fetchMember(true) //強制重抓最新資料，讓這頁跟上方導覽列同步更新
    showEditModal.value = false
    alert('會員資料修改成功！')
  } catch (err) { //回傳4xx,5xx，後端Bean Validation的錯誤訊息會放在message
    const message = err.response?.data?.message || '請稍後再試'
    alert(`修改失敗，原因: ${message}`)
  } finally {
    editSubmitting.value = false
  }
}
</script>

<template>
  <section class="member-info">
    <h2 class="page-title">會員資料</h2>

    <p v-if="loading" class="info-state">資料載入中...</p>
    <p v-else-if="!memberInfo" class="info-state">目前無法取得會員資料，請稍後再試。</p>

    <div v-else class="info-card">
      <div class="info-card-header">
        <div class="header-left">
          <div class="avatar-circle">
            <i class="bi bi-person-fill"></i>
          </div>
          <p class="info-nickname">{{ memberInfo.nickName }}</p>
        </div>

        <button type="button" class="edit-btn" @click="openEditModal()">
          <i class="bi bi-pencil-square"></i>
          修改個人資料
        </button>
      </div>

      <dl class="info-list">
        <div class="info-row">
          <dt class="info-label"><i class="bi bi-person"></i>真實姓名</dt>
          <dd class="info-value">{{ memberInfo.realName }}</dd>
        </div>
        <div class="info-row">
          <dt class="info-label"><i class="bi bi-envelope"></i>Email</dt>
          <dd class="info-value">{{ memberInfo.email }}</dd>
        </div>
        <div class="info-row">
          <dt class="info-label"><i class="bi bi-telephone"></i>手機號碼</dt>
          <dd class="info-value">{{ memberInfo.phone }}</dd>
        </div>
        <div class="info-row">
          <dt class="info-label"><i class="bi bi-geo-alt"></i>地址</dt>
          <dd class="info-value">{{ memberInfo.address }}</dd>
        </div>
        <div class="info-row">
          <dt class="info-label"><i class="bi bi-gender-ambiguous"></i>性別</dt>
          <dd class="info-value">{{ genderLabel(memberInfo.gender) }}</dd>
        </div>
      </dl>
    </div>

    <!-- 修改個人資料的彈窗，用共用的BaseModal殼，內容寫在這裡 -->
    <BaseModal v-model="showEditModal">
      <template #title>修改個人資料</template>

      <label class="form-label">暱稱</label>
      <input type="text" class="form-control mb-3" v-model="editNickName" maxlength="40" :disabled="editSubmitting">

      <label class="form-label">手機號碼</label>
      <input type="text" class="form-control mb-3" v-model="editPhone" placeholder="09xxxxxxxx" :disabled="editSubmitting">

      <label class="form-label">地址</label>
      <input type="text" class="form-control mb-3" v-model="editAddress" :disabled="editSubmitting">

      <label class="form-label d-block">性別</label>
      <div class="gender-group" role="group" aria-label="性別">
        <input type="radio" class="btn-check" id="editGenderMale" value="MALE" v-model="editGender" autocomplete="off" :disabled="editSubmitting">
        <label class="gender-circle" for="editGenderMale">男</label>

        <input type="radio" class="btn-check" id="editGenderFemale" value="FEMALE" v-model="editGender" autocomplete="off" :disabled="editSubmitting">
        <label class="gender-circle" for="editGenderFemale">女</label>
      </div>

      <template #footer>
        <button class="btn btn-secondary" @click="showEditModal = false" :disabled="editSubmitting">取消</button>
        <button class="btn btn-primary" @click="submitEdit()" :disabled="editSubmitting">
          {{ editSubmitting ? '送出中...' : '送出' }}
        </button>
      </template>
    </BaseModal>
  </section>
</template>

<style scoped>
.member-info {
  width: 100%;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1d324b;
  margin: 0 0 24px;
}

.info-state {
  font-size: 19px;
  color: #666666;
}

.info-card {
  width: 100%;
  background-color: #ffffff;
  border: 1px solid #eaeaea;
  border-radius: 12px;
  padding: 36px 40px;
}

.info-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 24px;
  margin-bottom: 24px;
  border-bottom: 1px solid #eaeaea;
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: #eef4fb;
  color: #2b77c5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
}

.info-nickname {
  font-size: 24px;
  font-weight: 700;
  color: #1d324b;
  margin: 0;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 20px;
  border-radius: 8px;
  border: 1px solid #e8c375;
  background-color: #ffffff;
  color: #c9962e;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.edit-btn i {
  font-size: 17px;
}

.edit-btn:hover {
  background-color: #fbf1dc;
  color: #c9962e;
}

.info-list {
  margin: 0;
}

.info-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  padding: 18px 0;
}

.info-row + .info-row {
  border-top: 1px solid #f2f6fb;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 160px;
  font-size: 19px;
  font-weight: 600;
  color: #2b77c5;
}

.info-label i {
  font-size: 19px;
}

.info-value {
  margin: 0;
  font-size: 20px;
  color: #444444;
  word-break: break-all;
}

/* ===== 彈窗內表單樣式，跟ClientNavBar的登入/註冊表單維持同一套視覺 ===== */
.form-label {
  font-size: 1.05rem;
  margin-bottom: 0.4rem;
}

.form-control {
  font-size: 1.05rem;
  padding: 0.55rem 0.75rem;
}

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
  background-color: #bcd9f2;
  border-color: #2b77c5;
  color: #1e3557;
}

.btn-check:disabled + .gender-circle {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
