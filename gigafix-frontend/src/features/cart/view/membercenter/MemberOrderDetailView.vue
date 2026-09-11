<script setup>
import {
    computed,
    onMounted,
    ref
} from 'vue'
import {
    useRoute,
    useRouter
} from 'vue-router'
import { useMemberAuth } from '../../composables/useMemberAuth'
import OrderInfo from '../../components/OrderInfo.vue'
import OrderItemList from '../../components/OrderItemList.vue'
import {
    cancelMemberOrder,
    getMemberOrder
} from '../../api/memberOrderApi'
import { redirectToEcpayPayment } from '../../api/ecpayPaymentApi'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const cancelling = ref(false)
const redirectingToPayment = ref(false)
const paymentMessage = ref('')

// 是否可以付款或取消
const canPay = computed(() => {
    return (
        order.value?.orderStatus === 'PENDING'
        && order.value?.paymentStatus === 'UNPAID'
    )
})

// 商品原始小計
const productSubtotal = computed(() => {
    return (
        order.value?.orderItems?.reduce(
            (total, item) => {
                return total + Number(item.unitPrice || 0)
            },
            0
        ) || 0
    )
})

// 從商品小計與訂單總額推算優惠折抵
const discountAmount = computed(() => {
    const totalAmount =
        Number(order.value?.totalAmount || 0)

    return Math.max(
        productSubtotal.value - totalAmount,
        0
    )
})

const formatPrice = (value) => {
    return `NT$ ${Number(value || 0)
        .toLocaleString('zh-TW')}`
}

// 載入訂單詳情
const loadOrder = async () => {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await getMemberOrder(
            route.params.orderId
        )

        order.value = response.data
    } catch (error) {
        console.error(error)

        errorMessage.value =
            error.response?.data?.message
            || error.response?.data?.error
            || '訂單載入失敗，請稍後再試'
    } finally {
        loading.value = false
    }
}

// 取消訂單
const handleCancel = async () => {
    if (!confirm('確定要取消這筆訂單嗎？')) {
        return
    }

    cancelling.value = true
    errorMessage.value = ''

    try {
        await cancelMemberOrder(order.value.orderId)

        // 取消成功後返回列表
        // 已取消訂單不會出現在會員列表
        await router.replace({
            name: 'member-orders'
        })
    } catch (error) {
        console.error(error)

        errorMessage.value =
            error.response?.data?.message
            || error.response?.data?.error
            || '取消訂單失敗'
    } finally {
        cancelling.value = false
    }
}

// 前往綠界付款
const handlePayment = () => {
    if (!order.value) {
        return
    }

    if (!canPay.value) {
        errorMessage.value =
            '目前訂單狀態無法付款'

        return
    }

    errorMessage.value = ''
    redirectingToPayment.value = true

    try {
        redirectToEcpayPayment(
            order.value.orderId
        )
    } catch (error) {
        console.error(error)

        errorMessage.value =
            '無法前往綠界付款'

        redirectingToPayment.value = false
    }
}

// 會員驗證與初始化訂單詳情
const { checkMemberLogin } = useMemberAuth()

onMounted(async () => {
    const loggedIn = await checkMemberLogin()

    if (!loggedIn) {
        return
    }

    await loadOrder()

    if (
        route.query.payment === 'result'
        && order.value
    ) {
        if (order.value.paymentStatus === 'PAID') {
            paymentMessage.value =
                '付款成功，訂單付款狀態已更新'
        } else {
            paymentMessage.value =
                '已返回訂單頁面，請確認付款狀態'
        }
    }
})
</script>

