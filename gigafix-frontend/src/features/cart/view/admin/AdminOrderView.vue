<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import OrderStatusBadge from '../../components/OrderStatusBadge.vue'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

use([
    CanvasRenderer,
    PieChart,
    TooltipComponent
])

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

//******訂單管理頁面******

// 使用 Vue Router 的 useRouter 來導航
const router = useRouter()
// 存放後端回傳的訂單資料
const orders = ref([])
const allOrdersForStatistics = ref([])
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
        allOrdersForStatistics.value = response.data

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

        // 刪除後重新查詢訂單與統計
        await Promise.all([loadOrders(), loadOrderStatistics()])
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
const pageSize = 8
const currentPage = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filteredOrders.value.length / pageSize)))
const paginatedOrders = computed(() => {
    const startIndex = (currentPage.value - 1) * pageSize

    return filteredOrders.value.slice(startIndex, startIndex + pageSize)
})
const firstDisplayedOrder = computed(() => {
    if (filteredOrders.value.length === 0) {
        return 0
    }

    return (currentPage.value - 1) * pageSize + 1
})
const lastDisplayedOrder = computed(() => Math.min(currentPage.value * pageSize, filteredOrders.value.length))
const visiblePageNumbers = computed(() => {
    const total = totalPages.value
    const current = currentPage.value

    if (total <= 5) {
        return Array.from({ length: total }, (_, index) => index + 1)
    }

    if (current <= 3) {
        return [1, 2, 3, 'end-ellipsis', total]
    }

    if (current >= total - 2) {
        return [1, 'start-ellipsis', total - 2, total - 1, total]
    }

    return [1, 'start-ellipsis', current, 'end-ellipsis', total]
})

const goToPage = (page) => {
    currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}

watch(
    [selectedMemberId, selectedOrderStatus, selectedPaymentStatus, selectedShippingStatus, searchKeyword, selectedSort],
    () => {
        currentPage.value = 1
    }
)

watch(
    () => filteredOrders.value.length,
    () => {
        if (currentPage.value > totalPages.value) {
            currentPage.value = totalPages.value
        }
    }
)

const toPercentage = (value, total) => {
    if (!total) {
        return 0
    }

    return Math.round((value / total) * 100)
}

const orderStatusStatistics = computed(() => {
    const total = Number(orderStatistics.value.totalOrders ?? 0)

    return {
        total,
        completed: Number(orderStatistics.value.completedOrders ?? 0),
        pending: Number(orderStatistics.value.pendingOrders ?? 0),
        cancelled: Number(orderStatistics.value.cancelledOrders ?? 0)
    }
})

const orderStatusChartOption = computed(() => ({
    animationDuration: 650,

    tooltip: {
        trigger: 'item',
        formatter: ({ name, value, percent }) =>
            `${name}<br/><strong>${value} 筆（${percent}%）</strong>`
    },

    series: [
        {
            name: '訂單狀態',
            type: 'pie',

            // 內圈 / 外圈大小
            radius: ['68%', '88%'],

            center: ['50%', '50%'],

            avoidLabelOverlap: true,

            itemStyle: {
                borderColor: '#fff',
                borderWidth: 3,
                borderRadius: 5
            },

            // 因為右邊已經有圖例，所以圓環本身不顯示文字
            label: {
                show: false
            },

            labelLine: {
                show: false
            },

            data: [
                {
                    value: orderStatusStatistics.value.completed,
                    name: '已完成',
                    itemStyle: {
                        color: '#1f9569'
                    }
                },
                {
                    value: orderStatusStatistics.value.pending,
                    name: '待處理',
                    itemStyle: {
                        color: '#dfa00c'
                    }
                },
                {
                    value: orderStatusStatistics.value.cancelled,
                    name: '已取消',
                    itemStyle: {
                        color: '#9ca7b0'
                    }
                }
            ]
        }
    ]
}))

const paymentStatistics = computed(() => {
    const result = {
        total: allOrdersForStatistics.value.length,
        paid: 0,
        unpaid: 0,
        other: 0
    }

    allOrdersForStatistics.value.forEach(order => {
        if (order.paymentStatus === 'PAID') {
            result.paid += 1
        } else if (order.paymentStatus === 'UNPAID') {
            result.unpaid += 1
        } else {
            result.other += 1
        }
    })

    return result
})

