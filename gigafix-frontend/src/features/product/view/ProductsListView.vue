<script setup>
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
// View 不再直接 import axios；所有商品 API 都由 api.js 提供。
import {
  deleteAllProducts,
  deleteProduct,
  exportProducts,
  getProducts,
  importProducts,
} from "../api";
import { useProductStore } from "../store";
import ProductAdvancedFilter from "../components/ProductAdvancedFilter.vue";
import ProductTable from "../components/ProductTable.vue";

const router = useRouter();
const productStore = useProductStore();

// 使用 storeToRefs 保持解構後的 Pinia state 仍具有響應性。
// 這些值在切換至詳情、新增或編輯頁後不會消失。
const {
  page,
  size,
  keyword,
  category,
  saleStatus,
  modelName,
  color,
  storage,
  sortOption,
  minPrice,
  maxPrice,
  currency,
} = storeToRefs(productStore);

const products = ref([]);
const totalElements = ref(0);
const totalPages = ref(0);
const loading = ref(false);
const deletingId = ref(null);
const deletingAll = ref(false);
const showDeleteAllConfirm = ref(false);
const importing = ref(false);
const exporting = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const showAdvancedFilter = ref(false);

const advancedFilterCount = computed(
  () => [modelName.value, color.value, storage.value].filter(Boolean).length,
);

// 計算第一頁與最後一頁之間要顯示的頁碼。
// 頁數少時全部顯示；頁數多時只顯示目前頁附近的頁碼。
const middlePages = computed(() => {
  if (totalPages.value <= 2) return [];

  // 總頁數不超過 7 時，不需要省略號。
  if (totalPages.value <= 7) {
    return Array.from(
      { length: totalPages.value - 2 },
      (_, index) => index + 1,
    );
  }

  // 目前位於前段：1 2 3 4 5 … 最後一頁。
  if (page.value <= 3) return [1, 2, 3, 4];

  // 目前位於後段：第一頁 … 倒數第 5 頁至最後一頁。
  if (page.value >= totalPages.value - 4) {
    return Array.from(
      { length: 4 },
      (_, index) => totalPages.value - 5 + index,
    );
  }

  // 目前位於中段：第一頁 … 前一頁 目前頁 下一頁 … 最後一頁。
  return [page.value - 1, page.value, page.value + 1];
});

// 是否在第一頁與中間頁碼之間顯示省略號。
const showLeadingEllipsis = computed(
  () => totalPages.value > 7 && page.value > 3,
);

// 是否在中間頁碼與最後一頁之間顯示省略號。
const showTrailingEllipsis = computed(
  () => totalPages.value > 7 && page.value < totalPages.value - 4,
);

const startItem = computed(() =>
  totalElements.value ? page.value * size.value + 1 : 0,
);
const endItem = computed(() =>
  Math.min((page.value + 1) * size.value, totalElements.value),
);

// 將目前畫面條件轉成後端 ProductQueryParams。
function buildQueryParams(targetPage) {
  // 將下拉選單值拆成後端 ProductQueryParams 使用的 orderBy 與 sort。
  const [orderBy, sort] = sortOption.value ? sortOption.value.split(":") : [];

  return {
    limit: size.value,
    offset: targetPage * size.value,
    ...(keyword.value && { search: keyword.value }),
    ...(category.value && { category: category.value }),
    ...(saleStatus.value && { saleStatus: saleStatus.value }),
    ...(modelName.value && { modelName: modelName.value }),
    ...(color.value && { color: color.value }),
    ...(storage.value && { storage: storage.value }),
    ...(orderBy && sort && { orderBy, sort }),
    ...(minPrice.value != null &&
      minPrice.value !== "" && { minPrice: minPrice.value }),
    ...(maxPrice.value != null &&
      maxPrice.value !== "" && { maxPrice: maxPrice.value }),
  };
}

