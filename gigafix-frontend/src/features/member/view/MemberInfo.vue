<script setup>
import { onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useFetchMemberInfoStore } from '@/stores/member'
import BaseModal from '../component/BaseModal.vue'
import AddressSelect from '@/components/AddressSelect.vue'
import { parseTaiwanAddress } from '@/static/taiwanDistricts'

//========== 變數宣告 ==========

//基本設定：router跟會員資料store
const router = useRouter()
const fetchMemberInfoStore = useFetchMemberInfoStore()
const { memberInfo, loading } = storeToRefs(fetchMemberInfoStore)

//性別代碼轉中文顯示用的對照表
const genderLabelMap = {
  MALE: '男',
  FEMALE: '女',
}

//修改個人資料彈窗的狀態
const showEditModal = ref(false)
const editNickName = ref('')
const editPhone = ref('')
const editAddressCity = ref('')
const editAddressDistrict = ref('')
const editAddressDetail = ref('')
const editGender = ref('')
const editSubmitting = ref(false)
const editErrorMsg = ref('') //不是空字串時代表欄位不符預期，畫面上會擋住送出按鈕

//修改密碼彈窗的狀態
const showPasswordModal = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const passwordSubmitting = ref(false)
const passwordErrorMsg = ref('')

//刪除使用者彈窗的狀態
const showDeleteModal = ref(false)
const deletePassword = ref('')
const deleteConfirmPassword = ref('') //確認密碼，純前端防呆用，不會送給後端
const deleteSubmitting = ref(false)
const deleteErrorMsg = ref('')

//========== 函數宣告 ==========

//進頁面時抓一次會員資料
//有 fetched 快取擋著，這裡呼叫不會造成重複打API，純粹是進頁面時確保資料是新的
onMounted(() => {
  fetchMemberInfoStore.fetchMember()
})

//把性別代碼(MALE/FEMALE)轉成中文顯示
const genderLabel = (gender) => genderLabelMap[gender] ?? '未提供'

//==修改個人資料相關函式==
//開啟彈窗前，把表單值同步成目前store裡的會員資料，避免帶著上次殘留的輸入值
const openEditModal = () => {
  editNickName.value = memberInfo.value.nickName
  editPhone.value = memberInfo.value.phone
  const parsedAddress = parseTaiwanAddress(memberInfo.value.address) //把既有地址字串拆回縣市/行政區/詳細地址，帶回下拉選單
  editAddressCity.value = parsedAddress.city
  editAddressDistrict.value = parsedAddress.district
  editAddressDetail.value = parsedAddress.detail
  editGender.value = memberInfo.value.gender
  checkEditError() //資料是既有的會員資料，理論上都合法，這裡順便算一次讓按鈕正確顯示成可送出
  showEditModal.value = true
}

