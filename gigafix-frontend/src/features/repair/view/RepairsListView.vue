<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { searchRepairs } from "../api";

const router = useRouter();

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

// 把畫面上的搜尋條件整理成要傳給後端的 params，空的欄位不傳
function buildParams() {
  const params = {};
  if (searchId.value !== "") params.id = searchId.value;
  if (searchMemberId.value !== "") params.memberId = searchMemberId.value;
  if (searchMemberName.value !== "") params.memberName = searchMemberName.value;
  if (searchTechnicianId.value !== "") params.technicianId = searchTechnicianId.value;
  if (searchTechnicianName.value !== "") params.technicianName = searchTechnicianName.value;
  if (searchStatus.value !== "") params.status = searchStatus.value;
  return params;
}

async function fetchRepairs() {
  loading.value = true;
  errorMessage.value = "";
  try {
    repairs.value = await searchRepairs(buildParams());
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `查詢失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
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

onMounted(() => fetchRepairs());
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <h1 class="fw-bold mb-4">維修單管理</h1>

    <section class="card mb-4">
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
        <button class="btn btn-primary" @click="fetchRepairs">搜尋</button>
        <button class="btn btn-outline-secondary" @click="resetSearch">
          清除條件
        </button>
      </div>
    </section>

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

    <section class="card overflow-hidden">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <div class="mt-2">載入中...</div>
      </div>

      <table v-else class="table table-hover mb-0 align-middle">
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
            v-for="r in repairs"
            :key="r.id"
            role="button"
            @click="goToDetail(r)"
          >
            <td>{{ r.id }}</td>
            <td>{{ r.memberName }}（id:{{ r.memberId }}）</td>
            <td>
              <span v-if="r.technicianId">
                {{ r.technicianName }}（id:{{ r.technicianId }}）
              </span>
              <span v-else class="text-secondary">尚未認領</span>
            </td>
            <td>
              <span class="badge text-bg-light border">{{ statusLabel(r.repairStatus) }}</span>
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
tbody tr {
  cursor: pointer;
}
</style>