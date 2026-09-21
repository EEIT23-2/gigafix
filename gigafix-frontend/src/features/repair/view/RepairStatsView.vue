<script setup>
// 後台維修單統計：全部9種repairStatus的筆數與佔比（圓餅圖，含已結案/已取消/未送檢），
// 拒絕維修(approvalStatus=REJECTED,不論是否結案)/結案的數字卡片，已結案單建立到結案的耗時分布（長條圖），
// 以及各分店/技師的維修單量與結案率比較（單軸堆疊長條圖：已結案/未結案疊加，依單量由多到少排序）
// 資料來自 GET /api/admin/repair/repairs/stats，結案耗時是用 repairUpdatedTime-repairCreatedTime 近似值（後端沒有獨立的結案時間欄位）
import { computed, onMounted, ref } from "vue";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { PieChart, BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent, LegendComponent } from "echarts/components";
import VChart from "vue-echarts";
import { getRepairStats } from "../api";
import { useSwalMessages } from "../../../utils/swal";

use([CanvasRenderer, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent]);

const loading = ref(true);
const errorMessage = ref("");
useSwalMessages(errorMessage, null);
const stats = ref(null);

// 強制拉高canvas的實際解析度，避免瀏覽器縮放/非整數dpr時圖表文字模糊
const chartInitOptions = { devicePixelRatio: Math.max(window.devicePixelRatio || 1, 2) };

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
// 顏色盡量跟狀態徽章的色系一致(待估價/已報價/維修中/維修完成/尚未取件都對得上)；
// 報價後不維修/已取消/未送檢三個都是「紅色系」徽章，這裡各用一個深淺不同的紅互相區分
const STATUS_COLORS = {
  PENDING_QUOTE: "#ffc107",
  QUOTED: "#0dcaf0",
  IN_REPAIR: "#0d6efd",
  QUOTE_REJECTED: "#b02a37",
  REPAIR_COMPLETED: "#198754",
  AWAITING_PICKUP: "#6f42c1",
  CLOSED: "#495057",
  CANCELLED: "#dc3545",
  NOT_DROPPED_OFF: "#e35d6a",
};

// 【改動】百分比統一四捨五入到整數顯示(後端給的是2位小數，這裡只負責顯示格式)
const fmtPct = (v) => `${Math.round(Number(v))}%`;

// 下面另外用文字列表列出每個狀態的精確筆數/百分比（0筆的狀態圓餅圖畫不出弧形，文字列表才看得到），所以圖表本身不用legend
const statusPieOption = ref({
  tooltip: { trigger: "item", formatter: (p) => `${p.name}：${p.value} 筆（${fmtPct(p.percent)}）` },
  series: [
    {
      type: "pie",
      radius: ["45%", "70%"],
      avoidLabelOverlap: true,
      label: { formatter: (p) => `${p.name}\n${fmtPct(p.percent)}` }, // 【改動】
      labelLine: { length: 8, length2: 8 },
      data: [],
    },
  ],
});

