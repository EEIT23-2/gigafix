<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

// 監聽組件掛載完成後，載入訂單資料
const props = defineProps({
    orderId: String
})
const router = useRouter()
// 存放訂單完整資料
const order = ref(null)
// 查詢目前訂單詳細資訊
const loadOrder = async () => {
    try {
        const response = await axios.get(
            `/api/admin/orders/${props.orderId}`
        )

        order.value = response.data

        console.log('出貨訂單資料：', response.data)
    } catch (error) {
        console.error('取得訂單資料失敗：', error)
        alert('取得訂單資料失敗')
    }
}
onMounted(() => {
    loadOrder()
})
// 物流單號
const trackingNumber = ref(
    `GF-${Date.now()}`
)

// 確認出貨
const submitShip = async () => {

    if (!trackingNumber.value.trim()) {
        alert('請輸入物流單號')
        return
    }

    const confirmed = confirm(
        `確定要將訂單 ${props.orderId} 設為已出貨嗎？`
    )

    if (!confirmed) {
        return
    }

    try {

        const response = await axios.post(
            `/api/admin/orders/${props.orderId}/ship`,
            {
                trackingNumber: trackingNumber.value
            }
        )

        console.log('出貨成功：', response.data)

        alert('出貨成功')

        router.push('/admin/orders')

    } catch (error) {

        console.error('出貨失敗：', error)

        alert('出貨失敗')
    }
}
// 物流方式轉換文字
const shippingMethodText = (method) => {
    const methodMap = {
        HOME: '宅配',
        STORE: '超商取貨'
    }

    return methodMap[method] || method
}
// 返回訂單列表
const backToOrders = () => {
    router.push('/admin/orders')
}
</script>

<template>
    <main class="container-fluid px-3 px-lg-4 py-4 order-admin-page">
        <div class="mx-auto order-ship-width">

            <!-- 頁面標題 -->
            <header class="mb-4">
                <h1 class="fw-bold mb-1">
                    訂單出貨
                </h1>

                <p class="text-secondary mb-0">
                    Confirm order and shipping information before shipment.
                </p>
            </header>

            <!-- 資料載入完成 -->
            <template v-if="order">

                <!-- 訂單 / 商品資訊 -->
                <section class="card shadow-sm border-0 mb-4">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">
                            訂單資訊
                        </h2>
                    </div>

                    <div class="card-body">
                        <div class="row g-4">

                            <!-- 訂單 ID -->
                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    訂單編號
                                </div>

                                <div class="detail-value fw-bold">
                                    #{{ order.orderId }}
                                </div>
                            </div>

                            <!-- 訂單金額 -->
                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    訂單金額
                                </div>

                                <div class="detail-value fw-bold text-primary">
                                    NT$ {{ Number(order.totalAmount).toLocaleString('zh-TW') }}
                                </div>
                            </div>

                            <!-- 商品 -->
                            <div v-for="item in order.orderItems" :key="item.productId" class="col-12">
                                <div class="product-box">
                                    <div class="row g-3">

                                        <div class="col-12 col-md-3">
                                            <div class="detail-label">
                                                商品 ID
                                            </div>

                                            <div class="detail-value">
                                                #{{ item.productId }}
                                            </div>
                                        </div>

                                        <div class="col-12 col-md-6">
                                            <div class="detail-label">
                                                商品名稱
                                            </div>

                                            <div class="detail-value fw-semibold">
                                                {{ item.productName }}
                                            </div>
                                        </div>

                                        <div class="col-12 col-md-3">
                                            <div class="detail-label">
                                                成交價格
                                            </div>

                                            <div class="detail-value">
                                                NT$
                                                {{ Number(item.unitPrice).toLocaleString('zh-TW') }}
                                            </div>
                                        </div>

                                    </div>
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

                            <div class="col-12 col-md-6">
                                <div class="detail-label">
                                    配送方式
                                </div>

                                <div class="detail-value">
                                    {{ shippingMethodText(order.shippingMethod) }}
                                </div>
                            </div>

                        </div>
                    </div>
                </section>

                <!-- 出貨資訊 -->
                <section class="card shadow-sm border-0 mb-4">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">
                            出貨資訊
                        </h2>
                    </div>

                    <div class="card-body">

                        <label class="form-label fw-semibold">
                            物流單號
                        </label>

                        <input v-model="trackingNumber" type="text" class="form-control font-monospace" readonly />

                        <div class="form-text">
                            物流單號由系統自動產生。
                        </div>

                    </div>
                </section>

                <!-- 出貨警告 -->
                <div class="alert alert-warning" role="alert">
                    <div class="fw-semibold mb-1">
                        出貨確認
                    </div>

                    <div class="small">
                        請確認商品及收件資訊正確。
                        確認出貨後，訂單物流狀態將更新為「已出貨」。
                    </div>
                </div>

                <!-- 操作 -->
                <div class="d-flex flex-column-reverse flex-sm-row justify-content-end gap-2">
                    <button class="btn btn-outline-secondary" type="button" @click="backToOrders">
                        返回訂單列表
                    </button>

                    <button class="btn btn-primary px-4" type="button" @click="submitShip">
                        確認出貨
                    </button>
                </div>

            </template>

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

.order-ship-width {
    max-width: 1000px;
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
}

.product-box {
    padding: 1rem;
    background: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 0.5rem;
}

.form-control {
    min-height: 42px;
}
</style>