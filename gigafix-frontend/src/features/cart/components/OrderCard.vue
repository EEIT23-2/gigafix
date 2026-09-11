<script setup>
import { computed, ref } from 'vue'
import OrderStatusBadge from './OrderStatusBadge.vue'
import { shippingMethodText } from '../status'

const props = defineProps({
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

const formatPrice = (value) => {
    if (value == null) {
        return '-'
    }

    return `NT$ ${Number(value).toLocaleString('zh-TW')}`
}

// 訂單中的第一項商品
const firstItem = computed(() => {
    return props.order.orderItems?.[0] || null
})

// 除了第一項商品以外的數量
const remainingItemCount = computed(() => {
    return Math.max(
        (props.order.orderItems?.length || 0) - 1,
        0
    )
})

// 卡片右上角顯示最重要的狀態
const primaryStatus = computed(() => {
    if (
        props.order.orderStatus === 'CANCELLED'
        || props.order.orderStatus === 'COMPLETED'
    ) {
        return {
            type: 'order',
            value: props.order.orderStatus
        }
    }

    if (props.order.paymentStatus === 'UNPAID') {
        return {
            type: 'payment',
            value: props.order.paymentStatus
        }
    }

    return {
        type: 'shipping',
        value: props.order.shippingStatus
    }
})

// 是否可以前往付款
const canPay = computed(() => {
    return (
        props.order.orderStatus === 'PENDING'
        && props.order.paymentStatus === 'UNPAID'
    )
})

// 訂單狀態提示
const statusHint = computed(() => {
    if (props.order.orderStatus === 'CANCELLED') {
        return '此訂單已取消'
    }

    if (canPay.value) {
        return '請完成付款以保留商品'
    }

    if (props.order.shippingStatus === 'SHIPPED') {
        return '商品配送中'
    }

    if (
        props.order.shippingStatus === 'DELIVERED'
        || props.order.orderStatus === 'COMPLETED'
    ) {
        return '訂單已完成'
    }

    return '店家正在準備您的商品'
})
</script>

<template>
    <article class="order-card">
        <!-- 訂單標頭 -->
        <header class="order-header">
            <div class="order-number">
                <strong>
                    訂單 #{{ order.orderId }}
                </strong>

                <span class="order-date">
                    {{ formatDateTime(order.createdAt) }}
                </span>
            </div>

            <OrderStatusBadge :type="primaryStatus.type" :value="primaryStatus.value" />
        </header>

        <!-- 商品摘要 -->
        <div class="order-product">
            <!-- 商品縮圖 -->
            <div class="product-placeholder">
                <img v-if="
                    firstItem?.imageUrl
                    && !imageLoadFailed
                " :src="firstItem.imageUrl" :alt="firstItem.productName" class="product-image" loading="lazy"
                    @error="imageLoadFailed = true">

                <i v-else class="bi bi-image" aria-label="商品暫無圖片"></i>
            </div>

            <div class="product-information">
                <h3 class="product-name">
                    <RouterLink v-if="firstItem?.productId" class="product-link" :to="{
                        name: 'mall-detail',
                        params: {
                            productId: firstItem.productId
                        }
                    }">
                        {{ firstItem.productName }}
                    </RouterLink>

                    <span v-else>
                        沒有商品資料
                    </span>
                </h3>

                <p v-if="remainingItemCount > 0" class="additional-items">
                    另有 {{ remainingItemCount }} 件商品
                </p>

                <p class="shipping-information">
                    {{
                        shippingMethodText(
                            order.shippingMethod
                        )
                    }}

                    <template v-if="order.trackingNumber">
                        ・物流編號
                        {{ order.trackingNumber }}
                    </template>
                </p>
            </div>

            <strong class="product-price">
                {{ formatPrice(firstItem?.unitPrice) }}
            </strong>
        </div>

        <!-- 訂單摘要與操作 -->
        <footer class="order-footer">
            <span class="status-hint">
                {{ statusHint }}
            </span>

            <div class="order-actions">
                <span class="order-total">
                    訂單總額

                    <strong>
                        {{ formatPrice(order.totalAmount) }}
                    </strong>
                </span>

                <RouterLink :to="{
                    name: 'member-order-detail',
                    params: {
                        orderId: order.orderId
                    }
                }" class="btn" :class="canPay
                    ? 'btn-primary'
                    : 'btn-outline-primary'
                    ">
                    {{
                        canPay
                            ? '前往付款'
                            : '查看詳情'
                    }}
                </RouterLink>
            </div>
        </footer>
    </article>
</template>

<style scoped>
.order-card {
    margin-bottom: 16px;
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #ffffff;
}

.order-header,
.order-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 13px 18px;
    background: #f7f8fa;
}

.order-number {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
}

.order-date {
    color: #6b7280;
    font-size: 0.85rem;
}

.order-product {
    display: grid;
    grid-template-columns:
        72px minmax(0, 1fr) auto;
    align-items: center;
    gap: 14px;
    padding: 16px 18px;
    border-top: 1px solid #d8dde6;
    border-bottom: 1px solid #d8dde6;
}

.product-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    color: #6b7280;
    background: #f3f6fb;
    font-size: 28px;
}

.product-image {
    width: 100%;
    height: 100%;
    padding: 5px;
    object-fit: contain;
}

.product-link {
    color: inherit;
    text-decoration: none;
}

.product-link:hover {
    color: #4169e1;
    text-decoration: underline;
}

.product-information {
    min-width: 0;
}

.product-name {
    margin: 0 0 6px;
    overflow-wrap: anywhere;
    color: #1f2937;
    font-size: 1rem;
    font-weight: 600;
}

.additional-items,
.shipping-information {
    margin: 0;
    color: #6b7280;
    font-size: 0.85rem;
}

.additional-items {
    margin-bottom: 3px;
}

.product-price {
    white-space: nowrap;
}

.status-hint {
    color: #6b7280;
    font-size: 0.9rem;
}

.order-actions {
    display: flex;
    align-items: center;
    gap: 16px;
}

.order-total {
    white-space: nowrap;
}

.order-total strong {
    margin-left: 6px;
}

.order-actions .btn {
    min-width: 96px;
    min-height: 40px;
}

@media (max-width: 767.98px) {

    .order-header,
    .order-footer {
        align-items: flex-start;
        flex-direction: column;
    }

    .order-product {
        grid-template-columns:
            64px minmax(0, 1fr);
    }

    .product-placeholder {
        width: 64px;
        height: 64px;
    }

    .product-price {
        grid-column: 2;
    }

    .order-actions {
        justify-content: space-between;
        width: 100%;
    }
}

@media (max-width: 479.98px) {
    .order-actions {
        align-items: stretch;
        flex-direction: column;
    }

    .order-actions .btn {
        width: 100%;
    }
}
</style>