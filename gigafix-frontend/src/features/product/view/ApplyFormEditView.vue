<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { getRecycleApplication, updateRecycleApplication } from "../api";

const route = useRoute();
const router = useRouter();

const form = ref({
  memberId: null,
  productName: "",
  category: "",
  appearance: "",
  imageUrl: "",
  description: "",
  estimatedPrice: 0,
});

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const initialFormSnapshot = ref("");
const formLoaded = ref(false);
const allowNavigation = ref(false);
const editingLocked = ref(false);
const imageFile = ref(null);
const localImagePreviewUrl = ref("");

const categoryOptions = [
  { value: "IPHONE", label: "iPhone" },
  { value: "WATCH", label: "Apple Watch" },
  { value: "IPAD", label: "iPad" },
];

const isDirty = computed(() => {
  if (!formLoaded.value) return false;
  return (
    JSON.stringify(form.value) !== initialFormSnapshot.value ||
    imageFile.value !== null
  );
});

// 新檔案優先預覽；未選檔案時仍顯示後端既有的 Cloudinary 圖片。
const imagePreviewUrl = computed(
  () => localImagePreviewUrl.value || form.value.imageUrl,
);

function getAppearance(data) {
  return data?.appearance ?? data?.appreance ?? "";
}

function handleImageChange(event) {
  imageFile.value = event.target.files?.[0] ?? null;

  if (localImagePreviewUrl.value) {
    URL.revokeObjectURL(localImagePreviewUrl.value);
    localImagePreviewUrl.value = "";
  }

  if (imageFile.value) {
    localImagePreviewUrl.value = URL.createObjectURL(imageFile.value);
  }
}

async function fetchApplication() {
  loading.value = true;
  errorMessage.value = "";
  editingLocked.value = false;

  try {
    const data = await getRecycleApplication(route.params.applyId);

    // 防止使用者直接輸入編輯網址進入已結案的回收單。
    if (data.recycleStatus === "COMPLETED") {
      editingLocked.value = true;
      return;
    }

    form.value = {
      memberId: data.memberId,
      productName: data.productName ?? "",
      category: data.category ?? "",
      appearance: getAppearance(data),
      imageUrl: data.imageUrl ?? "",
      description: data.description ?? "",
      estimatedPrice: data.estimatedPrice ?? 0,
    };

    initialFormSnapshot.value = JSON.stringify(form.value);
    formLoaded.value = true;
  } catch (error) {
    console.error(error);
    errorMessage.value =
      error.response?.status === 404
        ? "找不到這筆回收申請"
        : error.response
          ? `讀取回收申請失敗（HTTP ${error.response.status}）`
          : "無法連線至伺服器";
  } finally {
    loading.value = false;
  }
}

function goToDetail() {
  router.push({
    name: "admin-applyForms-detail",
    params: { applyId: route.params.applyId },
  });
}

async function handleSubmit() {
  if (editingLocked.value) return;

  saving.value = true;
  errorMessage.value = "";

  const request = {
    memberId: Number(form.value.memberId),
    productName: form.value.productName.trim(),
    category: form.value.category,
    appearance: form.value.appearance.trim(),
    description: form.value.description.trim() || null,
    estimatedPrice: Number(form.value.estimatedPrice),
  };

  try {
    await updateRecycleApplication(
      route.params.applyId,
      request,
      imageFile.value,
    );

    // 儲存成功後允許直接離開，不顯示未儲存警告。
    allowNavigation.value = true;

    await router.push({
      name: "admin-applyForms-detail",
      params: { applyId: route.params.applyId },
      query: { updated: "1" },
    });
  } catch (error) {
    console.error(error);
    errorMessage.value =
      error.response?.status === 409
        ? "這筆回收單已完成，不能再編輯。"
        : error.response
          ? `更新回收申請失敗（HTTP ${error.response.status}）`
          : "無法連線至伺服器";
  } finally {
    saving.value = false;
  }
}

function handleBeforeUnload(event) {
  if (!isDirty.value || allowNavigation.value) return;

  event.preventDefault();
  event.returnValue = "";
}

onBeforeRouteLeave(() => {
  if (!isDirty.value || allowNavigation.value) {
    return true;
  }

  return window.confirm("編輯尚未完成，確定要離開並放棄尚未儲存的內容嗎？");
});

