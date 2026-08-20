<script setup>
import { reactive, watch } from "vue";

const props = defineProps({
  // 編輯頁可傳入既有商品；新增頁不傳時使用空白預設值。
  initialProduct: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false },
  submitText: { type: String, default: "建立產品" },
});

const emit = defineEmits(["submit", "cancel"]);

// 欄位名稱依照你目前 API JSON 的 snake_case 格式。
const form = reactive({
  product_name: "",
  category: "",
  grade: "",
  appearance: "",
  description: "",
  price: null,
  sale_status: "AVAILABLE",
  image_url: "",
});

// 新增頁載入預設值；未來編輯頁傳入商品時，也能共用此表單。
watch(
  () => props.initialProduct,
  (product) => {
    form.product_name = product?.product_name ?? product?.productName ?? "";
    form.category = product?.category ?? "";
    form.grade = product?.grade ?? "";
    form.appearance = product?.appearance ?? "";
    form.description = product?.description ?? "";
    form.price = product?.price ?? null;
    form.sale_status =
      product?.sale_status ?? product?.saleStatus ?? "AVAILABLE";
    form.image_url = product?.image_url ?? product?.imageUrl ?? "";
  },
  { immediate: true },
);

function submitForm() {
  // ProductRequest 使用 Java camelCase，因此送往後端時轉成同名欄位。
  emit("submit", {
    productName: form.product_name,
    category: form.category,
    grade: form.grade,
    appearance: form.appearance,
    description: form.description,
    price: form.price,
    saleStatus: form.sale_status,
    imageUrl: form.image_url,
  });
}

function clearBrokenPreview(event) {
  event.target.style.display = "none";
}
</script>

