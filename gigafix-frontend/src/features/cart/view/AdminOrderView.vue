<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

// 使用 Vue Router 的 useRouter 來導航
const router = useRouter()
// 存放後端回傳的訂單資料
const orders = ref([])
// 進入頁面時自動查詢訂單
const createOrder = () => {
    router.push('/admin/orders/create')
}
// 會員下拉選單
const members = ref([])

// 目前選擇的會員 ID
const selectedMemberId = ref('')
// 查詢全部訂單
const loadOrders = async () => {
    try {
        const response = await axios.get('/api/admin/orders')

        orders.value = response.data

        console.log('訂單資料：', response.data)
    } catch (error) {
        console.error('查詢訂單失敗：', error)
    }
}
// 取得會員下拉選單
const loadMembers = async () => {
    try {
        const response = await axios.get(
            '/api/admin/orders/create-options'
        )

        members.value = response.data.members

    } catch (error) {
        console.error('取得會員選項失敗：', error)
    }
}
// 依會員查詢訂單
const searchByMember = async () => {

    if (!selectedMemberId.value) {
        alert('請先選擇會員')
        return
    }

    try {
        const response = await axios.get(
            `/api/admin/orders/member/${selectedMemberId.value}`
        )

        orders.value = response.data

        console.log('會員訂單：', response.data)

    } catch (error) {
        console.error('查詢會員訂單失敗：', error)
        alert('查詢失敗')
    }
}
// 顯示全部訂單
const showAllOrders = () => {
    selectedMemberId.value = ''
    loadOrders()
}
// 刪除訂單
const deleteOrder = async (orderId) => {
    const confirmed = confirm(`確定要刪除訂單 ${orderId} 嗎？`)

    if (!confirmed) {
        return
    }

    try {
        await axios.delete(`/api/admin/orders/${orderId}`)

        alert('刪除成功')

        // 刪除後重新查詢訂單
        loadOrders()
    } catch (error) {
        console.error('刪除訂單失敗：', error)
        alert('刪除失敗')
    }
}
// 前往訂單編輯頁
const editOrder = (orderId) => {
    router.push(`/admin/orders/${orderId}/edit`)
}
// 前往訂單詳情頁
const viewOrder = (orderId) => {
    router.push(`/admin/orders/${orderId}`)
}
// 進入頁面時，自動查詢訂單
onMounted(() => {
    loadOrders()
    loadMembers()
})
// 訂單狀態中文顯示
const orderStatusText = (status) => {
    const statusMap = {
        PENDING: '待處理',
        CANCELLED: '已取消',
        COMPLETED: '已完成'
    }

    return statusMap[status] || status
}

// 付款狀態中文顯示
const paymentStatusText = (status) => {
    const statusMap = {
        UNPAID: '未付款',
        PAID: '已付款',
        FAILED: '付款失敗',
        REFUNDED: '已退款'
    }

    return statusMap[status] || status
}

