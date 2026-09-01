<script setup>
import { useRouter } from 'vue-router'
import { ref } from 'vue'
import axios from 'axios';


const superAdminName = ref('')
const superAdminPassword = ref('')
const checkPassword = ref('')
const router = useRouter()
const errorMsg = ref('')

const checkSamePassword = () => { //確認密碼是前端防呆，跟後端沒關係
    if (superAdminPassword.value === '' || checkPassword.value === '') {
        errorMsg.value = '請輸入密碼或確認密碼'
    }else if (superAdminPassword.value.length < 8 || checkPassword.value.length < 8) {
        errorMsg.value = '密碼長度必須至少 8 位數'
    } else if (superAdminPassword.value !== checkPassword.value) {
        errorMsg.value = '確認密碼必須與密碼相符'
    }  else {
        errorMsg.value = ''
    }
}

const createSuperAdmin = async () => {
    console.log('程式被呼叫了')
    try {
        await axios.post("/api/admin/account/super-admin", {
            superAdminName: superAdminName.value,
            superAdminPassword: superAdminPassword.value
        })
        router.push('/adminLogin') //如果發出註冊請求後回傳的物件不是空值的話就轉跳到登入頁面
    } catch (err) {
        console.log(err)
        if (err.response) {
            alert(`新增管理員失敗，原因: ${err.response.data.message}`)
            errorMsg.value = '帳號或密碼錯誤'
        } else {
            errorMsg.value = '無法連接伺服器,請稍後再試'
        }

    }
}

</script>

<template>
    <div class="login-page d-flex align-items-center justify-content-center">
        <div class="card login-card shadow-lg border-0">
            <div class="card-body p-4 p-md-5">
                <div class="text-center mb-4">
                    <span class="fs-1">😊</span>
                    <h3 class="fw-bold mt-2 mb-0 text-primary-brand">Gigafix ADMIN</h3>
                    <p class="text-muted small mb-0">建立總管理員</p>
                </div>

                <form @submit.prevent="createSuperAdmin">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">帳號</label>
                        <input type="text" class="form-control" v-model="superAdminName" placeholder="請輸入帳號">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">密碼</label>
                        <input type="password" class="form-control" v-model="superAdminPassword" placeholder="請輸入密碼" @input="checkSamePassword()">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">確認密碼</label>
                        <input type="password" class="form-control" v-model="checkPassword" placeholder="請再次輸入登入密碼" @input="checkSamePassword()">
                    </div>

                    <p v-if="errorMsg" class="text-danger small mb-3">{{ errorMsg }}</p>

                    <div class="d-grid">
                        <button v-if="errorMsg" type="submit" class="btn btn-primary-brand fw-semibold py-2" disabled>請輸入正確格式的密碼</button>
                        <button v-if="errorMsg == ''" type="submit" class="btn btn-primary-brand fw-semibold py-2">註冊</button>
                    </div>
                </form>

                <div class="text-center mt-4">
                    <RouterLink to="/adminLogin" class="small create-admin-link">
                        已有總管理員？返回登入
                    </RouterLink>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.login-page {
    min-height: 100vh;
    background: linear-gradient(135deg, #a5b4fc 0%, #c7d2fe 100%);
}

.login-card {
    width: 100%;
    max-width: 400px;
    border-radius: 16px;
    background-color: #ffffff;
}

.text-primary-brand {
    color: #818cf8;
}

.btn-primary-brand {
    background-color: #6366f1;
    border-color: #6366f1;
    color: #fff;
}

.btn-primary-brand:hover {
    background-color: #4f46e5;
    border-color: #4f46e5;
    color: #fff;
}

.form-control:focus {
    border-color: #c7d2fe;
    box-shadow: 0 0 0 0.25rem rgba(199, 210, 254, 0.4);
}

.create-admin-link {
    color: #6b7280;
    text-decoration: none;
    transition: color 0.2s ease;
}

.create-admin-link:hover {
    color: #4f46e5;
    text-decoration: underline;
}
</style>