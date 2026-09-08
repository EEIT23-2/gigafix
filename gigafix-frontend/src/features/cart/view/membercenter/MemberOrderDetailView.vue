<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useMemberAuth } from '../../composables/useMemberAuth'
import OrderInfo from '../../components/OrderInfo.vue'
import OrderItemList from '../../components/OrderItemList.vue'

import {
    getMemberOrder,
    cancelMemberOrder
} from '../../api/memberOrderApi'
// 取得會員的單一訂單與取消訂單相關狀態與方法
const route = useRoute()

const order = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const cancelling = ref(false)
// 載入訂單詳情
const loadOrder = async () => {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await getMemberOrder(route.params.orderId)
        order.value = response.data
    } catch (error) {
        console.error(error)
        errorMessage.value = '訂單載入失敗'
    } finally {
        loading.value = false
    }
}
// 取消訂單
const handleCancel = async () => {
    if (!confirm('確定要取消這筆訂單嗎？')) {
        return
    }

    cancelling.value = true
    errorMessage.value = ''

    try {
        const response = await cancelMemberOrder(order.value.orderId)
        order.value = response.data
    } catch (error) {
        console.error(error)

        errorMessage.value =
            error.response?.data?.message ||
            error.response?.data?.error ||
            '取消訂單失敗'
    } finally {
        cancelling.value = false
    }
}
// 會員驗證與初始化訂單詳情
const { checkMemberLogin } = useMemberAuth()

onMounted(async () => {
    const loggedIn = await checkMemberLogin()

    if (!loggedIn) {
        return
    }

    loadOrder()
})
</script>

<template>
    <div>
        <h2 class="mb-4">訂單詳情</h2>
        <RouterLink to="/member-center/orders" class="btn btn-outline-secondary mb-3">
            返回我的訂單
        </RouterLink>
        <div v-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
        </div>

        <div v-if="loading">
            載入中...
        </div>

        <div v-else-if="order" class="card">
            <div class="card-body">

                <OrderInfo :order="order" />
                
                <hr>

                <OrderItemList :items="order.orderItems" />

                <hr>

                <p class="fw-bold">
                    總金額：
                    NT$ {{ order.totalAmount?.toLocaleString() }}
                </p>

                <button v-if="
                    order.orderStatus === 'PENDING' &&
                    order.paymentStatus === 'UNPAID'
                " class="btn btn-outline-danger" :disabled="cancelling" @click="handleCancel">
                    {{ cancelling ? '取消中...' : '取消訂單' }}
                </button>
            </div>
        </div>
    </div>
</template>