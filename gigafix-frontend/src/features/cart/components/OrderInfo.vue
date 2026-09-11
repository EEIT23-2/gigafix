<script setup>
import { computed } from 'vue'
import OrderStatusBadge from './OrderStatusBadge.vue'
import {
    paymentMethodText,
    shippingMethodText
} from '../status'

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

const formatStepTime = (value) => {
    if (!value) {
        return '-'
    }

    return new Date(value).toLocaleTimeString(
        'zh-TW',
        {
            hour: '2-digit',
            minute: '2-digit'
        }
    )
}

// 訂單處理進度
const progressSteps = computed(() => {
    const steps = [
        {
            label: '訂單成立',
            time: props.order.createdAt,
            done: Boolean(props.order.createdAt)
        },
        {
            label: '付款完成',
            time: props.order.paidAt,
            done: props.order.paymentStatus === 'PAID'
        },
        {
            label: '商品出貨',
            time: props.order.shippedAt,
            done: (
                props.order.shippingStatus === 'SHIPPED'
                || props.order.shippingStatus === 'DELIVERED'
                || Boolean(props.order.shippedAt)
            )
        },
        {
            label: '完成送達',
            time: props.order.deliveredAt,
            done: (
                props.order.shippingStatus === 'DELIVERED'
                || props.order.orderStatus === 'COMPLETED'
                || Boolean(props.order.deliveredAt)
            )
        }
    ]

    return steps.map((step, index) => ({
        ...step,
        connectorDone:
            index < steps.length - 1
            && steps[index + 1].done
    }))
})
</script>

<template>
    <section class="order-information">
        <!-- 訂單編號及主要狀態 -->
        <header class="order-status-header">
            <div class="order-number">
                <strong>
                    訂單 #{{ order.orderId }}
                </strong>

                <span>
                    {{ formatDateTime(order.createdAt) }}
                </span>
            </div>

            <OrderStatusBadge type="order" :value="order.orderStatus" />
        </header>

        <!-- 訂單進度 -->
        <div class="order-progress" aria-label="訂單處理進度">
            <div v-for="(step, index) in progressSteps" :key="step.label" class="progress-step" :class="{
                completed: step.done,
                'connector-completed':
                    step.connectorDone
            }">
                <div class="progress-dot">
                    <i v-if="step.done" class="bi bi-check-lg"></i>

                    <span v-else>
                        {{ index + 1 }}
                    </span>
                </div>

                <strong class="progress-label">
                    {{ step.label }}
                </strong>

                <span class="progress-time">
                    {{ formatStepTime(step.time) }}
                </span>
            </div>
        </div>

        <!-- 詳細資訊 -->
        <div class="order-information-grid">
            <section class="information-panel">
                <h3>
                    <i class="bi bi-geo-alt"></i>
                    收件與配送
                </h3>

                <dl class="information-list">
                    <div class="information-row">
                        <dt>收件人</dt>
                        <dd>{{ order.receiverName }}</dd>
                    </div>

                    <div class="information-row">
                        <dt>電話</dt>
                        <dd>{{ order.receiverPhone }}</dd>
                    </div>

                    <div class="information-row">
                        <dt>配送方式</dt>
                        <dd>
                            {{
                                shippingMethodText(
                                    order.shippingMethod
                                )
                            }}
                        </dd>
                    </div>

                    <div class="information-row">
                        <dt>
                            {{
                                order.shippingMethod === 'STORE'
                                    ? '取貨門市'
                                    : '配送地址'
                            }}
                        </dt>

                        <dd>
                            {{ order.receiverAddress || '-' }}
                        </dd>
                    </div>

                    <div v-if="order.customerRemark" class="information-row">
                        <dt>訂單備註</dt>
                        <dd>{{ order.customerRemark }}</dd>
                    </div>
                </dl>
            </section>

            <section class="information-panel">
                <h3>
                    <i class="bi bi-credit-card"></i>
                    付款與物流
                </h3>

                <dl class="information-list">
                    <div class="information-row">
                        <dt>付款方式</dt>
                        <dd>
                            {{
                                paymentMethodText(
                                    order.paymentMethod
                                )
                            }}
                        </dd>
                    </div>

                    <div class="information-row">
                        <dt>付款狀態</dt>
                        <dd>
                            <OrderStatusBadge type="payment" :value="order.paymentStatus" />
                        </dd>
                    </div>

                    <div class="information-row">
                        <dt>物流狀態</dt>
                        <dd>
                            <OrderStatusBadge type="shipping" :value="order.shippingStatus" />
                        </dd>
                    </div>

                    <div class="information-row">
                        <dt>物流追蹤編號</dt>
                        <dd>
                            {{ order.trackingNumber || '-' }}
                        </dd>
                    </div>

                    <div v-if="order.paidAt" class="information-row">
                        <dt>付款時間</dt>
                        <dd>
                            {{ formatDateTime(order.paidAt) }}
                        </dd>
                    </div>

                    <div v-if="order.shippedAt" class="information-row">
                        <dt>出貨時間</dt>
                        <dd>
                            {{ formatDateTime(order.shippedAt) }}
                        </dd>
                    </div>

                    <div v-if="order.deliveredAt" class="information-row">
                        <dt>送達時間</dt>
                        <dd>
                            {{ formatDateTime(order.deliveredAt) }}
                        </dd>
                    </div>
                </dl>
            </section>
        </div>
    </section>