async function fetchProducts(targetPage = 0) {
  if (
    minPrice.value != null &&
    maxPrice.value != null &&
    minPrice.value > maxPrice.value
  ) {
    errorMessage.value = "最低價格不能高於最高價格";
    return;
  }

  loading.value = true;
  errorMessage.value = "";

  try {
    // api.js 已直接 return response.data，所以 data 就是 Page<Product>。
    const data = await getProducts(buildQueryParams(targetPage));
    products.value = data.content ?? [];
    page.value = data.number ?? 0;
    size.value = data.size ?? size.value;
    totalElements.value = data.totalElements ?? 0;
    totalPages.value = data.totalPages ?? 0;
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `讀取失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

function changePage(targetPage) {
  if (
    targetPage >= 0 &&
    targetPage < totalPages.value &&
    targetPage !== page.value
  ) {
    fetchProducts(targetPage);
  }
}

function applyAdvancedFilters(filters) {
  modelName.value = filters.modelName;
  color.value = filters.color;
  storage.value = filters.storage;
  showAdvancedFilter.value = false;
  fetchProducts(0);
}

function resetAdvancedFilters() {
  modelName.value = "";
  color.value = "";
  storage.value = "";
  showAdvancedFilter.value = false;
  fetchProducts(0);
}

function goToCreate() {
  // route name 需與 adminRoutes.js 的新增商品路由一致。
  router.push({ name: "admin-product-create" });
}

function goToEdit(product) {
  // ProductTable 只發出 edit 事件，實際導頁由 View 負責。
  router.push({
    name: "admin-product-edit",
    params: { productId: product.productId },
  });
}

function goToDetail(product) {
  router.push({
    name: "admin-product-detail",
    params: { productId: product.productId },
  });
}

async function handleDelete(product) {
  if (!window.confirm(`確定要刪除「${product.product_name}」嗎？`)) return;

  deletingId.value = product.productId;
  errorMessage.value = "";

  try {
    await deleteProduct(product.productId);

    // 若刪掉目前頁最後一筆，改抓上一頁；否則重抓目前頁。
    const targetPage =
      products.value.length === 1 && page.value > 0
        ? page.value - 1
        : page.value;
    await fetchProducts(targetPage);
  } catch (error) {
    console.error(error);
    errorMessage.value =
      error.response?.status === 404
        ? "商品不存在或已被刪除"
        : `刪除失敗${error.response?.status ? `：HTTP ${error.response.status}` : ""}`;
  } finally {
    deletingId.value = null;
  }
}

// 呼叫後端 POST /api/products/import，匯入後端預先設定的 JSON 資料來源。
async function handleImport() {
  const confirmed = window.confirm(
    "確定要從後端設定的 JSON 資料來源匯入商品嗎？",
  );

  if (!confirmed) return;

  importing.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const result = await importProducts();
    const count = result?.productCount ?? 0;
    successMessage.value = result?.message
      ? `${result.message}，共匯入 ${count} 筆商品`
      : `商品匯入完成，共匯入 ${count} 筆商品`;

    // 匯入後重新取得第一頁與最新總筆數。
    await fetchProducts(0);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `匯入失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    importing.value = false;
  }
}

// 呼叫 GET /api/products/export，將後端回傳的 Blob 下載成 JSON 檔。
async function handleExport() {
  exporting.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const blobData = await exportProducts();
    const blob =
      blobData instanceof Blob
        ? blobData
        : new Blob([blobData], { type: "application/json" });
    const downloadUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = downloadUrl;
    link.download = `products-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(downloadUrl);

    successMessage.value = "商品 JSON 已成功匯出";
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `匯出失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    exporting.value = false;
  }
}

// 第一次點擊只顯示警告，不會立刻刪除資料。
function openDeleteAllConfirm() {
  errorMessage.value = "";
  successMessage.value = "";
  showDeleteAllConfirm.value = true;
}

function closeDeleteAllConfirm() {
  showDeleteAllConfirm.value = false;
}

