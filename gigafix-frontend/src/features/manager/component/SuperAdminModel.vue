<script setup>
import axios from 'axios';
import { Modal } from 'bootstrap'
import { ref, computed, onMounted } from 'vue';

const allAdmins = ref([])
const errorMsg = ref('')
// ===== 創建使用者相關 =====
const createAdminModalRef = ref(null)
let createAdminModalInstance = null
const adminName = ref('')
const password = ref('')
const role = ref('')
const createAdminLoading = ref(false)
const createAdminErrorMsg =ref('')

//====取得所有的管理者====
const fetchAllAdmin = async () => {
    errorMsg.value = ''
    try {
        const rep = await axios.get("/api/admin/account")
        allAdmins.value = rep.data
    } catch (err) {
        console.error('抓取管理員列表失敗', err)
        errorMsg.value = '無法載入管理員列表,請稍後再試'
    }
}
// 第一筆:總管理員,不參與迴圈,也不會有刪改按鈕
const superAdmin = computed(() => allAdmins.value[0])
// 其餘:從索引 1 開始的所有 admin,這些才有刪改功能
const otherAdmins = computed(() => allAdmins.value.slice(1))

//====創建管理員====
const openCreateAdminModal = () => {
    adminName.value = ''
    password.value = ''
    role.value = ''
    createAdminModalInstance.show()
}
const createAdmin = async () => {
   createAdminErrorMsg.value = null
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
        createAdminModalInstance.hide()
        fetchAllAdmin()
    } catch (err) {
        console.log(err)
        createAdminErrorMsg.value = '更新失敗,請稍後再試'
    } finally {
        createAdminLoading.value = false
    }

}


//只要元件掛載就去抓所有admin出來
onMounted(() => {
    createAdminModalInstance = new Modal(createAdminModalRef.value);
    fetchAllAdmin()
    createAdminModalRef.value.addEventListener('hide.bs.modal', () => {
        document.activeElement?.blur()
    })
})
</script>

<template>
    <!-- 所有管理員的資料 -->
    <div class="card shadow mb-4">
        <div class="card-header py-3 d-flex justify-content-between align-items-center">
            <h6 class="m-0 fw-bold text-primary">管理員列表</h6>
            <button class="btn btn-primary btn-sm shadow-sm" @click="openCreateAdminModal">
                <i class="bi bi-plus-lg me-1"></i>創建新管理員
            </button>
        </div>
        <div class="card-body">
            <p v-if="errorMsg" class="text-danger">{{ errorMsg }}</p>
            <div class="row mb-2 text-muted fw-bold">
                <div class="col-1">編號</div>
                <div class="col-3">名稱</div>
                <div class="col-3">角色</div>
                <div class="col-2">建立時間</div>
                <div class="col-3">操作</div>
            </div>

            <div class="row align-items-center py-2 border-top" v-if="superAdmin">
                <div class="col-1">{{ superAdmin.adminId }}</div>
                <div class="col-3">{{ superAdmin.adminName }}</div>
                <div class="col-3">
                    <span class="badge bg-danger">{{ superAdmin.role }}</span>
                </div>
                <div class="col-2 text-truncate">{{ superAdmin.createDateTime }}</div>
                <div class="col-3"></div>
            </div>

            <div class="row align-items-center py-2 border-top" v-for="admin in otherAdmins" :key="admin.adminId">
                <div class="col-1">{{ admin.adminId }}</div>
                <div class="col-3">{{ admin.adminName }}</div>
                <div class="col-3">
                    <span class="badge bg-danger">{{ admin.role }}</span>
                </div>
                <div class="col-2 text-truncate">{{ admin.createDateTime }}</div>
                <div class="col-3 d-flex gap-2 justify-content-end">
                    <button class="btn btn-sm btn-outline-warning square-btn" title="修改權限">
                        <i class="bi bi-person-gear"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-primary square-btn" title="修改密碼">
                        <i class="bi bi-lock"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger square-btn" title="刪除">
                        <i class="bi bi-trash-fill"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- 創建管理員 -->
    <div class="modal fade" ref="createAdminModalRef" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title ">新增管理員名稱</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <label class="form-label">名稱</label>
                    <input type="text" class="form-control" v-model="adminName" :disabled="createAdminLoading">
                    <label class="form-label">密碼</label>
                    <input type="text" class="form-control" v-model="password" :disabled="createAdminLoading" >
                    <label class="form-label">管理員類型</label>
                    <select class="form-select" v-model="role" :disabled="createAdminLoading">
                        <option value="" disabled>請選擇權限</option>
                        <option value="ROLE_DEPUTY_ADMIN">副管理員</option>
                        <option value="ROLE_FORUM_ADMIN">論壇管理員</option>
                        <option value="ROLE_ECOMMERCE_ADMIN">訂單商品管理員</option>
                        <option value="ROLE_REPAIR_ADMIN">手機維修管理員</option>
                    </select>
                    <p v-if="createAdminErrorMsg" class="text-danger mt-2 mb-0">{{ createAdminErrorMsg }}</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" :disabled="createAdminLoading">
                        取消
                    </button>
                    <button type="button" class="btn btn-primary" @click="createAdmin" :disabled="createAdminLoading" >
                        {{ createAdminLoading ? '創建會員中' : '送出' }}
                    </button>
                </div>
            </div>
        </div>
    </div>

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