</template>

<style scoped>
.order-information {
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #ffffff;
}

.order-status-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 16px 20px;
    border-bottom: 1px solid #d8dde6;
    background: #f7f8fa;
}

.order-number {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
}

.order-number span {
    color: #6b7280;
    font-size: 0.9rem;
}

.order-progress {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    padding: 28px 24px;
    border-bottom: 1px solid #d8dde6;
}

.progress-step {
    position: relative;
    display: flex;
    align-items: center;
    flex-direction: column;
    min-width: 0;
    text-align: center;
}

.progress-step:not(:last-child)::after {
    position: absolute;
    top: 17px;
    left: 50%;
    z-index: 0;
    width: 100%;
    height: 2px;
    background: #d8dde6;
    content: '';
}

.progress-step.connector-completed::after {
    background: #4169e1;
}

.progress-dot {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    margin-bottom: 9px;
    border: 2px solid #cbd2dc;
    border-radius: 50%;
    color: #6b7280;
    background: #ffffff;
    font-weight: 600;
}

.progress-step.completed .progress-dot {
    border-color: #4169e1;
    color: #ffffff;
    background: #4169e1;
}

.progress-label {
    color: #374151;
    font-size: 0.9rem;
}

.progress-time {
    min-height: 20px;
    margin-top: 3px;
    color: #6b7280;
    font-size: 0.8rem;
}

.order-information-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
}

.information-panel {
    min-width: 0;
    padding: 22px 24px;
}

.information-panel+.information-panel {
    border-left: 1px solid #d8dde6;
}

.information-panel h3 {
    margin: 0 0 18px;
    color: #1f2937;
    font-size: 1.05rem;
    font-weight: 700;
}

.information-panel h3 i {
    margin-right: 7px;
    color: #4169e1;
}

.information-list {
    display: grid;
    gap: 13px;
    margin: 0;
}

.information-row {
    display: grid;
    grid-template-columns: 110px minmax(0, 1fr);
    gap: 14px;
}

.information-row dt {
    color: #6b7280;
    font-weight: 400;
}

.information-row dd {
    min-width: 0;
    margin: 0;
    overflow-wrap: anywhere;
    color: #1f2937;
}

@media (max-width: 767.98px) {
    .order-status-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .order-progress {
        grid-template-columns: repeat(2, 1fr);
        gap: 24px 0;
        padding: 24px 12px;
    }

    .progress-step:nth-child(2)::after {
        display: none;
    }

    .order-information-grid {
        grid-template-columns: 1fr;
    }

    .information-panel {
        padding: 20px 18px;
    }

    .information-panel+.information-panel {
        border-top: 1px solid #d8dde6;
        border-left: 0;
    }

    .information-row {
        grid-template-columns: 96px minmax(0, 1fr);
    }
}
</style>