<script setup>
import axios from 'axios'
import { ref, onMounted } from 'vue'
import BaseModal from '../component/BaseModal.vue'

const allMembers = ref([])
const errorMsg = ref('')

//性別代碼轉中文顯示用的對照表
const genderLabelMap = {
    MALE: '男',
    FEMALE: '女',
}
const genderLabel = (gender) => genderLabelMap[gender] ?? '未提供'

//後端LocalDateTime格式化成yyyy/MM/dd HH:mm:ss顯示
const formatDateTime = (ldtString) => {
    if (!ldtString) return ''

    // 拆出日期跟時間兩部分
    const [datePart, timePart] = ldtString.split('T')
    if (!datePart || !timePart) return ldtString // 格式不符預期時,原樣顯示,不讓畫面壞掉

    const [year, month, day] = datePart.split('-')

    // 時間部分可能帶奈秒(.8387595),只取到秒
    const [hour, minute, secondWithNano] = timePart.split(':')
    const second = secondWithNano.split('.')[0]

    return `${year}/${month}/${day} ${hour}:${minute}:${second}`
}

//====取得所有的會員====
const fetchAllMembers = async () => {
    errorMsg.value = ''
    try {
        const rep = await axios.get('/api/admin/members')
        allMembers.value = rep.data
    } catch (err) {
        alert(`會員列表讀取失敗，原因: ${err.response.data.message}`)
        errorMsg.value = '無法載入會員列表,請稍後再試'
    }
}

// ====修改會員資料的相關宣告====
const showEditModal = ref(false)
const editMemberId = ref(null)
const editRealName = ref('')
const editNickName = ref('')
const editEmail = ref('')
const editPhone = ref('')
const editAddress = ref('')
const editGender = ref('')
const editSubmitting = ref(false)
const editErrorMsg = ref('')

//開啟彈窗前，把表單值同步成該會員目前的資料
const openEditModal = (member) => {
    editMemberId.value = member.id
    editRealName.value = member.realName
    editNickName.value = member.nickName
    editEmail.value = member.email
    editPhone.value = member.phone
    editAddress.value = member.address
    editGender.value = member.gender
    checkEditError() //資料是既有的會員資料，理論上都合法，這裡順便算一次讓按鈕正確顯示成可送出
    showEditModal.value = true
}

//每次欄位變動就重新檢查一次
const checkEditError = () => {
    if (!editRealName.value.trim()) {
        editErrorMsg.value = '真實姓名不可為空'
    } else if (editRealName.value.length > 40) {
        editErrorMsg.value = '真實姓名字數上限為40'
    } else if (!editNickName.value.trim()) {
        editErrorMsg.value = '暱稱不可為空'
    } else if (editNickName.value.length > 40) {
        editErrorMsg.value = '暱稱字數上限為40'
    } else if (!editEmail.value.trim()) {
        editErrorMsg.value = 'Email不可為空'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editEmail.value)) {
        editErrorMsg.value = 'Email格式錯誤'
    } else if (!editPhone.value.trim()) {
        editErrorMsg.value = '手機號碼不可為空'
    } else if (!/^09\d{8}$/.test(editPhone.value)) {
        editErrorMsg.value = '手機號碼格式錯誤，需為09開頭的10碼數字'
    } else if (!editAddress.value.trim()) {
        editErrorMsg.value = '地址不可為空'
    } else if (!editGender.value) {
        editErrorMsg.value = '請選擇性別'
    } else {
        editErrorMsg.value = ''
    }
}

//送出修改會員資料表單(整包資料送給後端PUT /api/admin/members/{memberId})
const submitEdit = async () => {
    editSubmitting.value = true
    try {
        await axios.put(`/api/admin/members/${editMemberId.value}`, {
            realName: editRealName.value,
            nickName: editNickName.value,
            email: editEmail.value,
            phone: editPhone.value,
            address: editAddress.value,
            gender: editGender.value
        })
        await fetchAllMembers()
        showEditModal.value = false
        alert('會員資料修改成功！')
    } catch (err) { //回傳4xx,5xx，後端Bean Validation的錯誤訊息會放在message
        const message = err.response?.data?.message || '請稍後再試'
        alert(`修改失敗，原因: ${message}`)
    } finally {
        editSubmitting.value = false
    }
}

