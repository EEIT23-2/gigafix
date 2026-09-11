<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMemberOrders } from '../../api/memberOrderApi'
import { useMemberAuth } from '../../composables/useMemberAuth'
import OrderCard from '../../components/OrderCard.vue'

const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const selectedStatus = ref('ALL')

const statusTabs = [
    {
        value: 'ALL',
        label: '全部'
    },
    {
        value: 'UNPAID',
        label: '待付款'
    },
    {
        value: 'PENDING_SHIPMENT',
        label: '待出貨'
    },
    {
        value: 'SHIPPED',
        label: '配送中'
    },
    {
        value: 'COMPLETED',
        label: '已完成'
    }
]
// 會員端不顯示已取消的訂單
const visibleOrders = computed(() => {
    return orders.value.filter((order) => {
        return order.orderStatus !== 'CANCELLED'
    })
})
// 根據頁籤篩選訂單
const filteredOrders = computed(() => {
    return visibleOrders.value.filter((order) => {
        switch (selectedStatus.value) {
            case 'UNPAID':
                return (
                    order.orderStatus === 'PENDING'
                    && order.paymentStatus === 'UNPAID'
                )

            case 'PENDING_SHIPMENT':
                return (
                    order.orderStatus !== 'CANCELLED'
                    && order.paymentStatus === 'PAID'
                    && order.shippingStatus === 'PENDING'
                )

            case 'SHIPPED':
                return order.shippingStatus === 'SHIPPED'

            case 'COMPLETED':
                return order.orderStatus === 'COMPLETED'

            default:
                return true
        }
    })
})

// 取得會員的訂單列表
const loadOrders = async () => {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await getMemberOrders()

        orders.value = [...response.data].sort((a, b) => {
            return new Date(b.createdAt) - new Date(a.createdAt)
        })
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

// 會員驗證與初始化訂單列表
const { checkMemberLogin } = useMemberAuth()

onMounted(async () => {
    const loggedIn = await checkMemberLogin()

    if (!loggedIn) {
        return
    }

    await loadOrders()
})
</script>

<template>
    <section class="member-orders-page">
        <!-- 頁面標題 -->
        <header class="order-page-header">
            <div>
                <h2>我的訂單</h2>

                <p>
                    查看付款狀態與商品配送進度
                </p>
            </div>

            <span v-if="!loading" class="order-count">
                共 {{ visibleOrders.length }} 筆訂單
            </span>
        </header>

        <!-- 載入狀態 -->
        <div v-if="loading" class="order-message">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">
                    載入中
                </span>
            </div>

            <p>正在載入訂單...</p>
        </div>

        <!-- 錯誤狀態 -->
        <div v-else-if="errorMessage" class="order-message">
            <i class="bi bi-exclamation-circle"></i>

            <p>{{ errorMessage }}</p>

            <button type="button" class="btn btn-outline-primary" @click="loadOrders">
                重新載入
            </button>
        </div>

        <!-- 完全沒有訂單 -->
        <div v-else-if="visibleOrders.length === 0" class="order-message">
            <i class="bi bi-receipt"></i>

            <h3>目前沒有訂單</h3>

            <p>
                完成購物後，訂單會顯示在這裡。
            </p>

            <RouterLink to="/mall" class="btn btn-primary">
                前往選購
            </RouterLink>
        </div>

        <template v-else>
            <!-- 訂單狀態篩選 -->
            <nav class="status-tabs" aria-label="訂單狀態篩選">
                <button v-for="tab in statusTabs" :key="tab.value" type="button" class="status-tab" :class="{
                    active:
                        selectedStatus === tab.value
                }" @click="selectedStatus = tab.value">
                    {{ tab.label }}
                </button>
            </nav>

            <!-- 篩選後沒有訂單 -->
            <div v-if="filteredOrders.length === 0" class="order-message compact">
                <i class="bi bi-inbox"></i>

                <h3>這個分類目前沒有訂單</h3>

                <p>
                    可以切換其他分類查看。
                </p>
            </div>

            <!-- 訂單列表 -->
            <div v-else class="order-list">
                <OrderCard v-for="order in filteredOrders" :key="order.orderId" :order="order" />
            </div>
        </template>
    </section>
</template>

<style scoped>
.member-orders-page {
    width: 100%;
    min-width: 0;
}

.order-page-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 22px;
}

.order-page-header h2 {
    margin: 0 0 6px;
    color: #1f2937;
    font-size: 1.75rem;
    font-weight: 700;
}

.order-page-header p {
    margin: 0;
    color: #6b7280;
}

.order-count {
    flex-shrink: 0;
    color: #6b7280;
    font-size: 0.9rem;
}

.status-tabs {
    display: flex;
    width: 100%;
    margin-bottom: 18px;
    overflow-x: auto;
    overflow-y: hidden;
    border-bottom: 1px solid #d8dde6;
    scrollbar-width: thin;
}

.status-tab {
    position: relative;
    flex: 1;
    min-width: 92px;
    padding: 13px 16px;
    border: 0;
    color: #6b7280;
    background: transparent;
    white-space: nowrap;
    transition:
        color 0.2s ease,
        background-color 0.2s ease;
}

.status-tab:hover {
    color: #4169e1;
    background: #f7f8fa;
}

.status-tab.active {
    color: #4169e1;
    font-weight: 600;
}

.status-tab.active::after {
    position: absolute;
    right: 12px;
    bottom: -1px;
    left: 12px;
    height: 3px;
    border-radius: 3px 3px 0 0;
    background: #4169e1;
    content: '';
}

.order-list {
    width: 100%;
}

.order-message {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    min-height: 300px;
    padding: 40px 20px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    color: #6b7280;
    background: #ffffff;
    text-align: center;
}

.order-message.compact {
    min-height: 240px;
}

.order-message>i {
    margin-bottom: 14px;
    color: #9ca3af;
    font-size: 42px;
}

.order-message h3 {
    margin-bottom: 8px;
    color: #1f2937;
    font-size: 1.1rem;
}

.order-message p {
    margin: 10px 0 18px;
}

.order-message .spinner-border {
    margin-bottom: 4px;
}

@media (max-width: 767.98px) {
    .order-page-header {
        align-items: flex-start;
        flex-direction: column;
        gap: 8px;
    }

    .order-page-header h2 {
        font-size: 1.5rem;
    }

    .status-tab {
        flex: 0 0 auto;
    }
}
</style>