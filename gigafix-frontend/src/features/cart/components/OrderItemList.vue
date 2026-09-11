<script setup>
import { ref } from 'vue'

defineProps({
    items: {
        type: Array,
        default: () => []
    }
})

const failedImageIds = ref(new Set())

const imageLoadFailed = (productId) => {
    return failedImageIds.value.has(productId)
}

const handleImageError = (productId) => {
    failedImageIds.value.add(productId)
}

const formatPrice = (value) => {
    if (value == null) {
        return '-'
    }

    return `NT$ ${Number(value).toLocaleString('zh-TW')}`
}
</script>

<template>
    <section class="order-products">
        <header class="products-header">
            <h3>商品明細</h3>

            <span>
                共 {{ items.length }} 件商品
            </span>
        </header>

        <div v-if="items.length === 0" class="empty-products">
            沒有商品資料
        </div>

        <div v-else class="product-list">
            <article v-for="item in items" :key="item.productId" class="product-row">
                <!-- 商品縮圖 -->
                <div class="product-image-wrapper">
                    <img v-if="
                        item.imageUrl
                        && !imageLoadFailed(item.productId)
                    " :src="item.imageUrl" :alt="item.productName" class="product-image" loading="lazy" @error="
                            handleImageError(item.productId)
                            ">

                    <div v-else class="product-image-placeholder" aria-label="商品暫無圖片">
                        <i class="bi bi-image"></i>
                    </div>
                </div>

                <!-- 商品資訊 -->
                <div class="product-information">
                    <RouterLink class="product-name" :to="{
                        name: 'mall-detail',
                        params: {
                            productId: item.productId
                        }
                    }">
                        {{ item.productName }}
                    </RouterLink>

                    <span class="product-quantity">
                        數量：1
                    </span>
                </div>

                <!-- 商品價格 -->
                <strong class="product-price">
                    {{ formatPrice(item.unitPrice) }}
                </strong>
            </article>
        </div>
    </section>
</template>

<style scoped>
.order-products {
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #ffffff;
}

.products-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 16px 20px;
    border-bottom: 1px solid #d8dde6;
    background: #f7f8fa;
}

.products-header h3 {
    margin: 0;
    color: #1f2937;
    font-size: 1.05rem;
    font-weight: 700;
}

.products-header span {
    color: #6b7280;
    font-size: 0.9rem;
}

.product-list {
    padding: 0 20px;
}

.product-row {
    display: grid;
    grid-template-columns:
        80px minmax(0, 1fr) auto;
    align-items: center;
    gap: 16px;
    min-height: 112px;
    padding: 16px 0;
}

.product-row+.product-row {
    border-top: 1px solid #e5e7eb;
}

.product-image-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 80px;
    height: 80px;
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #f7f8fa;
}

.product-image {
    width: 100%;
    height: 100%;
    padding: 5px;
    object-fit: contain;
}

.product-image-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: #9ca3af;
    font-size: 26px;
}

.product-information {
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    min-width: 0;
    gap: 5px;
}

.product-name {
    overflow-wrap: anywhere;
    color: #1f2937;
    font-weight: 600;
    line-height: 1.5;
    text-decoration: none;
}

.product-name:hover {
    color: #4169e1;
    text-decoration: underline;
}

.product-quantity {
    color: #6b7280;
    font-size: 0.85rem;
}

.product-price {
    color: #1f2937;
    white-space: nowrap;
}

.empty-products {
    padding: 48px 20px;
    color: #6b7280;
    text-align: center;
}

@media (max-width: 575.98px) {
    .product-list {
        padding: 0 14px;
    }

    .product-row {
        grid-template-columns:
            68px minmax(0, 1fr);
        gap: 12px;
    }

    .product-image-wrapper {
        width: 68px;
        height: 68px;
    }

    .product-price {
        grid-column: 2;
    }
}
</style>