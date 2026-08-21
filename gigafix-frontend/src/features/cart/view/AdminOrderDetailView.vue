<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrder } from '../api'
import {
    orderStatusText,
    paymentStatusText,
    shippingStatusText,
    paymentMethodText,
    shippingMethodText,
    orderStatusClass,
    paymentStatusClass,
    shippingStatusClass
} from '../status'

//******訂單詳情頁面******

const props = defineProps({
    orderId: String
})

const router = useRouter()

// 存放單筆訂單資料
const order = ref(null)

// 查詢指定訂單
const loadOrder = async () => {
    try {
        const response = await getOrder(props.orderId)

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
    <main class="container-fluid px-3 px-lg-4 py-4 order-admin-page">
        <div class="mx-auto order-detail-width">

            <!-- 頁面標題 -->
            <header class="d-flex flex-column flex-md-row align-items-md-end justify-content-between gap-3 mb-4">
                <div>
                    <h1 class="fw-bold mb-1">訂單詳情</h1>

                    <p class="text-secondary mb-0">
                        View complete order, payment and shipping information.
                    </p>
                </div>

                <button class="btn btn-outline-secondary" type="button" @click="goBack">
                    返回訂單列表
                </button>
            </header>

            <!-- 訂單資料 -->
            <div v-if="order">

                <!-- 訂單摘要 -->
                <section class="card shadow-sm border-0 mb-4">
                    <div class="card-body">
                        <div class="d-flex flex-column flex-md-row justify-content-between gap-4">
                            <div>
                                <div class="text-secondary small mb-1">
                                    訂單編號
                                </div>

                                <div class="fs-4 fw-bold">
                                    #{{ order.orderId }}
                                </div>
                            </div>

                            <div>
                                <div class="text-secondary small mb-1">
                                    會員 ID
                                </div>

                                <div class="fw-semibold">
                                    {{ order.memberId }}
                                </div>
                            </div>

                            <div>
                                <div class="text-secondary small mb-1">
                                    訂單金額
                                </div>

                                <div class="fs-4 fw-bold text-primary">
                                    NT$ {{ formatPrice(order.totalAmount) }}
                                </div>
                            </div>

                            <div>
                                <div class="text-secondary small mb-1">
                                    建立時間
                                </div>

                                <div class="fw-semibold">
                                    {{
                                        order.createdAt
                                            ? formatDateTime(order.createdAt)
                                            : '尚無'
                                    }}
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
                <!-- 商品資訊 -->
                <section class="card shadow-sm border-0 mb-4">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">
                            商品資訊
                        </h2>
                    </div>

                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>商品 ID</th>
                                    <th>商品名稱</th>
                                    <th class="text-end">成交單價</th>
                                </tr>
                            </thead>

                            <tbody>
                                <tr v-for="item in order.orderItems" :key="item.productId">
                                    <td>
                                        #{{ item.productId }}
                                    </td>

                                    <td class="fw-semibold">
                                        {{ item.productName }}
                                    </td>

                                    <td class="text-end font-monospace fw-semibold">
                                        NT$ {{ formatPrice(item.unitPrice) }}
                                    </td>
                                </tr>

                                <tr v-if="!order.orderItems || order.orderItems.length === 0">
                                    <td colspan="3" class="text-center text-secondary py-4">
                                        此訂單沒有商品明細
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
                <!-- 訂單與付款資訊 -->
                <section class="card shadow-sm border-0 mb-4">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">
                            訂單與付款資訊
                        </h2>
                    </div>

                    <div class="card-body">
                        <div class="row g-4">

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    訂單狀態
                                </div>

                                <span class="badge" :class="orderStatusClass(order.orderStatus)">
                                    {{ orderStatusText(order.orderStatus) }}
                                </span>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    付款狀態
                                </div>

                                <span class="badge" :class="paymentStatusClass(order.paymentStatus)">
                                    {{ paymentStatusText(order.paymentStatus) }}
                                </span>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    付款方式
                                </div>

                                <div class="detail-value">
                                    {{ paymentMethodText(order.paymentMethod) }}
                                </div>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    付款時間
                                </div>

                                <div class="detail-value">
                                    {{
                                        order.paidAt
                                            ? formatDateTime(order.paidAt)
                                            : '尚未付款'
                                    }}
                                </div>
                            </div>

                            <div class="col-12">
                                <div class="detail-label">
                                    交易編號
                                </div>

                                <div class="detail-value font-monospace">
                                    {{ order.transactionId || '尚無' }}
                                </div>
                            </div>

                        </div>
                    </div>
                </section>

                <!-- 收件資訊 -->
                <section class="card shadow-sm border-0 mb-4">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">
                            收件資訊
                        </h2>
                    </div>

                    <div class="card-body">
                        <div class="row g-4">

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    收件人
                                </div>

                                <div class="detail-value">
                                    {{ order.receiverName }}
                                </div>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    聯絡電話
                                </div>

                                <div class="detail-value">
                                    {{ order.receiverPhone }}
                                </div>
                            </div>

                            <div class="col-12">
                                <div class="detail-label">
                                    收件地址
                                </div>

                                <div class="detail-value">
                                    {{ order.receiverAddress }}
                                </div>
                            </div>

                        </div>
                    </div>
                </section>

                <!-- 物流資訊 -->
                <section class="card shadow-sm border-0 mb-4">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">
                            物流資訊
                        </h2>
                    </div>

                    <div class="card-body">
                        <div class="row g-4">

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    配送方式
                                </div>

                                <div class="detail-value">
                                    {{ shippingMethodText(order.shippingMethod) }}
                                </div>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    物流狀態
                                </div>

                                <span class="badge" :class="shippingStatusClass(order.shippingStatus)">
                                    {{ shippingStatusText(order.shippingStatus) }}
                                </span>
                            </div>

                            <div class="col-12">
                                <div class="detail-label">
                                    物流單號
                                </div>

                                <div class="detail-value font-monospace">
                                    {{ order.trackingNumber || '尚無' }}
                                </div>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    出貨時間
                                </div>

                                <div class="detail-value">
                                    {{
                                        order.shippedAt
                                            ? formatDateTime(order.shippedAt)
                                            : '尚未出貨'
                                    }}
                                </div>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    送達時間
                                </div>

                                <div class="detail-value">
                                    {{
                                        order.deliveredAt
                                            ? formatDateTime(order.deliveredAt)
                                            : '尚未送達'
                                    }}
                                </div>
                            </div>

                        </div>
                    </div>
                </section>

                <!-- 訂單備註 -->
                <section class="card shadow-sm border-0">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">
                            訂單備註
                        </h2>
                    </div>

                    <div class="card-body">
                        <p class="mb-0">
                            {{ order.customerRemark || '無' }}
                        </p>
                    </div>
                </section>

            </div>

            <!-- Loading -->
            <div v-else class="card shadow-sm border-0">
                <div class="card-body text-center text-secondary py-5">
                    <div class="spinner-border spinner-border-sm me-2" role="status"></div>

                    載入訂單資料中...
                </div>
            </div>

        </div>
    </main>
</template>

<style scoped>
.order-admin-page {
    min-height: 100vh;
    background: #f8f9ff;
}

.order-detail-width {
    max-width: 1400px;
}

.card {
    border-radius: 0.75rem;
}

.detail-label {
    margin-bottom: 0.35rem;
    color: #6c757d;
    font-size: 0.8rem;
    font-weight: 600;
}

.detail-value {
    color: #212529;
    font-size: 0.95rem;
    font-weight: 500;
}
</style>