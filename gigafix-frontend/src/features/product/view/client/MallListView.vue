<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import {
  addCartItem,
  getCartItems,
} from "@/features/cart/api/cartApi.js";
import { useFetchMemberInfoStore } from "@/stores/member";
import { getProducts } from "../../api.js";
import MallTable from "../../components/client/MallTable.vue";
import ProductCartDrawer from "../../components/client/ProductCartDrawer.vue";

const fetchMemberInfoStore = useFetchMemberInfoStore();
const { memberInfo } = storeToRefs(fetchMemberInfoStore);
const router = useRouter();

const openLoginModal = () => {
  const loginButton = document.querySelector(
    ".user-actions button.action-item",
  );
  loginButton?.click();
};

const categories = [
  { id: "IPHONE", label: "iPhone", icon: "bi-phone" },
  { id: "WATCH", label: "Apple Watch", icon: "bi-smartwatch" },
  { id: "IPAD", label: "iPad", icon: "bi-tablet" },
];

const products = ref([]);
const selectedCategory = ref("IPHONE");
const sort = ref("newest");
const filters = reactive({
  search: "",
  modelName: "",
  storage: "",
  color: "",
  min: "",
  max: "",
});
const pageNumber = ref(0);
const pageSize = ref(12);
const totalPages = ref(0);
const totalElements = ref(0);
const loading = ref(false);
const errorMessage = ref("");
const cartMessage = ref("");
const cartMessageType = ref("success");
const cartDrawerOpen = ref(false);
const cartRefreshKey = ref(0);
const cartProductIds = ref(new Set());
const addingProductIds = ref(new Set());
let requestSequence = 0;
let debounceTimer;
let cartMessageTimer;
let suppressFilterWatch = false;

const selectedCategoryLabel = computed(
  () =>
    categories.find((item) => item.id === selectedCategory.value)?.label ??
    "商品",
);
const sortParams = computed(
  () =>
    ({
      newest: { orderBy: "createdTime", sort: "desc" },
      oldest: { orderBy: "createdTime", sort: "asc" },
      low: { orderBy: "price", sort: "asc" },
      high: { orderBy: "price", sort: "desc" },
    })[sort.value],
);
const visiblePages = computed(() => {
  const start = Math.max(
    0,
    Math.min(pageNumber.value - 2, totalPages.value - 5),
  );
  return Array.from(
    { length: Math.min(5, totalPages.value) },
    (_, index) => start + index,
  );
});

const hasActiveFilters = computed(
  () =>
    selectedCategory.value !== "IPHONE" ||
    sort.value !== "newest" ||
    Object.values(filters).some((value) => value !== ""),
);

function compactParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== "" && value !== null && value !== undefined,
    ),
  );
}

const productIdKey = (productId) => String(productId);

async function loadCartProductIds() {
  if (!memberInfo.value) {
    cartProductIds.value = new Set();
    return;
  }

  try {
    const response = await getCartItems();
    cartProductIds.value = new Set(
      (response.data ?? []).map((item) => productIdKey(item.productId)),
    );
  } catch (error) {
    cartProductIds.value = new Set();
    console.error("購物車商品載入失敗", error);
  }
}

// 阻擋鍵盤直接輸入負號，避免價格欄位出現負數。
function preventNegativePriceKey(event) {
  if (event.key === "-" || event.key === "Subtract") {
    event.preventDefault();
  }
}

// 貼上或其他輸入方式仍可能帶入負數，因此輸入後再次修正為 0。
function normalizePriceFilter(field) {
  const value = filters[field];
  if (value === "" || value == null) return;

  const numericValue = Number(value);
  filters[field] = Number.isFinite(numericValue)
    ? Math.max(0, numericValue)
    : "";
}

