<script setup>
import { onMounted, reactive, ref } from "vue";
import { createRecycleApplication } from "../../api.js";
import { getStores } from "@/features/repair/api.js";

const emit = defineEmits(["back"]);

const categories = [
  { value: "IPHONE", label: "iPhone" },
  { value: "IPAD", label: "iPad" },
  { value: "WATCH", label: "Apple Watch" },
];

const form = reactive({
  productName: "",
  category: "",
  appearance: "",
  imageUrl: "",
  description: "",
  estimatedPrice: null,
  storeId: "",
});

const stores = ref([]);
const storesLoading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const createdApplication = ref(null);

const loadStores = async () => {
  storesLoading.value = true;
  try {
    stores.value = await getStores();
  } catch (error) {
    stores.value = [];
    errorMessage.value =
      error?.response?.data?.message ?? "目前無法取得回收門市，請稍後再試。";
  } finally {
    storesLoading.value = false;
  }
};

const compactOptionalText = (value) => {
  const result = value.trim();
  return result || null;
};

// 專題展示用：一次填入固定測試資料，但不會自動送出回收申請。
const fillDemoData = () => {
  Object.assign(form, {
    productName: "iPhone 16 Plus 256GB",
    category: "IPHONE",
    appearance: "95成新",
    imageUrl:
      "https://media.karousell.com/media/photos/products/2024/11/15/iphone_16_plus_256g__1731707667_36ad03f4_progressive.jpg",
    description: "功能正常，外觀保存良好，螢幕與機身僅有輕微使用痕跡。",
    estimatedPrice: 25000,
    // 門市資料載入完成時自動選第一間；沒有門市資料時維持暫不指定。
    storeId: stores.value[0]?.id ?? "",
  });
  errorMessage.value = "";
};

const submitApplication = async () => {
  errorMessage.value = "";
  submitting.value = true;

  const recycleRequest = {
    productName: form.productName.trim(),
    category: form.category,
    appearance: form.appearance.trim(),
    imageUrl: compactOptionalText(form.imageUrl),
    description: compactOptionalText(form.description),
    estimatedPrice:
      form.estimatedPrice === null || form.estimatedPrice === ""
        ? null
        : Number(form.estimatedPrice),
    storeId: form.storeId === "" ? null : Number(form.storeId),
  };

  try {
    createdApplication.value = await createRecycleApplication(recycleRequest);
    window.scrollTo({ top: 0, behavior: "smooth" });
  } catch (error) {
    if (error?.response?.status === 401) {
      errorMessage.value = "登入狀態已失效，請重新登入後再送出申請。";
    } else {
      errorMessage.value =
        error?.response?.data?.message ?? "回收申請送出失敗，請稍後再試。";
    }
  } finally {
    submitting.value = false;
  }
};

onMounted(loadStores);
</script>

<template>
  <section class="apply-form-page" aria-labelledby="recycle-form-title">
    <div v-if="createdApplication" class="success-card" role="status">
      <span class="success-icon" aria-hidden="true">
        <i class="bi bi-check-lg"></i>
      </span>
      <p class="eyebrow">APPLICATION RECEIVED</p>
      <h1>回收申請已送出</h1>
      <p>
        我們已收到你的申請
        <span v-if="createdApplication.applyId">
          ，申請編號為 #{{ createdApplication.applyId }}
        </span>
        。後續將由門市人員與你聯繫。
      </p>
      <button class="primary-button" type="button" @click="emit('back')">
        返回回收說明
      </button>
    </div>

    <template v-else>
      <header class="form-header">
        <button class="back-button" type="button" @click="emit('back')">
          <i class="bi bi-arrow-left" aria-hidden="true"></i>
          返回回收說明
        </button>
        <p class="eyebrow">RECYCLE APPLICATION</p>
        <h1 id="recycle-form-title">填寫回收申請單</h1>
        <p class="form-intro">
          請填寫裝置資訊，我們會依照實機檢測結果提供最終收購價格。
        </p>
      </header>

      <form class="application-form" @submit.prevent="submitApplication">
        <div class="demo-button-row">
          <button
            class="demo-fill-button"
            type="button"
            :disabled="submitting"
            @click="fillDemoData"
          >
            自動填寫
          </button>
        </div>

        <div class="form-grid">
          <label class="field-group field-product-name">
            <span
              >裝置名稱
              <b aria-hidden="true"
                >* 請務必填寫型號&容量,手錶請填寫錶面尺寸</b
              ></span
            >
            <input
              v-model="form.productName"
              type="text"
              required
              maxlength="100"
              placeholder="例如：iPhone 15 Pro 256GB"
            />
          </label>

          <label class="field-group field-category">
            <span>裝置類別 <b aria-hidden="true">*</b></span>
            <select v-model="form.category" required>
              <option value="" disabled>請選擇裝置類別</option>
              <option
                v-for="category in categories"
                :key="category.value"
                :value="category.value"
              >
                {{ category.label }}
              </option>
            </select>
          </label>

          <label class="field-group field-appearance">
            <span>外觀新舊程度 <b aria-hidden="true">* </b></span>
            <input
              v-model="form.appearance"
              type="text"
              required
              placeholder="請填寫如:9成新、8成新等.."
            />
          </label>

          <label class="field-group field-price">
            <span>期望價格</span>
            <div class="input-prefix">
              <span>NT$</span>
              <input
                v-model.number="form.estimatedPrice"
                type="number"
                min="0"
                step="1"
                placeholder="請輸入金額"
              />
            </div>
          </label>

          <label class="field-group field-store">
            <span>回收門市</span>
            <select v-model="form.storeId" :disabled="storesLoading">
              <option value="">
                {{ storesLoading ? "門市載入中..." : "暫不指定門市" }}
              </option>
              <option v-for="store in stores" :key="store.id" :value="store.id">
                {{ store.name }}
              </option>
            </select>
          </label>

          <label class="field-group field-image-url">
            <span>裝置圖片網址</span>
            <input
              v-model="form.imageUrl"
              type="url"
              placeholder="https://example.com/device.jpg"
            />
          </label>

          <label class="field-group field-description">
            <span>其他說明</span>
            <textarea
              v-model="form.description"
              rows="4"
              placeholder="請自述手機使用狀況,例如:外觀明顯使用痕跡
