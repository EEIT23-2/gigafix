<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { getMyRepairs } from "../api";
import { swalInfo, useSwalMessages } from "../../../utils/swal";

const router = useRouter();

const repairs = ref([]);
const loading = ref(false);
const errorMessage = ref("");
useSwalMessages(errorMessage, null);

// 狀態的中文顯示，跟 RepairDetailView 用同一套對照表
const STATUS_LABELS = {
  PENDING_QUOTE: "待估價",
  QUOTED: "已報價",
  IN_REPAIR: "維修中",
  QUOTE_REJECTED: "報價後不維修",
  REPAIR_COMPLETED: "維修完成",
  AWAITING_PICKUP: "尚未取件",
  CLOSED: "已結案",
  CANCELLED: "已取消",
  NOT_DROPPED_OFF: "未送檢",
};
// 狀態對應的徽章顏色：介於原本飽和版(text-bg-*)跟太淺的 subtle 版中間，
// 底色用 Bootstrap 5.3 的 border-subtle 色階(比 bg-subtle 深一階)，文字維持 emphasis 深色
const STATUS_BADGE_CLASS = {
  PENDING_QUOTE: "badge-mid-warning text-warning-emphasis",
  QUOTED: "badge-mid-info text-info-emphasis",
  IN_REPAIR: "badge-mid-primary text-primary-emphasis",
  QUOTE_REJECTED: "badge-mid-danger text-danger-emphasis",
  REPAIR_COMPLETED: "badge-mid-success text-success-emphasis",
  AWAITING_PICKUP: "badge-mid-purple",
  CLOSED: "badge-mid-dark text-secondary-emphasis",
  CANCELLED: "badge-mid-secondary text-secondary-emphasis",
  NOT_DROPPED_OFF: "badge-mid-secondary text-secondary-emphasis",
};
function statusLabel(status) {
  return STATUS_LABELS[status] ?? status;
}
function statusBadgeClass(status) {
  return STATUS_BADGE_CLASS[status] ?? "badge-mid-secondary text-secondary-emphasis";
}