async function fetchProducts() {
  const currentRequest = ++requestSequence;
  loading.value = true;
  errorMessage.value = "";
  try {
    const page = await getProducts(
      compactParams({
        category: selectedCategory.value,
        saleStatus: "AVAILABLE",
        search: filters.search.trim(),
        modelName: filters.modelName.trim(),
        color: filters.color,
        storage: filters.storage,
        minPrice: filters.min,
        maxPrice: filters.max,
        orderBy: sortParams.value.orderBy,
        sort: sortParams.value.sort,
        limit: pageSize.value,
        offset: pageNumber.value * pageSize.value,
      }),
    );
    if (currentRequest !== requestSequence) return;
    products.value = page.content ?? [];
    totalPages.value = page.totalPages ?? 0;
    totalElements.value = page.totalElements ?? 0;
    pageNumber.value = page.number ?? pageNumber.value;
  } catch (error) {
    if (currentRequest !== requestSequence) return;
    products.value = [];
    totalPages.value = 0;
    errorMessage.value =
      error?.response?.data?.message ?? "商品資料載入失敗，請稍後再試。";
    console.error("取得商城商品失敗：", error);
  } finally {
    if (currentRequest === requestSequence) loading.value = false;
  }
}

function changePage(page) {
  if (page < 0 || page >= totalPages.value || page === pageNumber.value) return;
  pageNumber.value = page;
  fetchProducts();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

// 回復商城預設分類與排序，並清空文字、規格及價格篩選條件。
async function clearAllFilters() {
  suppressFilterWatch = true;
  clearTimeout(debounceTimer);
  selectedCategory.value = "IPHONE";
  sort.value = "newest";
  Object.assign(filters, {
    search: "",
    modelName: "",
    storage: "",
    color: "",
    min: "",
    max: "",
  });
  pageNumber.value = 0;

  // 等待本輪 watch 完成且略過其請求，再只重新載入一次完整商品列表。
  await nextTick();
  suppressFilterWatch = false;
  fetchProducts();
}

function handleSelectProduct(product) {
  router.push({
    name: "mall-detail",
    params: { productId: product.productId },
  });
}

const addToCart = async (product) => {
  if (!memberInfo.value) {
    openLoginModal();
    return;
  }

  const productId = productIdKey(product.productId);
  if (
    cartProductIds.value.has(productId) ||
    addingProductIds.value.has(productId)
  ) {
    return;
  }

  clearTimeout(cartMessageTimer);
  const name =
    product.product_name ?? product.productName ?? product.name ?? "商品";
  addingProductIds.value = new Set(addingProductIds.value).add(productId);
  try {
    await addCartItem(product.productId);
    cartProductIds.value = new Set(cartProductIds.value).add(productId);
    cartMessageType.value = "success";
    cartMessage.value = `${name} 已加入購物車`;
    cartRefreshKey.value += 1;
  } catch (error) {
    cartMessageType.value = "danger";
    cartMessage.value =
      error?.response?.data?.message ?? `${name} 加入購物車失敗，請稍後再試`;
  } finally {
    const nextAddingProductIds = new Set(addingProductIds.value);
    nextAddingProductIds.delete(productId);
    addingProductIds.value = nextAddingProductIds;
  }
  cartMessageTimer = setTimeout(() => {
    cartMessage.value = "";
  }, 3000);
};

const openCartDrawer = () => {
  cartRefreshKey.value += 1;
  cartDrawerOpen.value = true;
};

watch([selectedCategory, sort, pageSize], () => {
  if (suppressFilterWatch) return;
  pageNumber.value = 0;
  fetchProducts();
});
watch(
  filters,
  () => {
    if (suppressFilterWatch) return;
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      pageNumber.value = 0;
      fetchProducts();
    }, 350);
  },
  { deep: true },
);
watch(memberInfo, loadCartProductIds, { immediate: true });
onMounted(fetchProducts);
onBeforeUnmount(() => {
  clearTimeout(debounceTimer);
  clearTimeout(cartMessageTimer);
});
</script>