// 使用者在警告區再次確認後，才呼叫 DELETE /api/products。
async function handleDeleteAll() {
  deletingAll.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    await deleteAllProducts();
    showDeleteAllConfirm.value = false;
    successMessage.value = "所有商品已刪除";

    // 清空畫面並重新向後端確認最新列表狀態。
    products.value = [];
    page.value = 0;
    totalElements.value = 0;
    totalPages.value = 0;
    await fetchProducts(0);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `刪除所有商品失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    deletingAll.value = false;
  }
}

// 回到列表頁時使用 Pinia 保存的頁碼與條件重新查詢。
onMounted(() => fetchProducts(page.value));
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="mx-auto content-width">
      <header
        class="d-flex flex-column flex-md-row align-items-md-end justify-content-between gap-3 mb-4"
      >
        <div>
          <div class="d-flex align-items-center gap-3">
            <h1 class="fw-bold mb-0">商品管理</h1>
            <span class="badge rounded-pill text-bg-light border"
              >Total: {{ totalElements }}</span
            >
          </div>
          <p class="text-secondary mb-0 mt-1">
            Manage and track pre-owned device inventory.
          </p>
        </div>
        <div class="d-flex flex-wrap align-items-center gap-2">
          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="importing || exporting"
            @click="handleImport"
          >
            <span
              v-if="importing"
              class="spinner-border spinner-border-sm me-1"
              aria-hidden="true"
            ></span>
            {{ importing ? "匯入中..." : "匯入 JSON" }}
          </button>

          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="importing || exporting"
            @click="handleExport"
          >
            <span
              v-if="exporting"
              class="spinner-border spinner-border-sm me-1"
              aria-hidden="true"
            ></span>
            {{ exporting ? "匯出中..." : "匯出 JSON" }}
          </button>

          <button
            class="btn btn-outline-danger"
            type="button"
            :disabled="
              importing || exporting || deletingAll || totalElements === 0
            "
            @click="openDeleteAllConfirm"
          >
            刪除所有商品
          </button>

          <button class="btn btn-primary" type="button" @click="goToCreate">
            ＋ 新增商品
          </button>
        </div>
      </header>

      <section class="card mb-4">
        <div
          class="card-body d-flex flex-column flex-md-row flex-md-wrap gap-3"
        >
          <input
            v-model.trim="keyword"
            class="form-control search-input"
            type="search"
            placeholder="搜尋商品名稱..."
            @keyup.enter="fetchProducts(0)"
          />
          <select
            v-model="category"
            class="form-select filter-select"
            @change="fetchProducts(0)"
          >
            <option value="">所有類別</option>
            <option value="IPHONE">iPhone</option>
            <option value="WATCH">Apple Watch</option>
            <option value="IPAD">iPad</option>
          </select>
          <select
            v-model="saleStatus"
            class="form-select filter-select"
            @change="fetchProducts(0)"
          >
            <option value="">所有狀態</option>
            <option value="AVAILABLE">可販售（待售中）</option>
            <option value="OFF_SHELF">已下架</option>
            <option value="RESERVED">已保留</option>
            <option value="SOLD">已售出</option>
          </select>
          <select
            v-model="sortOption"
            class="form-select filter-select"
            @change="fetchProducts(0)"
          >
            <option value="">預設排序</option>
            <option value="createdDate:desc">上架日期：新到舊</option>
            <option value="createdDate:asc">上架日期：舊到新</option>
            <option value="price:asc">價格：低到高</option>
            <option value="price:desc">價格：高到低</option>
          </select>
          <div class="input-group price-range">
            <span class="input-group-text">NT$</span>
            <input
              v-model.number="minPrice"
              class="form-control"
              type="number"
              min="0"
              placeholder="最低價"
              @keyup.enter="fetchProducts(0)"
            />
            <span class="input-group-text">－</span>
            <input
              v-model.number="maxPrice"
              class="form-control"
              type="number"
              min="0"
              placeholder="最高價"
              @keyup.enter="fetchProducts(0)"
            />
          </div>
          <select v-model="currency" class="form-select filter-select">
            <option value="TWD">新台幣（TWD）</option>
            <option value="USD">美元（USD）</option>
            <option value="JPY">日圓（JPY）</option>
          </select>
          <select
            v-model.number="size"
            class="form-select page-size-select"
            @change="fetchProducts(0)"
          >
            <option :value="48">每頁 48 筆</option>
            <option :value="36">每頁 36 筆</option>
            <option :value="12">每頁 12 筆</option>
          </select>
          <button
            class="btn btn-outline-primary text-nowrap"
            @click="fetchProducts(0)"
          >
            搜尋
          </button>
          <button
            class="btn btn-outline-secondary text-nowrap position-relative"
            type="button"
            @click="showAdvancedFilter = true"
          >
            進階篩選
            <span
              v-if="advancedFilterCount"
              class="position-absolute top-0 start-100 translate-middle badge rounded-pill text-bg-primary"
            >
              {{ advancedFilterCount }}
              <span class="visually-hidden">個進階篩選條件</span>
            </span>
          </button>
        </div>
      </section>

      <ProductAdvancedFilter
        v-if="showAdvancedFilter"
        :model-name="modelName"
        :color="color"
        :storage="storage"
        @apply="applyAdvancedFilters"
        @reset="resetAdvancedFilters"
        @close="showAdvancedFilter = false"
      />

      <!-- 危險操作的第二次確認區；按下確認前不會呼叫刪除 API。 -->
      <div
        v-if="showDeleteAllConfirm"
        class="alert alert-danger border-danger d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3"
        role="alert"
      >
        <div>
          <div class="fw-bold mb-1">警告：確定要刪除所有商品嗎？</div>
          <div>
            此操作會刪除資料庫內目前全部
            {{ totalElements }} 筆商品，而且無法復原。
          </div>
        </div>

        <div class="d-flex flex-shrink-0 gap-2">
          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="deletingAll"
            @click="closeDeleteAllConfirm"
          >
            取消
          </button>
          <button
            class="btn btn-danger"
            type="button"
            :disabled="deletingAll"
            @click="handleDeleteAll"
          >
            <span
              v-if="deletingAll"
              class="spinner-border spinner-border-sm me-1"
              aria-hidden="true"
            ></span>
            {{ deletingAll ? "刪除中..." : "確認全部刪除" }}
          </button>
        </div>
      </div>

      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>
      <div
        v-if="successMessage"
        class="alert alert-success alert-dismissible"
        role="alert"
      >
        {{ successMessage }}
        <button
          class="btn-close"
          type="button"
          aria-label="關閉成功訊息"
          @click="successMessage = ''"
        ></button>
      </div>

      <section class="card overflow-hidden">
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <div class="mt-2">載入中...</div>
        </div>

        <!-- 資料透過 props 傳入表格；操作透過 emits 傳回此 View。 -->
        <ProductTable
          v-else
          :products="products"
          :currency="currency"
          @view="goToDetail"
          @edit="goToEdit"
          @delete="handleDelete"
        />

        <footer
          class="card-footer bg-white d-flex flex-column flex-sm-row align-items-center justify-content-between gap-3 py-3"
        >
          <div
            class="text-secondary d-flex flex-column flex-md-row gap-md-3 text-center text-md-start"
          >
            <span
              >顯示 {{ startItem }} 至 {{ endItem }} 筆，共
              {{ totalElements }} 筆</span
            >
            <span
              >第 {{ totalPages > 0 ? page + 1 : 0 }} /
              {{ totalPages }} 頁</span
            >
          </div>
          <nav aria-label="商品分頁">
            <ul class="pagination pagination-sm mb-0">
              <!-- 直接回到第一頁。 -->
              <li class="page-item" :class="{ disabled: page === 0 }">
                <button class="page-link" @click="changePage(0)">最前頁</button>
              </li>

              <li class="page-item" :class="{ disabled: page === 0 }">
                <button class="page-link" @click="changePage(page - 1)">
                  上一頁
                </button>
              </li>

              <!-- 第一頁永遠顯示，讓使用者能直接回到最前面。 -->
              <li
                v-if="totalPages > 0"
                class="page-item"
                :class="{ active: page === 0 }"
              >
                <button class="page-link" @click="changePage(0)">1</button>
              </li>

              <li
                v-if="showLeadingEllipsis"
                class="page-item disabled"
                aria-hidden="true"
              >
                <span class="page-link">…</span>
              </li>

              <li
                v-for="pageNumber in middlePages"
                :key="pageNumber"
                class="page-item"
                :class="{ active: pageNumber === page }"
              >
                <button class="page-link" @click="changePage(pageNumber)">
                  {{ pageNumber + 1 }}
                </button>
              </li>

              <li
                v-if="showTrailingEllipsis"
                class="page-item disabled"
                aria-hidden="true"
              >
                <span class="page-link">…</span>
              </li>

              <!-- 最後一頁永遠顯示，讓使用者能直接跳到最末頁。 -->
              <li
                v-if="totalPages > 1"
                class="page-item"
                :class="{ active: page === totalPages - 1 }"
              >
                <button class="page-link" @click="changePage(totalPages - 1)">
                  {{ totalPages }}
                </button>
              </li>

              <li
                class="page-item"
                :class="{ disabled: page >= totalPages - 1 }"
              >
                <button class="page-link" @click="changePage(page + 1)">
                  下一頁
                </button>
              </li>

              <!-- 直接跳到最後一頁；Spring Data 頁碼從 0 開始，因此使用 totalPages - 1。 -->
              <li
                class="page-item"
                :class="{
                  disabled: totalPages === 0 || page >= totalPages - 1,
                }"
              >
                <button class="page-link" @click="changePage(totalPages - 1)">
                  最後頁
                </button>
              </li>
            </ul>
          </nav>
        </footer>
      </section>
    </div>
  </main>
</template>

<style scoped>
main {
  min-height: 100vh;
  background: #f8f9ff;
}
.content-width {
  max-width: 1600px;
}
.search-input {
  flex: 1 1 320px;
}
.filter-select {
  max-width: 190px;
}
.page-size-select {
  max-width: 150px;
}
.price-range {
  max-width: 360px;
}
@media (max-width: 767.98px) {
  .filter-select,
  .page-size-select,
  .price-range {
    max-width: none;
  }
}
</style>
