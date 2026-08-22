<script setup>
import { useRouter } from 'vue-router'
import { ref } from 'vue'
import axios from 'axios';


const superAdminName = ref('')
const superAdminPassword = ref('')
const router = useRouter()
const errorMsg = ref('')

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
            alert(err.response.status)
            alert(err.response.data)
            errorMsg.value = '帳號或密碼錯誤'
        } else {
            errorMsg.value = '無法連接伺服器,請稍後再試'
        }

    }
}

</script>

<template>
    <form @submit.prevent="createSuperAdmin">
        <div>
            <label>帳號：</label>
            <input type="text" v-model="superAdminName">
        </div>
        <div>
            <label>密碼：</label>
            <input type="password" v-model="superAdminPassword">
        </div>
        <p v-if="errorMsg" class="text-danger">{{ errorMsg }}</p>
        <div>
            <button type="submit">註冊</button>
        </div>
    </form>
</template>

<style scoped></style>