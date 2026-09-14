<script setup>
// 後台維修單統計：全部9種repairStatus的筆數與佔比（圓餅圖，含已結案/已取消/未送檢），
// 拒絕維修(approvalStatus=REJECTED,不論是否結案)/結案的數字卡片，已結案單建立到結案的耗時分布（長條圖），
// 以及各分店/技師的維修單量與結案率比較（表格，依單量由多到少排序）
// 資料來自 GET /api/repairs/stats，結案耗時是用 repairUpdatedTime-repairCreatedTime 近似值（後端沒有獨立的結案時間欄位）
import { computed, onMounted, ref } from "vue";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { PieChart, BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent, LegendComponent } from "echarts/components";
import VChart from "vue-echarts";
import { getRepairStats } from "../api";

use([CanvasRenderer, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent]);

const loading = ref(true);
const errorMessage = ref("");
const stats = ref(null);

// 狀態的中文顯示跟顏色，跟 RepairsListView 用同一套中文對照，顏色則為了圓餅圖易區分而各自不同
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
const STATUS_COLORS = {
  PENDING_QUOTE: "#ffc107",
  QUOTED: "#0dcaf0",
  IN_REPAIR: "#0d6efd",
  QUOTE_REJECTED: "#fd7e14",
  REPAIR_COMPLETED: "#20c997",
  AWAITING_PICKUP: "#6f42c1",
  CLOSED: "#495057",
  CANCELLED: "#dc3545",
  NOT_DROPPED_OFF: "#adb5bd",
};

// 下面另外用文字列表列出每個狀態的精確筆數/百分比（0筆的狀態圓餅圖畫不出弧形，文字列表才看得到），所以圖表本身不用legend
const statusPieOption = ref({
  tooltip: { trigger: "item", formatter: "{b}：{c} 筆（{d}%）" },
  series: [
    {
      type: "pie",
      radius: ["45%", "70%"],
      avoidLabelOverlap: false,
      label: { formatter: "{b}\n{d}%" },
      data: [],
    },
  ],
});

const durationBarOption = ref({
  tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
  grid: { left: 48, right: 24, top: 24, bottom: 32 },
  xAxis: { type: "category", data: [] },
  yAxis: { type: "value", minInterval: 1, name: "筆數" },
  series: [
    {
      type: "bar",
      name: "已結案單數",
      data: [],
      itemStyle: { color: "#2b77c5" },
    },
  ],
});

// 依維修單量由多到少排序，量最多的分店/技師排最前面比較好看績效
const sortedStoreStats = computed(() =>
  stats.value ? [...stats.value.storeStats].sort((a, b) => b.totalCount - a.totalCount) : [],
);
const sortedTechnicianStats = computed(() =>
  stats.value ? [...stats.value.technicianStats].sort((a, b) => b.totalCount - a.totalCount) : [],
);

function formatHours(hours) {
  if (hours == null) return "—";
  if (hours < 24) return `${hours} 小時`;
  return `${(hours / 24).toFixed(1)} 天（${hours} 小時）`;
}

