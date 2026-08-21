<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
// 所有商品 HTTP 請求統一透過 product/api.js。
import { getProduct, updateProduct } from "../api";
import ProductForm from "../components/ProductForm.vue";

const route = useRoute();
const router = useRouter();

// Router 的 /products/:productId/edit 提供此參數。
const productId = route.params.productId;

// 從後端取得的商品會傳入 ProductForm，作為表單初始值。
const product = ref(null);
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const showSaveConfirm = ref(false);
const pendingProductRequest = ref(null);
const showLeaveConfirm = ref(false);

// 進入編輯頁時，依 productId 讀取最新商品資料。
async function fetchProduct() {
  loading.value = true;
  errorMessage.value = "";

  try {
    // api.js 已經 return response.data，因此可直接取得 Product。
    product.value = await getProduct(productId);
  } catch (error) {
    console.error(error);

    if (error.response?.status === 404) {
      errorMessage.value = `找不到 ID 為 ${productId} 的商品`;
    } else {
      errorMessage.value = error.response
        ? `讀取商品失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
    }
  } finally {
    loading.value = false;
  }
}

// 第一次按下「儲存修改」只暫存內容並顯示確認 Alert。
function requestUpdate(productRequest) {
  pendingProductRequest.value = productRequest;
  showSaveConfirm.value = true;
  errorMessage.value = "";
}

// 取消確認只關閉 Alert，表單內已輸入的修改仍會保留。
function cancelPendingUpdate() {
  showSaveConfirm.value = false;
  pendingProductRequest.value = null;
}

// 使用者再次確認後，才呼叫修改 API。
async function confirmUpdate() {
  if (!pendingProductRequest.value) return;

  submitting.value = true;
  errorMessage.value = "";

  try {
    await updateProduct(productId, pendingProductRequest.value);

    showSaveConfirm.value = false;
    pendingProductRequest.value = null;

    // 修改成功後前往同一筆商品的詳情頁。
    await router.push({
      name: "admin-product-detail",
      params: { productId },
      // 讓詳情頁知道這次是編輯成功後的跳轉。
      query: { success: "updated" },
    });
  } catch (error) {
    console.error(error);
    // 關閉確認遮罩，讓使用者能看見頁面上的錯誤訊息並繼續修改。
    showSaveConfirm.value = false;
    pendingProductRequest.value = null;

    if (error.response?.status === 400) {
      errorMessage.value = "修改失敗，請檢查必填欄位與資料格式";
    } else if (error.response?.status === 404) {
      errorMessage.value = "商品不存在或已被刪除";
    } else {
      errorMessage.value = error.response
        ? `修改失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
    }
  } finally {
    submitting.value = false;
  }
}

function cancelEdit() {
  // 不立刻離開，先警告使用者尚未儲存的修改會消失。
  showLeaveConfirm.value = true;
}

function continueEditing() {
  showLeaveConfirm.value = false;
}

async function confirmLeave() {
  showLeaveConfirm.value = false;
  await router.push({ name: "admin-products" });
}

function retryFetch() {
  fetchProduct();
}

onMounted(fetchProduct);
</script>

<template>
  <main class="container-fluid px-3 px-md-4 py-4 edit-page">
    <div class="page-shell mx-auto">
      <header class="mb-4">
        <div
          class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3"
        >
          <div>
            <h1 class="h2 fw-bold mb-1">編輯商品</h1>
            <p class="text-secondary mb-0">商品 ID：{{ productId }}</p>
          </div>

          <button
            class="btn btn-outline-secondary"
            type="button"
            @click="cancelEdit"
          >
            返回商品列表
          </button>
        </div>
      </header>

      <!-- 單筆商品載入中。 -->
      <div v-if="loading" class="card">
        <div class="card-body text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">商品載入中</span>
          </div>
          <div class="text-secondary mt-3">正在讀取商品資料...</div>
        </div>
      </div>

      <!-- 讀取或修改失敗時顯示。 -->
      <div
        v-else-if="errorMessage && !product"
        class="alert alert-danger"
        role="alert"
      >
        <div
          class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3"
        >
          <span>{{ errorMessage }}</span>
          <button
            class="btn btn-sm btn-outline-danger"
            type="button"
            @click="retryFetch"
          >
            重新讀取
          </button>
        </div>
      </div>

      <template v-else-if="product">
        <div v-if="errorMessage" class="alert alert-danger" role="alert">
          {{ errorMessage }}
        </div>

        <!-- ProductForm 同時供新增頁與編輯頁共用。 -->
        <ProductForm
          :initial-product="product"
          :submitting="submitting"
          submit-text="儲存修改"
          @submit="requestUpdate"
          @cancel="cancelEdit"
        />
      </template>

      <!-- 儲存前二次確認；使用自訂遮罩，不需要 Bootstrap Modal JS。 -->
      <div
        v-if="showSaveConfirm"
        class="confirm-backdrop position-fixed top-0 start-0 end-0 bottom-0 d-flex align-items-center justify-content-center p-3"
      >
        <div
          class="alert alert-warning confirm-alert shadow-lg mb-0"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="save-confirm-title"
        >
          <h2 id="save-confirm-title" class="h5 fw-bold mb-2">
            確定要儲存這次修改嗎？
          </h2>
          <p class="mb-4">確認後會更新商品 ID {{ productId }} 的資料。</p>

          <div class="d-flex flex-column flex-sm-row justify-content-end gap-2">
            <button
              class="btn btn-outline-secondary"
              type="button"
              :disabled="submitting"
              @click="cancelPendingUpdate"
            >
              取消儲存
            </button>
            <button
              class="btn btn-warning"
              type="button"
              :disabled="submitting"
              @click="confirmUpdate"
            >
              <span
                v-if="submitting"
                class="spinner-border spinner-border-sm me-1"
                aria-hidden="true"
              ></span>
              {{ submitting ? "儲存中..." : "確定儲存" }}
            </button>
          </div>
        </div>
      </div>

      <!-- 點擊取消或返回列表時顯示的未儲存警告。 -->
      <div
        v-if="showLeaveConfirm"
        class="confirm-backdrop position-fixed top-0 start-0 end-0 bottom-0 d-flex align-items-center justify-content-center p-3"
      >
        <div
          class="alert alert-danger confirm-alert shadow-lg mb-0"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="leave-confirm-title"
        >
          <h2 id="leave-confirm-title" class="h5 fw-bold mb-2">
            尚未儲存，確定要離開嗎？
          </h2>
          <p class="mb-4">離開後，本次在表單中所做的修改將不會被保留。</p>

          <div class="d-flex flex-column flex-sm-row justify-content-end gap-2">
            <button
              class="btn btn-outline-secondary"
              type="button"
              @click="continueEditing"
            >
              繼續編輯
            </button>
            <button class="btn btn-danger" type="button" @click="confirmLeave">
              確定離開
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.edit-page {
  min-height: 100vh;
  background: #f8f9ff;
}

.page-shell {
  max-width: 1600px;
}

.confirm-backdrop {
  z-index: 1080;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
}

.confirm-alert {
  width: min(100%, 520px);
  padding: 1.5rem;
  border-radius: 0.75rem;
}
</style>
