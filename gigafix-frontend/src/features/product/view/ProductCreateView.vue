<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
// 所有 HTTP 請求都透過 product/api.js，不在 View 直接 import axios。
import { createProduct } from "../api";
import ProductForm from "../components/ProductForm.vue";

const router = useRouter();
const submitting = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const showLeaveConfirm = ref(false);

function wait(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function handleCreate(productRequest) {
  submitting.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    // Controller 會回傳剛建立完成的 Product，其中包含 productId。
    const createdProduct = await createProduct(productRequest);
    const createdProductId = createdProduct?.productId;

    if (!createdProductId) {
      throw new Error("新增成功，但後端回應中沒有 productId");
    }

    // 先讓使用者看到成功 Alert，再前往新商品的詳情頁。
    successMessage.value = "新增商品成功！即將前往商品詳情頁。";
    await wait(1000);

    await router.push({
      name: "admin-product-detail",
      params: { productId: createdProductId },
      // 讓詳情頁知道這次是新增成功後的跳轉。
      query: { success: "created" },
    });
  } catch (error) {
    console.error(error);

    if (error.response?.status === 400) {
      errorMessage.value = "新增失敗，請檢查必填欄位與資料格式";
    } else {
      if (error.response) {
        errorMessage.value = `新增失敗：HTTP ${error.response.status}`;
      } else if (error.request) {
        errorMessage.value = "無法連線到後端伺服器";
      } else {
        errorMessage.value = `前端操作失敗：${error.message}`;
      }
    }
  } finally {
    submitting.value = false;
  }
}

function cancelCreate() {
  // 不直接離開，先提醒尚未建立的商品資料會消失。
  showLeaveConfirm.value = true;
}

function continueCreating() {
  showLeaveConfirm.value = false;
}

async function confirmLeave() {
  showLeaveConfirm.value = false;
  await router.push({ name: "admin-products" });
}
</script>

<template>
  <main class="container-fluid px-3 px-md-4 py-4 create-page">
    <div class="page-shell mx-auto">
      <header class="mb-4">
        <h1 class="h2 fw-bold mb-1">新增商品</h1>
        <p class="text-secondary mb-0">建立商品基本資訊、價格、狀態與圖片。</p>
      </header>

      <div v-if="errorMessage" class="alert alert-danger" role="alert">
        {{ errorMessage }}
      </div>

      <div v-if="successMessage" class="alert alert-success" role="alert">
        <div class="d-flex align-items-center gap-2">
          <span class="material-symbols-outlined" aria-hidden="true"
            >check_circle</span
          >
          <span>{{ successMessage }}</span>
        </div>
      </div>

      <ProductForm
        :submitting="submitting"
        submit-text="建立產品"
        @submit="handleCreate"
        @cancel="cancelCreate"
      />

      <!-- 點擊取消時顯示未儲存警告。 -->
      <div
        v-if="showLeaveConfirm"
        class="confirm-backdrop position-fixed top-0 start-0 end-0 bottom-0 d-flex align-items-center justify-content-center p-3"
      >
        <div
          class="alert alert-danger confirm-alert shadow-lg mb-0"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="create-leave-confirm-title"
        >
          <h2 id="create-leave-confirm-title" class="h5 fw-bold mb-2">
            商品尚未建立，確定要離開嗎？
          </h2>
          <p class="mb-4">離開後，目前已填寫的商品資料將不會被保留。</p>

          <div class="d-flex flex-column flex-sm-row justify-content-end gap-2">
            <button
              class="btn btn-outline-secondary"
              type="button"
              @click="continueCreating"
            >
              繼續填寫
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
.create-page {
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
