<script setup>
defineProps({
    totalAmount: {
        type: Number,
        default: 0
    },
    itemCount: {
        type: Number,
        default: 0
    },
    clearing: {
        type: Boolean,
        default: false
    },
    checkoutDisabled: {
        type: Boolean,
        default: false
    },
    coupons: {
        type: Array,
        default: () => []
    },
    selectedCouponCode: {
        type: String,
        default: ''
    },
    couponDiscount: {
        type: Number,
        default: 0
    },
    finalAmount: {
        type: Number,
        default: 0
    }
})

defineEmits(['clear', 'checkout', 'update:selectedCouponCode'])
</script>

<template>
    <section class="cart-summary" aria-label="購物車結帳摘要">
        <!-- 優惠券 -->
        <div class="coupon-section">
            <label for="cartCoupon" class="coupon-label">
                優惠券
            </label>

            <select id="cartCoupon" class="form-select coupon-select" :value="selectedCouponCode" @change="$emit(
                'update:selectedCouponCode',
                $event.target.value
            )">
                <option value="">
                    不使用優惠券
                </option>

                <option v-for="coupon in coupons" :key="coupon.couponCode" :value="coupon.couponCode">
                    {{ coupon.couponName }}
                    -折 NT$
                    {{
                        coupon.discountAmount
                            .toLocaleString('zh-TW')
                    }}
                </option>
            </select>
        </div>

        <!-- 金額摘要 -->
        <div class="total-section">
            <span class="total-label">
                合計（{{ itemCount }} 件）
            </span>

            
            <div class="amount-details">
                <span>
                    商品小計：
                    NT$
                    {{
                        totalAmount
                        .toLocaleString('zh-TW')
                    }}
                </span>
                
                <span v-if="couponDiscount > 0">
                    優惠券折扣：
                    − NT$
                    {{
                        couponDiscount
                        .toLocaleString('zh-TW')
                    }}
                </span>
                
                <span>運費：免運</span>
                <strong class="final-amount">
                    NT$
                    {{
                        finalAmount
                            .toLocaleString('zh-TW')
                    }}
                </strong>
            </div>
        </div>

        <!-- 操作按鈕 -->
        <div class="summary-actions">
            <button class="btn btn-outline-danger" type="button" :disabled="clearing" @click="$emit('clear')">
                {{
                    clearing
                        ? '清空中...'
                        : '清空購物車'
                }}
            </button>

            <button class="btn btn-primary checkout-button" type="button" :disabled="checkoutDisabled || clearing
                " @click="$emit('checkout')">
                前往結帳
            </button>
        </div>
    </section>
</template>

<style scoped>
.cart-summary {
    display: grid;
    grid-template-columns:
        minmax(260px, 1fr) auto auto;
    align-items: end;
    gap: 24px;
    margin-top: 18px;
    padding: 18px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #ffffff;
}

.coupon-section {
    min-width: 0;
}

.coupon-label {
    display: block;
    margin-bottom: 7px;
    font-weight: 600;
}

.coupon-select {
    width: 100%;
    min-height: 42px;
}

.total-section {
    min-width: 230px;
    text-align: right;
}

.total-label {
    display: block;
    color: #6b7280;
    font-size: 0.9rem;
}

.final-amount {
    display: block;
    margin: 3px 0 5px;
    color: #c64736;
    font-size: 1.35rem;
    white-space: nowrap;
}

.amount-details {
    display: flex;
    flex-direction: column;
    gap: 2px;
    color: #6b7280;
    font-size: 0.8rem;
}

.summary-actions {
    display: flex;
    align-items: center;
    gap: 10px;
}

.summary-actions .btn {
    min-height: 42px;
    white-space: nowrap;
}

.checkout-button {
    min-width: 116px;
}

@media (max-width: 991.98px) {
    .cart-summary {
        grid-template-columns:
            minmax(240px, 1fr) auto;
    }

    .summary-actions {
        grid-column: 1 / -1;
        justify-content: flex-end;
    }
}

@media (max-width: 575.98px) {
    .cart-summary {
        grid-template-columns: 1fr;
        align-items: stretch;
        gap: 18px;
    }

    .total-section {
        min-width: 0;
        text-align: left;
    }

    .summary-actions {
        grid-column: auto;
        display: grid;
        grid-template-columns: 1fr 1fr;
    }

    .summary-actions .btn {
        white-space: normal;
    }
}
</style>