<template>
  <ProductCartDrawer
    :open="cartDrawerOpen"
    :refresh-key="cartRefreshKey"
    :show-trigger="Boolean(memberInfo)"
    @open="openCartDrawer"
    @close="cartDrawerOpen = false"
  />

  <main class="container-xxl shop-layout">
    <div class="row g-4">
      <aside class="col-lg-3 d-none d-lg-block">
        <div class="filter-panel sticky-top">
          <div class="mb-4">
            <h2 class="filter-title">篩選商品</h2>
            <p class="text-muted mb-0">快速找到理想的裝置</p>
          </div>
          <nav class="nav flex-column category-nav mb-4">
            <button
              v-for="item in categories"
              :key="item.id"
              class="nav-link text-start"
              :class="{ active: selectedCategory === item.id }"
              @click="selectedCategory = item.id"
            >
              <i class="bi me-3" :class="item.icon"></i>{{ item.label }}
            </button>
          </nav>
          <div class="filter-group">
            <h3>關鍵字搜尋</h3>
            <input
              v-model="filters.search"
              class="form-control"
              type="search"
              placeholder="搜尋商品"
            />
          </div>
          <div class="filter-group">
            <h3>型號名稱</h3>
            <input
              v-model="filters.modelName"
              class="form-control"
              type="search"
              placeholder="例如：iPhone 15"
            />
          </div>
          <div class="filter-group">
            <h3>儲存容量</h3>
            <select v-model="filters.storage" class="form-select">
              <option value="">全部容量</option>
              <option
                v-for="value in ['64G', '128G', '256G', '512G']"
                :key="value"
                :value="value"
              >
                {{ value }}
              </option>
            </select>
          </div>
          <div class="filter-group">
            <h3>顏色</h3>
            <select v-model="filters.color" class="form-select">
              <option value="">全部顏色</option>
              <option
                v-for="value in [
                  '紫',
                  '星空',
                  '午夜',
                  '銀',
                  '綠',
                  '藍',
                  '紅',
                  '黑',
                ]"
                :key="value"
                :value="value"
              >
                {{ value }}
              </option>
            </select>
          </div>
          <div class="filter-group">
            <h3>價格範圍</h3>
            <label class="small text-muted">最低價格</label
            ><input
              v-model.number="filters.min"
              class="form-control mb-3"
              type="number"
              min="0"
              step="1"
              inputmode="numeric"
              placeholder="NT$"
              @keydown="preventNegativePriceKey"
              @input="normalizePriceFilter('min')"
            /><label class="small text-muted">最高價格</label
            ><input
              v-model.number="filters.max"
              class="form-control"
              type="number"
              min="0"
              step="1"
              inputmode="numeric"
              placeholder="NT$"
              @keydown="preventNegativePriceKey"
              @input="normalizePriceFilter('max')"
            />
          </div>
        </div>
      </aside>

      <section class="col-lg-9">
        <div
          v-if="cartMessage"
          class="alert cart-message"
          :class="`alert-${cartMessageType}`"
          role="status"
          aria-live="polite"
        >
          <i class="bi bi-check-circle me-2" aria-hidden="true"></i>
          {{ cartMessage }}
        </div>
        <header
          class="page-header d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-end gap-4"
        >
          <div>
            <h1>{{ selectedCategoryLabel }}</h1>
            <p>
              探索經專業檢測、品質有保障的嚴選二手裝置，共
              {{ totalElements }} 件。
            </p>
          </div>
          <div class="toolbar d-flex flex-wrap align-items-center gap-3">
            <button
              type="button"
              class="btn btn-outline-secondary clear-filter-button"
              :disabled="!hasActiveFilters || loading"
              @click="clearAllFilters"
            >
              <i class="bi bi-x-circle me-1" aria-hidden="true"></i>
              清除篩選
            </button>
            <label class="page-size-selector d-flex align-items-center gap-2">
              <span>每頁顯示：</span>
              <select
                v-model.number="pageSize"
                class="form-select fw-semibold"
                aria-label="每頁顯示筆數"
              >
                <option :value="12">12 筆</option>
                <option :value="9">9 筆</option>
                <option :value="6">6 筆</option>
              </select>
            </label>
            <label class="sorter d-flex align-items-center">
              <span>排序：</span>
              <select v-model="sort" class="form-select border-0 fw-semibold">
                <option value="newest">最新上架</option>
                <option value="oldest">較早上架</option>
                <option value="low">價格：低至高</option>
                <option value="high">價格：高至低</option>
              </select>
            </label>
          </div>
        </header>
        <MallTable
          :products="products"
          :loading="loading"
          :error-message="errorMessage"
          :page-number="pageNumber"
          :total-pages="totalPages"
          :visible-pages="visiblePages"
          :cart-product-ids="cartProductIds"
          :adding-product-ids="addingProductIds"
          @retry="fetchProducts"
          @change-page="changePage"
          @select-product="handleSelectProduct"
          @add-to-cart="addToCart"
        />
      </section>
    </div>
  </main>
</template>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;500;600;700&family=Noto+Sans+TC:wght@400;500;600;700&display=swap");