// ====刪除會員的相關函式====
//管理員刪除會員不需要驗證該會員密碼，用瀏覽器原生confirm再次確認即可(跟AdminOrderView刪除訂單的作法一致)
const deleteMember = async (member) => {
    const confirmed = confirm(`確定要刪除會員「${member.nickName}」嗎？此操作無法復原。`)
    if (!confirmed) {
        return
    }
    try {
        await axios.delete(`/api/admin/members/${member.id}`)
        await fetchAllMembers()
        alert('刪除成功')
    } catch (err) { //回傳4xx,5xx
        const message = err.response?.data?.message || '請稍後再試'
        alert(`刪除會員失敗，原因: ${message}`)
    }
}

//只要元件掛載就去抓所有會員出來
onMounted(() => {
    fetchAllMembers()
})
</script>

<template>
    <h1>這是會員管理頁</h1>

    <!-- 所有會員的資料 -->
    <div class="card shadow mb-4">
        <div class="card-header py-3">
            <h6 class="m-0 fw-bold text-primary">會員列表</h6>
        </div>
        <div class="card-body">
            <p v-if="errorMsg" class="text-danger">{{ errorMsg }}</p>
            <div class="row mb-2 text-muted fw-bold">
                <div class="col-1">編號</div>
                <div class="col-1">真實姓名</div>
                <div class="col-1">暱稱</div>
                <div class="col-2">Email</div>
                <div class="col-1">手機號碼</div>
                <div class="col-1">地址</div>
                <div class="col-1">性別</div>
                <div class="col-3">加入時間</div>
                <div class="col-1">操作</div>
            </div>

            <div class="row align-items-center py-2 border-top" v-for="member in allMembers" :key="member.id">
                <div class="col-1">{{ member.id }}</div>
                <div class="col-1 text-truncate">{{ member.realName }}</div>
                <div class="col-1 text-truncate">{{ member.nickName }}</div>
                <div class="col-2 text-truncate">{{ member.email }}</div>
                <div class="col-1 text-truncate">{{ member.phone }}</div>
                <div class="col-1 text-truncate">{{ member.address }}</div>
                <div class="col-1">{{ genderLabel(member.gender) }}</div>
                <div class="col-3 text-truncate">{{ formatDateTime(member.createTime) }}</div>
                <div class="col-1 d-flex gap-2 justify-content-center">
                    <button class="btn btn-sm btn-outline-warning square-btn" title="修改該會員資料" @click="openEditModal(member)">
                        <i class="bi bi-pencil-square"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger square-btn" title="刪除該會員" @click="deleteMember(member)">
                        <i class="bi bi-trash-fill"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- 修改會員資料的彈窗 -->
    <BaseModal v-model="showEditModal">
        <template #title>修改會員資料</template>

        <label class="form-label">真實姓名</label>
        <input type="text" class="form-control mb-3" v-model="editRealName" maxlength="40" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">暱稱</label>
        <input type="text" class="form-control mb-3" v-model="editNickName" maxlength="40" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">Email</label>
        <input type="email" class="form-control mb-3" v-model="editEmail" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">手機號碼</label>
        <input type="text" class="form-control mb-3" v-model="editPhone" placeholder="09xxxxxxxx" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">地址</label>
        <input type="text" class="form-control mb-3" v-model="editAddress" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">性別</label>
        <select class="form-select" v-model="editGender" :disabled="editSubmitting" @change="checkEditError()">
            <option value="" disabled>請選擇性別</option>
            <option value="MALE">男</option>
            <option value="FEMALE">女</option>
        </select>

        <template #footer>
            <p v-if="editErrorMsg" class="text-danger small mb-3">{{ editErrorMsg }}</p>
            <button class="btn btn-secondary" @click="showEditModal = false" :disabled="editSubmitting">取消</button>
            <button v-if="editErrorMsg" type="button" class="btn btn-primary" disabled>請輸入正確資訊</button>
            <button v-if="editErrorMsg == ''" class="btn btn-primary" @click="submitEdit()" :disabled="editSubmitting">
                {{ editSubmitting ? '送出中...' : '送出' }}
            </button>
        </template>
    </BaseModal>
</template>

<style scoped>
.square-btn {
    width: 34px;
    height: 34px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1rem;
    line-height: 1;
    flex-shrink: 0;
    border-radius: 0.5rem;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.square-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 0.25rem 0.5rem rgba(0, 0, 0, 0.1);
}
</style>