async function fetchRepairs() {
  loading.value = true;
  errorMessage.value = "";
  try {
    repairs.value = await getMyRepairs();
    if (needsResponseCount.value > 0) {
      swalInfo(
        `您有 ${needsResponseCount.value} 張維修單已完成報價，請點進去確認是否維修。`,
        "報價待確認",
      );
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `查詢失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

// 已報價、且客戶還沒回應的維修單，額外提示要去確認
const needsResponseCount = computed(
  () =>
    repairs.value.filter(
      (r) => r.repairStatus === "QUOTED" && r.approvalStatus === "PENDING",
    ).length,
);

function goToDetail(repair) {
  router.push({ name: "membercenter-repair-detail", params: { repairId: repair.id } });
}

// ===== 分頁：10/20筆一頁或顯示全部，純前端切分，不用重打API =====
const pageSize = ref(10); // 10 | 20 | "all"
const currentPage = ref(1);

const totalPages = computed(() => {
  if (pageSize.value === "all") return 1;
  return Math.max(1, Math.ceil(repairs.value.length / pageSize.value));
});

const pagedRepairs = computed(() => {
  if (pageSize.value === "all") return repairs.value;
  const start = (currentPage.value - 1) * pageSize.value;
  return repairs.value.slice(start, start + pageSize.value);
});

watch([pageSize, repairs], () => {
  currentPage.value = 1;
});

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
}

onMounted(() => {
  fetchRepairs();
});
</script>

<template>
  <main>
    <h1 class="fw-bold mb-4">維修進度</h1>


    <!-- 分頁：選每頁筆數、上一頁/下一頁，放在列表上方 -->
    <div
      v-if="!loading && repairs.length > 0"
      class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3"
    >
      <div class="d-flex align-items-center gap-2">
        <span class="text-secondary">每頁顯示</span>
        <select v-model="pageSize" class="form-select form-select-sm page-size-select">
          <option :value="10">10 筆</option>
          <option :value="20">20 筆</option>
          <option value="all">全部</option>
        </select>
      </div>
      <nav v-if="pageSize !== 'all' && totalPages > 1">
        <ul class="pagination pagination-pill mb-0">
          <li class="page-item" :class="{ disabled: currentPage === 1 }">
            <button class="page-link" @click="goToPage(currentPage - 1)">
              <i class="bi bi-chevron-left"></i>
            </button>
          </li>
          <li class="page-item disabled">
            <span class="page-link">顯示第 {{ currentPage }} 頁，共 {{ totalPages }} 頁</span>
          </li>
          <li class="page-item" :class="{ disabled: currentPage === totalPages }">
            <button class="page-link" @click="goToPage(currentPage + 1)">
              <i class="bi bi-chevron-right"></i>
            </button>
          </li>
        </ul>
      </nav>
    </div>

    <section class="card section-card overflow-hidden">
      <div class="card-header fw-bold section-card-header">
        <i class="bi bi-list-ul"></i> 維修單列表
      </div>
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <div class="mt-2">載入中...</div>
      </div>

      <table v-else class="table table-hover mb-0 align-middle repair-table">
        <colgroup>
          <col style="width: 12%">
          <col style="width: 20%">
          <col style="width: 20%">
          <col style="width: 28%">
          <col style="width: 20%">
        </colgroup>
        <thead class="table-light">
          <tr>
            <th>維修單id</th>
            <th>狀態</th>
            <th>分店</th>
            <th>機型</th>
            <th>預約日期</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in pagedRepairs" :key="r.id" role="button" @click="goToDetail(r)">
            <td>{{ r.id }}</td>
            <td>
              <span class="badge" :class="statusBadgeClass(r.repairStatus)">{{
                statusLabel(r.repairStatus)
              }}</span>
              <span
                v-if="r.repairStatus === 'QUOTED' && r.approvalStatus === 'PENDING'"
                class="badge text-bg-danger ms-1"
                >待您確認</span
              >
            </td>
            <td>{{ r.storeName }}</td>
            <td>{{ r.repairBrand }} {{ r.repairModel }}</td>
            <td>{{ r.bookingDate ?? "—" }}</td>
          </tr>
          <tr v-if="repairs.length === 0">
            <td colspan="5" class="text-center text-secondary py-4">
              目前沒有維修單
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>
</template>

<style scoped>
tbody tr {
  cursor: pointer;
}

/* 區塊卡片：圓角+柔和陰影，跟維修單管理同一套風格 */
.section-card {
  border: none;
  border-radius: 1rem;
  box-shadow: 0 2px 10px rgba(30, 53, 87, 0.08);
}

/* 區塊標題色塊：跟站內品牌藍統一風格 */
.section-card-header {
  background-color: #a8cdf0;
  color: #14263d;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
}

/* 欄寬固定(搭配上面的 colgroup)，不會因為內容長短跑位 */
.repair-table {
  table-layout: fixed;
}
.repair-table td {
  word-break: break-word;
}

/* 表格文字放大、加粗 */
.repair-table th {
  font-size: 16px;
}
.repair-table td {
  font-size: 18px;
  font-weight: 700;
  color: #1d324b;
}
.repair-table .badge {
  font-size: 16px;
}

/* 每頁筆數選單固定寬度，不會因為選到的文字長度不同而跑位 */
.page-size-select {
  width: 90px;
}

/* 分頁按鈕改藥丸形+品牌藍 */
.pagination-pill .page-link {
  border: none;
  border-radius: 999px;
  margin: 0 3px;
  color: #14263d;
  background-color: #edf5fc;
  font-weight: 600;
}
.pagination-pill .page-item:not(.disabled) .page-link:hover {
  background-color: #a8cdf0;
}
.pagination-pill .page-item.disabled .page-link {
  background-color: transparent;
  color: var(--bs-secondary-color, #6c757d);
  font-weight: 500;
  box-shadow: none;
}

/* 狀態徽章中間色階：用 Bootstrap 5.3 的 border-subtle 色階當底色，比 bg-subtle 深、比飽和版 text-bg-* 淺 */
.badge-mid-warning { background-color: var(--bs-warning-border-subtle); }
.badge-mid-info { background-color: var(--bs-info-border-subtle); }
.badge-mid-primary { background-color: var(--bs-primary-border-subtle); }
.badge-mid-danger { background-color: var(--bs-danger-border-subtle); }
.badge-mid-success { background-color: var(--bs-success-border-subtle); }
.badge-mid-dark { background-color: var(--bs-dark-border-subtle); }
.badge-mid-secondary { background-color: var(--bs-secondary-border-subtle); }

/* 尚未取件：跟待估價原本都是黃色會混淆，Bootstrap 沒有現成的紫色主題，改用跟維修統計圖表同一個紫色(#6f42c1)自訂淺中色階 */
.badge-mid-purple {
  background-color: #cbb6ee;
  color: #4a2c85;
}
</style>