onMounted(() => {
  fetchApplication();
  window.addEventListener("beforeunload", handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
  if (localImagePreviewUrl.value) {
    URL.revokeObjectURL(localImagePreviewUrl.value);
  }
});
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="mx-auto content-width">
      <button
        type="button"
        class="btn btn-link text-decoration-none px-0 mb-2"
        @click="goToDetail"
      >
        <i class="bi bi-arrow-left me-1"></i>
        返回申請明細
      </button>

      <div class="d-flex flex-wrap align-items-center gap-3 mb-4">
        <h1 class="fw-bold mb-0">編輯回收申請 #{{ route.params.applyId }}</h1>

        <span v-if="isDirty" class="badge rounded-pill text-bg-warning">
          尚未儲存
        </span>
      </div>

      <div
        v-if="errorMessage"
        class="alert alert-danger alert-dismissible"
        role="alert"
      >
        {{ errorMessage }}

        <button
          type="button"
          class="btn-close"
          aria-label="關閉"
          @click="errorMessage = ''"
        ></button>
      </div>

      <section v-if="loading" class="card border-0 shadow-sm text-center py-5">
        <div class="spinner-border text-primary mx-auto" role="status"></div>
        <div class="text-secondary mt-2">正在載入編輯資料...</div>
      </section>

      <section
        v-else-if="editingLocked"
        class="alert alert-warning d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3"
        role="alert"
      >
        <span>這筆回收單已完成，為保持結案紀錄與商品庫存一致，不能再編輯。</span>
        <button type="button" class="btn btn-outline-dark" @click="goToDetail">
          返回申請明細
        </button>
      </section>

      <form
        v-else
        class="card border-0 shadow-sm"
        @submit.prevent="handleSubmit"
      >
        <div class="card-header bg-white py-3">
          <h2 class="fs-5 mb-0">申請資料</h2>
        </div>

        <div class="card-body p-4">
          <div class="row g-4">
            <div class="col-md-8">
              <label for="productName" class="form-label">商品名稱</label>
              <input
                id="productName"
                v-model.trim="form.productName"
                type="text"
                class="form-control"
                maxlength="255"
                required
              />
            </div>

            <div class="col-md-4">
              <label for="category" class="form-label">商品分類</label>
              <select
                id="category"
                v-model="form.category"
                class="form-select"
                required
              >
                <option value="" disabled>請選擇分類</option>
                <option
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </option>
              </select>
            </div>

            <div class="col-12">
              <label for="appearance" class="form-label">商品外觀</label>
              <textarea
                id="appearance"
                v-model.trim="form.appearance"
                class="form-control"
                rows="3"
                required
              ></textarea>
            </div>

            <div class="col-12">
              <label for="description" class="form-label">商品描述</label>
              <textarea
                id="description"
                v-model.trim="form.description"
                class="form-control"
                rows="4"
              ></textarea>
            </div>

            <div class="col-md-6">
              <label for="imageFile" class="form-label">更換商品圖片</label>
              <input
                id="imageFile"
                type="file"
                class="form-control"
                accept="image/*"
                @change="handleImageChange"
              />
              <div class="form-text">
                圖片會上傳至 Cloudinary；未選新檔時保留目前圖片。
              </div>
            </div>

            <div class="col-md-6">
              <label for="estimatedPrice" class="form-label">預估價格</label>
              <div class="input-group">
                <span class="input-group-text">NT$</span>
                <input
                  id="estimatedPrice"
                  v-model.number="form.estimatedPrice"
                  type="number"
                  class="form-control"
                  min="0"
                  required
                />
              </div>
            </div>

            <div v-if="imagePreviewUrl" class="col-12">
              <label class="form-label">圖片預覽</label>
              <div>
                <img
                  :src="imagePreviewUrl"
                  :alt="form.productName"
                  class="image-preview"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="card-footer bg-white d-flex justify-content-end gap-2 py-3">
          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="saving"
            @click="goToDetail"
          >
            取消
          </button>

          <button
            type="submit"
            class="btn btn-primary"
            :disabled="saving || !isDirty"
          >
            <span
              v-if="saving"
              class="spinner-border spinner-border-sm me-1"
            ></span>
            <i v-else class="bi bi-check-lg me-1"></i>
            {{ saving ? "儲存中..." : "儲存修改" }}
          </button>
        </div>
      </form>
    </div>
  </main>
</template>

<style scoped>
main {
  min-height: 100vh;
  background: #f8f9ff;
}

.content-width {
  max-width: 1000px;
}

.image-preview {
  width: 180px;
  height: 180px;
  border: 1px solid #dee2e6;
  border-radius: 0.75rem;
  object-fit: contain;
  background: #f8f9fa;
}
</style>