// 物流狀態中文顯示
const shippingStatusText = (status) => {
    const statusMap = {
        PENDING: '待出貨',
        SHIPPED: '已出貨',
        DELIVERED: '已送達',
        CANCELLED: '已取消'
    }

    return statusMap[status] || status
}
// 前往出貨頁
const shipOrder = (orderId) => {
    router.push(`/admin/orders/${orderId}/ship`)
}
const deliverOrder = async (orderId) => {

    const confirmed = confirm(
        `確定訂單 ${orderId} 已送達嗎？`
    )

    if (!confirmed) {
        return
    }

    try {

        await axios.post(
            `/api/admin/orders/${orderId}/deliver`
        )

        alert('訂單已標記為送達')

        // 重新查詢列表
        loadOrders()

    } catch (error) {

        console.error('更新送達狀態失敗：', error)

        alert('更新送達狀態失敗')
    }
}
// 管理員取消訂單
const cancelOrder = async (orderId) => {

    const confirmed = confirm(
        `確定要取消訂單 ${orderId} 嗎？`
    )

    if (!confirmed) {
        return
    }

    try {

        await axios.post(
            `/api/admin/orders/${orderId}/cancel`
        )

        alert('訂單取消成功')

        // 重新查詢訂單列表
        loadOrders()

    } catch (error) {

        console.error('取消訂單失敗：', error)

        alert('取消訂單失敗')
    }
}
// Demo：模擬會員付款成功
const demoPayOrder = async (order) => {

    const confirmed = confirm(
        `確定要模擬訂單 ${order.orderId} 付款成功嗎？`
    )

    if (!confirmed) {
        return
    }

    try {

        // Demo 用交易編號
        const transactionId =
            `DEMO-${order.orderId}-${Date.now()}`

        await axios.post(
            `/api/members/${order.memberId}/orders/${order.orderId}/payment`,
            {
                transactionId: transactionId
            }
        )

        alert('模擬付款成功')

        // 重新查詢訂單
        loadOrders()

    } catch (error) {

        console.error('模擬付款失敗：', error)

        alert('模擬付款失敗')
    }
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
        <h1>訂單管理</h1>
        <button @click="createOrder">
            新增訂單
        </button>
        <div>
            <label>會員：</label>

            <select v-model="selectedMemberId">
                <option value="">請選擇會員</option>

                <option v-for="member in members" :key="member.memberId" :value="member.memberId">
                    {{ member.memberName }}
                    （ID：{{ member.memberId }}）
                </option>
            </select>

            <button @click="searchByMember">
                查詢該會員訂單
            </button>

            <button @click="showAllOrders">
                返回全部訂單
            </button>
        </div>
        <table border="1">
            <thead>
                <tr>
                    <th>訂單 ID</th>
                    <th>總金額</th>
                    <th>訂單狀態</th>
                    <th>付款狀態</th>
                    <th>收件人</th>
                    <th>物流狀態</th>
                    <th>操作</th>
                </tr>
            </thead>

            <tbody>
                <tr v-for="order in orders" :key="order.orderId">
                    <td>{{ order.orderId }}</td>
                    <td>{{ formatPrice(order.totalAmount) }}</td>
                    <td>{{ orderStatusText(order.orderStatus) }}</td>
                    <td>{{ paymentStatusText(order.paymentStatus) }}</td>
                    <td>{{ order.receiverName }}</td>
                    <td>{{ shippingStatusText(order.shippingStatus) }}</td>
                    <td>
                        <!-- 所有訂單都可查看 -->
                        <button @click="viewOrder(order.orderId)">
                            查看
                        </button>
                        <!-- 未付款、尚未出貨才可以修改 -->
                        <button v-if="
                            order.orderStatus !== 'CANCELLED' &&
                            order.paymentStatus === 'UNPAID' &&
                            order.shippingStatus === 'PENDING'
                        " @click="editOrder(order.orderId)">
                            編輯
                        </button>

                        <!-- 已取消、未付款、尚未出貨才可以刪除 -->
                        <button v-if="
                            order.orderStatus === 'CANCELLED' &&
                            order.paymentStatus === 'UNPAID' &&
                            order.shippingStatus === 'PENDING'
                        " @click="deleteOrder(order.orderId)">
                            刪除
                        </button>
                        <!-- 付款完成、尚未出貨才可以出貨 -->
                        <button v-if="
                            order.paymentStatus === 'PAID' &&
                            order.shippingStatus === 'PENDING' &&
                            order.orderStatus !== 'CANCELLED'
                        " @click="shipOrder(order.orderId)">
                            出貨
                        </button>
                        <button v-if="order.shippingStatus === 'SHIPPED'" @click="deliverOrder(order.orderId)">
                            確認送達
                        </button>
                        <button v-if="
                            order.orderStatus === 'PENDING' &&
                            order.paymentStatus === 'UNPAID' &&
                            order.shippingStatus === 'PENDING'
                        " @click="cancelOrder(order.orderId)">
                            取消訂單
                        </button>
                        <button v-if="
                            order.orderStatus === 'PENDING' &&
                            order.paymentStatus === 'UNPAID' &&
                            order.shippingStatus === 'PENDING'
                        " @click="demoPayOrder(order)">
                            模擬付款
                        </button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<style scoped></style>