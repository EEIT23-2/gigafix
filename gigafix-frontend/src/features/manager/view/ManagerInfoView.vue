<script setup>
import { RouterView } from 'vue-router';
import axios from 'axios';
import { ref } from 'vue';
import { useFetchAdminInfoStore } from '@/stores/admin';
import { storeToRefs } from 'pinia'
import { onMounted } from 'vue';
import { Modal } from 'bootstrap'

//====取得使用者資訊====
const fetchAdminInfoStore = useFetchAdminInfoStore()
const { adminInfo } = storeToRefs(fetchAdminInfoStore)

// ===== 修改名稱相關 =====
const nameModalRef = ref(null)
let nameModalInstance = null
const newName = ref('')
const nameErrorMsg = ref('')
const nameLoading = ref(false)
// ===== 修改密碼相關 =====
const passwordModalRef = ref(null)
let passwordModalInstance = null
const newPassword = ref('')
const oldPassword = ref('')
const passwordErrorMsg = ref('')
const passwordLoading = ref(false)


// ==== 修改名稱相關 ====
const openNameModal = () => {
    if (!adminInfo.value) return
    newName.value = adminInfo.value.adminName  // 打開時預帶目前的名稱
    nameErrorMsg.value = ''
    nameModalInstance.show()
}

// 修改名稱的事件
const handleNameSubmit = async () => {
    nameErrorMsg.value = ''
    
    if (!newName.value.trim()) {
        nameErrorMsg.value = '名稱不可為空'
        return
    }
    
    nameLoading.value = true
    try {
        await axios.patch('/api/admin/account/me/name', {
            newName: newName.value
        })
        
        adminInfo.value.adminName = newName.value  // 更新成功,同步畫面顯示
        nameModalInstance.hide()
    } catch (err) {
        console.log(err)
        nameErrorMsg.value = '更新失敗,請稍後再試'
    } finally {
        nameLoading.value = false
    }
}

// ==== 修密碼稱相關 ====
const openPasswordModal = () => {
    oldPassword.value = ''
    newPassword.value = ''
    passwordErrorMsg.value = ''
    passwordModalInstance.show()
}

// 修改密碼的事件
const handlePasswordSubmit = async () => {
    passwordErrorMsg.value = ''
    
    if (!oldPassword.value || !newPassword.value) {
        passwordErrorMsg.value = '請完整填寫所有欄位'
        return
    }
    
    passwordLoading.value = true
    try {
        await axios.patch('/api/admin/account/me/password', {
            oldPassword: oldPassword.value,
            newPassword: newPassword.value
        })
        passwordModalInstance.hide()
    } catch (err) {
        console.log(err)
        passwordErrorMsg.value = '密碼修改失敗,請確認原密碼是否正確'
    } finally {
        passwordLoading.value = false
    }
}

// ====掛元件時會初始化的動作====
onMounted(()=>{
    nameModalInstance = new Modal(nameModalRef.value)
    passwordModalInstance = new Modal(passwordModalRef.value)
})

</script>

<template>
    <div class="card shadow mb-4" v-if="adminInfo">
        <div class="card-header py-3">
            <div class="d-flex justify-content-between align-items-center">
                <h6 class="m-0 fw-bold text-primary">管理員資訊</h6>
                <button class="btn btn-sm btn-outline-primary" @click="openPasswordModal">
                變更密碼
                </button>
            </div>
        </div>
        <div class="card-body">
        <div class="row mb-3">
            <div class="col-3 text-muted">管理員編號</div>
            <div class="col-9">{{ adminInfo.adminId }}</div>
        </div>
        <div class="row mb-3">
            <div class="col-3 text-muted">管理員名稱</div>
            <div class="col-8">
                {{ adminInfo.adminName }}
                <button class="btn btn-sm btn-outline-primary ms-2" @click="openNameModal">
                    更新名稱
                </button>
            </div>
        </div>
        <div class="row mb-3">
            <div class="col-3 text-muted">角色</div>
            <div class="col-9">
            <span class="badge bg-danger">{{ adminInfo.role }}</span>
            </div>
        </div>
        <div class="row">
            <div class="col-3 text-muted">建立時間</div>
            <div class="col-9">{{ adminInfo.createDateTime }}</div>
        </div>
        </div>
    </div>

    <!-- 更新名稱的彈窗 -->
    <div class="modal fade" ref="nameModalRef" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">更新管理員名稱</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <label class="form-label">新名稱</label>
                    <input type="text" class="form-control" v-model="newName" :disabled="nameLoading" @keyup.enter="handleNameSubmit">
                    <p v-if="nameErrorMsg" class="text-danger mt-2 mb-0">{{ nameErrorMsg }}</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" :disabled="nameLoading">
                        取消
                    </button>
                    <button type="button" class="btn btn-primary" @click="handleNameSubmit" :disabled="nameLoading">
                        {{ nameLoading ? '更新中...' : '確定更新' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
    <!-- 更新密碼的彈窗 -->
    <div class="modal fade" ref="passwordModalRef" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">修改密碼</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <label class="form-label">原密碼</label>
                <input type="password" class="form-control mb-2" v-model="oldPassword" :disabled="passwordLoading">

                <label class="form-label">新密碼</label>
                <input type="password" class="form-control mb-2" v-model="newPassword" :disabled="passwordLoading">

                <p v-if="passwordErrorMsg" class="text-danger mt-2 mb-0">{{ passwordErrorMsg }}</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" :disabled="passwordLoading">
                    取消
                </button>
                <button type="button" class="btn btn-primary" @click="handlePasswordSubmit" :disabled="passwordLoading">
                    {{ passwordLoading ? '更新中...' : '確定更新' }}
                </button>
            </div>
        </div>
    </div>
</div>

</template>

<style scoped></style>