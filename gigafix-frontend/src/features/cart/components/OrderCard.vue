<script setup>
import OrderStatusBadge from './OrderStatusBadge.vue'

defineProps({
    order: {
        type: Object,
        required: true
    }
})

const formatDateTime = (value) => {
    if (!value) {
        return '-'
    }

    return new Date(value).toLocaleString('zh-TW')
}
</script>

<template>
    <div class="card mb-3">
        <div class="card-body">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <h5 class="mb-2">
                        訂單 #{{ order.orderId }}
                    </h5>

                    <p class="mb-2">
                        訂單狀態：
                        <OrderStatusBadge type="order" :value="order.orderStatus" />
                    </p>

                    <p class="mb-2">
                        付款狀態：
                        <OrderStatusBadge type="payment" :value="order.paymentStatus" />
                    </p>

                    <p class="mb-2 text-muted">
                        建立時間：{{ formatDateTime(order.createdAt) }}
                    </p>

                    <p class="mb-0 fw-bold">
                        NT$ {{ order.totalAmount?.toLocaleString() }}
                    </p>
                </div>

                <RouterLink :to="`/member-center/orders/${order.orderId}`" class="btn btn-outline-primary">
                    查看詳情
                </RouterLink>
            </div>
        </div>
    </div>
</template>