<script setup>
import axios from 'axios';
import { ref, computed, onMounted } from 'vue';
import BaseModal from './BaseModal.vue';
import { getRoleLabel } from '../util/roleMap.js';
import { formatDateTime } from '../util/timeMap.js';

const allAdmins = ref([])
const errorMsg = ref('')
const updateOrDeleteAdmin =ref(null) //刪除或修改時會傳給後端的id變數
// ====刪除管理員的相關宣告====
const showDeleteModal = ref(false)
const superAdminComfirmPWD = ref('')
const deleteErrorMsg =ref('')
// ====創建管理員相關的宣告====
const showCreateModal = ref(false)
const adminName = ref('')
const password = ref('')
const role = ref('')
const createErrorMsg = ref('')
const createAdminLoading = ref(false)
const createAdminErrorMsg = ref('')
// ====修改管理員腳色相關的宣告====
const showUpdateRoleModal = ref(false)
const updateAdminRole =ref('')
const updateRoleErrorMsg =ref('')
// ====重設管理員密碼相關的宣告====
const showRestPasswordModal = ref(false)
const updateAdminPassword =ref('')
const updatePasswordErrorMsg =ref('')
//====取得所有的管理者====
const fetchAllAdmin = async () => {
    errorMsg.value = ''
    try {
        const rep = await axios.get("/api/admin/account")
        allAdmins.value = rep.data
    } catch (err) {
        alert(`管理員列表讀取失敗，原因: ${err.response.data.message}`)
        errorMsg.value = '無法載入管理員列表,請稍後再試'
    }
}
// 第一筆:總管理員,不參與迴圈,也不會有刪改按鈕
const superAdmin = computed(() => allAdmins.value[0])
// 其餘:從索引 1 開始的所有 admin,這些才有刪改功能
const otherAdmins = computed(() => allAdmins.value.slice(1))

// ====創建管理員====
const openCreateAdminModal = () => {
    createErrorMsg.value = '請輸入帳號'
    adminName.value = ''
    password.value = ''
    role.value = ''
    showCreateModal.value = true
}
const createAdmin = async () => {
    createAdminErrorMsg.value = ''
    if (!adminName.value.trim()) {
        createAdminErrorMsg.value = '名稱不可為空'
        return
    }
    if (!password.value.trim()) {
        createAdminErrorMsg.value = '密碼不可為空'
        return
    }
    if (!role.value.trim()) {
        createAdminErrorMsg.value = '未選擇管理員'
        return
    }
    createAdminLoading.value = true
    
    try {
        await axios.post('/api/admin/account', {
            password: password.value,
            adminName: adminName.value,
            role: role.value
        })
        showCreateModal.value = false
        adminName.value = ''
        password.value = ''
        role.value = ''
        fetchAllAdmin()
    } catch (err) {
        alert(`創建管理員失敗，原因: ${err.response.data.message}`)
        createAdminErrorMsg.value = '建立帳號失敗'
    } finally {
        createAdminLoading.value = false
    }
    
}
// createErrorMsg
const checkCreateError = () => {
    if (adminName.value === '') {
        createErrorMsg.value = '請輸入帳號'
    }else if (password.value === '') {
        createErrorMsg.value = '請輸入密碼'
    }else if (password.value.length < 8 ) {
        createErrorMsg.value = '密碼長度必須至少 8 位數'
    }else if (role.value === '') {
        createErrorMsg.value = '請選擇權限'
    }else {
        createErrorMsg.value = ''
    }
}


//====修改管理員權限====
const openUpdateRoleModal = (adminInfo) =>{
    updateOrDeleteAdmin.value = adminInfo
    updateAdminRole.value = ''
    updateRoleErrorMsg.value = '選擇管理員權限'
    showUpdateRoleModal.value = true
}

const updateRoleAdmin = async () => {
    try {
        await axios.patch(`/api/admin/account/${updateOrDeleteAdmin.value.adminId}/role`,{
            role: updateAdminRole.value
        })
        showUpdateRoleModal.value = false
        alert('更新成功')
    } catch (err) { //回傳4xx,5xx
        alert(`更新管理員權限失敗，原因: ${err.response.data.message}`)
    }finally{
        fetchAllAdmin()
    }
}

const checkUpdateRoleError = () => {
    if (updateAdminRole.value === '') {
        updateRoleErrorMsg.value = '選擇管理員權限'
    }else {
        updateRoleErrorMsg.value = ''
    }
}

//====重設管理員密碼====
const openResetPasswordModal = (adminInfo) =>{
    updateOrDeleteAdmin.value = adminInfo
    updatePasswordErrorMsg.value = '請輸入密碼'
    updateAdminPassword.value = ''
    showRestPasswordModal.value = true
}