//送出修改個人資料表單
const submitEdit = async () => {
  editSubmitting.value = true
  try {
    await axios.patch('/api/gigafix/members/me', {
      nickName: editNickName.value,
      phone: editPhone.value,
      address: `${editAddressCity.value}${editAddressDistrict.value}${editAddressDetail.value}`,
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

//每次欄位變動就重新檢查一次，跟後端Bean Validation的規則對齊，不符合就不讓使用者送出
const checkEditError = () => {
  if (!editNickName.value.trim()) {
    editErrorMsg.value = '暱稱不可為空'
  } else if (editNickName.value.length > 40) {
    editErrorMsg.value = '暱稱字數上限為40'
  } else if (!editPhone.value.trim()) {
    editErrorMsg.value = '手機號碼不可為空'
  } else if (!/^09\d{8}$/.test(editPhone.value)) {
    editErrorMsg.value = '手機號碼格式錯誤，需為09開頭的10碼數字'
  } else if (!editAddressCity.value) {
    editErrorMsg.value = '請選擇縣市'
  } else if (!editAddressDistrict.value) {
    editErrorMsg.value = '請選擇行政區'
  } else if (!editAddressDetail.value.trim()) {
    editErrorMsg.value = '請輸入詳細地址'
  } else if (!editGender.value) {
    editErrorMsg.value = '請選擇性別'
  } else {
    editErrorMsg.value = ''
  }
}
//AddressSelect選縣市/行政區或打詳細地址都會更新這三個值，一併重新驗證讓送出鈕即時反映
watch([editAddressCity, editAddressDistrict, editAddressDetail], () => checkEditError())

//==修改密碼相關函式==
//開啟彈窗前先清空欄位，避免殘留上次輸入的密碼
const openPasswordModal = () => {
  oldPassword.value = ''
  newPassword.value = ''
  passwordErrorMsg.value = '請輸入原密碼' //欄位一開始是空的，直接給提示訊息，不用等使用者打字才跳出來
  showPasswordModal.value = true
}

//送出修改密碼表單
const submitPassword = async () => {
  passwordSubmitting.value = true
  try {
    await axios.patch('/api/gigafix/members/me/password', {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })
    showPasswordModal.value = false
    alert('密碼修改成功！')
  } catch (err) { //回傳4xx,5xx，後端Bean Validation的錯誤訊息會放在message
    const message = err.response?.data?.message || '請稍後再試'
    alert(`修改密碼失敗，原因: ${message}`)
  } finally {
    passwordSubmitting.value = false
    oldPassword.value = ''
    newPassword.value = ''
  }
}

//每次欄位變動就重新檢查一次，兩個密碼都至少要8個字元(跟後端Bean Validation的規則對齊)
const checkPasswordError = () => {
  if (!oldPassword.value) {
    passwordErrorMsg.value = '請輸入原密碼'
  } else if (oldPassword.value.length < 8) {
    passwordErrorMsg.value = '原密碼長度不可低於8個字元'
  } else if (!newPassword.value) {
    passwordErrorMsg.value = '請輸入新密碼'
  } else if (newPassword.value.length < 8) {
    passwordErrorMsg.value = '新密碼長度不可低於8個字元'
  } else {
    passwordErrorMsg.value = ''
  }
}

//==刪除使用者相關函式==
//開啟彈窗前先清空密碼欄位
const openDeleteModal = () => {
  deletePassword.value = ''
  deleteConfirmPassword.value = ''
  deleteErrorMsg.value = '請輸入密碼' //欄位一開始是空的，直接給提示訊息，不用等使用者打字才跳出來
  showDeleteModal.value = true
}

//送出刪除帳號請求
const submitDelete = async () => {
  deleteSubmitting.value = true
  try {
    await axios.delete('/api/gigafix/members/me', {
      data: { password: deletePassword.value } //DELETE帶body要放在axios config的data裡，確認密碼只是前端防呆用不會送出去
    })
    showDeleteModal.value = false
    memberInfo.value = null //帳號已刪除，直接把store清空，行為比照登出
    fetchMemberInfoStore.fetched = false
    alert('帳號已刪除，感謝您曾經使用Gigafix！')
    router.push('/gigafix')
  } catch (err) { //回傳4xx,5xx，後端Bean Validation的錯誤訊息會放在message
    const message = err.response?.data?.message || '請稍後再試'
    alert(`刪除失敗，原因: ${message}`)
  } finally {
    deleteSubmitting.value = false
    deletePassword.value = ''
    deleteConfirmPassword.value = ''
  }
}

//每次欄位變動就重新檢查一次：密碼至少8個字元，且確認密碼要跟密碼一致，不符合就不讓使用者送出
const checkDeleteError = () => {
  if (!deletePassword.value) {
    deleteErrorMsg.value = '請輸入密碼'
  } else if (deletePassword.value.length < 8) {
    deleteErrorMsg.value = '密碼長度不可低於8個字元'
  } else if (!deleteConfirmPassword.value) {
    deleteErrorMsg.value = '請輸入確認密碼'
  } else if (deletePassword.value !== deleteConfirmPassword.value) {
    deleteErrorMsg.value = '密碼輸入錯誤'
  } else {
    deleteErrorMsg.value = ''
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

        <button type="button" class="action-btn edit-btn" @click="openEditModal()">
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

      <div class="card-actions">
        <button type="button" class="action-btn password-btn" @click="openPasswordModal()">
          <i class="bi bi-shield-lock"></i>
          修改密碼
        </button>
        <button type="button" class="action-btn delete-btn" @click="openDeleteModal()">
          <i class="bi bi-trash3"></i>
          刪除使用者
        </button>
      </div>
    </div>

    <!-- 修改個人資料的彈窗，用共用的BaseModal殼，內容寫在這裡 -->
    <BaseModal v-model="showEditModal">
      <template #title>修改個人資料</template>

      <label class="form-label">暱稱</label>
      <input type="text" class="form-control mb-3" v-model="editNickName" maxlength="40" :disabled="editSubmitting" @input="checkEditError()">

      <label class="form-label">手機號碼</label>
      <input type="text" class="form-control mb-3" v-model="editPhone" placeholder="09xxxxxxxx" :disabled="editSubmitting" @input="checkEditError()">

      <label class="form-label">地址</label>
      <div class="mb-3">
        <AddressSelect v-model:city="editAddressCity" v-model:district="editAddressDistrict" v-model:detail="editAddressDetail" :disabled="editSubmitting" />
      </div>

      <label class="form-label d-block">性別</label>
      <div class="gender-group" role="group" aria-label="性別">
        <input type="radio" class="btn-check" id="editGenderMale" value="MALE" v-model="editGender" autocomplete="off" :disabled="editSubmitting" @change="checkEditError()">
        <label class="gender-circle" for="editGenderMale">男</label>

        <input type="radio" class="btn-check" id="editGenderFemale" value="FEMALE" v-model="editGender" autocomplete="off" :disabled="editSubmitting" @change="checkEditError()">
        <label class="gender-circle" for="editGenderFemale">女</label>
      </div>

      <template #footer>
        <p v-if="editErrorMsg" class="text-danger small mb-3">{{ editErrorMsg }}</p>
        <button class="btn btn-secondary" @click="showEditModal = false" :disabled="editSubmitting">取消</button>
        <button v-if="editErrorMsg" type="button" class="btn btn-primary" disabled>請輸入正確資訊</button>
        <button v-if="editErrorMsg == ''" class="btn btn-primary" @click="submitEdit()" :disabled="editSubmitting">
          {{ editSubmitting ? '送出中...' : '送出' }}
        </button>
      </template>
    </BaseModal>

    <!-- 修改密碼的彈窗 -->
    <BaseModal v-model="showPasswordModal">
      <template #title>修改密碼</template>

      <label class="form-label">原密碼</label>
      <input type="password" class="form-control mb-3" v-model="oldPassword" :disabled="passwordSubmitting" @input="checkPasswordError()">

      <label class="form-label">新密碼</label>
      <input type="password" class="form-control" v-model="newPassword" :disabled="passwordSubmitting" @input="checkPasswordError()">
      <p class="form-hint">密碼長度至少需 8 個字元</p>

      <template #footer>
        <p v-if="passwordErrorMsg" class="text-danger small mb-3">{{ passwordErrorMsg }}</p>
        <button class="btn btn-secondary" @click="showPasswordModal = false" :disabled="passwordSubmitting">取消</button>
        <button v-if="passwordErrorMsg" type="button" class="btn btn-primary" disabled>請輸入正確密碼</button>
        <button v-if="passwordErrorMsg == ''" class="btn btn-primary" @click="submitPassword()" :disabled="passwordSubmitting">
          {{ passwordSubmitting ? '送出中...' : '送出' }}
        </button>
      </template>
    </BaseModal>

    <!-- 刪除使用者的彈窗 -->
    <BaseModal v-model="showDeleteModal">
      <template #title>刪除帳號</template>

      <p class="delete-warning">此操作無法復原，帳號刪除後所有資料將被永久移除。請輸入您的密碼以確認。</p>
      <label class="form-label">密碼</label>
      <input type="password" class="form-control mb-3" v-model="deletePassword" :disabled="deleteSubmitting" @input="checkDeleteError()">

      <label class="form-label">確認密碼</label>
      <input type="password" class="form-control" v-model="deleteConfirmPassword" :disabled="deleteSubmitting" @input="checkDeleteError()">

      <template #footer>
        <p v-if="deleteErrorMsg" class="text-danger small mb-3">{{ deleteErrorMsg }}</p>
        <button class="btn btn-secondary" @click="showDeleteModal = false" :disabled="deleteSubmitting">取消</button>
        <button v-if="deleteErrorMsg" type="button" class="btn btn-primary" disabled>請輸入正確密碼</button>
        <button v-if="deleteErrorMsg == ''" class="btn btn-danger" @click="submitDelete()" :disabled="deleteSubmitting">
          {{ deleteSubmitting ? '刪除中...' : '確認刪除' }}
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

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 20px;
  border-radius: 8px;
  background-color: #ffffff;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.action-btn i {
  font-size: 17px;
}

.edit-btn {
  border: 1px solid #e8c375;
  color: #c9962e;
}

.edit-btn:hover {
  background-color: #fbf1dc;
  color: #c9962e;
}

.password-btn {
  border: 1px solid #2b77c5;
  color: #2b77c5;
}

.password-btn:hover {
  background-color: #eef4fb;
  color: #2b77c5;
}

.delete-btn {
  border: 1px solid #d64545;
  color: #c0392b;
}

.delete-btn:hover {
  background-color: #fdecea;
  color: #c0392b;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #eaeaea;
  flex-wrap: wrap;
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

/* AddressSelect是子元件，樣式有scoped隔開，用:deep()讓裡面的select/input跟本頁其他欄位同一套字級/內距 */
:deep(.address-select .form-select),
:deep(.address-select .form-control) {
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

.form-hint {
  margin: 0.4rem 0 0;
  font-size: 0.9rem;
  color: #888888;
}

.delete-warning {
  margin: 0 0 1rem;
  padding: 0.75rem 1rem;
  border-radius: 0.6rem;
  background-color: #fdecea;
  color: #c0392b;
  font-size: 0.95rem;
}
</style>
