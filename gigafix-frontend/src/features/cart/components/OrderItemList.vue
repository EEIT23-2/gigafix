<script setup>
defineProps({
    items: {
        type: Array,
        default: () => []
    }
})
</script>

<template>
    <div>
        <h6 class="mb-3">商品明細</h6>

        <div v-if="items.length === 0" class="text-muted">
            沒有商品資料
        </div>

        <div v-for="item in items" :key="item.productId" class="d-flex justify-content-between border-bottom py-2">
            <span>
                {{ item.productName }}
            </span>

            <strong>
                NT$ {{ item.unitPrice?.toLocaleString() }}
            </strong>
        </div>
    </div>
</template>