const resetAdminPassword = async () => {
    try {
        await axios.patch(`/api/admin/account/${updateOrDeleteAdmin.value.adminId}/password`,{
            newPassword: updateAdminPassword.value
        })
        showRestPasswordModal.value = false
        alert('更新成功')
    } catch (err) { //回傳4xx,5xx
        alert(`重新設定管理員密碼失敗，原因: ${err.response.data.message}`)
    }finally{
        fetchAllAdmin()
    }
}
//updatePasswordErrorMsg
const checkUpdatePasswordError = () => {
    if (updateAdminPassword.value === '') {
        updatePasswordErrorMsg.value = '請輸入密碼'
    }else if (updateAdminPassword.value.length < 8) {
        updatePasswordErrorMsg.value = '密碼長度必須至少 8 位數'
    }else {
        updatePasswordErrorMsg.value = ''
    }
}

//====刪除管理員====
const openDeleteModal = (adminInfo) => {
    superAdminComfirmPWD.value = ''
    deleteErrorMsg.value = '請輸入密碼'
    updateOrDeleteAdmin.value = adminInfo // 複製一份,避免直接改到原始列表資料
    showDeleteModal.value = true
}

const deleteAdmin = async () => {
    try {
        await axios.delete(`/api/admin/account/${updateOrDeleteAdmin.value.adminId}`,{
            // delete:第二個參數是 config,body 要包在 config.data 裡面
            data: {
                SAPassword: superAdminComfirmPWD.value
            }
        })
        showDeleteModal.value = false
        alert('刪除成功')
    } catch (err) { //回傳4xx,5xx
        alert(`刪除管理員失敗，原因: ${err.response.data.message}`)
    }finally{
        fetchAllAdmin()
    }
}
const checkDeleteError = () => {
    if (superAdminComfirmPWD.value === '') {
        deleteErrorMsg.value = '請輸入密碼'
    }else if (superAdminComfirmPWD.value.length < 8) {
        deleteErrorMsg.value = '密碼長度必須至少 8 位數'
    }else {
        deleteErrorMsg.value = ''
    }
}

//只要元件掛載就去抓所有admin出來
onMounted(() => {
    fetchAllAdmin()
})
</script>

