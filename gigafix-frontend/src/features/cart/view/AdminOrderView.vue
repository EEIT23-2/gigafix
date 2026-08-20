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
// 付款狀態篩選
const selectedPaymentStatus = ref('')

// 物流狀態篩選
const selectedShippingStatus = ref('')

// 關鍵字搜尋
const searchKeyword = ref('')

// 快速篩選
const quickUnpaid = ref(false)
const quickPendingShipping = ref(false)
const quickShipped = ref(false)
const quickCancelled = ref(false)
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
// 訂單列表篩選
const filteredOrders = computed(() => {

    const keyword = searchKeyword.value.trim().toLowerCase()

    return orders.value.filter(order => {

        // 訂單狀態
        const matchOrderStatus =
            !selectedOrderStatus.value ||
            order.orderStatus === selectedOrderStatus.value

        // 付款狀態
        const matchPaymentStatus =
            !selectedPaymentStatus.value ||
            order.paymentStatus === selectedPaymentStatus.value

        // 物流狀態
        const matchShippingStatus =
            !selectedShippingStatus.value ||
            order.shippingStatus === selectedShippingStatus.value

        // 關鍵字：訂單 ID、商品名稱、收件人
        const productName =
            order.orderItems?.[0]?.productName || ''

        const matchKeyword =
            !keyword ||
            String(order.orderId).includes(keyword) ||
            productName.toLowerCase().includes(keyword) ||
            (order.receiverName || '').toLowerCase().includes(keyword)

        // 快速篩選
        const quickFilters = []

        if (quickUnpaid.value) {
            quickFilters.push(
                order.paymentStatus === 'UNPAID'
            )
        }

        if (quickPendingShipping.value) {
            quickFilters.push(
                order.paymentStatus === 'PAID' &&
                order.shippingStatus === 'PENDING'
            )
        }

        if (quickShipped.value) {
            quickFilters.push(
                order.shippingStatus === 'SHIPPED'
            )
        }

        if (quickCancelled.value) {
            quickFilters.push(
                order.orderStatus === 'CANCELLED'
            )
        }

        // 沒勾快速篩選 = 不限制
        // 有勾 = 符合其中一項即可
        const matchQuickFilter =
            quickFilters.length === 0 ||
            quickFilters.some(Boolean)

        return (
            matchOrderStatus &&
            matchPaymentStatus &&
            matchShippingStatus &&
            matchKeyword &&
            matchQuickFilter
        )
    })
})
// 重設所有篩選條件
const resetFilters = () => {
    selectedMemberId.value = ''
    selectedOrderStatus.value = ''
    selectedPaymentStatus.value = ''
    selectedShippingStatus.value = ''
    searchKeyword.value = ''

    quickUnpaid.value = false
    quickPendingShipping.value = false
    quickShipped.value = false
    quickCancelled.value = false

    loadOrders()
}
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

            <!-- 訂單篩選 -->
            <section class="card shadow-sm border-0 mb-4 filter-card">
                <div class="card-body p-4">

                    <!-- 第一排 -->
                    <div class="row g-3">

                        <!-- 會員 -->
                        <div class="col-12 col-md-6 col-xl-3">
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
                        <div class="col-12 col-md-6 col-xl-3">
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

                        <!-- 付款狀態 -->
                        <div class="col-12 col-md-6 col-xl-3">
                            <label class="form-label fw-semibold">
                                付款狀態
                            </label>

                            <select v-model="selectedPaymentStatus" class="form-select">
                                <option value="">所有付款狀態</option>
                                <option value="UNPAID">未付款</option>
                                <option value="PAID">已付款</option>
                                <option value="FAILED">付款失敗</option>
                                <option value="REFUNDED">已退款</option>
                            </select>
                        </div>

                        <!-- 物流狀態 -->
                        <div class="col-12 col-md-6 col-xl-3">
                            <label class="form-label fw-semibold">
                                物流狀態
                            </label>

                            <select v-model="selectedShippingStatus" class="form-select">
                                <option value="">所有物流狀態</option>
                                <option value="PENDING">待出貨</option>
                                <option value="SHIPPED">已出貨</option>
                                <option value="DELIVERED">已送達</option>
                                <option value="CANCELLED">已取消</option>
                            </select>
                        </div>

                    </div>


                    <!-- 第二排 -->
                    <div class="row g-3 mt-1 align-items-end">

                        <!-- 關鍵字 -->
                        <div class="col-12 col-lg-8">
                            <label class="form-label fw-semibold">
                                關鍵字搜尋
                            </label>

                            <div class="input-group">
                                <span class="input-group-text bg-white">
                                    🔍
                                </span>

                                <input v-model="searchKeyword" type="text" class="form-control"
                                    placeholder="搜尋訂單 ID、商品名稱、收件人">
                            </div>
                        </div>

                        <!-- 按鈕 -->
                        <div class="col-12 col-lg-4">
                            <div class="d-flex gap-2">

                                <button class="btn btn-outline-secondary flex-fill" type="button" @click="resetFilters">
                                    重設條件
                                </button>

                                <button class="btn btn-primary flex-fill" type="button"
                                    @click="selectedMemberId ? searchByMember() : loadOrders()">
                                    查詢
                                </button>

                            </div>
                        </div>

                    </div>


                    <!-- 快速篩選 -->
                    <div class="quick-filter-area mt-4 pt-3 border-top">

                        <div class="fw-semibold mb-3">
                            快速查看
                        </div>

                        <div class="d-flex flex-wrap gap-4">

                            <!-- 未付款 -->
                            <div class="form-check">
                                <input id="quickUnpaid" v-model="quickUnpaid" class="form-check-input" type="checkbox">

                                <label class="form-check-label" for="quickUnpaid">
                                    未付款
                                </label>
                            </div>

                            <!-- 待出貨 -->
                            <div class="form-check">
                                <input id="quickPendingShipping" v-model="quickPendingShipping" class="form-check-input"
                                    type="checkbox">

                                <label class="form-check-label" for="quickPendingShipping">
                                    待出貨
                                </label>
                            </div>

                            <!-- 已出貨 -->
                            <div class="form-check">
                                <input id="quickShipped" v-model="quickShipped" class="form-check-input"
                                    type="checkbox">

                                <label class="form-check-label" for="quickShipped">
                                    已出貨待確認
                                </label>
                            </div>

                            <!-- 已取消 -->
                            <div class="form-check">
                                <input id="quickCancelled" v-model="quickCancelled" class="form-check-input"
                                    type="checkbox">

                                <label class="form-check-label" for="quickCancelled">
                                    已取消
                                </label>
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


.filter-card {
    border-radius: 0.85rem;
}

.filter-card .form-select,
.filter-card .form-control,
.filter-card .input-group-text {
    min-height: 42px;
}

.quick-filter-area {
    font-size: 0.9rem;
}

.quick-filter-area .form-check {
    margin-bottom: 0;
}

.quick-filter-area .form-check-input {
    cursor: pointer;
}

.quick-filter-area .form-check-label {
    cursor: pointer;
    user-select: none;
}
</style>