const durationBarOption = ref({
  tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
  // yAxis的name預設畫在軸的最上方(grid.top以上的空間)，top留太少會被畫布上緣裁掉
  grid: { left: 70, right: 24, top: 60, bottom: 48 },
  xAxis: { type: "category", data: [], axisLabel: { interval: 0, fontSize: 18 } },
  yAxis: {
    type: "value",
    minInterval: 1,
    name: "筆數",
    nameGap: 16,
    nameTextStyle: { fontSize: 18 },
    axisLabel: { fontSize: 18 },
  },
  series: [
    {
      type: "bar",
      name: "已結案單數",
      data: [],
      itemStyle: { color: "#7c93f0" },
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

// 橫式單軸堆疊長條圖：長條總長度=維修單量，內部分成「已結案」(淺綠，跟維修完成徽章同色)/「未結案」(紅)兩段，每段色塊裡直接標百分比。
// 只有整條長條的兩端(最左、最右)要是圓角，中間兩段交接處要是直角接在一起，這樣兩段合起來才會是一條完整的長橢圓、
// 不會變成兩個獨立膠囊中間斷開；如果某一段筆數是0(那一段整條都空的)，圓角就要換給剩下那一整段的兩端都用
const BAR_RADIUS = 10;
function buildStackedBarOption(items, nameOf) {
  return {
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: (params) => {
        const item = items[params[0].dataIndex];
        return `${nameOf(item)}<br/>維修單量：${item.totalCount} 筆<br/>已結案：${item.closedCount} 筆<br/>結案率：${fmtPct(item.closedRate)}`; // 【改動】
      },
    },
    legend: { top: 0, textStyle: { fontSize: 18 } },
    grid: { left: 130, right: 70, top: 50, bottom: 32 },
    xAxis: {
      type: "value",
      name: "筆數",
      nameGap: 12,
      minInterval: 1,
      nameTextStyle: { fontSize: 18 },
      axisLabel: { fontSize: 18 },
    },
    yAxis: { type: "category", data: items.map(nameOf), inverse: true, axisLabel: { fontSize: 18 } },
    series: [
      {
        type: "bar",
        name: "已結案",
        stack: "total",
        data: items.map((i) => {
          const openCount = i.totalCount - i.closedCount;
          // 左邊永遠是圓角(整條長橢圓的左端)；右邊只有在「未結案」那段是空的、自己是唯一一段時才圓角，否則跟右邊那段接直角
          const radius = openCount === 0 ? BAR_RADIUS : [BAR_RADIUS, 0, 0, BAR_RADIUS];
          return { value: i.closedCount, itemStyle: { borderRadius: radius } };
        }),
        itemStyle: { color: "#a3cfbb" },
        label: {
          show: true,
          position: "inside",
          color: "#0a3622",
          fontSize: 17,
          formatter: (params) => (items[params.dataIndex].closedCount > 0 ? fmtPct(items[params.dataIndex].closedRate) : ""), // 【改動】
        },
      },
      {
        type: "bar",
        name: "未結案",
        stack: "total",
        data: items.map((i) => {
          const openCount = i.totalCount - i.closedCount;
          // 右邊永遠是圓角(整條長橢圓的右端)；左邊只有在「已結案」那段是空的、自己是唯一一段時才圓角，否則跟左邊那段接直角
          const radius = i.closedCount === 0 ? BAR_RADIUS : [0, BAR_RADIUS, BAR_RADIUS, 0];
          return { value: openCount, itemStyle: { borderRadius: radius } };
        }),
        itemStyle: { color: "#f08a94" },
        label: {
          show: true,
          position: "inside",
          color: "#7a1f27",
          fontSize: 17,
          formatter: (params) => {
            const item = items[params.dataIndex];
            const openCount = item.totalCount - item.closedCount;
            // 【改動】用100減掉「已結案」四捨五入後的整數，兩段加起來才會剛好100%(各自四捨五入可能變成101%)
            return openCount > 0 ? `${100 - Math.round(item.closedRate)}%` : "";
          },
        },
      },
    ],
  };
}
const storeComboOption = computed(() => buildStackedBarOption(sortedStoreStats.value, (s) => s.storeName));
const technicianComboOption = computed(() =>
  buildStackedBarOption(sortedTechnicianStats.value, (t) => t.technicianName),
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


    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
      <div class="mt-2">載入中...</div>
    </div>

    <template v-else-if="stats">
      <!-- 統計數字卡片 -->
      <div class="row g-3 mb-4">
        <div class="col-6 col-lg-3">
          <div class="card stat-card text-center h-100">
            <div class="card-body">
              <div class="stat-label">全部維修單</div>
              <div class="fs-3 fw-bold">{{ stats.totalCount }}</div>
            </div>
          </div>
        </div>
        <div class="col-6 col-lg-3">
          <div class="card stat-card text-center h-100">
            <div class="card-body">
              <div class="stat-label">拒絕維修</div>
              <div class="fs-3 fw-bold text-danger">
                {{ stats.rejectedCount }}<small class="fs-6 fw-normal">（{{ fmtPct(stats.rejectedPercentage) }}）</small>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-lg-3">
          <div class="card stat-card text-center h-100">
            <div class="card-body">
              <div class="stat-label">已結案</div>
              <div class="fs-3 fw-bold">
                {{ stats.closedCount }}<small class="fs-6 fw-normal">（{{ fmtPct(stats.closedPercentage) }}）</small>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-lg-3">
          <div class="card stat-card text-center h-100">
            <div class="card-body">
              <div class="stat-label">平均建立到結案耗時</div>
              <div class="fs-4 fw-bold">{{ formatHours(stats.avgCloseDurationHours) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 維修單狀態分布(左) / 結案耗時分布(右) 並排一排；各分店/各技師各自獨立佔一整排 -->
      <div class="row g-3 mb-3 align-items-start">
        <div class="col-lg-5">
          <section class="card section-card">
            <div class="card-header fw-bold section-card-header">
              <i class="bi bi-pie-chart"></i> 維修單狀態分布
            </div>
            <div class="card-body">
              <v-chart class="chart" :option="statusPieOption" :init-options="chartInitOptions" autoresize />
              <div class="d-flex gap-4 mt-3">
                <ul class="list-unstyled status-legend mb-0 small flex-fill">
                  <li
                    v-for="b in stats.statusBreakdown.slice(0, 5)"
                    :key="b.status"
                    class="d-flex align-items-center gap-2 py-1"
                  >
                    <span class="dot" :style="{ backgroundColor: STATUS_COLORS[b.status] }"></span>
                    <span class="flex-grow-1">{{ STATUS_LABELS[b.status] ?? b.status }}</span>
                    <span class="text-secondary">{{ b.count }} 筆（{{ fmtPct(b.percentage) }}）</span>
                  </li>
                </ul>
                <ul class="list-unstyled status-legend mb-0 small flex-fill">
                  <li
                    v-for="b in stats.statusBreakdown.slice(5)"
                    :key="b.status"
                    class="d-flex align-items-center gap-2 py-1"
                  >
                    <span class="dot" :style="{ backgroundColor: STATUS_COLORS[b.status] }"></span>
                    <span class="flex-grow-1">{{ STATUS_LABELS[b.status] ?? b.status }}</span>
                    <span class="text-secondary">{{ b.count }} 筆（{{ fmtPct(b.percentage) }}）</span>
                  </li>
                </ul>
              </div>
            </div>
          </section>
        </div>
        <div class="col-lg-7">
          <section class="card section-card">
            <div class="card-header fw-bold section-card-header">
              <i class="bi bi-bar-chart"></i> 已結案單：建立到結案耗時分布
            </div>
            <p class="text-secondary chart-note mb-0 mt-2 px-3 pt-2">以最後一次更新時間近似結案時間，僅供參考</p>
            <div class="card-body">
              <v-chart
                v-if="stats.closedCount > 0"
                class="chart"
                :option="durationBarOption"
                :init-options="chartInitOptions"
                autoresize
              />
              <p v-else class="text-secondary text-center py-5 mb-0">目前還沒有已結案的維修單</p>
            </div>
          </section>
        </div>
      </div>

      <section class="card section-card mb-3">
        <div class="card-header fw-bold section-card-header">
          <i class="bi bi-shop"></i> 各分店維修單量與結案率
        </div>
        <div class="card-body">
          <v-chart
            v-if="sortedStoreStats.length > 0"
            class="chart"
            :option="storeComboOption"
            :init-options="chartInitOptions"
            autoresize
          />
          <p v-else class="text-secondary text-center py-5 mb-0">目前沒有資料</p>
        </div>
      </section>

      <section class="card section-card mb-3">
        <div class="card-header fw-bold section-card-header">
          <i class="bi bi-person-gear"></i> 各技師維修單量與結案率
        </div>
        <p class="text-secondary chart-note mb-0 mt-2 px-3 pt-2">尚未被認領的維修單不計入任何技師</p>
        <div class="card-body">
          <v-chart
            v-if="sortedTechnicianStats.length > 0"
            class="chart"
            :option="technicianComboOption"
            :init-options="chartInitOptions"
            autoresize
          />
          <p v-else class="text-secondary text-center py-5 mb-0">目前沒有資料</p>
        </div>
      </section>
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

/* 圖表說明文字，比原本的 Bootstrap .small 放大1.5倍 */
.chart-note {
  font-size: 21px;
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

/* 統計數字卡片：圓角+柔和陰影，字級放大 */
.stat-card {
  border: none;
  border-radius: 1rem;
  box-shadow: 0 2px 10px rgba(30, 53, 87, 0.08);
}
.stat-label {
  font-size: 16px;
  color: var(--bs-secondary-color, #6c757d);
}
</style>