<template>
    <!-- 所有管理員的資料 -->
    <div class="card shadow mb-4 overflow-hidden">
        <div class="card-header py-3 d-flex justify-content-between align-items-center">
            <h6 class="m-0 fw-bold text-primary">管理員列表</h6>
            <button class="btn btn-primary btn-sm shadow-sm" @click="openCreateAdminModal">
                <i class="bi bi-plus-lg me-1"></i>創建新管理員
            </button>
        </div>
        <p v-if="errorMsg" class="text-danger px-3 pt-3 mb-0">{{ errorMsg }}</p>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead>
                    <tr class="text-muted">
                        <th>編號</th>
                        <th>名稱</th>
                        <th>角色</th>
                        <th>建立時間</th>
                        <th class="text-center">操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-if="superAdmin">
                        <td>{{ superAdmin.adminId }}</td>
                        <td class="cell-truncate">{{ superAdmin.adminName }}</td>
                        <td>
                            <span class="badge bg-danger">{{ getRoleLabel(superAdmin.role) }}</span>
                        </td>
                        <td>{{ formatDateTime(superAdmin.createDateTime) }}</td>
                        <td></td>
                    </tr>

                    <tr v-for="admin in otherAdmins" :key="admin.adminId">
                        <td>{{ admin.adminId }}</td>
                        <td class="cell-truncate">{{ admin.adminName }}</td>
                        <td>
                            <span class="badge bg-danger">{{ getRoleLabel(admin.role) }}</span>
                        </td>
                        <td>{{ formatDateTime(admin.createDateTime) }}</td>
                        <td>
                            <div class="d-flex gap-2 justify-content-center">
                                <button class="btn btn-sm btn-outline-warning square-btn" title="修改該管理員角色" @click="openUpdateRoleModal(admin)">
                                    <i class="bi bi-person-gear"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-primary square-btn" title="重設該管理員密碼" @click="openResetPasswordModal(admin)">
                                    <i class="bi bi-key-fill rotate-45"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-danger square-btn" title="刪除該管理員" @click="openDeleteModal(admin)">
                                    <i class="bi bi-trash-fill"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- 總管理員創建其他管理員的彈窗 -->
    <BaseModal v-model="showCreateModal">
        <template #title>管理員名稱</template>
        <label class="form-label">名稱</label>
        <input type="text" class="form-control" v-model="adminName" :disabled="createAdminLoading" @input="checkCreateError()">
        
        <label class="form-label">密碼</label>
        <input type="password" class="form-control" v-model="password" :disabled="createAdminLoading" @input="checkCreateError()">
        
        <label class="form-label">管理員角色類型</label>
        <select class="form-select" v-model="role" :disabled="createAdminLoading" @change="checkCreateError()">
            <option value="" disabled>請選擇角色</option>
            <option value="ROLE_DEPUTY_ADMIN">副管理員</option>
            <option value="ROLE_FORUM_ADMIN">論壇管理員</option>
            <option value="ROLE_ECOMMERCE_ADMIN">訂單商品管理員</option>
            <option value="ROLE_REPAIR_ADMIN">手機維修管理員</option>
        </select>
        <p v-if="createAdminErrorMsg" class="text-danger mt-2 mb-0">{{ createAdminErrorMsg }}</p>

        <template #footer>
            <p  v-if="createErrorMsg" class="text-danger small mb-3">{{ createErrorMsg }}</p>
            <button type="button" class="btn btn-secondary" @click="showCreateModal = false" :disabled="createAdminLoading">取消</button>
            <button v-if="createErrorMsg" type="button" class="btn btn-primary" disabled>請輸入正確資訊</button>
            <button v-if="createErrorMsg == ''" type="button" class="btn btn-primary" @click="createAdmin()" :disabled="createAdminLoading">{{ createAdminLoading ? '創建會員中' : '送出' }}</button>
        </template>
    </BaseModal>

    <!-- 總管理員修改其他管理員腳色的彈窗 -->
    <BaseModal v-model="showUpdateRoleModal" :adminInfo="updateOrDeleteAdmin" @closed="updateAdminRole = ''; updateOrDeleteAdmin = null">
        <template #title>修改管理員角色</template>

        <label class="form-label">修改的管理員角色類型</label>
        <select class="form-select" v-model="updateAdminRole" @change="checkUpdateRoleError()">
            <option value="" disabled>請選擇角色</option>
            <option value="ROLE_DEPUTY_ADMIN">副管理員</option>
            <option value="ROLE_FORUM_ADMIN">論壇管理員</option>
            <option value="ROLE_ECOMMERCE_ADMIN">訂單商品管理員</option>
            <option value="ROLE_REPAIR_ADMIN">手機維修管理員</option>
        </select>
        <template #footer>
            <p  v-if="updateRoleErrorMsg" class="text-danger small mb-3">{{ updateRoleErrorMsg }}</p>
            <button class="btn btn-secondary" @click="showUpdateRoleModal = false">取消</button>
            <button v-if="updateRoleErrorMsg" type="button" class="btn btn-primary" disabled>請選擇管理員權限</button>
            <button v-if="updateRoleErrorMsg == ''" class="btn btn-primary" @click="updateRoleAdmin()">送出</button>
        </template>
    </BaseModal>

    <!-- 總管理員重設其他管理員密碼的彈窗 -->
    <BaseModal v-model="showRestPasswordModal" :adminInfo="updateOrDeleteAdmin" @closed="updateAdminPassword = ''; updateOrDeleteAdmin = null">
        <template #title>重新設定管理員密碼</template>

        <label class="form-label">密碼</label>
        <input type="password" class="form-control" v-model="updateAdminPassword" :disabled="createAdminLoading" placeholder="請輸入重設的密碼" @input="checkUpdatePasswordError()">

        <template #footer>
            <p  v-if="updatePasswordErrorMsg" class="text-danger small mb-3">{{ updatePasswordErrorMsg }}</p>
            <button class="btn btn-secondary" @click="showDeleteModal = false">取消</button>
            <button v-if="updatePasswordErrorMsg" type="button" class="btn btn-primary" disabled>請輸入密碼</button>
            <button v-if="updatePasswordErrorMsg == ''" class="btn btn-primary" @click="resetAdminPassword()">送出</button>
        </template>
    </BaseModal>

    <!-- 總管理員刪除其他管理員的彈窗 -->
    <BaseModal v-model="showDeleteModal" :adminInfo="updateOrDeleteAdmin" @closed="superAdminComfirmPWD = ''; updateOrDeleteAdmin = null">
        <template #title>確認刪除管理員</template>
        <input type="password" v-model="superAdminComfirmPWD" class="form-control" placeholder="請輸入密碼以確認要刪除該管理員" @input="checkDeleteError()">
        <template #footer>
            <p  v-if="deleteErrorMsg" class="text-danger small mb-3">{{ deleteErrorMsg }}</p>
            <button class="btn btn-secondary" @click="showDeleteModal = false">取消</button>
            <button v-if="deleteErrorMsg" type="button" class="btn btn-primary" disabled>請輸入密碼</button>
            <button v-if="deleteErrorMsg == ''" class="btn btn-primary" @click="deleteAdmin()">確定刪除</button>
        </template>
    </BaseModal>

</template>

<style scoped>
.cell-truncate {
    max-width: 200px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

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

.rotate-45 {
  transform: rotate(45deg);
  display: inline-block; /* 確保旋轉正常運作 */
}
</style>