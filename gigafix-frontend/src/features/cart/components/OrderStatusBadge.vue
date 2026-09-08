<script setup>
import { computed } from 'vue'

const props = defineProps({
    type: {
        type: String,
        required: true
    },
    value: {
        type: String,
        required: true
    }
})

const statusConfig = computed(() => {
    const configs = {
        order: {
            PENDING: {
                text: '待處理',
                class: 'bg-warning text-dark'
            },
            CANCELLED: {
                text: '已取消',
                class: 'bg-secondary'
            },
            COMPLETED: {
                text: '已完成',
                class: 'bg-success'
            }
        },

        payment: {
            UNPAID: {
                text: '未付款',
                class: 'bg-danger'
            },
            PAID: {
                text: '已付款',
                class: 'bg-success'
            }
        },

        shipping: {
            PENDING: {
                text: '待出貨',
                class: 'bg-warning text-dark'
            },
            SHIPPED: {
                text: '已出貨',
                class: 'bg-primary'
            },
            DELIVERED: {
                text: '已送達',
                class: 'bg-success'
            }
        }
    }

    return (
        configs[props.type]?.[props.value] || {
            text: props.value,
            class: 'bg-secondary'
        }
    )
})
</script>

<template>
    <span class="badge" :class="statusConfig.class">
        {{ statusConfig.text }}
    </span>
</template>