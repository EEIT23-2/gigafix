<script setup>
import OrderStatusBadge from './OrderStatusBadge.vue'
import { shippingMethodText } from '../status'

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
    <div>
        <h5 class="mb-3">
            訂單 #{{ order.orderId }}
        </h5>

        <p>
            訂單狀態：
            <OrderStatusBadge type="order" :value="order.orderStatus" />
        </p>

        <p>
            付款狀態：
            <OrderStatusBadge type="payment" :value="order.paymentStatus" />
        </p>

        <p>
            物流狀態：
            <OrderStatusBadge type="shipping" :value="order.shippingStatus" />
        </p>

        <p>
            建立時間：{{ formatDateTime(order.createdAt) }}
        </p>

        <p v-if="order.paidAt">
            付款時間：{{ formatDateTime(order.paidAt) }}
        </p>

        <p v-if="order.shippedAt">
            出貨時間：{{ formatDateTime(order.shippedAt) }}
        </p>

        <p v-if="order.deliveredAt">
            送達時間：{{ formatDateTime(order.deliveredAt) }}
        </p>

        <hr>

        <p>收件人：{{ order.receiverName }}</p>
        <p>電話：{{ order.receiverPhone }}</p>
        <p>配送方式：{{ shippingMethodText(order.shippingMethod) }}</p>
        <p>{{ order.shippingMethod === 'STORE' ? '取貨門市' : '配送地址' }}：{{ order.receiverAddress }}</p>
        <p v-if="order.trackingNumber">物流追蹤編號：{{ order.trackingNumber }}</p>
    </div>
</template>