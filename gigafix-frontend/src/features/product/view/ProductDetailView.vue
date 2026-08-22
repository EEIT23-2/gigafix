<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getProduct } from "../api";

const route = useRoute();
const router = useRouter();
const productId = route.params.productId;

const product = ref(null);
const loading = ref(false);
const errorMessage = ref("");

// 依來源頁面的 query 顯示新增或編輯成功訊息。
const actionSuccessMessage = ref(
  route.query.success === "created"
    ? "新增商品成功！以下是新建立的商品資料。"
    : route.query.success === "updated"
      ? "編輯商品成功！商品資料已完成更新。"
      : "",
);

const saleStatusMap = {
  OFF_SHELF: { label: "已下架", badgeClass: "text-bg-secondary" },
  AVAILABLE: { label: "可販售（待售中）", badgeClass: "text-bg-success" },
  RESERVED: { label: "已保留", badgeClass: "text-bg-warning" },
  SOLD: { label: "已售出", badgeClass: "text-bg-danger" },
};

// 同時相容目前 API 回傳的 snake_case 與 Java camelCase。
const productName = computed(
  () => product.value?.product_name ?? product.value?.productName ?? "-",
);
const imageUrl = computed(
  () => product.value?.image_url ?? product.value?.imageUrl ?? "",
);
const saleStatus = computed(
  () => product.value?.sale_status ?? product.value?.saleStatus ?? "",
);

async function fetchProduct() {
  loading.value = true;
  errorMessage.value = "";

  try {
    // 這裡會呼叫 GET /api/products/{productId}。
    product.value = await getProduct(productId);
  } catch (error) {
    console.error(error);
    errorMessage.value =
      error.response?.status === 404
        ? `找不到 ID 為 ${productId} 的商品`
        : error.response
          ? `讀取商品失敗：HTTP ${error.response.status}`
          : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

function formatPrice(value) {
  return new Intl.NumberFormat("zh-TW", {
    style: "currency",
    currency: "TWD",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

function formatDate(value) {
  return value?.slice(0, 19)?.replace("T", " ") ?? "-";
}

function statusText(status) {
  return saleStatusMap[status]?.label ?? status ?? "未知狀態";
}

function statusClass(status) {
  return saleStatusMap[status]?.badgeClass ?? "text-bg-dark";
}

function goBack() {
  router.push({ name: "admin-products" });
}

function goToEdit() {
  router.push({
    name: "admin-product-edit",
    params: { productId },
  });
}

function useFallbackImage(event) {
  event.target.src = "https://placehold.co/640x480?text=No+Image";
}

async function closeSuccessMessage() {
  actionSuccessMessage.value = "";

  // 清除網址上的 success，避免重新整理時再次顯示舊提示。
  const query = { ...route.query };
  delete query.success;
  await router.replace({ query });
}

onMounted(fetchProduct);
</script>

<template>
  <main class="container-fluid px-3 px-md-4 py-4 detail-page">
    <div class="page-shell mx-auto">
      <header class="mb-4">
        <div>
          <h1 class="h2 fw-bold mb-1">商品詳情</h1>
          <p class="text-secondary mb-0">查看商品的完整資料與目前狀態。</p>
        </div>
      </header>

      <div
        v-if="actionSuccessMessage"
        class="alert alert-success alert-dismissible fade show"
        role="alert"
      >
        <div class="d-flex align-items-center gap-2 pe-4">
          <span class="material-symbols-outlined" aria-hidden="true"
            >check_circle</span
          >
          <span>{{ actionSuccessMessage }}</span>
        </div>
        <button
          class="btn-close"
          type="button"
          aria-label="關閉成功訊息"
          @click="closeSuccessMessage"
        ></button>
      </div>

      <div v-if="loading" class="card">
        <div class="card-body text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">載入中</span>
          </div>
          <div class="text-secondary mt-3">正在讀取商品資料...</div>
        </div>
      </div>

      <div v-else-if="errorMessage" class="alert alert-danger" role="alert">
        <div
          class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3"
        >
          <span>{{ errorMessage }}</span>
          <button
            class="btn btn-sm btn-outline-danger"
            type="button"
            @click="fetchProduct"
          >
            重新讀取
          </button>
        </div>
      </div>

      <section v-else-if="product" class="card overflow-hidden">
        <div class="card-body p-4">
          <div class="row g-4">
            <div class="col-12 col-lg-4">
              <div
                class="image-panel rounded-3 p-3 d-flex align-items-center justify-content-center"
              >
                <img
                  v-if="imageUrl"
                  :src="imageUrl"
                  :alt="productName"
                  class="detail-image rounded-3"
                  @error="useFallbackImage"
                />
                <div v-else class="text-secondary py-5">沒有商品圖片</div>
              </div>
            </div>

            <div class="col-12 col-lg-8">
              <div class="mb-4">
                <div>
                  <div class="text-secondary small mb-1">
                    商品 ID：{{ product.productId }}
                  </div>
                  <h2 class="h3 fw-bold mb-2">{{ productName }}</h2>
                  <span class="badge" :class="statusClass(saleStatus)">{{
                    statusText(saleStatus)
                  }}</span>
                </div>
              </div>

              <dl class="row detail-list mb-0">
                <dt class="col-sm-4">類別</dt>
                <dd class="col-sm-8">{{ product.category ?? "-" }}</dd>
                <dt class="col-sm-4">外觀等級</dt>
                <dd class="col-sm-8">{{ product.grade ?? "-" }}</dd>
                <dt class="col-sm-4">外觀狀況</dt>
                <dd class="col-sm-8">{{ product.appearance ?? "-" }}</dd>
                <dt class="col-sm-4">價格</dt>
                <dd class="col-sm-8 fw-semibold">
                  {{ formatPrice(product.price) }}
                </dd>
                <dt class="col-sm-4">商品描述</dt>
                <dd class="col-sm-8">{{ product.description || "-" }}</dd>
                <dt class="col-sm-4">上架日期</dt>
                <dd class="col-sm-8">{{ formatDate(product.createdTime) }}</dd>
                <dt class="col-sm-4">最後修改</dt>
                <dd class="col-sm-8">
                  {{ formatDate(product.lastModifiedTime) }}
                </dd>
              </dl>
            </div>
          </div>
        </div>

        <!-- 詳情內容底部操作列，方便看完資料後直接返回或前往編輯。 -->
        <div
          class="card-footer bg-white d-flex flex-column flex-sm-row justify-content-end gap-2 px-4 py-3"
        >
          <button
            class="btn btn-outline-secondary"
            type="button"
            @click="goBack"
          >
            回到商品列表
          </button>
          <button class="btn btn-primary" type="button" @click="goToEdit">
            編輯商品
          </button>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f8f9ff;
}
.page-shell {
  max-width: 1400px;
}
.image-panel {
  min-height: 360px;
  background: #f3f5f9;
  border: 1px solid #d8dee9;
}
.detail-image {
  display: block;
  width: 100%;
  max-height: 480px;
  object-fit: contain;
}
.detail-list dt,
.detail-list dd {
  padding-top: 1rem;
  padding-bottom: 1rem;
  margin-bottom: 0;
  border-bottom: 1px solid #e8ebf0;
  display: flex;
  align-items: center;
}

.detail-list dt {
  color: #64748b;
}

/* 移除最後一行的底線，讓介面看起來更乾淨 */
.detail-list dt:last-of-type,
.detail-list dd:last-of-type {
  border-bottom: none;
}
</style>
