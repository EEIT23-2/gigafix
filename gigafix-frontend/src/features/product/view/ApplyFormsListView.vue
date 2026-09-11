<script setup>
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import {
  deleteAllRecycleApplications,
  deleteRecycleApplication,
  exportRecycleApplications,
  getRecycleApplications,
} from "../api";
import ApplyFormTable from "../components/ApplyFormTable.vue";
import { useRecycleApplicationStore } from "../store";

const router = useRouter();
const recycleApplicationStore = useRecycleApplicationStore();

const {
  page,
  size,
  applyId: searchApplyId,
  productName: searchProductName,
  appearance: searchAppearance,
  category: searchCategory,
  recycleStatus: searchStatus,
  sortOption,
} = storeToRefs(recycleApplicationStore);

const applications = ref([]);
const searchMemberId = ref("");
const totalElements = ref(0);
const totalPages = ref(0);
const loading = ref(false);
const deletingId = ref(null);
const deletingAll = ref(false);
const exporting = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const showDeleteConfirm = ref(false);
const deleteMode = ref("");
const deleteTarget = ref(null);

const categoryOptions = [
  { value: "IPHONE", label: "iPhone" },
  { value: "WATCH", label: "Apple Watch" },
  { value: "IPAD", label: "iPad" },
];

const statusOptions = [
  { value: "CANCELLED", label: "已取消" },
  { value: "APPLIED", label: "已提出申請" },
  { value: "INSPECTING", label: "檢查估價中" },
  { value: "WAITING_FOR_AGREEMENT", label: "等待客戶同意" },
  { value: "WIPING", label: "資料清除中" },
  { value: "COMPLETED", label: "回收完成" },
];

const paginationItems = computed(() => {
  const count = totalPages.value;
  const currentPage = page.value;
  const lastPage = count - 1;

  if (count <= 0) return [];
  if (count <= 7) {
    return Array.from({ length: count }, (_, index) => index);
  }
  if (currentPage <= 3) {
    return [0, 1, 2, 3, 4, "right-ellipsis", lastPage];
  }
  if (currentPage >= lastPage - 3) {
    return [
      0,
      "left-ellipsis",
      lastPage - 4,
      lastPage - 3,
      lastPage - 2,
      lastPage - 1,
      lastPage,
    ];
  }
  return [
    0,
    "left-ellipsis",
    currentPage - 1,
    currentPage,
    currentPage + 1,
    "right-ellipsis",
    lastPage,
  ];
});

const startItem = computed(() =>
  totalElements.value === 0 ? 0 : page.value * size.value + 1,
);
const endItem = computed(() =>
  Math.min((page.value + 1) * size.value, totalElements.value),
);

const deleteDialogTitle = computed(() =>
  deleteMode.value === "all" ? "確認刪除所有回收申請" : "確認刪除回收申請",
);

const deleteDialogMessage = computed(() =>
  deleteMode.value === "all"
    ? "您即將刪除系統內所有回收申請，請確認是否繼續。"
    : "您即將刪除以下回收申請，請確認是否繼續。",
);

const deleteDialogDetail = computed(() => {
  if (deleteMode.value === "all") {
    return `全部 ${totalElements.value} 筆回收申請`;
  }
  if (!deleteTarget.value) return "";
  return `申請 #${deleteTarget.value.applyId}－${deleteTarget.value.productName}`;
});

const deleteDialogLoading = computed(() =>
  deleteMode.value === "all" ? deletingAll.value : deletingId.value !== null,
);

function buildQueryParams(targetPage) {
  const [orderBy, sort] = sortOption.value.split(":");

  return {
    limit: size.value,
    offset: targetPage * size.value,
    orderBy,
    sort,
    ...(searchApplyId.value && { applyId: searchApplyId.value }),
    ...(searchMemberId.value && { memberId: searchMemberId.value }),
    ...(searchProductName.value && { productName: searchProductName.value }),
    ...(searchAppearance.value && { appearance: searchAppearance.value }),
    ...(searchCategory.value && { productCategory: searchCategory.value }),
    ...(searchStatus.value && { recycleStatus: searchStatus.value }),
  };
}