const shippingStatistics = computed(() => {
    const result = {
        total: allOrdersForStatistics.value.length,
        pending: 0,
        shipped: 0,
        delivered: 0,
        cancelled: 0
    }

    allOrdersForStatistics.value.forEach(order => {
        if (order.shippingStatus === 'PENDING') {
            result.pending += 1
        } else if (order.shippingStatus === 'SHIPPED') {
            result.shipped += 1
        } else if (order.shippingStatus === 'DELIVERED') {
            result.delivered += 1
        } else if (order.shippingStatus === 'CANCELLED') {
            result.cancelled += 1
        }
    })

    return result
})

const filterPendingShipmentOrders = () => {
    selectedMemberId.value = ''
    selectedOrderStatus.value = ''
    selectedPaymentStatus.value = 'PAID'
    selectedShippingStatus.value = 'PENDING'
    searchKeyword.value = ''
    orders.value = [...allOrdersForStatistics.value]
    currentPage.value = 1
}
// 重設所有篩選條件
const resetFilters = () => {
    selectedMemberId.value = ''
    selectedOrderStatus.value = ''
    selectedPaymentStatus.value = ''
    selectedShippingStatus.value = ''
    searchKeyword.value = ''
    selectedSort.value = 'createdAtDesc'
    currentPage.value = 1

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

        // 重新查詢列表與統計
        await Promise.all([loadOrders(), loadOrderStatistics()])

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

        // 重新查詢訂單列表與統計
        await Promise.all([loadOrders(), loadOrderStatistics()])

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
            <header class="order-page-header">
                <div>
                    <p class="order-breadcrumb">後台 / 訂單管理</p>
                    <h1>訂單管理</h1>
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
                <div class="order-kpi-grid">
                    <article class="order-kpi-card pending-shipment-card">
                        <div class="order-kpi-heading">
                            <span class="order-kpi-label">
                                <span class="order-kpi-icon order-kpi-icon-warning" aria-hidden="true">
                                    <i class="bi bi-truck"></i>
                                </span>
                                待出貨訂單
                            </span>
                            <span class="attention-badge">需處理</span>
                        </div>

                        <div class="order-kpi-content">
                            <div>
                                <strong>{{ orderStatistics.pendingShipmentOrders }}</strong>
                                <span>筆</span>
                                <small>已付款、尚未出貨</small>
                            </div>
                            <button type="button" class="shipment-filter-button" @click="filterPendingShipmentOrders">
                                篩選待出貨 →
                            </button>
                        </div>
                    </article>

                    <article class="order-kpi-card revenue-card">
                        <span class="order-kpi-label">
                            <span class="order-kpi-icon order-kpi-icon-blue" aria-hidden="true">
                                <i class="bi bi-graph-up-arrow"></i>
                            </span>
                            總營業額
                        </span>
                        <div>
                            <strong><span>NT$</span> {{ formatPrice(orderStatistics.totalRevenue) }}</strong>
                            <small>已付款訂單累計・{{ paymentStatistics.paid }} 筆</small>
                        </div>
                    </article>

                    <article class="order-kpi-card total-orders-card">
                        <span class="order-kpi-label">
                            <span class="order-kpi-icon order-kpi-icon-blue" aria-hidden="true">
                                <i class="bi bi-clipboard2-data"></i>
                            </span>
                            總訂單數
                        </span>
                        <div>
                            <strong>{{ orderStatistics.totalOrders }} <span>筆</span></strong>
                            <small>本期全部訂單</small>
                        </div>
                    </article>
                </div>

                <article class="order-overview-card" :aria-busy="isStatisticsLoading">
                    <div class="order-status-overview">
                        <div class="order-status-donut" role="img"
                            :aria-label="`共 ${orderStatusStatistics.total} 筆訂單，已完成 ${orderStatusStatistics.completed} 筆、待處理 ${orderStatusStatistics.pending} 筆、已取消 ${orderStatusStatistics.cancelled} 筆`">
                            <v-chart class="order-status-chart" :option="orderStatusChartOption" autoresize />

                            <div class="order-status-donut-center">
                                <strong>{{ orderStatusStatistics.total }}</strong>
                                <span>筆訂單</span>
                            </div>
                        </div>

                        <div class="order-status-legend">
                            <h2>訂單狀態</h2>
                            <div class="order-status-row">
                                <span class="status-square status-square-completed" aria-hidden="true"></span>
                                <span>已完成</span>
                                <strong>{{ orderStatusStatistics.completed }}</strong>
                                <small>{{ toPercentage(orderStatusStatistics.completed, orderStatusStatistics.total)
                                    }}%</small>
                            </div>
                            <div class="order-status-row">
                                <span class="status-square status-square-pending" aria-hidden="true"></span>
                                <span>待處理</span>
                                <strong>{{ orderStatusStatistics.pending }}</strong>
                                <small>{{ toPercentage(orderStatusStatistics.pending, orderStatusStatistics.total)
                                    }}%</small>
                            </div>
                            <div class="order-status-row">
                                <span class="status-square status-square-cancelled" aria-hidden="true"></span>
                                <span>已取消</span>
                                <strong>{{ orderStatusStatistics.cancelled }}</strong>
                                <small>{{ toPercentage(orderStatusStatistics.cancelled, orderStatusStatistics.total)
                                    }}%</small>
                            </div>
                        </div>
                    </div>

                    <div class="order-flow-overview">
                        <section>
                            <header>
                                <h3>付款狀態</h3>
                                <span>
                                    已付款 {{ paymentStatistics.paid }}・未付款 {{ paymentStatistics.unpaid }}
                                    <template v-if="paymentStatistics.other">・其他 {{ paymentStatistics.other
                                        }}</template>
                                </span>
                            </header>
                            <div class="statistics-track" role="img"
                                :aria-label="`已付款 ${paymentStatistics.paid} 筆，未付款 ${paymentStatistics.unpaid} 筆，其他 ${paymentStatistics.other} 筆`">
                                <span class="statistics-segment payment-paid"
                                    :style="{ width: `${toPercentage(paymentStatistics.paid, paymentStatistics.total)}%` }"></span>
                                <span class="statistics-segment payment-unpaid"
                                    :style="{ width: `${toPercentage(paymentStatistics.unpaid, paymentStatistics.total)}%` }"></span>
                                <span v-if="paymentStatistics.other" class="statistics-segment statistics-other"
                                    :style="{ width: `${toPercentage(paymentStatistics.other, paymentStatistics.total)}%` }"></span>
                            </div>
                        </section>

                        <section>
                            <header>
                                <h3>物流狀態</h3>
                                <span>
                                    待出貨 {{ shippingStatistics.pending }}・已出貨 {{ shippingStatistics.shipped }}・已送達
                                    {{ shippingStatistics.delivered }}・已取消 {{ shippingStatistics.cancelled }}
                                </span>
                            </header>
                            <div class="statistics-track" role="img"
                                :aria-label="`待出貨 ${shippingStatistics.pending} 筆、已出貨 ${shippingStatistics.shipped} 筆、已送達 ${shippingStatistics.delivered} 筆、已取消 ${shippingStatistics.cancelled} 筆`">
                                <span class="statistics-segment shipping-pending"
                                    :style="{ width: `${toPercentage(shippingStatistics.pending, shippingStatistics.total)}%` }"></span>
                                <span class="statistics-segment shipping-shipped"
                                    :style="{ width: `${toPercentage(shippingStatistics.shipped, shippingStatistics.total)}%` }"></span>
                                <span class="statistics-segment shipping-delivered"
                                    :style="{ width: `${toPercentage(shippingStatistics.delivered, shippingStatistics.total)}%` }"></span>
                                <span class="statistics-segment shipping-cancelled"
                                    :style="{ width: `${toPercentage(shippingStatistics.cancelled, shippingStatistics.total)}%` }"></span>
                            </div>
                        </section>
                    </div>
                </article>
            </section>

            <!-- 訂單篩選 -->
            <section class="card shadow-sm border-0 mb-4 filter-card">
                <div class="filter-grid">
                    <label class="filter-field">
                        <span>會員</span>
                        <select v-model="selectedMemberId" class="form-select" @change="searchByMember">
                            <option value="">全部會員</option>
                            <option v-for="member in members" :key="member.memberId" :value="member.memberId">
                                {{ member.memberName }}（ID：{{ member.memberId }}）
                            </option>
                        </select>
                    </label>

                    <label class="filter-field">
                        <span>訂單狀態</span>
                        <select v-model="selectedOrderStatus" class="form-select">
                            <option value="">所有訂單狀態</option>
                            <option value="PENDING">待處理</option>
                            <option value="COMPLETED">已完成</option>
                            <option value="CANCELLED">已取消</option>
                        </select>
                    </label>

                    <label class="filter-field">
                        <span>付款狀態</span>
                        <select v-model="selectedPaymentStatus" class="form-select">
                            <option value="">所有付款狀態</option>
                            <option value="UNPAID">未付款</option>
                            <option value="PAID">已付款</option>
                            <option value="FAILED">付款失敗</option>
                            <option value="REFUNDED">已退款</option>
                        </select>
                    </label>

                    <label class="filter-field">
                        <span>物流狀態</span>
                        <select v-model="selectedShippingStatus" class="form-select">
                            <option value="">所有物流狀態</option>
                            <option value="PENDING">待出貨</option>
                            <option value="SHIPPED">已出貨</option>
                            <option value="DELIVERED">已送達</option>
                            <option value="CANCELLED">已取消</option>
                        </select>
                    </label>

                    <label class="filter-field filter-search-field">
                        <span>搜尋</span>
                        <input v-model="searchKeyword" type="search" class="form-control"
                            placeholder="搜尋訂單 ID、商品名稱、收件人">
                    </label>

                    <label class="filter-field">
                        <span>排序</span>
                        <select v-model="selectedSort" class="form-select">
                            <option value="createdAtDesc">建立時間：新 → 舊</option>
                            <option value="createdAtAsc">建立時間：舊 → 新</option>
                            <option value="amountDesc">金額：高 → 低</option>
                            <option value="amountAsc">金額：低 → 高</option>
                            <option value="orderIdDesc">訂單編號：新 → 舊</option>
                        </select>
                    </label>

                    <button class="btn btn-outline-secondary filter-reset-button" type="button" @click="resetFilters">
                        重設
                    </button>
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
                            <tr v-for="order in paginatedOrders" :key="order.orderId">
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
                                        <button v-if="order.shippingStatus === 'SHIPPED'"
                                            class="btn btn-sm btn-outline-success" type="button"
                                            @click="deliverOrder(order.orderId)">
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
                <footer v-if="filteredOrders.length > 0" class="order-pagination-footer">
                    <span>
                        顯示第 {{ firstDisplayedOrder }}–{{ lastDisplayedOrder }} 筆，共 {{ filteredOrders.length }} 筆
                    </span>

                    <nav class="order-pagination" aria-label="訂單分頁">
                        <button type="button" :disabled="currentPage === 1" aria-label="上一頁"
                            @click="goToPage(currentPage - 1)">
                            ‹
                        </button>

                        <template v-for="page in visiblePageNumbers" :key="page">
                            <span v-if="typeof page === 'string'" aria-hidden="true">…</span>
                            <button v-else type="button" :class="{ active: currentPage === page }"
                                :aria-current="currentPage === page ? 'page' : undefined" @click="goToPage(page)">
                                {{ page }}
                            </button>
                        </template>

                        <button type="button" :disabled="currentPage === totalPages" aria-label="下一頁"
                            @click="goToPage(currentPage + 1)">
                            ›
                        </button>
                    </nav>
                </footer>
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

.order-summary-card>span:nth-child(2) {
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

.chart-card-header>span {
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

.order-admin-page {
    background: #f4f5f2;
}

.order-page-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.5rem;
}

.order-page-header h1 {
    margin: 0.25rem 0 0;
    color: #1e252d;
    font-size: clamp(1.75rem, 3vw, 2.25rem);
    font-weight: 700;
}

.order-breadcrumb {
    margin: 0;
    color: #69717c;
    font-size: 0.85rem;
}

.order-kpi-grid {
    display: grid;
    grid-template-columns: 1.15fr 1fr 0.85fr;
    gap: 0.875rem;
}

.order-kpi-card {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    min-width: 0;
    min-height: 160px;
    padding: 1.25rem;
    border: 1px solid #e0e4df;
    border-radius: 1rem;
    color: #1e252d;
    background: #fff;
}

.order-kpi-heading,
.order-kpi-content,
.order-kpi-label {
    display: flex;
    align-items: center;
}

.order-kpi-heading,
.order-kpi-content {
    justify-content: space-between;
    gap: 0.75rem;
}

.order-kpi-content {
    align-items: flex-end;
}

.order-kpi-label {
    gap: 0.6rem;
    color: #69717c;
    font-size: 0.9rem;
    font-weight: 600;
}

.order-kpi-icon {
    display: inline-grid;
    width: 2.35rem;
    height: 2.35rem;
    place-items: center;
    border-radius: 0.7rem;
    font-size: 1.05rem;
}

.order-kpi-icon-warning {
    color: #b87900;
    background: rgb(223 160 12 / 13%);
}

.order-kpi-icon-blue {
    color: #3978d4;
    background: rgb(57 120 212 / 11%);
}

.pending-shipment-card {
    border-color: #f0cf86;
    background: #fff8e8;
}

.attention-badge {
    padding: 0.3rem 0.55rem;
    border-radius: 999px;
    color: #b87900;
    background: rgb(223 160 12 / 13%);
    font-size: 0.75rem;
    font-weight: 700;
}

.order-kpi-content strong,
.order-kpi-card>div>strong {
    font-size: clamp(2rem, 3.4vw, 2.55rem);
    line-height: 1;
}

.order-kpi-content strong+span,
.total-orders-card strong span,
.revenue-card strong span {
    color: #69717c;
    font-size: 0.85rem;
    font-weight: 600;
}

.order-kpi-card small {
    display: block;
    margin-top: 0.55rem;
    color: #69717c;
    font-size: 0.8rem;
}

.shipment-filter-button {
    padding: 0.5rem 0;
    border: 0;
    color: #9b6800;
    background: transparent;
    font-weight: 700;
    white-space: nowrap;
}

.revenue-card {
    border-color: #cfe0f7;
    background: #edf5ff;
}

.revenue-card .order-kpi-label {
    color: #4f6f97;
}

.revenue-card>div,
.total-orders-card>div {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}

.order-overview-card {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 1.5rem;
    align-items: center;
    margin-top: 0.875rem;
    padding: 1.25rem;
    border: 1px solid #e0e4df;
    border-radius: 1rem;
    background: #fff;
}

.order-status-overview {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 32px;
    padding-left: 28px;
}

.order-status-donut {
    position: relative;
    width: 150px;
    height: 150px;
    flex: 0 0 150px;
}

.order-status-chart {
    width: 100%;
    height: 100%;
}

.order-status-donut-center {
    position: absolute;
    top: 50%;
    left: 50%;
    z-index: 2;

    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    transform: translate(-50%, -50%);
    pointer-events: none;
}

.order-status-donut-center strong {
    color: #1d324b;
    font-size: 28px;
    font-weight: 800;
    line-height: 1;
}

.order-status-donut-center span {
    margin-top: 5px;
    color: #7b8a99;
    font-size: 13px;
    font-weight: 600;
}

.order-status-legend {
    flex: 0 0 auto;
    min-width: 200px;
}

.order-status-legend h2 {
    margin-bottom: 14px;
    font-size: 24px;
    font-weight: 800;
    color: #0f2747;
}

.order-status-row {
    display: grid;
    grid-template-columns: 12px 72px 40px 40px;
    align-items: center;
    column-gap: 10px;
    margin-bottom: 10px;
}

.order-status-row span {
    color: #1d324b;
    font-size: 15px;
}

.order-status-row strong {
    color: #0f2747;
    font-size: 18px;
    font-weight: 800;
    text-align: right;
}

.order-status-row small {
    color: #6f7f90;
    font-size: 14px;
    text-align: right;
}

.status-square {
    width: 0.65rem;
    height: 0.65rem;
    border-radius: 0.2rem;
}

.status-square-completed {
    background: #1f9569;
}

.status-square-pending {
    background: #dfa00c;
}

.status-square-cancelled {
    background: #9ca7b0;
}

.order-flow-overview {
    display: grid;
    gap: 1.2rem;
    padding-left: 1.5rem;
    border-left: 1px solid #e0e4df;
}

.order-flow-overview header {
    display: flex;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 0.55rem;
}

.order-flow-overview h3 {
    margin: 0;
    color: #4d5965;
    font-size: 0.9rem;
    font-weight: 700;
}

.order-flow-overview header span {
    color: #69717c;
    font-size: 0.78rem;
    text-align: right;
}

.statistics-track {
    display: flex;
    height: 0.55rem;
    overflow: hidden;
    border-radius: 999px;
    background: #e8ece9;
}

.statistics-segment {
    display: block;
    height: 100%;
}

.payment-paid,
.shipping-shipped {
    background: #3978d4;
}

.payment-unpaid {
    background: #df868d;
}

.statistics-other,
.shipping-cancelled {
    background: #9ca7b0;
}

.shipping-pending {
    background: #dfa00c;
}

.shipping-delivered {
    background: #1f9569;
}

.filter-card {
    padding: 1rem;
    border: 1px solid #e0e4df !important;
    background: #fff;
}

.filter-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(130px, 1fr)) minmax(220px, 1.5fr) minmax(175px, 1.2fr) auto;
    gap: 0.75rem;
    align-items: end;
}

