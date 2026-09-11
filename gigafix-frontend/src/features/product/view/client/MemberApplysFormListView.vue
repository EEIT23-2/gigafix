<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  getMemberRecycleApplication,
  getMemberRecycleApplications,
} from "../../api";

const router = useRouter();

const applications = ref([]);
const page = ref(0);
const size = ref(8);
const totalPages = ref(0);
const totalElements = ref(0);
const loading = ref(false);
const errorMessage = ref("");
const searchApplyId = ref("");
const searchLoading = ref(false);
const searchError = ref("");

const categoryLabels = {
  IPHONE: "iPhone",
  IPAD: "iPad",
  WATCH: "Apple Watch",
};

const statusLabels = {
  CANCELLED: "已取消",
  APPLIED: "已預約交件",
  INSPECTING: "現場檢測評估中",
  WAITING_FOR_AGREEMENT: "待簽署同意",
  WIPING: "資料清除中",
  COMPLETED: "完成回收",
};

const startItem = computed(() =>
  totalElements.value === 0 ? 0 : page.value * size.value + 1,
);
const endItem = computed(() =>
  Math.min((page.value + 1) * size.value, totalElements.value),
);

function statusLabel(status) {
  return statusLabels[status] ?? status ?? "狀態未設定";
}

function formatPrice(price) {
  if (price == null) return "尚未估價";
  return `NT$ ${Number(price).toLocaleString("zh-TW")}`;
}

function formatDateTime(dateTime) {
  if (!dateTime) return "—";
  return String(dateTime).replace("T", " ");
}

async function fetchApplications(targetPage = page.value) {
  loading.value = true;
  errorMessage.value = "";

  try {
    const data = await getMemberRecycleApplications({
      limit: size.value,
      offset: targetPage * size.value,
      orderBy: "createdTime",
      sort: "desc",
    });

    applications.value = data.content ?? [];
    page.value = data.number ?? targetPage;
    size.value = data.size ?? size.value;
    totalPages.value = data.totalPages ?? 0;
    totalElements.value = data.totalElements ?? 0;
  } catch (error) {
    console.error(error);
    applications.value = [];
    totalPages.value = 0;
    totalElements.value = 0;
    errorMessage.value =
      error?.response?.status === 401
        ? "登入狀態已失效，請重新登入。"
        : "目前無法載入回收手機紀錄，請稍後再試。";
  } finally {
    loading.value = false;
  }
}

function goToDetail(application) {
  router.push({
    name: "member-recycle-application-detail",
    params: { applyId: application.applyId },
  });
}

async function searchApplication() {
  const applyId = Number(searchApplyId.value);

  if (!Number.isInteger(applyId) || applyId <= 0) {
    searchError.value = "請輸入正確的回收單號";
    return;
  }

  searchLoading.value = true;
  searchError.value = "";

  try {
    await getMemberRecycleApplication(applyId);
    await router.push({
      name: "member-recycle-application-detail",
      params: { applyId },
    });
  } catch (error) {
    console.error(error);
    searchError.value =
      error?.response?.status === 404
        ? "找不到這筆回收申請，請確認回收單號。"
        : error?.response?.status === 401
          ? "登入狀態已失效，請重新登入。"
          : "目前無法查詢回收單，請稍後再試。";
  } finally {
    searchLoading.value = false;
  }
}

function changePage(targetPage) {
  if (
    targetPage < 0 ||
    targetPage >= totalPages.value ||
    targetPage === page.value
  ) {
    return;
  }

  fetchApplications(targetPage);
}

onMounted(() => fetchApplications());
</script>

