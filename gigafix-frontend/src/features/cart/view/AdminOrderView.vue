<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

// 使用 Vue Router 的 useRouter 來導航
const router = useRouter()
// 存放後端回傳的訂單資料
const orders = ref([])
// 訂單狀態篩選
const selectedOrderStatus = ref('')
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
// 依訂單狀態篩選列表
const filteredOrders = computed(() => {

    // 沒有選擇狀態時顯示全部
    if (!selectedOrderStatus.value) {
        return orders.value
    }

    // 顯示指定狀態的訂單
    return orders.value.filter(
        order => order.orderStatus === selectedOrderStatus.value
    )
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
    <main class="container-fluid px-3 px-lg-4 py-4 order-admin-page">
        <div class="mx-auto order-content-width">

            <!-- 頁面標題 -->
            <header class="d-flex flex-column flex-md-row align-items-md-end justify-content-between gap-3 mb-4">
                <div>
                    <div class="d-flex align-items-center gap-3">
                        <h1 class="fw-bold mb-0">訂單管理</h1>

                        <span class="badge rounded-pill text-bg-light border">
                            Total: {{ orders.length }}
                        </span>
                    </div>

                    <p class="text-secondary mb-0 mt-1">
                        Manage customer orders, payments and shipping status.
                    </p>
                </div>

                <button class="btn btn-primary" type="button" @click="createOrder">
                    ＋ 新增訂單
                </button>
            </header>

            <!-- 會員篩選 -->
            <section class="card shadow-sm border-0 mb-4">
                <div class="card-body">
                    <div class="row g-3 align-items-end">

                        <!-- 會員 -->
                        <div class="col-12 col-lg">
                            <label class="form-label fw-semibold">
                                會員
                            </label>

                            <select v-model="selectedMemberId" class="form-select">
                                <option value="">請選擇會員</option>

                                <option v-for="member in members" :key="member.memberId" :value="member.memberId">
                                    {{ member.memberName }}
                                    （ID：{{ member.memberId }}）
                                </option>
                            </select>
                        </div>

                        <!-- 訂單狀態 -->
                        <div class="col-12 col-md-4 col-lg-3">
                            <label class="form-label fw-semibold">
                                訂單狀態
                            </label>

                            <select v-model="selectedOrderStatus" class="form-select">
                                <option value="">所有訂單狀態</option>
                                <option value="PENDING">待處理</option>
                                <option value="COMPLETED">已完成</option>
                                <option value="CANCELLED">已取消</option>
                            </select>
                        </div>

                        <!-- 按鈕 -->
                        <div class="col-12 col-lg-auto">
                            <div class="d-flex gap-2">
                                <button class="btn btn-outline-primary" type="button" @click="searchByMember">
                                    查詢會員訂單
                                </button>

                                <button class="btn btn-outline-secondary" type="button" @click="showAllOrders">
                                    顯示全部
                                </button>
                            </div>
                        </div>

                    </div>
                </div>
            </section>

            <!-- 訂單表格 -->
            <section class="card shadow-sm border-0">
                <div class="card-header bg-white border-bottom py-3">
                    <h2 class="h5 fw-bold mb-0">
                        訂單列表
                    </h2>
                </div>

                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0 order-table">
                        <thead class="table-light">
                            <tr>
                                <th>訂單 ID</th>
                                <th>商品</th>
                                <th class="text-end">總金額</th>
                                <th>訂單狀態</th>
                                <th>付款狀態</th>
                                <th>收件人</th>
                                <th>物流狀態</th>
                                <th class="text-end">操作</th>
                            </tr>
                        </thead>

                        <tbody>
                            <tr v-for="order in filteredOrders" :key="order.orderId">
                                <!-- 訂單 ID -->
                                <td>
                                    <span class="fw-semibold">
                                        #{{ order.orderId }}
                                    </span>
                                </td>
                                <!-- 商品 -->
                                <td>
                                    <span v-if="order.orderItems && order.orderItems.length > 0" class="fw-semibold">
                                        {{ order.orderItems[0].productName }}
                                    </span>

                                    <span v-else class="text-secondary">
                                        -
                                    </span>
                                </td>
                                <!-- 金額 -->
                                <td class="text-end font-monospace fw-semibold">
                                    NT$ {{ formatPrice(order.totalAmount) }}
                                </td>

                                <!-- 訂單狀態 -->
                                <td>
                                    <span class="badge" :class="{
                                        'text-bg-warning':
                                            order.orderStatus === 'PENDING',

                                        'text-bg-danger':
                                            order.orderStatus === 'CANCELLED',

                                        'text-bg-success':
                                            order.orderStatus === 'COMPLETED'
                                    }">
                                        {{ orderStatusText(order.orderStatus) }}
                                    </span>
                                </td>

                                <!-- 付款狀態 -->
                                <td>
                                    <span class="badge" :class="{
                                        'text-bg-secondary':
                                            order.paymentStatus === 'UNPAID',

                                        'text-bg-success':
                                            order.paymentStatus === 'PAID',

                                        'text-bg-danger':
                                            order.paymentStatus === 'FAILED',

                                        'text-bg-info':
                                            order.paymentStatus === 'REFUNDED'
                                    }">
                                        {{ paymentStatusText(order.paymentStatus) }}
                                    </span>
                                </td>

                                <!-- 收件人 -->
                                <td>
                                    {{ order.receiverName }}
                                </td>

                                <!-- 物流狀態 -->
                                <td>
                                    <span class="badge" :class="{
                                        'text-bg-secondary':
                                            order.shippingStatus === 'PENDING',

                                        'text-bg-primary':
                                            order.shippingStatus === 'SHIPPED',

                                        'text-bg-success':
                                            order.shippingStatus === 'DELIVERED',

                                        'text-bg-danger':
                                            order.shippingStatus === 'CANCELLED'
                                    }">
                                        {{ shippingStatusText(order.shippingStatus) }}
                                    </span>
                                </td>

                                <!-- 操作 -->
                                <td class="text-end">
                                    <div class="d-flex justify-content-end flex-wrap gap-1 order-actions">

                                        <!-- 所有訂單都可查看 -->
                                        <button class="btn btn-sm btn-outline-secondary" type="button"
                                            @click="viewOrder(order.orderId)">
                                            查看
                                        </button>

                                        <!-- 未付款、尚未出貨才可以修改 -->
                                        <button v-if="
                                            order.orderStatus !== 'CANCELLED' &&
                                            order.paymentStatus === 'UNPAID' &&
                                            order.shippingStatus === 'PENDING'
                                        " class="btn btn-sm btn-outline-primary" type="button"
                                            @click="editOrder(order.orderId)">
                                            編輯
                                        </button>

                                        <!-- 已取消、未付款、尚未出貨才可以刪除 -->
                                        <button v-if="
                                            order.orderStatus === 'CANCELLED' &&
                                            order.paymentStatus === 'UNPAID' &&
                                            order.shippingStatus === 'PENDING'
                                        " class="btn btn-sm btn-outline-danger" type="button"
                                            @click="deleteOrder(order.orderId)">
                                            刪除
                                        </button>

                                        <!-- 付款完成、尚未出貨才可以出貨 -->
                                        <button v-if="
                                            order.paymentStatus === 'PAID' &&
                                            order.shippingStatus === 'PENDING' &&
                                            order.orderStatus !== 'CANCELLED'
                                        " class="btn btn-sm btn-primary" type="button"
                                            @click="shipOrder(order.orderId)">
                                            出貨
                                        </button>

                                        <!-- 已出貨才可以確認送達 -->
                                        <button v-if="order.shippingStatus === 'SHIPPED'" class="btn btn-sm btn-success"
                                            type="button" @click="deliverOrder(order.orderId)">
                                            確認送達
                                        </button>

                                        <!-- 待處理、未付款、未出貨才可以取消 -->
                                        <button v-if="
                                            order.orderStatus === 'PENDING' &&
                                            order.paymentStatus === 'UNPAID' &&
                                            order.shippingStatus === 'PENDING'
                                        " class="btn btn-sm btn-outline-danger" type="button"
                                            @click="cancelOrder(order.orderId)">
                                            取消訂單
                                        </button>

                                        <!-- Demo：模擬付款 -->
                                        <button v-if="
                                            order.orderStatus === 'PENDING' &&
                                            order.paymentStatus === 'UNPAID' &&
                                            order.shippingStatus === 'PENDING'
                                        " class="btn btn-sm btn-success" type="button" @click="demoPayOrder(order)">
                                            模擬付款
                                        </button>
                                    </div>
                                </td>
                            </tr>

                            <!-- 沒有訂單 -->
                            <tr v-if="filteredOrders.length === 0">
                                <td colspan="8" class="text-center text-secondary py-5">
                                    目前沒有訂單資料
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

        </div>
    </main>
</template>

<style scoped>
.order-admin-page {
    min-height: 100vh;
    background: #f8f9ff;
}

.order-content-width {
    max-width: 1600px;
}

.order-table th {
    white-space: nowrap;
    font-size: 0.8rem;
    letter-spacing: 0.03em;
}

.order-table td {
    font-size: 0.875rem;
}

.order-actions {
    min-width: 230px;
}

.card {
    border-radius: 0.75rem;
}
</style>
