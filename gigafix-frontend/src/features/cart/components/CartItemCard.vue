<script setup>
import { ref } from 'vue'

defineProps({
    item: {
        type: Object,
        required: true
    },
    selected: {
        type: Boolean,
        default: false
    }
})

defineEmits([
    'delete',
    'update:selected'
])

//// 團片載入失敗時const imageLoadFailed = ref(false)
</script>

<template>
    <article class="cart-item-row">
        <!-- 商品資訊訊 -->
        <div class="product-section">
            <input class="form-check-input product-checkbox" type="checkbox" :checked="selected"
                :aria-label="`選取 ${item.productName}`" @change="$emit(
                    'update:selected',
                    $event.target.checked
                )">

            <!-- 商品縮圖 -->
            <div class="product-image-wrapper">
                <img v-if="item.imageUrl && !imageLoadFailed" :src="item.imageUrl" :alt="item.productName"
                    class="product-image" loading="lazy" @error="imageLoadFailed = true">

                <div v-else class="product-image-placeholder" aria-label="商品暫無圖片">
                    <i class="bi bi-image" aria-hidden="true"></i>
                </div>
            </div>

            <!-- 商品名稱 -->
            <RouterLink class="product-name" :to="{
                name: 'mall-detail',
                params: {
                    productId: item.productId
                }
            }">
                {{ item.productName }}
            </RouterLink>
        </div>

        <!-- 商品價格 -->
        <div class="product-price">
            NT$
            {{
                Number(item.price || 0)
                    .toLocaleString('zh-TW')
            }}
        </div>

        <!-- 操作 -->
        <div class="product-actions">
            <button class="delete-button" type="button" @click="$emit('delete', item.cartItemId)">
                刪除
            </button>
        </div>
    </article>
</template>

<style scoped>
.cart-item-row {
    display: grid;
    grid-template-columns:
        minmax(360px, 1fr) 140px 80px;
    align-items: center;
    gap: 18px;
    min-height: 116px;
    margin-bottom: 12px;
    padding: 14px 18px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #ffffff;
}

.product-section {
    display: grid;
    grid-template-columns:
        24px 88px minmax(0, 1fr);
    align-items: center;
    gap: 14px;
    min-width: 0;
}

.product-checkbox {
    width: 18px;
    height: 18px;
    cursor: pointer;
}

.product-image-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 88px;
    height: 88px;
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #f7f8fa;
}

.product-image {
    width: 100%;
    height: 100%;
    padding: 6px;
    object-fit: contain;
}

.product-image-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: #9ca3af;
    font-size: 28px;
}

.product-info {
    min-width: 0;
}

.product-name {
    display: inline-block;
    margin: 0 0 8px;
    overflow-wrap: anywhere;
    color: #1f2937;
    font-size: 1rem;
    font-weight: 600;
    line-height: 1.5;
    text-decoration: none;
}

.product-name:hover {
    color: #4169e1;
    text-decoration: underline;
}

.product-price {
    color: #1f2937;
    font-weight: 600;
    text-align: right;
    white-space: nowrap;
}

.product-actions {
    text-align: center;
}

.delete-button {
    min-height: 40px;
    padding: 8px 6px;
    border: 0;
    color: #dc3545;
    background: transparent;
}

.delete-button:hover {
    text-decoration: underline;
}

@media (max-width: 767.98px) {
    .cart-item-row {
        grid-template-columns: 1fr auto;
        gap: 10px;
        padding: 14px;
    }

    .product-section {
        grid-column: 1 / -1;
        grid-template-columns:
            24px 72px minmax(0, 1fr);
        gap: 11px;
    }

    .product-image-wrapper {
        width: 72px;
        height: 72px;
    }

    .product-price {
        padding-left: 35px;
        text-align: left;
    }
}
</style>