...等等資訊"
            ></textarea>
          </label>
        </div>

        <div v-if="errorMessage" class="error-message" role="alert">
          <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
          {{ errorMessage }}
        </div>

        <button
          class="primary-button submit-button"
          type="submit"
          :disabled="submitting"
        >
          <span
            v-if="submitting"
            class="spinner-border spinner-border-sm"
            aria-hidden="true"
          ></span>
          {{ submitting ? "申請送出中..." : "送出回收申請" }}
          <i
            v-if="!submitting"
            class="bi bi-arrow-right"
            aria-hidden="true"
          ></i>
        </button>
      </form>
    </template>
  </section>
</template>

<style scoped>
.apply-form-page {
  --ink: #1b1b1b;
  --muted: #635d5e;
  --soft: #f9f9f9;
  color: var(--ink);
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
}

.form-header {
  margin-bottom: 40px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 40px;
  padding: 0;
  border: 0;
  color: var(--muted);
  background: transparent;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.back-button:hover,
.back-button:focus-visible {
  color: var(--ink);
}

.eyebrow {
  margin: 0 0 10px;
  color: #005ab7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.form-header h1,
.success-card h1 {
  margin: 0 0 12px;
  font-size: clamp(36px, 5vw, 58px);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.form-intro,
.success-card > p:not(.eyebrow) {
  margin: 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.7;
}

.application-form {
  padding: 36px;
  border: 1px solid #eeeeee;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 24px 60px -36px rgba(0, 0, 0, 0.28);
}

.demo-button-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 18px;
}

.demo-fill-button {
  flex: 0 0 auto;
  padding: 10px 16px;
  border: 1px solid #005ab7;
  border-radius: 9px;
  color: #005ab7;
  background: #fff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.demo-fill-button:hover:not(:disabled),
.demo-fill-button:focus-visible:not(:disabled) {
  color: #fff;
  background: #005ab7;
}

.demo-fill-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 26px 24px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
}

.field-product-name,
.field-appearance,
.field-image-url {
  grid-column: span 7;
}

.field-category,
.field-price,
.field-store {
  grid-column: span 5;
}

.field-description {
  grid-column: 1 / -1;
}

.field-group > span {
  font-size: 14px;
  font-weight: 600;
}

.field-group b {
  color: #005ab7;
}

.field-group input,
.field-group select,
.field-group textarea {
  width: 100%;
  border: 1px solid #cfc4c5;
  border-radius: 10px;
  padding: 13px 14px;
  color: var(--ink);
  background: #f3f3f3;
  font: inherit;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.field-group textarea {
  resize: vertical;
}

.field-group input:focus,
.field-group select:focus,
.field-group textarea:focus {
  border-color: #005ab7;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(0, 90, 183, 0.12);
  outline: none;
}

.field-group select:disabled {
  cursor: wait;
  opacity: 0.65;
}

.input-prefix {
  display: flex;
  align-items: stretch;
}

.input-prefix > span {
  display: flex;
  align-items: center;
  padding: 0 14px;
  border: 1px solid #cfc4c5;
  border-right: 0;
  border-radius: 10px 0 0 10px;
  color: var(--muted);
  background: #e9e9e9;
  font-weight: 600;
  white-space: nowrap;
}

.input-prefix input {
  border-radius: 0 10px 10px 0;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 24px;
  padding: 14px 16px;
  border-radius: 10px;
  color: #842029;
  background: #f8d7da;
}

.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 56px;
  padding: 15px 28px;
  border: 2px solid var(--ink);
  border-radius: 12px;
  color: #fff;
  background: var(--ink);
  font: inherit;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease;
}

.primary-button:hover:not(:disabled),
.primary-button:focus-visible:not(:disabled) {
  color: var(--ink);
  background: #fff;
  transform: translateY(-2px);
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.submit-button {
  width: 100%;
  margin-top: 32px;
}

.success-card {
  padding: 72px 40px;
  border: 1px solid #eeeeee;
  border-radius: 20px;
  background: var(--soft);
  text-align: center;
}

.success-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: 24px;
  border-radius: 50%;
  color: #fff;
  background: #005ab7;
  font-size: 36px;
}

.success-card .primary-button {
  margin-top: 32px;
}

@media (max-width: 1099.98px) {
  .field-product-name,
  .field-category,
  .field-appearance,
  .field-price,
  .field-store,
  .field-image-url,
  .field-description {
    grid-column: 1 / -1;
  }
}

@media (max-width: 767.98px) {
  .application-form {
    padding: 22px 18px;
    border-radius: 14px;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;
  }

  .demo-fill-button {
    width: 100%;
  }

  .back-button {
    margin-bottom: 28px;
  }

  .success-card {
    padding: 48px 20px;
  }
}
</style>
