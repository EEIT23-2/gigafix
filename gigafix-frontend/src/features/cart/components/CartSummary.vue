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
    <div class="border-top pt-3 mt-4">

        <!-- 已選數量 -->
        <div class="d-flex justify-content-between mb-2">
            <span>已選數量</span>
            <strong>{{ itemCount }} 件</strong>
        </div>

        <!-- 商品小計 -->
        <div class="d-flex justify-content-between mb-2">
            <span>商品小計</span>
            <strong>
                NT$ {{ totalAmount.toLocaleString() }}
            </strong>
        </div>

        <!-- 運費 -->
        <div class="d-flex justify-content-between mb-3">
            <span>運費</span>
            <strong>免運</strong>
        </div>

        <!-- 優惠券 -->
        <div class="mt-3">
            <label class="form-label fw-bold">
                優惠券
            </label>

            <select class="form-select" :value="selectedCouponCode" @change="$emit(
                'update:selectedCouponCode',
                $event.target.value
            )">
                <option value="">
                    不使用優惠券
                </option>

                <option v-for="coupon in coupons" :key="coupon.couponCode" :value="coupon.couponCode">
                    {{ coupon.couponName }}
                    - 折 NT$
                    {{ coupon.discountAmount.toLocaleString() }}
                </option>
            </select>
        </div>

        <!-- 優惠券折扣 -->
        <div v-if="couponDiscount > 0" class="d-flex justify-content-between mt-3">
            <span>優惠券折扣</span>

            <strong>
                -NT$ {{ couponDiscount.toLocaleString() }}
            </strong>
        </div>

        <hr>

        <!-- 預估應付金額 -->
        <div class="d-flex justify-content-between mb-3">
            <strong>總金額</strong>

            <strong class="fs-5">
                NT$ {{ finalAmount.toLocaleString() }}
            </strong>
        </div>

        <!-- 操作按鈕 -->
        <div class="d-flex gap-2 justify-content-end">
            <button class="btn btn-outline-danger" :disabled="clearing" @click="$emit('clear')">
                {{ clearing ? '清空中...' : '清空購物車' }}
            </button>

            <button class="btn btn-primary" :disabled="checkoutDisabled" @click="$emit('checkout')">
                前往結帳
            </button>
        </div>

    </div>
</template>