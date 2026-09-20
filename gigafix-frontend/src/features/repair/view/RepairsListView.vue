<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import {
  assignRepair,
  downloadBlob,
  exportRepairs,
  getTechnicians,
  getTodayString, // ★改：新增
  searchRepairs,
} from "../api";
import { useExportMenu } from "../useExportMenu";
import { hasUnseenChange, initSeenBaseline } from "../repairSeen";
import { swalConfirm, useSwalMessages } from "../../../utils/swal";

const router = useRouter();

const technicians = ref([]);
async function fetchTechnicians() {
  try {
    technicians.value = await getTechnicians();
  } catch (error) {
    console.error(error);
  }
}

async function handleAssign(repair, technicianId, selectEl) {
  if (!technicianId) return;
  const tech = technicians.value.find(
    (t) => String(t.id) === String(technicianId),
  );
  const techLabel = tech ? `${tech.id} - ${tech.name}` : technicianId;
  if (!(await swalConfirm(`確定要指派技師「${techLabel}」認領這張維修單嗎？`))) {
    if (selectEl) selectEl.value = "";
    return;
  }
  try {
    await assignRepair(repair.id, technicianId);
    await fetchRepairs();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `認領失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
    if (selectEl) selectEl.value = "";
  }
}

// 搜尋條件，全部可以不填
const searchId = ref("");
const searchMemberId = ref("");
const searchMemberName = ref("");
const searchTechnicianId = ref("");
const searchTechnicianName = ref("");
const searchStatus = ref("");

const repairs = ref([]);
const loading = ref(false);
const errorMessage = ref("");
useSwalMessages(errorMessage, null);

// 狀態的中文顯示，跟後端 RepairStatus enum 對應
const statusOptions = [
  { value: "PENDING_QUOTE", label: "待估價" },
  { value: "QUOTED", label: "已報價" },
  { value: "IN_REPAIR", label: "維修中" },
  { value: "QUOTE_REJECTED", label: "報價後不維修" },
  { value: "REPAIR_COMPLETED", label: "維修完成" },
  { value: "AWAITING_PICKUP", label: "尚未取件" },
  { value: "CLOSED", label: "已結案" },
  { value: "CANCELLED", label: "已取消" },
  { value: "NOT_DROPPED_OFF", label: "未送檢" },
];

function statusLabel(status) {
  return statusOptions.find((s) => s.value === status)?.label ?? status;
}

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
function statusBadgeClass(status) {
  return STATUS_BADGE_CLASS[status] ?? "badge-mid-secondary text-secondary-emphasis";
}

// 把畫面上的搜尋條件整理成要傳給後端的 params，空的欄位不傳
function buildParams() {
  const params = {};
  if (searchId.value !== "") params.id = searchId.value;
  if (searchMemberId.value !== "") params.memberId = searchMemberId.value;
  if (searchMemberName.value !== "") params.memberName = searchMemberName.value;
  if (searchTechnicianId.value !== "")
    params.technicianId = searchTechnicianId.value;
  if (searchTechnicianName.value !== "")
    params.technicianName = searchTechnicianName.value;
  if (searchStatus.value !== "") params.status = searchStatus.value;
  return params;
}

let lastSearchParams = {}; //最近一次按「查詢」用的條件，背景自動更新沿用它，不受輸入框打到一半的內容影響

async function fetchRepairs() {
  loading.value = true;
  errorMessage.value = "";
  try {
    lastSearchParams = buildParams();
    repairs.value = await searchRepairs(lastSearchParams);
    initSeenBaseline(repairs.value);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `查詢失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

// ===== 背景自動更新：每 30 秒安靜地重抓一次，客戶有新操作時紅點才會自己冒出來 =====
// 不顯示載入中、資料沒變就不動、停留在原本的頁碼，避免技師操作到一半畫面跳掉
const REFRESH_INTERVAL_MS = 30000;
let refreshTimer = null;

async function refreshQuietly() {
  if (loading.value) return;
  try {
    const latest = await searchRepairs(lastSearchParams);
    if (JSON.stringify(latest) === JSON.stringify(repairs.value)) return;
    const page = currentPage.value;
    repairs.value = latest;
    await nextTick(); //等 watch 把頁碼重設成第1頁之後，再切回原本的頁碼
    currentPage.value = Math.min(page, totalPages.value);
  } catch (error) {
    console.error(error); //背景更新失敗不打擾使用者，下一輪再試
  }
}

// ===== 匯出：沿用目前的搜尋條件，只匯出符合條件的維修單 =====
const exportMenu = useExportMenu();
const exporting = ref(false);

async function handleExport(format) {
  exportMenu.close();
  exporting.value = true;
  errorMessage.value = "";
  try {
    const blob = await exportRepairs(format, buildParams());
    downloadBlob(blob, `repairs-${getTodayString()}.${format}`);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `匯出失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    exporting.value = false;
  }
}

function resetSearch() {
  searchId.value = "";
  searchMemberId.value = "";
  searchMemberName.value = "";
  searchTechnicianId.value = "";
  searchTechnicianName.value = "";
  searchStatus.value = "";
  fetchRepairs();
}

function goToDetail(repair) {
  router.push({ name: "admin-repair-detail", params: { repairId: repair.id } });
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

// 換頁筆數、或查詢結果變動時，都跳回第1頁，避免停在一個已經不存在的頁碼
watch([pageSize, repairs], () => {
  currentPage.value = 1;
});

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
}

onMounted(() => {
  fetchRepairs();
  fetchTechnicians();
  refreshTimer = setInterval(refreshQuietly, REFRESH_INTERVAL_MS);
});

onUnmounted(() => {
  clearInterval(refreshTimer);
});
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="fw-bold mb-0">維修單管理</h1>
      <!-- 匯出：下拉選格式，沿用下方目前的搜尋條件 -->
      <div class="dropdown" :ref="(el) => (exportMenu.containerRef.value = el)">
        <button
          class="btn btn-outline-secondary dropdown-toggle"
          type="button"
          :disabled="exporting"
          @click="exportMenu.toggle"
        >
          {{ exporting ? "匯出中..." : "匯出" }}
        </button>
        <ul class="dropdown-menu dropdown-menu-end" :class="{ show: exportMenu.open.value }">
          <li>
            <button class="dropdown-item" @click="handleExport('xlsx')">
              Excel
            </button>
          </li>
          <li>
            <button class="dropdown-item" @click="handleExport('json')">
              JSON
            </button>
          </li>
          <li>
            <button class="dropdown-item" @click="handleExport('xml')">
              XML
            </button>
          </li>
        </ul>
      </div>
    </div>

    <section class="card section-card mb-4">
      <div class="card-header fw-bold section-card-header">
        <i class="bi bi-search"></i> 搜尋條件
      </div>
      <div class="card-body d-flex flex-wrap gap-3">
        <input
          v-model.trim="searchId"
          type="number"
          class="form-control search-input"
          placeholder="維修單id"
          @keyup.enter="fetchRepairs"
        />
        <input
          v-model.trim="searchMemberId"
          type="number"
          class="form-control search-input"
          placeholder="客戶id"
          @keyup.enter="fetchRepairs"
        />
        <input
          v-model.trim="searchMemberName"
          type="text"
          class="form-control search-input"
          placeholder="客戶姓名"
          @keyup.enter="fetchRepairs"
        />
        <input
          v-model.trim="searchTechnicianId"
          type="number"
          class="form-control search-input"
          placeholder="技師id"
          @keyup.enter="fetchRepairs"
        />
        <input
          v-model.trim="searchTechnicianName"
          type="text"
          class="form-control search-input"
          placeholder="技師姓名"
          @keyup.enter="fetchRepairs"
        />
        <select v-model="searchStatus" class="form-select filter-select">
          <option value="">所有狀態</option>
          <option v-for="s in statusOptions" :key="s.value" :value="s.value">
            {{ s.label }}
          </option>
        </select>
        <button class="btn btn-primary rounded-pill px-4" @click="fetchRepairs">
          <i class="bi bi-search me-1"></i>搜尋
        </button>
        <button class="btn btn-outline-secondary rounded-pill px-4" @click="resetSearch">
          清除條件
        </button>
      </div>
    </section>

    <!-- 分頁：選每頁筆數、上一頁/下一頁，放在搜尋條件下方、列表上方 -->
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
          <col style="width: 8%">
          <col style="width: 18%">
          <col style="width: 18%">
          <col style="width: 14%">
          <col style="width: 14%">
          <col style="width: 28%">
        </colgroup>
        <thead class="table-light">
          <tr>
            <th>維修單id</th>
            <th>客戶</th>
            <th>技師</th>
            <th>狀態</th>
            <th>分店</th>
            <th>機型</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in pagedRepairs"
            :key="r.id"
            role="button"
            @click="goToDetail(r)"
          >
            <td>{{ r.id }}</td>
            <td>{{ r.memberName }}（id:{{ r.memberId }}）</td>
            <td @click.stop>
              <span v-if="r.technicianId">
                {{ r.technicianName }}（id:{{ r.technicianId }}）
              </span>
              <select
                v-else-if="r.repairStatus === 'PENDING_QUOTE'"
                class="form-select form-select-sm"
                style="width: 160px"
                @change="handleAssign(r, $event.target.value, $event.target)"
              >
                <option value="">選擇技師認領</option>
                <option v-for="t in technicians" :key="t.id" :value="t.id">
                  {{ t.id }} - {{ t.name }}
                </option>
              </select>
              <span v-else class="text-secondary">—</span>
            </td>
            <td>
              <!-- 客戶造成的新異動：徽章右上角亮紅點，打開詳情頁後消失 -->
              <span class="position-relative d-inline-block">
                <span class="badge" :class="statusBadgeClass(r.repairStatus)">{{
                  statusLabel(r.repairStatus)
                }}</span>
                <span
                  v-if="hasUnseenChange(r)"
                  class="position-absolute start-100 translate-middle p-2 bg-danger border border-light rounded-circle"
                  style="top: 25%"
                  title="客戶有新的異動"
                ></span>
              </span>
            </td>
            <td>{{ r.storeName }}</td>
            <td>{{ r.repairBrand }} {{ r.repairModel }}</td>
          </tr>
          <tr v-if="repairs.length === 0">
            <td colspan="6" class="text-center text-secondary py-4">
              沒有符合條件的維修單
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>
</template>

<style scoped>
.search-input {
  flex: 1 1 160px;
}
.filter-select {
  max-width: 190px;
}

/* 區塊卡片：圓角+柔和陰影，跟維修單詳表同一套風格 */
.section-card {
  border: none;
  border-radius: 1rem;
  box-shadow: 0 2px 10px rgba(30, 53, 87, 0.08);
}

/* 區塊標題色塊：跟站內品牌藍(詳表同一套色)統一風格 */
.section-card-header {
  background-color: #a8cdf0;
  color: #14263d;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
}

/* 欄寬固定(搭配上面的 colgroup)，不會因為某幾列內容比較長/比較短就整個跑位 */
.repair-table {
  table-layout: fixed;
}
.repair-table td {
  word-break: break-word;
}

/* 表格文字放大、加粗，跟詳表的 info-value 同一個級距 */
.repair-table th {
  font-size: 16px;
}
.repair-table td {
  font-size: 18px;
  font-weight: 700;
  color: #1d324b;
}

/* 狀態徽章文字跟表頭「狀態」同樣大小，不用 Bootstrap 徽章預設的縮小字級 */
.repair-table .badge {
  font-size: 16px;
}

/* 狀態欄位跟前一欄拉開一點距離，不要黏在一起 */
.repair-table th:nth-child(4),
.repair-table td:nth-child(4) {
  padding-left: 36px;
}

/* 分店/機型再多拉開一點(比狀態多2個字寬) */
.repair-table th:nth-child(5),
.repair-table td:nth-child(5),
.repair-table th:nth-child(6),
.repair-table td:nth-child(6) {
  padding-left: 72px;
}

/* 每頁筆數選單固定寬度，不會因為選到「10 筆/20 筆/全部」文字長度不同而跑位 */
.page-size-select {
  width: 90px;
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

/* 分頁按鈕改藥丸形+品牌藍，取代 Bootstrap 預設的方正樣式 */
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
tbody tr {
  cursor: pointer;
}
/* 匯出選單不靠 Bootstrap JS(Popper)定位，改用固定的向右對齊 */
.dropdown-menu {
  right: 0;
  left: auto;
}
</style>
