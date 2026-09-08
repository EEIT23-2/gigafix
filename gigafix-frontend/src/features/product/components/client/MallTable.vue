<script setup>
defineOptions({ name: "MallTable" });

defineProps({
  products: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: "" },
  pageNumber: { type: Number, default: 0 },
  totalPages: { type: Number, default: 0 },
  visiblePages: { type: Array, default: () => [] },
});

const emit = defineEmits(["retry", "change-page", "select-product"]);
const formatter = new Intl.NumberFormat("zh-TW");

function productName(product) {
  return (
    product.product_name ?? product.productName ?? product.name ?? "未命名商品"
  );
}

function productGrade(product) {
  return product.grade ?? "嚴選";
}

function productImage(product) {
  return product.image_url ?? product.imageUrl ?? product.image ?? "";
}

function categoryLabel(category) {
  return (
    { IPHONE: "iPhone", WATCH: "Apple Watch", IPAD: "iPad" }[category] ??
    category ??
    "精選商品"
  );
}
</script>

<template>
  <div v-if="loading" class="loading-state text-center" aria-live="polite">
    <div class="spinner-border" role="status">
      <span class="visually-hidden">載入中</span>
    </div>
    <p>正在載入商品…</p>
  </div>

  <div v-else-if="errorMessage" class="empty-state text-center">
    <i class="bi bi-exclamation-circle"></i>
    <h2>{{ errorMessage }}</h2>
    <button class="btn btn-dark mt-2" type="button" @click="emit('retry')">
      重新載入
    </button>
  </div>

  <div v-else-if="products.length" class="row g-4">
    <div
      v-for="product in products"
      :key="product.productId"
      class="col-sm-6 col-xl-4"
    >
      <article
        class="product-card h-100 text-center"
        tabindex="0"
        @click="emit('select-product', product)"
        @keydown.enter="emit('select-product', product)"
      >
        <div class="product-image">
          <img
            v-if="productImage(product)"
            :src="productImage(product)"
            :alt="productName(product)"
            loading="lazy"
          />
          <i
            v-else
            class="bi bi-image placeholder-image"
            aria-hidden="true"
          ></i>
        </div>
        <span
          class="grade"
          :class="
            productGrade(product).toString().startsWith('A') ? 'grade-a' : ''
          "
          >{{ productGrade(product) }} 級品</span
        >
        <h2>{{ productName(product) }}</h2>
        <p>
          {{ product.appearance || "外觀狀況未標示" }} ・
          {{ categoryLabel(product.category) }}
        </p>
        <strong>NT$ {{ formatter.format(product.price ?? 0) }}</strong>
      </article>
    </div>
  </div>

  <div v-else class="empty-state text-center">
    <i class="bi bi-search"></i>
    <h2>找不到符合條件的商品</h2>
    <p>請調整篩選條件後再試一次。</p>
  </div>

  <nav
    v-if="!loading && !errorMessage && totalPages > 1"
    class="pagination-wrap"
    aria-label="商品分頁"
  >
    <button
      :disabled="pageNumber === 0"
      class="edge-button"
      aria-label="第一頁"
      title="第一頁"
      @click="emit('change-page', 0)"
    >
      <i class="bi bi-chevron-double-left"></i>
    </button>
    <button
      :disabled="pageNumber === 0"
      aria-label="上一頁"
      @click="emit('change-page', pageNumber - 1)"
    >
      <i class="bi bi-chevron-left"></i>
    </button>
    <button
      v-for="page in visiblePages"
      :key="page"
      :class="{ active: page === pageNumber }"
      :aria-current="page === pageNumber ? 'page' : undefined"
      @click="emit('change-page', page)"
    >
      {{ page + 1 }}
    </button>
    <button
      :disabled="pageNumber >= totalPages - 1"
      aria-label="下一頁"
      @click="emit('change-page', pageNumber + 1)"
    >
      <i class="bi bi-chevron-right"></i>
    </button>
    <button
      :disabled="pageNumber >= totalPages - 1"
      class="edge-button"
      aria-label="最後一頁"
      title="最後一頁"
      @click="emit('change-page', totalPages - 1)"
    >
      <i class="bi bi-chevron-double-right"></i>
    </button>
    <span class="page-summary" aria-live="polite"
      >第 {{ pageNumber + 1 }} 頁／共 {{ totalPages }} 頁</span
    >
  </nav>
</template>

<style scoped>
.product-card {
  padding: 24px;
  border-radius: 20px;
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.5s ease,
    box-shadow 0.5s ease;
}
.product-card:hover,
.product-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 30px 60px -15px rgba(0, 0, 0, 0.09);
  outline: none;
}
.product-image {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 256px;
  margin-bottom: 24px;
  overflow: hidden;
  border-radius: 8px;
  background: #f9f9f9;
}
.product-image img {
  width: 75%;
  height: 75%;
  object-fit: contain;
  transition: transform 0.7s ease;
}
.product-card:hover img {
  transform: scale(1.05);
}
.placeholder-image {
  color: #c6c6c6;
  font-size: 48px;
}
.grade {
  display: block;
  margin-bottom: 8px;
  color: #7e7576;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.grade-a {
  color: #005ab7;
}
.product-card h2 {
  margin: 0 0 4px;
  color: #000;
  font-size: 24px;
  font-weight: 600;
}
.product-card p {
  margin: 0 0 16px;
  color: #635d5e;
}
.product-card strong {
  color: #000;
  font-size: 24px;
  font-weight: 600;
}
.loading-state,
.empty-state {
  margin: 80px 0;
  color: #635d5e;
}
.loading-state p {
  margin-top: 16px;
}
.empty-state i {
  font-size: 42px;
}
.empty-state h2 {
  margin-top: 16px;
  color: #1b1b1b;
  font-size: 24px;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 64px;
}
.pagination-wrap button {
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 50%;
  color: #1b1b1b;
  background: transparent;
}
.pagination-wrap button:first-child,
.pagination-wrap button:last-child {
  border-color: #cfc4c5;
  color: #635d5e;
}
.pagination-wrap button:hover {
  background: #e8e8e8;
}
.pagination-wrap button.active {
  color: #fff;
  background: #000;
}
.pagination-wrap button:disabled {
  opacity: 0.35;
  pointer-events: none;
}
.page-summary {
  margin-left: 8px;
  color: #635d5e;
  font-size: 14px;
  white-space: nowrap;
}
@media (max-width: 575.98px) {
  .pagination-wrap {
    gap: 6px;
  }
  .pagination-wrap button {
    width: 36px;
    height: 36px;
  }
  .page-summary {
    width: 100%;
    margin: 8px 0 0;
    text-align: center;
  }
}
</style>
