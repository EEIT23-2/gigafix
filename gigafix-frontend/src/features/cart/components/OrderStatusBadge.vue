<script setup>
import { computed } from 'vue'

import {
    orderStatusText,
    paymentStatusText,
    shippingStatusText,
    orderStatusClass,
    paymentStatusClass,
    shippingStatusClass
} from '../status'

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

    if (props.type === 'order') {
        return {
            text: orderStatusText(props.value),
            class: orderStatusClass(props.value)
        }
    }

    if (props.type === 'payment') {
        return {
            text: paymentStatusText(props.value),
            class: paymentStatusClass(props.value)
        }
    }

    if (props.type === 'shipping') {
        return {
            text: shippingStatusText(props.value),
            class: shippingStatusClass(props.value)
        }
    }

    return {
        text: props.value,
        class: 'text-bg-secondary'
    }
})
</script>

<template>
    <span class="badge order-status-badge" :class="statusConfig.class">
        {{ statusConfig.text }}
    </span>
</template>
<style scoped>
.order-status-badge {
    font-size: 0.9rem;
    padding: 0.45rem 0.75rem;
    font-weight: 600;
    border-radius: 0.5rem;
}
</style>