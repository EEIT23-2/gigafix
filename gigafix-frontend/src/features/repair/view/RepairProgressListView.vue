<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getMyRepairs } from "../api";

const router = useRouter();

const repairs = ref([]);
const loading = ref(false);
const errorMessage = ref("");

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
const STATUS_BADGE_CLASS = {
  PENDING_QUOTE: "text-bg-warning",
  QUOTED: "text-bg-info",
  IN_REPAIR: "text-bg-primary",
  QUOTE_REJECTED: "text-bg-danger",
  REPAIR_COMPLETED: "text-bg-success",
  AWAITING_PICKUP: "text-bg-warning",
  CLOSED: "text-bg-dark",
  CANCELLED: "text-bg-secondary",
  NOT_DROPPED_OFF: "text-bg-secondary",
};
function statusLabel(status) {
  return STATUS_LABELS[status] ?? status;
}
function statusBadgeClass(status) {
  return STATUS_BADGE_CLASS[status] ?? "text-bg-secondary";
}

async function fetchRepairs() {
  loading.value = true;
  errorMessage.value = "";
  try {
    repairs.value = await getMyRepairs();
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

onMounted(() => {
  fetchRepairs();
});
</script>

<template>
  <main>
    <h1 class="fw-bold mb-4">維修進度</h1>

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
    <div v-if="needsResponseCount > 0" class="alert alert-info">
      您有 {{ needsResponseCount }} 張維修單已完成報價，請點進去確認是否維修。
    </div>

    <section class="card overflow-hidden">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <div class="mt-2">載入中...</div>
      </div>

      <table v-else class="table table-hover mb-0 align-middle">
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
          <tr v-for="r in repairs" :key="r.id" role="button" @click="goToDetail(r)">
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
</style>