<template>
  <form id="productForm" class="product-form" @submit.prevent="submitForm">
    <div class="row g-4">
      <div class="col-12 col-lg-8">
        <div class="d-grid gap-4">
          <!-- 基本資訊 -->
          <section class="card form-card rounded-3">
            <div class="card-body p-4">
              <h2 class="h5 fw-semibold d-flex align-items-center gap-2 mb-4">
                <span
                  class="material-symbols-outlined text-primary"
                  aria-hidden="true"
                  >info</span
                >
                基本資訊
              </h2>

              <div class="mb-3">
                <label class="form-label" for="product_name"
                  >產品名稱（型號）</label
                >
                <input
                  id="product_name"
                  v-model.trim="form.product_name"
                  class="form-control"
                  type="text"
                  placeholder="例如：iPhone 13 Pro 256GB"
                  required
                />
              </div>

              <div class="row g-3 mb-3">
                <div class="col-12 col-md-6">
                  <label class="form-label" for="category">類別</label>
                  <select
                    id="category"
                    v-model="form.category"
                    class="form-select"
                    required
                  >
                    <option value="" disabled>選擇類別</option>
                    <option value="IPHONE">iPhone</option>
                    <option value="WATCH">Apple Watch</option>
                    <option value="IPAD">iPad</option>
                  </select>
                </div>

                <div class="col-12 col-md-6">
                  <label class="form-label" for="grade">外觀等級</label>
                  <input
                    id="grade"
                    v-model.trim="form.grade"
                    class="form-control"
                    type="text"
                    placeholder="例如：A級、B級-、S級+"
                    required
                  />
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label" for="appearance">外觀狀況</label>
                <input
                  id="appearance"
                  v-model.trim="form.appearance"
                  class="form-control"
                  type="text"
                  placeholder="例如：95成新"
                  required
                />
              </div>

              <div>
                <label class="form-label" for="description">描述</label>
                <textarea
                  id="description"
                  v-model.trim="form.description"
                  class="form-control"
                  rows="4"
                  placeholder="輸入產品狀況細節..."
                  required
                ></textarea>
              </div>
            </div>
          </section>

          <!-- 定價與狀態 -->
          <section class="card form-card rounded-3">
            <div class="card-body p-4">
              <h2 class="h5 fw-semibold d-flex align-items-center gap-2 mb-4">
                <span
                  class="material-symbols-outlined text-primary"
                  aria-hidden="true"
                  >sell</span
                >
                定價與狀態
              </h2>

              <div class="row g-3">
                <div class="col-12 col-md-6">
                  <label class="form-label" for="price">價格（TWD）</label>
                  <div class="input-group">
                    <span class="input-group-text">NT$</span>
                    <input
                      id="price"
                      v-model.number="form.price"
                      class="form-control"
                      type="number"
                      min="0"
                      step="1"
                      placeholder="0"
                      required
                    />
                  </div>
                </div>

                <div class="col-12 col-md-6">
                  <label class="form-label" for="sale_status">狀態</label>
                  <select
                    id="sale_status"
                    v-model="form.sale_status"
                    class="form-select"
                    required
                  >
                    <option value="AVAILABLE">可販售（待售中）</option>
                    <option value="OFF_SHELF">已下架</option>
                    <option value="RESERVED">已保留</option>
                    <option value="SOLD">已售出</option>
                  </select>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- 圖片網址與預覽 -->
      <div class="col-12 col-lg-4">
        <section class="card form-card rounded-3">
          <div class="card-body p-4">
            <h2 class="h5 fw-semibold d-flex align-items-center gap-2 mb-4">
              <span
                class="material-symbols-outlined text-primary"
                aria-hidden="true"
                >add_photo_alternate</span
              >
              產品圖片
            </h2>

            <label class="form-label" for="image_url">圖片網址</label>
            <input
              id="image_url"
              v-model.trim="form.image_url"
              class="form-control"
              type="url"
              placeholder="https://example.com/product.jpg"
            />
            <div class="form-text mb-3">
              目前後端使用 JSON Request，因此這裡填入圖片網址。
            </div>

            <div
              class="upload-zone rounded-3 d-flex align-items-center justify-content-center text-center p-3"
            >
              <img
                v-if="form.image_url"
                :key="form.image_url"
                :src="form.image_url"
                alt="商品圖片預覽"
                class="image-preview rounded-2"
                @error="clearBrokenPreview"
              />
              <div v-else class="text-secondary py-5">
                <span
                  class="material-symbols-outlined fs-1 d-block mb-2"
                  aria-hidden="true"
                  >image</span
                >
                輸入圖片網址後會顯示預覽
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- 固定在視窗底部的操作列。 -->
    <div
      class="action-bar position-fixed bottom-0 start-0 end-0 bg-white border-top shadow-sm"
    >
      <div
        class="container-fluid page-shell px-3 px-md-4 py-3 mx-auto d-flex justify-content-end gap-2"
      >
        <button
          class="btn btn-outline-secondary px-4"
          type="button"
          :disabled="submitting"
          @click="emit('cancel')"
        >
          取消
        </button>
        <button
          class="btn btn-primary px-4 d-inline-flex align-items-center gap-2"
          type="submit"
          :disabled="submitting"
        >
          <span
            v-if="submitting"
            class="spinner-border spinner-border-sm"
            aria-hidden="true"
          ></span>
          <span v-else class="material-symbols-outlined" aria-hidden="true"
            >check_circle</span
          >
          {{ submitting ? "處理中..." : submitText }}
        </button>
      </div>
    </div>
  </form>
</template>

<style scoped>
.product-form {
  padding-bottom: 90px;
}
.form-card {
  border-color: #d8dee9;
  box-shadow: 0 0.125rem 0.5rem rgba(30, 41, 59, 0.04);
}
.upload-zone {
  min-height: 260px;
  border: 2px dashed #c7cfdb;
  background: #f8f9fc;
}
.image-preview {
  display: block;
  width: 100%;
  max-height: 320px;
  object-fit: contain;
}
.action-bar {
  z-index: 1030;
}
.page-shell {
  max-width: 1600px;
}
</style>
