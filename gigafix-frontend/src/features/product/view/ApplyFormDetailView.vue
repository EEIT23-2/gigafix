<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  cancelRecycleApplication,
  completeRecycleApplication,
  deleteRecycleApplication,
  getRecycleApplication,
  markRecycleApplicationAsInspecting,
  requestRecycleAgreementOtp,
  updateRecycleApplication,
} from "../api";
import { estimateRecyclePrice } from "../recyclePriceEstimator";

const route = useRoute();
const router = useRouter();

const application = ref(null);
const loading = ref(false);
const deleting = ref(false);
const updatingStatus = ref(false);
const estimatingPrice = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const statusError = ref("");
const selectedStatus = ref("");
// 保存最近一次估價的辨識依據，讓管理者能確認系統使用了哪個型號與規格。
const estimateError = ref("");
const estimateResult = ref(null);
const agreementRequested = ref(false);
const requestingAgreementOtp = ref(false);
const showDeleteConfirm = ref(false);

let successTimer = null;

const categoryLabels = {
  IPHONE: "iPhone",
  WATCH: "Apple Watch",
  IPAD: "iPad",
};

const statusLabels = {
  CANCELLED: "已取消",
  APPLIED: "已提出申請",
  INSPECTING: "檢查估價中",
  WAITING_FOR_AGREEMENT: "等待客戶同意",
  WIPING: "資料清除中",
  COMPLETED: "回收完成",
};

function statusClass(status) {
  return (
    {
      CANCELLED: "text-bg-secondary",
      APPLIED: "text-bg-primary",
      INSPECTING: "text-bg-warning",
      WAITING_FOR_AGREEMENT: "text-bg-info",
      WIPING: "text-bg-dark",
      COMPLETED: "text-bg-success",
    }[status] ?? "text-bg-light"
  );
}

function getAppearance(data) {
  return data?.appearance ?? data?.appreance ?? "—";
}

const deleteDialogDetail = computed(() => {
  if (!application.value) return "";
  return `申請 #${application.value.applyId}－${application.value.productName}`;
});

function showUpdateSuccess() {
  if (route.query.updated !== "1") return;

  successMessage.value = "回收申請編輯成功";

  // 清除網址參數，避免重新整理後再次顯示成功訊息。
  const { updated, ...remainingQuery } = route.query;
  router.replace({ query: remainingQuery });

  successTimer = window.setTimeout(() => {
    successMessage.value = "";
  }, 3500);
}

