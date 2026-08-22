<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
    getOrder,
    updateOrder as updateOrderApi
} from '../api'
import {
    orderStatusText,
    paymentStatusText,
    orderStatusClass,
    paymentStatusClass
} from '../status'

//******編輯訂單頁面******

const props = defineProps({
    orderId: String
})

const router = useRouter()

// 存放單筆訂單資料
const order = ref({})

// 查詢單筆訂單
const loadOrder = async () => {
    try {
        const response = await getOrder(props.orderId)

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

        await updateOrderApi(props.orderId, request)

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

// 金額格式化
const formatPrice = (price) => {
    if (price == null) {
        return '0'
    }

    return Number(price).toLocaleString('zh-TW')
}
</script>

<template>
    <main class="container-fluid px-3 px-lg-4 py-4 order-admin-page">
        <div class="mx-auto order-form-width">

            <!-- 頁面標題 -->
            <header class="mb-4">
                <h1 class="fw-bold mb-1">
                    編輯訂單
                </h1>

                <p class="text-secondary mb-0">
                    Update customer shipping and order information.
                </p>
            </header>

            <!-- 訂單摘要 -->
            <section class="card shadow-sm border-0 mb-4">
                <div class="card-body">
                    <div class="row g-4">

                        <div class="col-12 col-md-3">
                            <div class="summary-label">
                                訂單編號
                            </div>

                            <div class="summary-value fs-5">
                                #{{ order.orderId }}
                            </div>
                        </div>

                        <div class="col-12 col-md-3">
                            <div class="summary-label">
                                總金額
                            </div>

                            <div class="summary-value fs-5 text-primary">
                                NT$ {{ formatPrice(order.totalAmount) }}
                            </div>
                        </div>

                        <div class="col-12 col-md-3">
                            <div class="summary-label">
                                訂單狀態
                            </div>

                            <span class="badge" :class="orderStatusClass(order.orderStatus)">
                                {{ orderStatusText(order.orderStatus) }}
                            </span>
                        </div>

                        <div class="col-12 col-md-3">
                            <div class="summary-label">
                                付款狀態
                            </div>

                            <span class="badge" :class="paymentStatusClass(order.paymentStatus)">
                                {{ paymentStatusText(order.paymentStatus) }}
                            </span>
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

                        <!-- 收件人 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                收件人
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="order.receiverName" type="text" class="form-control"
                                placeholder="請輸入收件人姓名" />
                        </div>

                        <!-- 電話 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                聯絡電話
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="order.receiverPhone" type="tel" class="form-control"
                                placeholder="例如：0912345678" />
                        </div>

                        <!-- 地址 -->
                        <div class="col-12">
                            <label class="form-label fw-semibold">
                                收件地址
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="order.receiverAddress" type="text" class="form-control"
                                placeholder="請輸入完整收件地址" />
                        </div>

                        <!-- 配送方式 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                配送方式
                                <span class="text-danger">*</span>
                            </label>

                            <select v-model="order.shippingMethod" class="form-select">
                                <option value="HOME">
                                    宅配
                                </option>

                                <option value="STORE">
                                    超商取貨
                                </option>
                            </select>
                        </div>

                    </div>
                </div>
            </section>

            <!-- 訂單備註 -->
            <section class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white border-bottom py-3">
                    <h2 class="h5 fw-bold mb-0">
                        訂單備註
                    </h2>
                </div>

                <div class="card-body">
                    <label class="form-label fw-semibold">
                        備註
                    </label>

                    <textarea v-model="order.customerRemark" class="form-control" rows="4"
                        placeholder="可填寫配送或訂單相關備註"></textarea>

                    <div class="form-text">
                        此欄位可留空。
                    </div>
                </div>
            </section>

            <!-- 操作 -->
            <div class="d-flex flex-column-reverse flex-sm-row justify-content-end gap-2">
                <button class="btn btn-outline-secondary" type="button" @click="goBack">
                    返回訂單列表
                </button>

                <button class="btn btn-primary px-4" type="button" @click="updateOrder">
                    儲存修改
                </button>
            </div>

        </div>
    </main>
</template>

<style scoped>
.order-admin-page {
    min-height: 100vh;
    background: #f8f9ff;
}

.order-form-width {
    max-width: 1200px;
}

.card {
    border-radius: 0.75rem;
}

.form-label {
    font-size: 0.9rem;
}

.form-control,
.form-select {
    min-height: 42px;
}

.summary-label {
    margin-bottom: 0.35rem;
    color: #6c757d;
    font-size: 0.8rem;
    font-weight: 600;
}

.summary-value {
    font-weight: 700;
}
</style>