async function fetchApplications(targetPage = page.value) {
  loading.value = true;
  errorMessage.value = "";

  try {
    const data = await getRecycleApplications(buildQueryParams(targetPage));
    applications.value = data.content ?? [];
    page.value = data.number ?? targetPage;
    size.value = data.size ?? size.value;
    totalElements.value = data.totalElements ?? 0;
    totalPages.value = data.totalPages ?? 0;
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `讀取回收申請失敗（HTTP ${error.response.status}）`
      : "無法連線至伺服器";
  } finally {
    loading.value = false;
  }
}

function search() {
  if (searchApplyId.value && !/^[1-9]\d*$/.test(searchApplyId.value)) {
    errorMessage.value = "回收單 ID 必須是正整數";
    return;
  }
  if (searchMemberId.value && !/^[1-9]\d*$/.test(searchMemberId.value)) {
    errorMessage.value = "會員 ID 必須是正整數";
    return;
  }
  fetchApplications(0);
}

function resetSearch() {
  searchMemberId.value = "";
  recycleApplicationStore.resetListState();
  fetchApplications(0);
}

function changePage(targetPage) {
  if (
    targetPage < 0 ||
    targetPage >= totalPages.value ||
    targetPage === page.value
  )
    return;
  fetchApplications(targetPage);
}

function goToFirstPage() {
  changePage(0);
}

function goToPreviousPage() {
  changePage(page.value - 1);
}

function goToNextPage() {
  changePage(page.value + 1);
}

function goToLastPage() {
  changePage(totalPages.value - 1);
}

function goToDetail(application) {
  router.push({
    name: "admin-applyForms-detail",
    params: { applyId: application.applyId },
  });
}

function goToEdit(application) {
  router.push({
    name: "admin-applyForms-edit",
    params: { applyId: application.applyId },
  });
}

function openSingleDeleteConfirm(application) {
  deleteMode.value = "single";
  deleteTarget.value = application;
  showDeleteConfirm.value = true;
}

function openDeleteAllConfirm() {
  if (totalElements.value === 0) return;
  deleteMode.value = "all";
  deleteTarget.value = null;
  showDeleteConfirm.value = true;
}

function closeDeleteConfirm() {
  if (deleteDialogLoading.value) return;
  showDeleteConfirm.value = false;
  deleteMode.value = "";
  deleteTarget.value = null;
}

async function confirmDelete() {
  if (deleteMode.value === "all") {
    await confirmDeleteAll();
  } else {
    await confirmDeleteSingle();
  }
}

async function confirmDeleteSingle() {
  const application = deleteTarget.value;
  if (!application) return;

  deletingId.value = application.applyId;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    await deleteRecycleApplication(application.applyId);
    showDeleteConfirm.value = false;
    deleteMode.value = "";
    deleteTarget.value = null;
    successMessage.value = `回收申請 #${application.applyId} 已刪除`;
    const targetPage =
      applications.value.length === 1 && page.value > 0
        ? page.value - 1
        : page.value;
    await fetchApplications(targetPage);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `刪除回收申請失敗（HTTP ${error.response.status}）`
      : "無法連線至伺服器";
  } finally {
    deletingId.value = null;
  }
}

async function confirmDeleteAll() {
  deletingAll.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    await deleteAllRecycleApplications();
    showDeleteConfirm.value = false;
    deleteMode.value = "";
    deleteTarget.value = null;
    recycleApplicationStore.resetListState();
    successMessage.value = "全部回收申請已刪除";
    await fetchApplications(0);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `刪除全部回收申請失敗（HTTP ${error.response.status}）`
      : "無法連線至伺服器";
  } finally {
    deletingAll.value = false;
  }
}

