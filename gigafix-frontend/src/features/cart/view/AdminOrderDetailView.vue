<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const props = defineProps({
    orderId: String
})

const router = useRouter()

// 存放單筆訂單資料
const order = ref(null)

// 查詢指定訂單
const loadOrder = async () => {
    try {
        const response = await axios.get(
            `/api/admin/orders/${props.orderId}`
        )

        order.value = response.data

        console.log('訂單詳情：', response.data)

    } catch (error) {
        console.error('查詢訂單詳情失敗：', error)
        alert('查詢訂單失敗')
    }
}

// 返回訂單列表
const goBack = () => {
    router.push('/admin/orders')
}

// 進入頁面時查詢訂單
onMounted(() => {
    loadOrder()
})
// 訂單狀態中文
const orderStatusText = (status) => {
    const statusMap = {
        PENDING: '待處理',
        CANCELLED: '已取消',
        COMPLETED: '已完成'
    }

    return statusMap[status] || status
}

// 付款狀態中文
const paymentStatusText = (status) => {
    const statusMap = {
        UNPAID: '未付款',
        PAID: '已付款',
        FAILED: '付款失敗',
        REFUNDED: '已退款'
    }

    return statusMap[status] || status
}

// 物流狀態中文
const shippingStatusText = (status) => {
    const statusMap = {
        PENDING: '待出貨',
        SHIPPED: '已出貨',
        DELIVERED: '已送達',
        CANCELLED: '已取消'
    }

    return statusMap[status] || status
}

// 付款方式中文
const paymentMethodText = (method) => {
    const methodMap = {
        CREDIT_CARD: '信用卡',
        CASH_ON_DELIVERY: '貨到付款'
    }

    return methodMap[method] || method
}

// 配送方式中文
const shippingMethodText = (method) => {
    const methodMap = {
        HOME: '宅配',
        STORE: '超商取貨'
    }

    return methodMap[method] || method
}
const formatDateTime = (dateTime) => {
    if (!dateTime) {
        return '尚無'
    }

    const date = new Date(dateTime)

    return date.toLocaleString('zh-TW', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    })
}
const formatPrice = (price) => {
    if (price == null) {
        return '0'
    }

    return Number(price).toLocaleString('zh-TW')
}
</script>
<template>
    <div>
        <h1>訂單詳情</h1>

        <div v-if="order">

            <p>訂單 ID：{{ order.orderId }}</p>
            <p>會員 ID：{{ order.memberId }}</p>
            <p>總金額：${{ formatPrice(order.totalAmount) }}</p>
            <hr>
            <h3>訂單狀態</h3>
            <p>訂單狀態：{{ orderStatusText(order.orderStatus) }}</p>
            <p>付款方式：{{ paymentMethodText(order.paymentMethod) }}</p>
            <p>付款狀態：{{ paymentStatusText(order.paymentStatus) }}</p>
            <p>交易編號：{{ order.transactionId || '尚無' }}</p>
            <p>付款時間：{{ order.paidAt ? formatDateTime(order.paidAt) : '尚未付款' }}</p>
            <hr>
            <h3>收件資訊</h3>
            <p>收件人：{{ order.receiverName }}</p>
            <p>電話：{{ order.receiverPhone }}</p>
            <p>地址：{{ order.receiverAddress }}</p>
            <hr>
            <h3>物流資訊</h3>
            <p>配送方式：{{ shippingMethodText(order.shippingMethod) }}</p>
            <p>物流狀態：{{ shippingStatusText(order.shippingStatus) }}</p>
            <p>物流單號：{{ order.trackingNumber || '尚無' }}</p>
            <p>出貨時間：{{ order.shippedAt ? formatDateTime(order.shippedAt) : '尚未出貨' }}</p>
            <p>送達時間：{{ order.deliveredAt ? formatDateTime(order.deliveredAt) : '尚未送達' }}</p>

            <hr>
            <p>備註：{{ order.customerRemark || '無' }}</p>
            <p>建立時間：{{ order.createdAt ? formatDateTime(order.createdAt) : '尚無' }}</p>

        </div>

        <p v-else>
            載入中...
        </p>

        <button @click="goBack">
            返回訂單列表
        </button>
    </div>
</template>