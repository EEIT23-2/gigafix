<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import {
    getOrders,
    getOrdersByMember,
    getCreateOptions,
    generateDemoOrders as generateDemoOrdersApi,
    getOrderStatistics,
    exportOrders as exportOrdersApi,
    deleteOrder as deleteOrderApi,
    deliverOrder as deliverOrderApi,
    cancelOrder as cancelOrderApi
} from '../../api/adminOrderApi'
import OrderStatusBadge from '../../components/OrderStatusBadge.vue'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

//******訂單管理頁面******

// 使用 Vue Router 的 useRouter 來導航
const router = useRouter()
// 存放後端回傳的訂單資料
const orders = ref([])
// 全部訂單的後台統計
const orderStatistics = ref({
    totalOrders: 0,
    completedOrders: 0,
    pendingShipmentOrders: 0,
    totalRevenue: 0,
    pendingOrders: 0,
    cancelledOrders: 0
})
const isStatisticsLoading = ref(false)
// Excel 匯出狀態
const isExportingOrders = ref(false)
// Demo 訂單產生狀態
const isGeneratingDemoOrders = ref(false)
// 訂單狀態篩選
const selectedOrderStatus = ref('')
// 付款狀態篩選
const selectedPaymentStatus = ref('')
// 物流狀態篩選
const selectedShippingStatus = ref('')
// 關鍵字搜尋
const searchKeyword = ref('')
// 訂單列表排序，預設依建立時間由新到舊
const selectedSort = ref('createdAtDesc')
const toTimestamp = (value) => {
    const timestamp = Date.parse(value)
    return Number.isNaN(timestamp) ? 0 : timestamp
}
// 進入頁面時自動查詢訂單
const createOrder = () => {
    router.push('/admin/orders/create')
}
// 匯出全部後台訂單為 Excel
const exportOrders = async () => {
    let downloadUrl = null

    isExportingOrders.value = true

    try {
        const response = await exportOrdersApi()
        downloadUrl = URL.createObjectURL(response.data)

        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = 'orders.xlsx'
        document.body.appendChild(link)
        link.click()
        link.remove()
    } catch (error) {
        console.error('匯出訂單 Excel 失敗：', error)
        alert('匯出訂單 Excel 失敗')
    } finally {
        if (downloadUrl) {
            URL.revokeObjectURL(downloadUrl)
        }

        isExportingOrders.value = false
    }
}
// 一次產生 50 筆後台 Demo 訂單
const generateDemoOrders = async () => {
    const confirmed = confirm(
        '確定要產生 50 筆 Demo 訂單嗎？每次執行都會新增 50 筆資料。'
    )

    if (!confirmed) {
        return
    }

    isGeneratingDemoOrders.value = true

    try {
        const response = await generateDemoOrdersApi()
        const createdCount = response.data?.createdCount ?? 50

        alert(`成功產生 ${createdCount} 筆 Demo 訂單`)
        selectedMemberId.value = ''
        await loadOrders()
        await loadOrderStatistics()
    } catch (error) {
        console.error('產生 Demo 訂單失敗：', error)
        alert(error.response?.data?.message || '產生 Demo 訂單失敗')
    } finally {
        isGeneratingDemoOrders.value = false
    }
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
// 查詢資料庫全部訂單的統計資料
const loadOrderStatistics = async () => {
    isStatisticsLoading.value = true

    try {
        const response = await getOrderStatistics()
        orderStatistics.value = response.data
    } catch (error) {
        console.error('查詢訂單統計失敗：', error)
    } finally {
        isStatisticsLoading.value = false
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
    loadOrderStatistics()
})
// 訂單列表篩選
const filteredOrders = computed(() => {

    const keyword = searchKeyword.value.trim().toLowerCase()

    const matchedOrders = orders.value.filter(order => {

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

    return [...matchedOrders].sort((firstOrder, secondOrder) => {
        const firstCreatedAt = toTimestamp(firstOrder.createdAt)
        const secondCreatedAt = toTimestamp(secondOrder.createdAt)
        const firstAmount = Number(firstOrder.totalAmount ?? 0)
        const secondAmount = Number(secondOrder.totalAmount ?? 0)
        const firstOrderId = Number(firstOrder.orderId ?? 0)
        const secondOrderId = Number(secondOrder.orderId ?? 0)

        switch (selectedSort.value) {
            case 'createdAtAsc':
                return firstCreatedAt - secondCreatedAt
            case 'amountDesc':
                return secondAmount - firstAmount
            case 'amountAsc':
                return firstAmount - secondAmount
            case 'orderIdDesc':
                return secondOrderId - firstOrderId
            case 'createdAtDesc':
            default:
                return secondCreatedAt - firstCreatedAt
        }
    })
})
// 訂單狀態長條圖
const orderStatusChartOption = computed(() => ({
    animationDuration: 650,
    tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        valueFormatter: value => `${value} 筆`
    },
    grid: {
        left: 18,
        right: 64,
        top: 24,
        bottom: 12,
        containLabel: true
    },
    xAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
            color: '#718096',
            fontSize: 12
        },
        splitLine: {
            lineStyle: { color: '#e9eef4' }
        }
    },
    yAxis: {
        type: 'category',
        inverse: true,
        data: ['待處理', '已完成', '已取消'],
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
            color: '#34495e',
            fontSize: 13,
            fontWeight: 700
        }
    },
    series: [
        {
            name: '訂單數量',
            type: 'bar',
            barWidth: 34,
            data: [
                {
                    value: orderStatistics.value.pendingOrders,
                    itemStyle: { color: '#d9a441', borderRadius: [0, 8, 8, 0] }
                },
                {
                    value: orderStatistics.value.completedOrders,
                    itemStyle: { color: '#4d9f7c', borderRadius: [0, 8, 8, 0] }
                },
                {
                    value: orderStatistics.value.cancelledOrders,
                    itemStyle: { color: '#c96f6f', borderRadius: [0, 8, 8, 0] }
                }
            ],
            label: {
                show: true,
                position: 'right',
                color: '#253b50',
                fontWeight: 800,
                formatter: '{c} 筆'
            }
        }
    ]
}))
// 重設所有篩選條件
const resetFilters = () => {
    selectedMemberId.value = ''
    selectedOrderStatus.value = ''
    selectedPaymentStatus.value = ''
    selectedShippingStatus.value = ''
    searchKeyword.value = ''
    selectedSort.value = 'createdAtDesc'

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

                <div class="d-flex flex-wrap gap-2">
                    <button class="btn btn-outline-success" type="button" :disabled="isExportingOrders"
                        @click="exportOrders">
                        {{ isExportingOrders ? '匯出中...' : '匯出 Excel' }}
                    </button>

                    <button class="btn btn-outline-primary" type="button" :disabled="isGeneratingDemoOrders"
                        @click="generateDemoOrders">
                        {{ isGeneratingDemoOrders ? '產生中...' : '產生 50 筆 Demo 訂單' }}
                    </button>

                    <button class="btn btn-primary" type="button" @click="createOrder">
                        ＋ 新增訂單
                    </button>
                </div>
            </header>

            <!-- 訂單統計 -->
            <section class="mb-4 order-statistics" aria-label="訂單統計">
                <div class="revenue-summary-card">
                    <span>總營業額</span>
                    <strong>NT$ {{ formatPrice(orderStatistics.totalRevenue) }}</strong>
                    <small>已付款訂單累計</small>
                </div>

                <div class="order-summary-grid" aria-label="訂單摘要">
                    <div class="order-summary-card">
                        <span class="summary-dot summary-dot-total" aria-hidden="true"></span>
                        <span>總訂單數</span>
                        <strong>{{ orderStatistics.totalOrders }}</strong>
                    </div>

                    <div class="order-summary-card">
                        <span class="summary-dot summary-dot-completed" aria-hidden="true"></span>
                        <span>已完成訂單</span>
                        <strong>{{ orderStatistics.completedOrders }}</strong>
                    </div>

                    <div class="order-summary-card">
                        <span class="summary-dot summary-dot-shipping" aria-hidden="true"></span>
                        <span>待出貨訂單</span>
                        <strong>{{ orderStatistics.pendingShipmentOrders }}</strong>
                    </div>
                </div>

                <div class="chart-card">
                    <div class="chart-card-header">
                        <div>
                            <p>ORDER OVERVIEW</p>
                            <h2>訂單狀態統計</h2>
                        </div>
                        <span>全部訂單</span>
                    </div>
                    <div class="chart-card-body">
                        <v-chart class="order-status-chart" :option="orderStatusChartOption"
                            :loading="isStatisticsLoading" autoresize />
                    </div>
                </div>
            </section>

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
                        <!-- 排序 -->
                        <div class="col-12 col-md-6 col-lg-3">
                            <label class="form-label fw-semibold">
                                排序方式
                            </label>
                            <select v-model="selectedSort" class="form-select">
                                <option value="createdAtDesc">建立時間：新 → 舊</option>
                                <option value="createdAtAsc">建立時間：舊 → 新</option>
                                <option value="amountDesc">金額：高 → 低</option>
                                <option value="amountAsc">金額：低 → 高</option>
                                <option value="orderIdDesc">訂單編號：新 → 舊</option>
                            </select>
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
                                    <OrderStatusBadge type="order" :value="order.orderStatus" />
                                </td>
                                <!-- 付款狀態 -->
                                <td>
                                    <OrderStatusBadge type="payment" :value="order.paymentStatus" />
                                </td>
                                <!-- 收件人 -->
                                <td>
                                    {{ order.receiverName }}
                                </td>
                                <!-- 物流狀態 -->
                                <td>
                                    <OrderStatusBadge type="shipping" :value="order.shippingStatus" />
                                </td>
                                <!-- 操作 -->
                                <td class="text-end">
                                    <div class="d-flex justify-content-end flex-wrap gap-2 order-actions">

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
                                        " class="btn btn-sm btn-outline-primary" type="button"
                                            @click="shipOrder(order.orderId)">
                                            出貨
                                        </button>

                                        <!-- 已出貨才可以確認送達 -->
                                        <button v-if="order.shippingStatus === 'SHIPPED'" class="btn btn-sm btn-outline-success"
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

.order-statistics {
    color: #1d324b;
}

.revenue-summary-card {
    padding: 22px 24px;
    border-radius: 16px;
    color: #fff;
    background: linear-gradient(135deg, #1c5f99, #2b87c8);
    box-shadow: 0 12px 28px rgb(35 100 153 / 20%);
}

.revenue-summary-card span,
.revenue-summary-card small {
    display: block;
    opacity: 0.86;
}

.revenue-summary-card span {
    font-size: 0.875rem;
    font-weight: 700;
    letter-spacing: 0.03em;
}

.revenue-summary-card strong {
    display: inline-block;
    margin: 7px 10px 4px 0;
    font-size: clamp(2rem, 4vw, 2.6rem);
    line-height: 1.1;
}

.revenue-summary-card small {
    font-size: 0.8rem;
}

.order-summary-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    margin: 18px 0;
}

.order-summary-card {
    display: grid;
    grid-template-columns: auto 1fr;
    align-items: center;
    gap: 6px 9px;
    min-width: 0;
    padding: 16px 18px;
    border: 1px solid #e1e7ee;
    border-radius: 13px;
    background: #fff;
}

.summary-dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
}

