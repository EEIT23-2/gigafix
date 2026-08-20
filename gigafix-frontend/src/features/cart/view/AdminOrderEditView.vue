<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const props = defineProps({
    orderId: String
})

const router = useRouter()

// 存放單筆訂單資料
const order = ref({})

// 查詢單筆訂單
const loadOrder = async () => {
    try {
        const response = await axios.get(
            `/api/admin/orders/${props.orderId}`
        )

        order.value = response.data

        console.log('單筆訂單資料：', response.data)
    } catch (error) {
        console.error('查詢訂單失敗：', error)
    }
}
// 修改訂單
const updateOrder = async () => {
    try {

        const request = {
            receiverName: order.value.receiverName,
            receiverPhone: order.value.receiverPhone,
            receiverAddress: order.value.receiverAddress,
            shippingMethod: order.value.shippingMethod,
            customerRemark: order.value.customerRemark
        }
        if (!order.value.receiverName.trim()) {
            alert('請輸入收件人姓名')
            return
        }

        if (!order.value.receiverPhone.trim()) {
            alert('請輸入收件人電話')
            return
        }

        if (!order.value.receiverAddress.trim()) {
            alert('請輸入收件地址')
            return
        }

        if (!order.value.shippingMethod) {
            alert('請選擇配送方式')
            return
        }

        await axios.put(
            `/api/admin/orders/${props.orderId}`,
            request
        )

        alert('訂單修改成功')

        router.push('/admin/orders')

    } catch (error) {
        console.error('修改訂單失敗：', error)
        alert('修改失敗')
    }
}
// 回訂單列表
const goBack = () => {
    router.push('/admin/orders')
}

// 進入頁面時自動查詢
onMounted(() => {
    loadOrder()
})
// 訂單狀態中文
const orderStatusText = (status) => {
    const statusMap = {
        PENDING: '待處理',
        COMPLETED: '已完成',
        CANCELLED: '已取消'
    }

    return statusMap[status] || status
}

// 付款狀態中文
const paymentStatusText = (status) => {
    const statusMap = {
        UNPAID: '未付款',
        PAID: '已付款'
    }

    return statusMap[status] || status
}
// 金額格式化
const formatPrice = (price) => {
    if (price == null) {
        return '0'
    }

    return Number(price).toLocaleString('zh-TW')
}
</script>

<template>
    <div>
        <h1>編輯訂單</h1>

        <p>訂單 ID：{{ order.orderId }}</p>
        <p>總金額：{{ formatPrice(order.totalAmount) }}</p>
        <p>訂單狀態：{{ orderStatusText(order.orderStatus) }}</p>
        <p>付款狀態：{{ paymentStatusText(order.paymentStatus) }}</p>
        <div>
            <label>收件人：</label>
            <input v-model="order.receiverName">
        </div>

        <div>
            <label>電話：</label>
            <input v-model="order.receiverPhone">
        </div>

        <div>
            <label>地址：</label>
            <input v-model="order.receiverAddress">
        </div>

        <div>
            <label>配送方式：</label>
            <select v-model="order.shippingMethod">
                <option value="HOME">宅配</option>
                <option value="STORE">超商取貨</option>
            </select>
        </div>

        <div>
            <label>備註：</label>
            <input v-model="order.customerRemark">
        </div>
        <button @click="updateOrder">
            保存修改
        </button>
        <button @click="goBack">
            返回訂單列表
        </button>
    </div>
</template>

<style scoped></style>