<template>
  <section class="member-applications">
    <header class="page-header">
      <div>
        <p class="eyebrow">RECYCLE HISTORY</p>
        <h1>回收手機紀錄</h1>
        <p class="subtitle">查看您提交的回收申請與目前處理進度。</p>
      </div>
      <span class="record-count">共 {{ totalElements }} 筆</span>
    </header>

    <form class="application-search" @submit.prevent="searchApplication">
      <label for="member-recycle-list-apply-id">查詢回收單號</label>
      <div class="search-row">
        <div class="search-input-wrap">
          <span aria-hidden="true">#</span>
          <input
            id="member-recycle-list-apply-id"
            v-model.trim="searchApplyId"
            type="text"
            inputmode="numeric"
            autocomplete="off"
            placeholder="請輸入回收單號"
            aria-describedby="member-recycle-list-search-error"
          />
        </div>
        <button type="submit" :disabled="searchLoading">
          <span
            v-if="searchLoading"
            class="spinner-border spinner-border-sm"
            aria-hidden="true"
          ></span>
          <i v-else class="bi bi-search" aria-hidden="true"></i>
          {{ searchLoading ? "查詢中..." : "查詢" }}
        </button>
      </div>
      <p
        v-if="searchError"
        id="member-recycle-list-search-error"
        class="search-error"
        role="alert"
      >
        {{ searchError }}
      </p>
    </form>

    <div v-if="errorMessage" class="state-card error-state" role="alert">
      <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
      <p>{{ errorMessage }}</p>
      <button type="button" @click="fetchApplications(page)">重新載入</button>
    </div>

    <div v-else-if="loading" class="state-card" aria-live="polite">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">載入中</span>
      </div>
      <p>正在載入回收紀錄...</p>
    </div>

    <div v-else-if="applications.length === 0" class="state-card empty-state">
      <i class="bi bi-recycle" aria-hidden="true"></i>
      <h2>目前沒有回收紀錄</h2>
      <p>送出回收申請後，處理進度會顯示在這裡。</p>
    </div>

    <div v-else class="application-list">
      <button
        v-for="application in applications"
        :key="application.applyId"
        class="application-card"
        type="button"
        @click="goToDetail(application)"
      >
        <div class="product-image-wrap">
          <img
            v-if="application.imageUrl"
            :src="application.imageUrl"
            :alt="application.productName"
          />
          <i v-else class="bi bi-phone" aria-hidden="true"></i>
        </div>

        <div class="application-main">
          <div class="card-heading">
            <div>
              <span class="application-number"
                >申請編號 #{{ application.applyId }}</span
              >
              <h2>{{ application.productName || "未命名裝置" }}</h2>
            </div>
            <span
              class="status-badge"
              :class="`status-${application.recycleStatus?.toLowerCase()}`"
            >
              {{ statusLabel(application.recycleStatus) }}
            </span>
          </div>

          <dl class="application-meta">
            <div>
              <dt>裝置類型</dt>
              <dd>
                {{
                  categoryLabels[application.category] ??
                  application.category ??
                  "—"
                }}
              </dd>
            </div>
            <div>
              <dt>預估價格</dt>
              <dd>{{ formatPrice(application.estimatedPrice) }}</dd>
            </div>
            <div>
              <dt>回收門市</dt>
              <dd>{{ application.storeName || "未指定" }}</dd>
            </div>
            <div>
              <dt>申請時間</dt>
              <dd>{{ formatDateTime(application.createdTime) }}</dd>
            </div>
          </dl>
        </div>

        <i class="bi bi-chevron-right card-arrow" aria-hidden="true"></i>
      </button>
    </div>

    <footer v-if="totalPages > 1" class="pagination-bar">
      <span>顯示第 {{ startItem }}–{{ endItem }} 筆</span>
      <div class="pagination-actions">
        <button
          type="button"
          :disabled="page === 0"
          @click="changePage(page - 1)"
        >
          <i class="bi bi-chevron-left" aria-hidden="true"></i>
          上一頁
        </button>
        <span>第 {{ page + 1 }} / {{ totalPages }} 頁</span>
        <button
          type="button"
          :disabled="page >= totalPages - 1"
          @click="changePage(page + 1)"
        >
          下一頁
          <i class="bi bi-chevron-right" aria-hidden="true"></i>
        </button>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.member-applications {
  --blue: #1769aa;
  --ink: #1d324b;
  --muted: #6b7785;
  width: min(100%, 1120px);
  margin: 0 auto;
  color: var(--ink);
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--blue);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.15em;
}

h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 44px);
  font-weight: 750;
  letter-spacing: -0.03em;
}

.subtitle {
  margin: 8px 0 0;
  color: var(--muted);
}

.record-count {
  flex: 0 0 auto;
  padding: 8px 14px;
  border: 1px solid #dce5ef;
  border-radius: 999px;
  color: var(--blue);
  background: #fff;
  font-size: 14px;
  font-weight: 700;
}

