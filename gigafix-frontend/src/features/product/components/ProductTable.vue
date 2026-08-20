<script setup>
// 此元件只負責顯示表格，不直接呼叫 API。
const props = defineProps({
  products: { type: Array, default: () => [] },
  currency: { type: String, default: "TWD" },
});

// 將使用者的操作通知 ProductListView，由父層決定導頁或呼叫 API。
const emit = defineEmits(["view", "edit", "delete"]);

const currencyMap = {
  TWD: { field: "price", locale: "zh-TW", code: "TWD" },
  USD: { field: "priceUSD", locale: "en-US", code: "USD" },
  JPY: { field: "priceJPY", locale: "ja-JP", code: "JPY" },
};

const saleStatusMap = {
  OFF_SHELF: { label: "已下架", badgeClass: "text-bg-secondary" },
  AVAILABLE: { label: "可販售（待售中）", badgeClass: "text-bg-success" },
  RESERVED: { label: "已保留", badgeClass: "text-bg-warning" },
  SOLD: { label: "已售出", badgeClass: "text-bg-danger" },
};

function formatProductPrice(product) {
  const setting = currencyMap[props.currency] ?? currencyMap.TWD;
  const value = product?.[setting.field];

  return new Intl.NumberFormat(setting.locale, {
    style: "currency",
    currency: setting.code,
    minimumFractionDigits: setting.code === "USD" ? 2 : 0,
    maximumFractionDigits: setting.code === "USD" ? 2 : 0,
  }).format(value ?? 0);
}

function formatDate(value) {
  return value?.slice(0, 10) ?? "-";
}

function gradeClass(grade) {
  if (grade?.startsWith("S")) return "text-bg-success";
  if (grade?.startsWith("A")) return "text-bg-primary";
  return "text-bg-secondary";
}

function saleStatusText(status) {
  return saleStatusMap[status]?.label ?? status ?? "未知狀態";
}

function saleStatusClass(status) {
  return saleStatusMap[status]?.badgeClass ?? "text-bg-dark";
}

function useFallbackImage(event) {
  event.target.src = "https://placehold.co/48x48?text=N/A";
}
</script>

<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th>圖片</th>
          <th>型號</th>
          <th>類別</th>
          <th>等級</th>
          <th class="text-end">價格（{{ currency }}）</th>
          <th>狀態</th>
          <th>上架日期</th>
          <th class="text-end">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="product in products" :key="product.productId">
          <td>
            <img
              :src="product.image_url"
              :alt="product.product_name"
              class="product-image rounded border"
              @error="useFallbackImage"
            />
          </td>
          <td>
            <!-- 商品名稱可點擊，通知父層導向商品詳情頁。 -->
            <button
              class="product-name-button fw-semibold"
              type="button"
              @click="emit('view', product)"
            >
              {{ product.product_name }}
            </button>
            <small class="text-secondary">{{ product.appearance }}</small>
          </td>
          <td>{{ product.category }}</td>
          <td>
            <span class="badge" :class="gradeClass(product.grade)">{{
              product.grade
            }}</span>
          </td>
          <td class="text-end font-monospace">
            {{ formatProductPrice(product) }}
          </td>
          <td>
            <span class="badge" :class="saleStatusClass(product.sale_status)">
              {{ saleStatusText(product.sale_status) }}
            </span>
          </td>
          <td>{{ formatDate(product.createdDate) }}</td>
          <td class="text-end text-nowrap">
            <button
              class="btn btn-sm btn-outline-primary me-1"
              @click="emit('edit', product)"
            >
              編輯
            </button>
            <button
              class="btn btn-sm btn-outline-danger"
              @click="emit('delete', product)"
            >
              刪除
            </button>
          </td>
        </tr>
        <tr v-if="products.length === 0">
          <td colspan="8" class="text-center text-secondary py-5">
            沒有商品資料
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.product-image {
  width: 48px;
  height: 48px;
  object-fit: cover;
  background: #eef2f7;
}
.product-name-button {
  display: block;
  margin: 0 0 0.125rem;
  padding: 0;
  color: #111827;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}
.product-name-button:hover {
  color: #0d6efd;
  text-decoration: underline;
}
.product-name-button:focus-visible {
  outline: 2px solid #0d6efd;
  outline-offset: 3px;
  border-radius: 2px;
}
th {
  white-space: nowrap;
  font-size: 0.8rem;
  letter-spacing: 0.03em;
}
td {
  font-size: 0.875rem;
}
</style>
