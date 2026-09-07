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
    }
})

defineEmits(['clear', 'checkout'])
</script>

<template>
    <div class="border-top pt-3 mt-4">
        <div class="d-flex justify-content-between mb-2">
            <span>已選數量</span>
            <strong>{{ itemCount }} 件</strong>
        </div>

        <div class="d-flex justify-content-between mb-3">
            <strong>商品小計</strong>
            <strong>
                NT$ {{ totalAmount.toLocaleString() }}
            </strong>
        </div>

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