async function handleExport() {
  exporting.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const blobData = await exportRecycleApplications();
    const blob =
      blobData instanceof Blob
        ? blobData
        : new Blob([blobData], { type: "application/json" });
    const downloadUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = downloadUrl;
    link.download = `recycle-applications-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(downloadUrl);

    successMessage.value = "回收申請 JSON 已成功匯出";
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `匯出失敗（HTTP ${error.response.status}）`
      : "無法連線至伺服器";
  } finally {
    exporting.value = false;
  }
}

// 使用 Pinia 中保留的頁碼及篩選條件重新查詢。
onMounted(() => fetchApplications(page.value));
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="mx-auto content-width">
      <header
        class="d-flex flex-column flex-md-row align-items-md-end justify-content-between gap-3 mb-4"
      >
        <div>
          <div class="d-flex align-items-center gap-3">
            <h1 class="fw-bold mb-0">回收申請管理</h1>
            <span class="badge rounded-pill text-bg-light border"
              >共 {{ totalElements }} 筆</span
            >
          </div>
          <p class="text-secondary mb-0 mt-2">
            查詢、檢視及管理會員提交的商品回收申請。
          </p>
        </div>

        <div class="d-flex flex-wrap gap-2">
          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="exporting || deletingAll"
            @click="handleExport"
          >
            <span
              v-if="exporting"
              class="spinner-border spinner-border-sm me-1"
              aria-hidden="true"
            ></span>
            <i v-else class="bi bi-download me-1" aria-hidden="true"></i>
            {{ exporting ? "匯出中..." : "匯出 JSON" }}
          </button>

          <button
            type="button"
            class="btn btn-outline-danger"
            :disabled="deletingAll || exporting || totalElements === 0"
            @click="openDeleteAllConfirm"
          >
            <span
              v-if="deletingAll"
              class="spinner-border spinner-border-sm me-1"
            ></span>
            <i v-else class="bi bi-trash3 me-1"></i>
            {{ deletingAll ? "刪除中..." : "刪除全部" }}
          </button>
        </div>
      </header>

      <section class="card border-0 shadow-sm mb-4">
        <div class="card-body d-flex flex-column flex-lg-row flex-wrap gap-3">
          <input
            v-model.trim="searchApplyId"
            type="text"
            inputmode="numeric"
            class="form-control apply-id-input"
            placeholder="搜尋回收單 ID"
            aria-label="搜尋回收單 ID"
            @keyup.enter="search"
          />
          <input
            v-model.trim="searchMemberId"
            type="text"
            inputmode="numeric"
            class="form-control member-id-input"
            placeholder="搜尋會員 ID"
            aria-label="搜尋會員 ID"
            @keyup.enter="search"
          />
          <input
            v-model.trim="searchProductName"
            type="search"
            class="form-control search-input"
            placeholder="搜尋商品名稱"
            @keyup.enter="search"
          />
          <input
            v-model.trim="searchAppearance"
            type="search"
            class="form-control search-input"
            placeholder="搜尋商品外觀"
            @keyup.enter="search"
          />
          <select
            v-model="searchCategory"
            class="form-select filter-select"
            @change="search"
          >
            <option value="">全部分類</option>
            <option
              v-for="item in categoryOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </option>
          </select>
          <select
            v-model="searchStatus"
            class="form-select filter-select"
            @change="search"
          >
            <option value="">全部狀態</option>
            <option
              v-for="item in statusOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </option>
          </select>
          <select
            v-model="sortOption"
            class="form-select sort-select"
            @change="search"
          >
            <option value="createdTime:desc">申請時間：新到舊</option>
            <option value="createdTime:asc">申請時間：舊到新</option>
            <option value="estimatedPrice:desc">估價：高到低</option>
            <option value="estimatedPrice:asc">估價：低到高</option>
            <option value="productName:asc">商品名稱：正序</option>
            <option value="productName:desc">商品名稱：倒序</option>
          </select>
          <select
            v-model.number="size"
            class="form-select page-size-select"
            @change="search"
          >
            <option :value="10">每頁 10 筆</option>
            <option :value="20">每頁 20 筆</option>
            <option :value="50">每頁 50 筆</option>
          </select>
          <button
            type="button"
            class="btn btn-primary text-nowrap"
            @click="search"
          >
            <i class="bi bi-search me-1"></i>查詢
          </button>
          <button
            type="button"
            class="btn btn-outline-secondary text-nowrap"
            @click="resetSearch"
          >
            清除條件
          </button>
        </div>
      </section>

      <div v-if="errorMessage" class="alert alert-danger alert-dismissible">
        {{ errorMessage }}
        <button
          type="button"
          class="btn-close"
          aria-label="關閉"
          @click="errorMessage = ''"
        ></button>
      </div>
      <div v-if="successMessage" class="alert alert-success alert-dismissible">
        {{ successMessage }}
        <button
          type="button"
          class="btn-close"
          aria-label="關閉"
          @click="successMessage = ''"
        ></button>
      </div>

      <section class="card border-0 shadow-sm overflow-hidden">
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-primary"></div>
          <div class="text-secondary mt-2">正在載入回收申請...</div>
        </div>

        <ApplyFormTable
          v-else
          :applications="applications"
          :deleting-id="deletingId"
          @view="goToDetail"
          @edit="goToEdit"
          @delete="openSingleDeleteConfirm"
        />

        <footer
          class="card-footer bg-white d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 py-3"
        >
          <div class="text-secondary">
            顯示第 {{ startItem }} 至 {{ endItem }} 筆，共
            {{ totalElements }} 筆
          </div>
          <nav aria-label="回收申請分頁">
            <ul class="pagination pagination-sm mb-0">
              <li
                class="page-item"
                :class="{ disabled: page === 0 || totalPages === 0 }"
              >
                <button class="page-link" @click="goToFirstPage">最前頁</button>
              </li>
              <li
                class="page-item"
                :class="{ disabled: page === 0 || totalPages === 0 }"
              >
                <button class="page-link" @click="goToPreviousPage">
                  上一頁
                </button>
              </li>
              <template v-for="item in paginationItems" :key="item">
                <li
                  v-if="item === 'left-ellipsis' || item === 'right-ellipsis'"
                  class="page-item disabled"
                >
                  <span class="page-link">…</span>
                </li>
                <li v-else class="page-item" :class="{ active: item === page }">
                  <button class="page-link" @click="changePage(item)">
                    {{ item + 1 }}
                  </button>
                </li>
              </template>
              <li
                class="page-item"
                :class="{
                  disabled: totalPages === 0 || page >= totalPages - 1,
                }"
              >
                <button class="page-link" @click="goToNextPage">下一頁</button>
              </li>
              <li
                class="page-item"
                :class="{
                  disabled: totalPages === 0 || page >= totalPages - 1,
                }"
              >
                <button class="page-link" @click="goToLastPage">最後頁</button>
              </li>
            </ul>
          </nav>
        </footer>
      </section>
    </div>

    <Transition name="delete-alert">
      <div
        v-if="showDeleteConfirm"
        class="delete-alert-backdrop"
        @click.self="!deleteDialogLoading && closeDeleteConfirm()"
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
              <h2 id="delete-dialog-title" class="mb-0">
                {{ deleteDialogTitle }}
              </h2>
            </div>
          </div>

          <div class="delete-alert-body">
            <p class="main-message">{{ deleteDialogMessage }}</p>
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
              :disabled="deleteDialogLoading"
              @click="closeDeleteConfirm"
            >
              取消
            </button>
            <button
              type="button"
              class="btn btn-lg btn-danger"
              :disabled="deleteDialogLoading"
              @click="confirmDelete"
            >
              <span
                v-if="deleteDialogLoading"
                class="spinner-border spinner-border-sm me-2"
              ></span>
              <i v-else class="bi bi-trash3-fill me-2"></i>
              {{
                deleteDialogLoading
                  ? "刪除中..."
                  : deleteMode === "all"
                    ? "刪除全部"
                    : "確認刪除"
              }}
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
  max-width: 1700px;
}
.search-input {
  flex: 1 1 210px;
}
.apply-id-input,
.member-id-input {
  flex: 0 1 180px;
}
.filter-select {
  max-width: 180px;
}
.sort-select {
  max-width: 210px;
}
.page-size-select {
  max-width: 145px;
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
@media (max-width: 991.98px) {
  .filter-select,
  .sort-select,
  .page-size-select {
    max-width: none;
  }
}
</style>
