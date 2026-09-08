<script setup>
import { onMounted, ref } from 'vue'
import { getMemberOrders } from '../../api/memberOrderApi'
import { useMemberAuth } from '../../composables/useMemberAuth'
import OrderCard from '../../components/OrderCard.vue'

const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')
// 取得會員的訂單列表
const loadOrders = async () => {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await getMemberOrders()

        orders.value = [...response.data].sort((a, b) => {
            return new Date(b.createdAt) - new Date(a.createdAt)
        })
    } catch (error) {
        console.error(error)
        errorMessage.value = '訂單載入失敗'
    } finally {
        loading.value = false
    }
}
// 會員驗證與初始化訂單列表
const { checkMemberLogin } = useMemberAuth()

onMounted(async () => {
    const loggedIn = await checkMemberLogin()

    if (!loggedIn) {
        return
    }

    loadOrders()
})
</script>

<template>
    <div>
        <h2 class="mb-4">我的訂單</h2>

        <div v-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
        </div>

        <div v-if="loading">
            載入中...
        </div>

        <div v-else-if="orders.length === 0" class="text-muted">
            目前沒有訂單
        </div>

        <div v-else>
            <OrderCard v-for="order in orders" :key="order.orderId" :order="order" />
        </div>
    </div>
</template>