.filter-field {
    display: grid;
    min-width: 0;
    gap: 0.4rem;
}

.filter-field>span {
    color: #69717c;
    font-size: 0.78rem;
    font-weight: 700;
}

.filter-card .form-select,
.filter-card .form-control,
.filter-reset-button {
    min-height: 42px;
}

.filter-reset-button {
    min-width: 4.5rem;
}

.order-pagination-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    min-height: 58px;
    padding: 0.75rem 1rem;
    border-top: 1px solid #e0e4df;
    color: #69717c;
    font-size: 0.82rem;
}

.order-pagination {
    display: flex;
    align-items: center;
    gap: 0.35rem;
}

.order-pagination button {
    min-width: 2rem;
    height: 2rem;
    padding: 0 0.45rem;
    border: 1px solid #e0e4df;
    border-radius: 0.45rem;
    color: #69717c;
    background: #fff;
}

.order-pagination button.active {
    border-color: #1d2b3a;
    color: #fff;
    background: #1d2b3a;
}

.order-pagination button:disabled {
    cursor: not-allowed;
    opacity: 0.45;
}

@media (max-width: 1399.98px) {
    .filter-grid {
        grid-template-columns: repeat(4, minmax(140px, 1fr));
    }

    .filter-search-field {
        grid-column: span 2;
    }
}

@media (max-width: 991.98px) {
    .order-page-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .order-kpi-grid {
        grid-template-columns: 1fr 1fr;
    }

    .pending-shipment-card {
        grid-column: 1 / -1;
    }

    .order-overview-card {
        grid-template-columns: 1fr;
    }

    .order-flow-overview {
        padding-top: 1.25rem;
        padding-left: 0;
        border-top: 1px solid #e0e4df;
        border-left: 0;
    }

    .filter-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .filter-search-field {
        grid-column: auto;
    }
}

@media (max-width: 575.98px) {

    .order-kpi-grid,
    .filter-grid {
        grid-template-columns: 1fr;
    }

    .pending-shipment-card {
        grid-column: auto;
    }

    .order-status-overview {
        grid-template-columns: 1fr;
        justify-items: center;
    }

    .order-status-legend {
        flex: 0 0 auto;
        min-width: 220px;
    }

    .order-flow-overview header,
    .order-pagination-footer {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
