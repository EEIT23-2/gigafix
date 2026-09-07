<script setup>
defineProps({
    item: {
        type: Object,
        required: true
    }
})

defineEmits(['delete'])
</script>
<!-- 抽出成購物車商品卡片組件 -->
<template>
    <div class="card mb-3">
        <div class="card-body d-flex justify-content-between align-items-center">
            <div>
                <h5 class="mb-1">
                    {{ item.productName }}
                </h5>

                <p class="mb-0 text-muted">
                    NT$ {{ item.price?.toLocaleString() }}
                </p>
            </div>

            <button class="btn btn-outline-danger" @click="$emit('delete', item.cartItemId)">
                刪除
            </button>
        </div>
    </div>
</template>