.application-search {
  margin-bottom: 22px;
  padding: 20px 22px;
  border: 1px solid #e1e8ef;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(37 64 91 / 7%);
}

.application-search label {
  display: block;
  margin-bottom: 9px;
  font-size: 15px;
  font-weight: 700;
}

.search-row {
  display: flex;
  gap: 10px;
}

.search-input-wrap {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  overflow: hidden;
  border: 1px solid #cbd7e2;
  border-radius: 10px;
  background: #f8fafc;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.search-input-wrap:focus-within {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgb(23 105 170 / 12%);
}

.search-input-wrap span {
  padding-left: 14px;
  color: var(--muted);
  font-weight: 700;
}

.search-input-wrap input {
  width: 100%;
  min-width: 0;
  padding: 11px 14px 11px 7px;
  border: 0;
  outline: 0;
  color: var(--ink);
  background: transparent;
  font-size: 16px;
}

.search-row > button {
  display: inline-flex;
  min-width: 112px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 10px 18px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  background: var(--blue);
  font-weight: 700;
}

.search-row > button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.search-error {
  margin: 8px 0 0;
  color: #b02a37;
  font-size: 14px;
}

.application-list {
  display: grid;
  gap: 14px;
}

.application-card {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  width: 100%;
  padding: 20px;
  border: 1px solid #e1e8ef;
  border-radius: 18px;
  color: inherit;
  text-align: left;
  background: #fff;
  box-shadow: 0 10px 28px rgb(37 64 91 / 7%);
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.application-card:hover,
.application-card:focus-visible {
  border-color: #9fc2e3;
  box-shadow: 0 16px 36px rgb(37 64 91 / 13%);
  transform: translateY(-2px);
  outline: none;
}

.product-image-wrap {
  display: grid;
  width: 112px;
  height: 96px;
  place-items: center;
  overflow: hidden;
  border-radius: 13px;
  color: #8ca3b8;
  background: #f1f5f9;
  font-size: 34px;
}

.product-image-wrap img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.application-main {
  min-width: 0;
}

.card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.application-number {
  color: var(--muted);
  font-size: 13px;
}

.card-heading h2 {
  margin: 4px 0 0;
  font-size: 21px;
  font-weight: 700;
}

.status-badge {
  flex: 0 0 auto;
  padding: 7px 11px;
  border-radius: 999px;
  color: #23527c;
  background: #eaf3fb;
  font-size: 12px;
  font-weight: 700;
}

.status-cancelled {
  color: #646b73;
  background: #eef0f2;
}

.status-inspecting,
.status-waiting_for_agreement {
  color: #8a5b00;
  background: #fff2cf;
}

.status-wiping {
  color: #6651a3;
  background: #f0ebff;
}

.status-completed {
  color: #24704a;
  background: #e2f5ea;
}

.application-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 24px;
  margin: 18px 0 0;
}

.application-meta dt {
  margin-bottom: 3px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 500;
}

.application-meta dd {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: 14px;
  font-weight: 650;
}

.card-arrow {
  color: #88a0b5;
  font-size: 20px;
}

.state-card {
  display: grid;
  min-height: 260px;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 32px;
  border: 1px solid #e1e8ef;
  border-radius: 18px;
  text-align: center;
  background: #fff;
}

.state-card p,
.state-card h2 {
  margin: 0;
}

.state-card > i {
  color: #8da6ba;
  font-size: 42px;
}

.state-card button,
.pagination-actions button {
  padding: 9px 16px;
  border: 1px solid #bed0e0;
  border-radius: 9px;
  color: var(--blue);
  background: #fff;
  font-weight: 700;
}

.error-state {
  color: #9d2e2e;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  color: var(--muted);
  font-size: 14px;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

@media (max-width: 900px) {
  .application-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .page-header,
  .card-heading,
  .pagination-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .application-card {
    grid-template-columns: 76px minmax(0, 1fr);
    gap: 14px;
    padding: 16px;
  }

  .search-row {
    flex-direction: column;
  }

  .search-row > button {
    width: 100%;
  }

  .product-image-wrap {
    width: 76px;
    height: 76px;
  }

  .card-heading {
    gap: 10px;
  }

  .application-meta {
    grid-template-columns: 1fr;
  }

  .card-arrow {
    display: none;
  }

  .pagination-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