<template>
    <section class="member-order-detail">
        <!-- 頁面標題 -->
        <header class="detail-page-header">
            <RouterLink :to="{ name: 'member-orders' }" class="back-link">
                <i class="bi bi-arrow-left"></i>
                返回我的訂單
            </RouterLink>

            <div>
                <h2>訂單詳情</h2>

                <p>
                    查看付款、配送與商品資訊
                </p>
            </div>
        </header>

        <!-- 訊息 -->
        <div v-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
        </div>

        <div v-if="paymentMessage" class="alert alert-success">
            {{ paymentMessage }}
        </div>

        <!-- 載入中 -->
        <div v-if="loading" class="detail-message">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">
                    載入中
                </span>
            </div>

            <p>正在載入訂單詳情...</p>
        </div>

        <!-- 訂單內容 -->
        <div v-else-if="order" class="detail-content">
            <OrderInfo :order="order" />

            <div class="product-summary-grid">
                <OrderItemList :items="order.orderItems" />

                <!-- 金額與操作 -->
                <aside class="amount-summary">
                    <h3>訂單金額</h3>

                    <div class="amount-row">
                        <span>商品小計</span>

                        <strong>
                            {{
                                formatPrice(
                                    productSubtotal
                                )
                            }}
                        </strong>
                    </div>

                    <div v-if="discountAmount > 0" class="amount-row discount">
                        <span>優惠折抵</span>

                        <strong>
                            − {{
                                formatPrice(
                                    discountAmount
                                )
                            }}
                        </strong>
                    </div>

                    <div class="amount-row">
                        <span>運費</span>
                        <strong>免運</strong>
                    </div>

                    <div class="total-row">
                        <span>訂單總金額</span>

                        <strong>
                            {{
                                formatPrice(
                                    order.totalAmount
                                )
                            }}
                        </strong>
                    </div>

                    <div v-if="canPay" class="order-actions">
                        <button type="button" class="btn btn-primary" :disabled="cancelling
                            || redirectingToPayment
                            " @click="handlePayment">
                            {{
                                redirectingToPayment
                                    ? '前往綠界中...'
                                    : '前往綠界付款'
                            }}
                        </button>

                        <button type="button" class="btn btn-outline-danger" :disabled="cancelling
                            || redirectingToPayment
                            " @click="handleCancel">
                            {{
                                cancelling
                                    ? '取消中...'
                                    : '取消訂單'
                            }}
                        </button>
                    </div>
                </aside>
            </div>
        </div>

        <!-- 找不到資料 -->
        <div v-else class="detail-message">
            <i class="bi bi-receipt"></i>

            <h3>找不到訂單資料</h3>

            <RouterLink :to="{ name: 'member-orders' }" class="btn btn-outline-primary">
                返回我的訂單
            </RouterLink>
        </div>
    </section>
</template>

<style scoped>
.member-order-detail {
    width: 100%;
    min-width: 0;
}

.detail-page-header {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 22px;
}

.detail-page-header h2 {
    margin: 0 0 5px;
    color: #1f2937;
    font-size: 1.75rem;
    font-weight: 700;
}

.detail-page-header p {
    margin: 0;
    color: #6b7280;
}

.back-link {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
    gap: 7px;
    min-height: 40px;
    padding: 8px 13px;
    border: 1px solid #9ca3af;
    border-radius: 6px;
    color: #4b5563;
    text-decoration: none;
}

.back-link:hover {
    border-color: #4169e1;
    color: #4169e1;
}

.detail-content {
    display: grid;
    gap: 20px;
}

.product-summary-grid {
    display: grid;
    grid-template-columns:
        minmax(0, 1fr) 340px;
    align-items: start;
    gap: 20px;
}

.amount-summary {
    padding: 20px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #ffffff;
}

.amount-summary h3 {
    margin: 0 0 18px;
    padding-bottom: 14px;
    border-bottom: 1px solid #d8dde6;
    color: #1f2937;
    font-size: 1.05rem;
    font-weight: 700;
}

.amount-row,
.total-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.amount-row {
    margin-bottom: 13px;
    color: #4b5563;
}

.amount-row strong {
    color: #1f2937;
    white-space: nowrap;
}

.amount-row.discount,
.amount-row.discount strong {
    color: #dc3545;
}

.total-row {
    margin-top: 18px;
    padding-top: 18px;
    border-top: 1px solid #d8dde6;
    color: #1f2937;
    font-weight: 700;
}

.total-row strong {
    color: #4169e1;
    font-size: 1.2rem;
    white-space: nowrap;
}

.order-actions {
    display: grid;
    gap: 10px;
    margin-top: 22px;
}

.order-actions .btn {
    min-height: 42px;
}

.detail-message {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    min-height: 320px;
    padding: 40px 20px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    color: #6b7280;
    background: #ffffff;
    text-align: center;
}

.detail-message>i {
    margin-bottom: 14px;
    color: #9ca3af;
    font-size: 42px;
}

.detail-message h3 {
    margin-bottom: 18px;
    color: #1f2937;
    font-size: 1.1rem;
}

.detail-message p {
    margin: 12px 0 0;
}

@media (max-width: 991.98px) {
    .product-summary-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 575.98px) {
    .detail-page-header {
        align-items: flex-start;
        flex-direction: column;
        gap: 14px;
    }

    .detail-page-header h2 {
        font-size: 1.5rem;
    }

    .back-link {
        min-height: auto;
        padding: 0;
        border: 0;
    }

    .amount-summary {
        padding: 18px 16px;
    }
}
</style>