async function fetchApplication() {
  loading.value = true;
  errorMessage.value = "";

  try {
    application.value = await getRecycleApplication(route.params.applyId);
    selectedStatus.value = application.value.recycleStatus;
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

async function updateStatus() {
  if (!application.value) return;

  if (selectedStatus.value === "CANCELLED") {
    await cancelRecycle();
    return;
  }

  // 資料清除確認完成後呼叫結案 API，由後端同步建立商品庫存與寄送會員通知。
  if (
    application.value.recycleStatus === "WIPING" &&
    selectedStatus.value === "COMPLETED"
  ) {
    await completeRecycle();
    return;
  }

  // 第一段轉換直接改為檢測中；第二段只寄 OTP，狀態需等會員驗證與簽名。
  if (
    application.value.recycleStatus === "INSPECTING" &&
    selectedStatus.value === "WAITING_FOR_AGREEMENT"
  ) {
    await requestAgreementOtp();
    return;
  }

  if (
    application.value.recycleStatus !== "APPLIED" ||
    selectedStatus.value !== "INSPECTING"
  ) return;

  updatingStatus.value = true;
  statusError.value = "";
  closeSuccessMessage();

  try {
    application.value = await markRecycleApplicationAsInspecting(
      application.value.applyId,
    );
    selectedStatus.value = application.value.recycleStatus;
    successMessage.value = "回收單已進入現場檢測評估中";
  } catch (error) {
    console.error(error);
    selectedStatus.value = application.value.recycleStatus;
    statusError.value =
      error.response?.status === 409
        ? "目前狀態無法改為現場檢測評估中，請重新載入確認。"
        : error.response?.status === 404
          ? "找不到這筆回收申請。"
          : error.response
            ? `更新檢測狀態失敗（HTTP ${error.response.status}）`
            : "無法連線至伺服器";
  } finally {
    updatingStatus.value = false;
  }
}

async function cancelRecycle() {
  updatingStatus.value = true;
  statusError.value = "";
  closeSuccessMessage();

  try {
    application.value = await cancelRecycleApplication(
      application.value.applyId,
    );
    selectedStatus.value = application.value.recycleStatus;
    agreementRequested.value = false;
    successMessage.value = "回收單已取消，現在可視需要永久刪除。";
  } catch (error) {
    console.error(error);
    selectedStatus.value = application.value.recycleStatus;
    statusError.value =
      error.response?.status === 409
        ? "目前回收進度已無法取消，請重新載入確認。"
        : error.response?.status === 404
          ? "找不到這筆回收申請。"
          : "取消回收申請失敗，請稍後再試。";
  } finally {
    updatingStatus.value = false;
  }
}

async function completeRecycle() {
  updatingStatus.value = true;
  statusError.value = "";
  closeSuccessMessage();

  try {
    application.value = await completeRecycleApplication(
      application.value.applyId,
    );
    selectedStatus.value = application.value.recycleStatus;
    successMessage.value =
      "回收單已結案，商品庫存新增完成，結案通知已寄給會員";
  } catch (error) {
    console.error(error);
    selectedStatus.value = application.value.recycleStatus;
    statusError.value =
      error.response?.status === 400
        ? "回收單的外觀程度或估價無法建立庫存商品，請先確認資料。"
        : error.response?.status === 409
          ? "目前狀態無法完成回收，請重新載入確認。"
          : error.response?.status === 404
            ? "找不到這筆回收申請。"
            : "完成回收失敗，請稍後再試。";
  } finally {
    updatingStatus.value = false;
  }
}

// 只有現場檢測階段可執行估價；成功後沿用完整回收單內容，只更新 estimatedPrice。
async function autoEstimatePrice() {
  if (!application.value || application.value.recycleStatus !== "INSPECTING") {
    return;
  }

  estimateError.value = "";
  estimateResult.value = null;
  closeSuccessMessage();

  let result;
  try {
    // 純前端規則先完成解析；資料不完整時會在此顯示可修正的欄位提示。
    result = estimateRecyclePrice(application.value);
  } catch (error) {
    estimateError.value = error.message;
    return;
  }

  estimatingPrice.value = true;

  try {
    // PUT API 需要完整 RecycleRequest，因此保留原欄位並替換計算出的價格。
    application.value = await updateRecycleApplication(
      application.value.applyId,
      {
        productName: application.value.productName,
        category: application.value.category,
        appearance: application.value.appearance,
        imageUrl: application.value.imageUrl,
        description: application.value.description,
        estimatedPrice: result.price,
        storeId: application.value.storeId,
      },
    );
    selectedStatus.value = application.value.recycleStatus;
    estimateResult.value = result;
    successMessage.value = `自動估價完成：NT$ ${result.price.toLocaleString("zh-TW")}`;
  } catch (error) {
    console.error(error);
    estimateError.value = error.response
      ? `儲存估價失敗（HTTP ${error.response.status}）`
      : "無法連線至伺服器";
  } finally {
    estimatingPrice.value = false;
  }
}

// 選擇「待簽署同意」時先寄 OTP；此時資料庫狀態仍維持 INSPECTING。
async function requestAgreementOtp() {
  if (!application.value || application.value.recycleStatus !== "INSPECTING") {
    return;
  }
  if (application.value.estimatedPrice == null) {
    statusError.value = "請先完成自動估價，再寄送同意驗證碼。";
    return;
  }

  requestingAgreementOtp.value = true;
  statusError.value = "";
  closeSuccessMessage();

  try {
    await requestRecycleAgreementOtp(application.value.applyId);
    agreementRequested.value = true;
    successMessage.value =
      "6 位數驗證碼已寄到會員信箱，請會員在前台回收明細完成簽署";
  } catch (error) {
    console.error(error);
    statusError.value =
      error.response?.status === 409
        ? "目前狀態無法寄送驗證碼，請確認已完成現場檢測與估價。"
        : error.response?.status === 404
          ? "找不到這筆回收申請。"
          : "驗證碼寄送失敗，請稍後再試。";
  } finally {
    requestingAgreementOtp.value = false;
  }
}

function closeSuccessMessage() {
  successMessage.value = "";

  if (successTimer !== null) {
    window.clearTimeout(successTimer);
    successTimer = null;
  }
}

function goBack() {
  router.push({ name: "admin-applyForms" });
}

function goToEdit() {
  router.push({
    name: "admin-applyForms-edit",
    params: { applyId: route.params.applyId },
  });
}

function openDeleteConfirm() {
  // 前端先阻擋誤操作，後端 DELETE API 仍會再次檢查 CANCELLED 狀態。
  if (!application.value || application.value.recycleStatus !== "CANCELLED") {
    return;
  }
  showDeleteConfirm.value = true;
}

function closeDeleteConfirm() {
  if (deleting.value) return;
  showDeleteConfirm.value = false;
}

async function confirmDelete() {
  if (!application.value || application.value.recycleStatus !== "CANCELLED") {
    return;
  }

  deleting.value = true;
  errorMessage.value = "";

  try {
    await deleteRecycleApplication(application.value.applyId);
    showDeleteConfirm.value = false;
    await router.push({ name: "admin-applyForms" });
  } catch (error) {
    console.error(error);
    errorMessage.value =
      error.response?.status === 409
        ? "只有已取消的回收單可以刪除。"
        : error.response
          ? `刪除回收申請失敗（HTTP ${error.response.status}）`
          : "無法連線至伺服器";
  } finally {
    deleting.value = false;
  }
}

onMounted(() => {
  showUpdateSuccess();
  fetchApplication();
});

onBeforeUnmount(() => {
  if (successTimer !== null) {
    window.clearTimeout(successTimer);
  }
});
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <Transition name="success-toast">
      <div
        v-if="successMessage"
        class="success-toast alert alert-success shadow"
        role="status"
        aria-live="polite"
      >
        <div class="d-flex align-items-center gap-2">
          <i class="bi bi-check-circle-fill fs-5"></i>
          <span class="fw-semibold">{{ successMessage }}</span>
          <button
            type="button"
            class="btn-close ms-3"
            aria-label="關閉"
            @click="closeSuccessMessage"
          ></button>
        </div>
      </div>
    </Transition>

    <div class="mx-auto content-width">
      <div
        class="d-flex flex-column flex-sm-row justify-content-between gap-3 mb-4"
      >
        <div>
          <button
            type="button"
            class="btn btn-link text-decoration-none px-0 mb-2"
            @click="goBack"
          >
            <i class="bi bi-arrow-left me-1"></i>
            返回回收申請列表
          </button>
          <h1 class="fw-bold mb-0">回收申請明細</h1>
        </div>

        <div v-if="application" class="d-flex align-items-end gap-2">
          <button
            type="button"
            class="btn btn-outline-primary"
            @click="goToEdit"
          >
            <i class="bi bi-pencil-square me-1"></i>
            編輯
          </button>

          <button
            type="button"
            class="btn btn-outline-danger"
            :disabled="deleting || application.recycleStatus !== 'CANCELLED'"
            :title="
              application.recycleStatus === 'CANCELLED'
                ? '永久刪除此筆回收單'
                : '請先將回收單狀態改為已取消'
            "
            @click="openDeleteConfirm"
          >
            <span
              v-if="deleting"
              class="spinner-border spinner-border-sm me-1"
            ></span>
            <i v-else class="bi bi-trash3 me-1"></i>
            {{ deleting ? "刪除中..." : "刪除" }}
          </button>
        </div>
      </div>

      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
        <button
          type="button"
          class="btn btn-sm btn-outline-danger ms-3"
          @click="fetchApplication"
        >
          重新載入
        </button>
      </div>

      <section v-if="loading" class="card border-0 shadow-sm text-center py-5">
        <div class="spinner-border text-primary mx-auto"></div>
        <div class="text-secondary mt-2">正在載入申請明細...</div>
      </section>

      <section
        v-else-if="application"
        class="card border-0 shadow-sm overflow-hidden"
      >
        <div
          class="card-header bg-white d-flex flex-wrap justify-content-between align-items-center gap-2 py-3"
        >
          <h2 class="fs-5 mb-0">申請編號 #{{ application.applyId }}</h2>
          <span
            class="badge rounded-pill"
            :class="statusClass(application.recycleStatus)"
          >
            {{
              statusLabels[application.recycleStatus] ??
              application.recycleStatus
            }}
          </span>
        </div>

        <div class="card-body p-4">
          <div class="row g-4">
            <div class="col-lg-4">
              <img
                v-if="application.imageUrl"
                :src="application.imageUrl"
                :alt="application.productName"
                class="detail-image"
              />
              <div v-else class="detail-image detail-placeholder">
                <i class="bi bi-image fs-1"></i>
                <span>沒有商品圖片</span>
              </div>
            </div>

            <div class="col-lg-8">
              <dl class="row detail-list mb-0">
                <dt class="col-sm-4">商品名稱</dt>
                <dd class="col-sm-8">{{ application.productName }}</dd>

                <dt class="col-sm-4">商品分類</dt>
                <dd class="col-sm-8">
                  {{
                    categoryLabels[application.category] ?? application.category
                  }}
                </dd>

                <dt class="col-sm-4">會員</dt>
                <dd class="col-sm-8">
                  {{ application.memberName || "—" }}
                  （ID：{{ application.memberId ?? "—" }}）
                </dd>

                <dt class="col-sm-4">商品外觀</dt>
                <dd class="col-sm-8 text-break">
                  {{ getAppearance(application) }}
                </dd>

                <dt class="col-sm-4">商品描述</dt>
                <dd class="col-sm-8 text-break">
                  {{ application.description || "—" }}
                </dd>

                <dt class="col-sm-4">預估價格</dt>
                <dd class="col-sm-8">
                  <div class="d-flex flex-wrap align-items-center gap-2">
                    <span class="estimated-price">
                      {{
                        application.estimatedPrice == null
                          ? "尚未估價"
                          : `NT$ ${Number(application.estimatedPrice).toLocaleString("zh-TW")}`
                      }}
                    </span>
                    <button
                      type="button"
                      class="btn btn-sm btn-outline-success"
                      :disabled="
                        estimatingPrice ||
                        application.recycleStatus !== 'INSPECTING'
                      "
                      @click="autoEstimatePrice"
                    >
                      <span
                        v-if="estimatingPrice"
                        class="spinner-border spinner-border-sm me-1"
                      ></span>
                      <i v-else class="bi bi-calculator me-1"></i>
                      {{ estimatingPrice ? "估價中..." : "自動估價" }}
                    </button>
                  </div>
                  <div
                    v-if="application.recycleStatus !== 'INSPECTING'"
                    class="text-secondary small mt-1"
                  >
                    請先將檢測狀態改為「現場檢測評估中」。
                  </div>
                  <div v-if="estimateResult" class="estimate-breakdown mt-2">
                    <div>
                      {{ estimateResult.modelLabel }}・{{ estimateResult.specificationLabel }}・{{ estimateResult.conditionLabel }}
                    </div>
                    <div>
                      參考區間 NT$ {{ estimateResult.minPrice.toLocaleString("zh-TW") }}～NT$ {{ estimateResult.maxPrice.toLocaleString("zh-TW") }}
                    </div>
                  </div>
                  <div v-if="estimateError" class="text-danger small mt-2">
                    {{ estimateError }}
                  </div>
                </dd>

                <dt class="col-sm-4">回收門市</dt>
                <dd class="col-sm-8">
                  {{ application.storeName || "未指定" }}
                  （ID：{{ application.storeId ?? "—" }}）
                </dd>

                <dt class="col-sm-4">申請時間</dt>
                <dd class="col-sm-8">{{ application.createdTime || "—" }}</dd>

                <dt class="col-sm-4">最後修改時間</dt>
                <dd class="col-sm-8">
                  {{ application.lastModifiedTime || "—" }}
                </dd>

                <template v-if="application.agreementSignedTime">
                  <dt class="col-sm-4">電子簽署時間</dt>
                  <dd class="col-sm-8">
                    {{ application.agreementSignedTime }}
                  </dd>
                </template>
              </dl>
            </div>
          </div>
        </div>

        <div class="card-footer bg-light p-4">
          <div
            class="d-flex flex-column flex-lg-row align-items-lg-end justify-content-between gap-3"
          >
            <div>
              <label for="recycle-status" class="form-label fw-semibold mb-1">
                手機檢測狀態
              </label>
              <div class="text-secondary small">
                狀態依序推進；待簽署階段必須先寄送 OTP，再由客戶輸入驗證碼並簽名。
              </div>
            </div>
            <div class="d-flex flex-column flex-sm-row gap-2 status-controls">
              <select
                id="recycle-status"
                v-model="selectedStatus"
                class="form-select"
                :disabled="
                  updatingStatus ||
                  requestingAgreementOtp ||
                  ![
                    'APPLIED',
                    'INSPECTING',
                    'WAITING_FOR_AGREEMENT',
                    'WIPING',
                  ].includes(
                    application.recycleStatus,
                  )
                "
              >
                <option :value="application.recycleStatus">
                  {{ statusLabels[application.recycleStatus] }}
                </option>
                <option
                  v-if="application.recycleStatus === 'APPLIED'"
                  value="INSPECTING"
                >
                  現場檢測評估中
                </option>
                <option
                  v-if="application.recycleStatus === 'INSPECTING'"
                  value="WAITING_FOR_AGREEMENT"
                >
                  待簽署同意
                </option>
                <option
                  v-if="application.recycleStatus === 'WIPING'"
                  value="COMPLETED"
                >
                  完成回收
                </option>
                <option
                  v-if="
                    ['APPLIED', 'INSPECTING', 'WAITING_FOR_AGREEMENT'].includes(
                      application.recycleStatus,
                    )
                  "
                  value="CANCELLED"
                >
                  已取消
                </option>
              </select>
              <button
                type="button"
                class="btn btn-warning text-nowrap"
                :disabled="
                  updatingStatus ||
                  requestingAgreementOtp ||
                  !(
                    (selectedStatus === 'CANCELLED' &&
                      ['APPLIED', 'INSPECTING', 'WAITING_FOR_AGREEMENT'].includes(
                        application.recycleStatus,
                      )) ||
                    (application.recycleStatus === 'APPLIED' &&
                      selectedStatus === 'INSPECTING') ||
                    (application.recycleStatus === 'INSPECTING' &&
                      selectedStatus === 'WAITING_FOR_AGREEMENT') ||
                    (application.recycleStatus === 'WIPING' &&
                      selectedStatus === 'COMPLETED')
                  )
                "
                @click="updateStatus"
              >
                <span
                  v-if="updatingStatus || requestingAgreementOtp"
                  class="spinner-border spinner-border-sm me-1"
                ></span>
                {{
                  updatingStatus
                    ? "更新中..."
                    : requestingAgreementOtp
                      ? "寄送中..."
                      : selectedStatus === "CANCELLED"
                        ? "確認取消"
                        : selectedStatus === "WAITING_FOR_AGREEMENT"
                          ? agreementRequested
                            ? "重新寄送 OTP"
                            : "寄送 OTP"
                          : "確認更新"
                }}
              </button>
            </div>
          </div>
          <div v-if="statusError" class="alert alert-danger mt-3 mb-0">
            {{ statusError }}
          </div>
        </div>
      </section>
    </div>

    <Transition name="delete-alert">
      <div
        v-if="showDeleteConfirm"
        class="delete-alert-backdrop"
        @click.self="!deleting && closeDeleteConfirm()"
      >
        <section
          class="delete-alert-dialog"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="delete-dialog-title"
        >
          <div class="delete-alert-header">
            <div class="warning-icon">
              <i class="bi bi-exclamation-triangle-fill"></i>
            </div>
            <div>
              <div class="warning-label">危險操作</div>
              <h2 id="delete-dialog-title" class="mb-0">確認刪除回收申請</h2>
            </div>
          </div>

          <div class="delete-alert-body">
            <p class="main-message">您即將刪除以下回收申請，請確認是否繼續。</p>
            <div class="target-detail">{{ deleteDialogDetail }}</div>
            <div class="irreversible-warning">
              <i class="bi bi-shield-exclamation me-2"></i>
              此操作完成後無法復原，請再次確認。
            </div>
          </div>

          <div class="delete-alert-footer">
            <button
              type="button"
              class="btn btn-lg btn-outline-secondary"
              :disabled="deleting"
              @click="closeDeleteConfirm"
            >
              取消
            </button>
            <button
              type="button"
              class="btn btn-lg btn-danger"
              :disabled="deleting"
              @click="confirmDelete"
            >
              <span
                v-if="deleting"
                class="spinner-border spinner-border-sm me-2"
              ></span>
              <i v-else class="bi bi-trash3-fill me-2"></i>
              {{ deleting ? "刪除中..." : "確認刪除" }}
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </main>
</template>

<style scoped>
main {
  min-height: 100vh;
  background: #f8f9ff;
}

.content-width {
  max-width: 1100px;
}

.success-toast {
  position: fixed;
  top: 1.25rem;
  right: 1.25rem;
  z-index: 1080;
  margin: 0;
  border-color: #badbcc;
}

.success-toast-enter-active,
.success-toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.success-toast-enter-from,
.success-toast-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

.detail-image {
  width: 100%;
  min-height: 260px;
  max-height: 420px;
  border: 1px solid #dee2e6;
  border-radius: 0.75rem;
  object-fit: contain;
  background: #f8f9fa;
}

.detail-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: #6c757d;
}

.detail-list dt {
  color: #6c757d;
  font-weight: 500;
}

.detail-list dd {
  margin-bottom: 1.1rem;
}

.estimated-price {
  color: #198754;
  font-size: 1.15rem;
  font-weight: 700;
}

.estimate-breakdown {
  padding: 0.65rem 0.75rem;
  border-left: 3px solid #198754;
  border-radius: 0.25rem;
  color: #386641;
  font-size: 0.82rem;
  background: #eef8f1;
}

.status-controls .form-select {
  min-width: 220px;
}

.delete-alert-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgb(33 37 41 / 72%);
  backdrop-filter: blur(2px);
}

