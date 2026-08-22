<script setup>
import { useRouter, RouterLink } from 'vue-router'
import { ref } from 'vue'
import axios from 'axios';

const adminName = ref('')
const password = ref('')

const router = useRouter()
const errorMsg = ref('')

const adminLogin = async () => {
    errorMsg.value = ''
    try {
        const res = await axios.post("/api/adminlogin", {
            adminName: adminName.value,
            password: password.value
        })
        
        router.push('/admin') //如果發出請求後回傳的物件不是空值的話就轉跳到管理者頁面
    } catch (err) {
        if (err.response) {
            alert(err.response.status)
            alert(err.response.data)
            errorMsg.value = '帳號或密碼錯誤'
        } else {
            errorMsg.value = '無法連接伺服器,請稍後再試'
        }

    }
}
const createSuperAdmin = async () => {
    await axios.post("/api/admin/account/super-admin", {
        adminName: adminName.value,
        password: password.value
    })
}

</script>

<template>
    <form @submit.prevent="adminLogin">
        <div>
            <label>帳號：</label>
            <input type="text" v-model="adminName">
        </div>
        <div>
            <label>密碼：</label>
            <input type="password" v-model="password">
        </div>
        <p v-if="errorMsg" class="text-danger">{{ errorMsg }}</p>
        <div>
            <button type="submit">登入</button>
        </div>
    </form>
    <RouterLink to="/creatSuperAdminView">尚未建立總管理員？按此建立</RouterLink>
    
</template>

<style scoped>

</style>