async function fetchStats() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await getRepairStats();
    stats.value = data;

    statusPieOption.value.series[0].data = data.statusBreakdown.map((b) => ({
      name: STATUS_LABELS[b.status] ?? b.status,
      value: b.count,
      itemStyle: { color: STATUS_COLORS[b.status] },
    }));

    durationBarOption.value.xAxis.data = data.closeDurationDistribution.map((b) => b.label);
    durationBarOption.value.series[0].data = data.closeDurationDistribution.map((b) => b.count);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `統計資料讀取失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchStats();
});
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <h1 class="fw-bold mb-4">維修單統計</h1>

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
      <div class="mt-2">載入中...</div>
    </div>

    <template v-else-if="stats">
      <!-- 統計數字卡片 -->
      <div class="row g-3 mb-4">
        <div class="col-6 col-lg-3">
          <div class="card shadow-sm text-center h-100">
            <div class="card-body">
              <div class="text-secondary small">全部維修單</div>
              <div class="fs-3 fw-bold">{{ stats.totalCount }}</div>
            </div>
          </div>
        </div>
        <div class="col-6 col-lg-3">
          <div class="card shadow-sm text-center h-100">
            <div class="card-body">
              <div class="text-secondary small">拒絕維修</div>
              <div class="fs-3 fw-bold text-danger">
                {{ stats.rejectedCount }}<small class="fs-6 fw-normal">（{{ stats.rejectedPercentage }}%）</small>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-lg-3">
          <div class="card shadow-sm text-center h-100">
            <div class="card-body">
              <div class="text-secondary small">已結案</div>
              <div class="fs-3 fw-bold">
                {{ stats.closedCount }}<small class="fs-6 fw-normal">（{{ stats.closedPercentage }}%）</small>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-lg-3">
          <div class="card shadow-sm text-center h-100">
            <div class="card-body">
              <div class="text-secondary small">平均建立到結案耗時</div>
              <div class="fs-4 fw-bold">{{ formatHours(stats.avgCloseDurationHours) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-lg-5">
          <section class="card shadow-sm h-100">
            <div class="card-header py-3">
              <h6 class="m-0 fw-bold">維修單狀態分布</h6>
            </div>
            <div class="card-body">
              <v-chart class="chart" :option="statusPieOption" autoresize />
              <ul class="list-unstyled status-legend mt-3 mb-0 small">
                <li
                  v-for="b in stats.statusBreakdown"
                  :key="b.status"
                  class="d-flex align-items-center gap-2 py-1"
                >
                  <span
                    class="dot"
                    :style="{ backgroundColor: STATUS_COLORS[b.status] }"
                  ></span>
                  <span class="flex-grow-1">{{ STATUS_LABELS[b.status] ?? b.status }}</span>
                  <span class="text-secondary">{{ b.count }} 筆（{{ b.percentage }}%）</span>
                </li>
              </ul>
            </div>
          </section>
        </div>
        <div class="col-lg-7">
          <section class="card shadow-sm h-100">
            <div class="card-header py-3">
              <h6 class="m-0 fw-bold">已結案單：建立到結案耗時分布</h6>
              <p class="text-secondary small mb-0 mt-1">
                以最後一次更新時間近似結案時間，僅供參考
              </p>
            </div>
            <div class="card-body">
              <v-chart
                v-if="stats.closedCount > 0"
                class="chart"
                :option="durationBarOption"
                autoresize
              />
              <p v-else class="text-secondary text-center py-5 mb-0">
                目前還沒有已結案的維修單
              </p>
            </div>
          </section>
        </div>
      </div>

      <div class="row g-3 mt-1">
        <div class="col-lg-6">
          <section class="card shadow-sm h-100">
            <div class="card-header py-3">
              <h6 class="m-0 fw-bold">各分店維修單量與結案率</h6>
            </div>
            <div class="card-body p-0">
              <table class="table table-sm mb-0 align-middle">
                <thead class="table-light">
                  <tr>
                    <th>分店</th>
                    <th class="text-end">維修單量</th>
                    <th class="text-end">已結案</th>
                    <th class="text-end">結案率</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in sortedStoreStats" :key="s.storeId">
                    <td>{{ s.storeName }}</td>
                    <td class="text-end">{{ s.totalCount }}</td>
                    <td class="text-end">{{ s.closedCount }}</td>
                    <td class="text-end">{{ s.closedRate }}%</td>
                  </tr>
                  <tr v-if="sortedStoreStats.length === 0">
                    <td colspan="4" class="text-center text-secondary py-4">目前沒有資料</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
        <div class="col-lg-6">
          <section class="card shadow-sm h-100">
            <div class="card-header py-3">
              <h6 class="m-0 fw-bold">各技師維修單量與結案率</h6>
              <p class="text-secondary small mb-0 mt-1">尚未被認領的維修單不計入任何技師</p>
            </div>
            <div class="card-body p-0">
              <table class="table table-sm mb-0 align-middle">
                <thead class="table-light">
                  <tr>
                    <th>技師</th>
                    <th class="text-end">維修單量</th>
                    <th class="text-end">已結案</th>
                    <th class="text-end">結案率</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="t in sortedTechnicianStats" :key="t.technicianId">
                    <td>{{ t.technicianName }}（id:{{ t.technicianId }}）</td>
                    <td class="text-end">{{ t.totalCount }}</td>
                    <td class="text-end">{{ t.closedCount }}</td>
                    <td class="text-end">{{ t.closedRate }}%</td>
                  </tr>
                  <tr v-if="sortedTechnicianStats.length === 0">
                    <td colspan="4" class="text-center text-secondary py-4">目前沒有資料</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </template>
  </main>
</template>

<style scoped>
.chart {
  height: 380px;
}
.status-legend .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
