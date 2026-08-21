<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
    getOrders,
    getOrdersByMember,
    getCreateOptions,
    deleteOrder as deleteOrderApi,
    deliverOrder as deliverOrderApi,
    cancelOrder as cancelOrderApi,
    payOrder
} from '../api'
import {
    orderStatusText,
    paymentStatusText,
    shippingStatusText,
    orderStatusClass,
    paymentStatusClass,
    shippingStatusClass
} from '../status'

//******訂單管理頁面******

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
        const response = await getOrders()

        orders.value = response.data

        console.log('訂單資料：', response.data)
    } catch (error) {
        console.error('查詢訂單失敗：', error)
    }
}
// 取得會員下拉選單
const loadMembers = async () => {
    try {
        const response = await getCreateOptions()

        members.value = response.data.members

    } catch (error) {
        console.error('取得會員選項失敗：', error)
    }
}
// 依目前會員篩選重新載入訂單
const searchByMember = async () => {

    // 全部會員
    if (!selectedMemberId.value) {
        await loadOrders()
        return
    }

    try {
        const response = await getOrdersByMember(
            selectedMemberId.value
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
        await deleteOrderApi(orderId)

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

        return (
            matchOrderStatus &&
            matchPaymentStatus &&
            matchShippingStatus &&
            matchKeyword
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

    loadOrders()
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

        await deliverOrderApi(orderId)

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

        await cancelOrderApi(orderId)

        alert('訂單取消成功')

        // 重新查詢訂單列表
        loadOrders()

    } catch (error) {

        console.error('取消訂單失敗：', error)

        alert('取消訂單失敗')
    }
}
// Demo：模擬會員付款成功
// 呼叫member-pay-api的payOrder，傳入交易編號
// 假裝交易成功，並更新訂單的付款狀態為PAID
// 交易編號使用時間戳記加上訂單編號，例如：DEMO-123456-1690000000000
// 正式環境中，交易編號應該由支付平台回傳，而不是自己生成
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

        await payOrder(
            order.memberId,
            order.orderId,
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
                            <select v-model="selectedMemberId" class="form-select" @change="searchByMember">
                                <option value="">全部會員</option>

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
                        <div class="col-12 col-lg-3">
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
                        <div class="col-12 col-lg-1">
                            <div class="d-flex gap-2">
                                <button class="btn btn-outline-secondary flex-fill" type="button" @click="resetFilters">
                                    重設
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
                                    <span class="badge" :class="orderStatusClass(order.orderStatus)">
                                        {{ orderStatusText(order.orderStatus) }}
                                    </span>
                                </td>
                                <!-- 付款狀態 -->
                                <td>
                                    <span class="badge" :class="paymentStatusClass(order.paymentStatus)">
                                        {{ paymentStatusText(order.paymentStatus) }}
                                    </span>
                                </td>
                                <!-- 收件人 -->
                                <td>
                                    {{ order.receiverName }}
                                </td>
                                <!-- 物流狀態 -->
                                <td>
                                    <span class="badge" :class="shippingStatusClass(order.shippingStatus)">
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
</style>