.delete-alert-dialog {
  width: min(100%, 660px);
  overflow: hidden;
  border: 3px solid #dc3545;
  border-radius: 1rem;
  background: #fff;
  box-shadow: 0 1.5rem 4rem rgb(0 0 0 / 35%);
}

.delete-alert-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem 1.75rem;
  color: #fff;
  background: #dc3545;
}

.delete-alert-header h2 {
  font-size: 1.65rem;
  font-weight: 700;
}

.warning-icon {
  display: grid;
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  place-items: center;
  border: 2px solid rgb(255 255 255 / 75%);
  border-radius: 50%;
  font-size: 1.8rem;
}

.warning-label {
  margin-bottom: 0.2rem;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  opacity: 0.85;
}

.delete-alert-body {
  padding: 2rem 1.75rem 1.5rem;
}

.main-message {
  margin-bottom: 1rem;
  font-size: 1.2rem;
  font-weight: 600;
}

.target-detail {
  padding: 1rem;
  border-left: 5px solid #dc3545;
  border-radius: 0.35rem;
  color: #842029;
  font-size: 1.05rem;
  font-weight: 700;
  overflow-wrap: anywhere;
  background: #f8d7da;
}

.irreversible-warning {
  margin-top: 1.25rem;
  color: #842029;
}

.delete-alert-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.75rem 1.5rem;
  border-top: 1px solid #f1aeb5;
  background: #fff5f5;
}

.delete-alert-enter-active,
.delete-alert-leave-active {
  transition: opacity 0.2s ease;
}

.delete-alert-enter-from,
.delete-alert-leave-to {
  opacity: 0;
}

@media (max-width: 575.98px) {
  .success-toast {
    top: 0.75rem;
    right: 0.75rem;
    left: 0.75rem;
  }
}
</style>