.summary-dot-total {
    background: #2878c7;
}

.summary-dot-completed {
    background: #4d9f7c;
}

.summary-dot-shipping {
    background: #d9a441;
}

.order-summary-card > span:nth-child(2) {
    overflow: hidden;
    color: #647486;
    font-size: 0.8rem;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.order-summary-card strong {
    grid-column: 1 / -1;
    font-size: 1.65rem;
    line-height: 1.15;
}

.chart-card {
    overflow: hidden;
    border: 1px solid #e1e7ee;
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 6px 18px rgb(35 61 86 / 7%);
}

.chart-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    padding: 18px 22px;
    border-bottom: 1px solid #e7ecf2;
}

.chart-card-header p {
    margin: 0 0 3px;
    color: #2878c7;
    font-size: 0.68rem;
    font-weight: 800;
    letter-spacing: 0.13em;
}

.chart-card-header h2 {
    margin: 0;
    font-size: 1.15rem;
    font-weight: 800;
}

.chart-card-header > span {
    flex: 0 0 auto;
    padding: 5px 10px;
    border-radius: 999px;
    color: #60758a;
    background: #eef3f8;
    font-size: 0.75rem;
    font-weight: 700;
}

.chart-card-body {
    padding: 10px 18px 14px;
    background: #fbfcfe;
}

.order-status-chart {
    width: 100%;
    height: 300px;
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
    min-width: 250px;
}

.order-actions .btn {
    min-width: 78px;
    min-height: 36px;
    padding: 0.4rem 0.7rem;
    font-size: 0.875rem;
    font-weight: 600;
    border-radius: 0.5rem;
    white-space: nowrap;
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

@media (max-width: 767.98px) {
    .order-summary-grid {
        grid-template-columns: 1fr;
    }

    .order-summary-card {
        grid-template-columns: auto 1fr auto;
    }

    .order-summary-card strong {
        grid-column: auto;
        font-size: 1.35rem;
    }

    .chart-card-body {
        padding-right: 10px;
        padding-left: 10px;
    }
}
</style>