.shop-layout {
  --ink: #1b1b1b;
  --muted: #635d5e;
  --soft: #f9f9f9;
  --blue: #005ab7;
  padding: 64px 5rem 80px;
  color: var(--ink);
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
  font-size: 17px;
}
.filter-panel {
  top: 40px;
  max-height: calc(100vh - 70px);
  overflow: auto;
  padding-right: 16px;
}
.filter-title {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
}
.category-nav .nav-link {
  border: 0;
  border-left: 2px solid transparent;
  border-radius: 0 8px 8px 0;
  padding: 12px 16px;
  color: var(--muted);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  background: transparent;
}
.category-nav .nav-link:hover {
  color: #000;
  background: #f3f3f3;
}
.category-nav .nav-link.active {
  color: #000;
  border-left-color: #000;
  background: var(--soft);
}
.filter-group {
  border-top: 1px solid #e2e2e2;
  margin-top: 28px;
  padding-top: 28px;
  color: var(--muted);
}
.filter-group h3 {
  margin-bottom: 16px;
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
}
.form-check-input:checked {
  background-color: var(--blue);
  border-color: var(--blue);
}
.form-control {
  background: #f3f3f3;
  border-color: #cfc4c5;
}
.page-header {
  margin-bottom: 64px;
}
.cart-message {
  position: sticky;
  top: 16px;
  z-index: 10;
  margin-bottom: 24px;
}
.page-header h1 {
  margin: 0 0 8px;
  color: #000;
  font-size: 64px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.page-header p {
  margin: 0;
  color: var(--muted);
  font-size: 19px;
}
.sorter {
  color: var(--muted);
  font-size: 14px;
  white-space: nowrap;
}
.sorter .form-select {
  width: auto;
  padding-right: 2.5rem;
  background-color: transparent;
}
.toolbar {
  color: var(--muted);
  font-size: 14px;
  white-space: nowrap;
}
.clear-filter-button {
  white-space: nowrap;
}
.page-size-selector .form-select {
  width: auto;
  min-width: 92px;
  background-color: #f9f9f9;
  border-color: #e2e2e2;
}
.product-card {
  padding: 24px;
  border-radius: 20px;
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.5s ease,
    box-shadow 0.5s ease;
}
.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 30px 60px -15px rgba(0, 0, 0, 0.09);
}
.product-image {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 256px;
  margin-bottom: 24px;
  overflow: hidden;
  border-radius: 8px;
  background: var(--soft);
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
.grade {
  display: block;
  margin-bottom: 8px;
  color: #7e7576;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.grade-a {
  color: var(--blue);
}
.product-card h2 {
  margin: 0 0 4px;
  color: #000;
  font-size: 24px;
  font-weight: 600;
}
.product-card p {
  margin: 0 0 16px;
  color: var(--muted);
}
.product-card strong {
  color: #000;
  font-size: 24px;
  font-weight: 600;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 64px;
}
.pagination-wrap button {
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 50%;
  color: var(--ink);
  background: transparent;
}
.pagination-wrap button:first-child,
.pagination-wrap button:last-child {
  border-color: #cfc4c5;
  color: var(--muted);
}
.pagination-wrap button:hover {
  background: #e8e8e8;
}
.pagination-wrap .active {
  color: #fff;
  background: #000;
}
.empty-state {
  margin: 80px 0;
  color: var(--muted);
}
.empty-state i {
  font-size: 42px;
}
.empty-state h2 {
  margin-top: 16px;
  color: var(--ink);
  font-size: 24px;
}
.loading-state {
  margin: 80px 0;
  color: var(--muted);
}
.loading-state p {
  margin-top: 16px;
}
.placeholder-image {
  color: #c6c6c6;
  font-size: 48px;
}
.pagination-wrap button:disabled {
  opacity: 0.35;
  pointer-events: none;
}
@media (max-width: 991.98px) {
  .shop-layout {
    padding: 56px 32px 72px;
  }
}
@media (max-width: 767.98px) {
  .shop-layout {
    padding: 40px 20px 64px;
  }
  .page-header {
    margin-bottom: 48px;
  }
  .page-header h1 {
    font-size: 40px;
  }
  .page-header p {
    font-size: 17px;
  }
}
</style>
