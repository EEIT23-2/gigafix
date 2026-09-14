<script setup>
import { computed, onMounted, ref } from "vue";
import { use } from "echarts/core";
import { PieChart } from "echarts/charts";
import { LegendComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import VChart from "vue-echarts";
import { getRecycleApplications } from "../api";

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent]);

defineEmits(["close"]);

// 狀態順序依回收流程排列，已取消另外放在最後方便辨識。
const statusDefinitions = [
  { value: "APPLIED", label: "已提出申請", color: "#3b82c4" },
  { value: "INSPECTING", label: "檢查估價中", color: "#e2a83b" },
  {
    value: "WAITING_FOR_AGREEMENT",
    label: "等待客戶同意",
    color: "#e0794e",
  },
  { value: "WIPING", label: "資料清除中", color: "#8067c4" },
  { value: "COMPLETED", label: "回收完成", color: "#3ca378" },
  { value: "CANCELLED", label: "已取消", color: "#8c98a4" },
];

const statusCounts = ref(
  statusDefinitions.map((status) => ({ ...status, count: 0 })),
);
const loading = ref(false);
const errorMessage = ref("");

const totalApplications = computed(() =>
  statusCounts.value.reduce((total, status) => total + status.count, 0),
);

// 使用圓環式圓餅圖，同時顯示各狀態數量與所占比例。
const chartOption = computed(() => ({
  color: statusCounts.value.map((status) => status.color),
  animationDuration: 650,
  tooltip: {
    trigger: "item",
    formatter: ({ name, value, percent }) =>
      `${name}<br/><strong>${value} 筆（${percent}%）</strong>`,
  },
  legend: {
    type: "scroll",
    orient: "horizontal",
    left: "center",
    bottom: 0,
    itemWidth: 11,
    itemHeight: 11,
    textStyle: { color: "#4e6174", fontSize: 11 },
  },
  series: [
    {
      name: "回收單狀態",
      type: "pie",
      radius: ["43%", "68%"],
      center: ["50%", "43%"],
      minAngle: 2,
      avoidLabelOverlap: true,
      itemStyle: {
        borderColor: "#fff",
        borderWidth: 3,
        borderRadius: 6,
      },
      label: {
        show: true,
        color: "#30485e",
        fontSize: 11,
        fontWeight: 700,
        formatter: ({ name, value }) => (value > 0 ? `${name}\n${value} 筆` : ""),
      },
      labelLine: { length: 10, length2: 8 },
      data: statusCounts.value.map((status) => ({
        name: status.label,
        value: status.count,
      })),
    },
  ],
}));

// 每個狀態只取一筆內容，使用分頁回傳的 totalElements 取得該狀態完整數量。
async function fetchStatusCounts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const pages = await Promise.all(
      statusDefinitions.map((status) =>
        getRecycleApplications({
          recycleStatus: status.value,
          limit: 1,
          offset: 0,
        }),
      ),
    );

    statusCounts.value = statusDefinitions.map((status, index) => ({
      ...status,
      count: Number(pages[index]?.totalElements ?? 0),
    }));
  } catch (error) {
    console.error(error);
    errorMessage.value = error?.response
      ? `回收單統計讀取失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器，請稍後再試。";
  } finally {
    loading.value = false;
  }
}

onMounted(fetchStatusCounts);
</script>

<template>
  <div class="recycle-chart-panel">
    <header class="chart-header">
      <div>
        <p>RECYCLE APPLICATIONS</p>
        <h2 id="recycle-chart-title">回收單狀態統計</h2>
      </div>
      <button
        class="close-button"
        type="button"
        aria-label="關閉回收單統計"
        @click="$emit('close')"
      >
        <i class="bi bi-x-lg" aria-hidden="true"></i>
      </button>
    </header>

    <div class="chart-body">
      <div class="summary-card">
        <span>目前回收單總數</span>
        <strong>{{ totalApplications }}</strong>
        <small>筆回收申請</small>
      </div>

      <div class="status-summary" aria-label="各狀態回收單數量">
        <div
          v-for="status in statusCounts"
          :key="status.value"
          class="status-card"
        >
          <span
            class="status-dot"
            :style="{ backgroundColor: status.color }"
            aria-hidden="true"
          ></span>
          <span>{{ status.label }}</span>
          <strong>{{ status.count }}</strong>
        </div>
      </div>

      <div v-if="errorMessage" class="chart-error" role="alert">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <v-chart
        v-else
        class="chart"
        :option="chartOption"
        :loading="loading"
        autoresize
      />

      <button
        class="refresh-button"
        type="button"
        :disabled="loading"
        @click="fetchStatusCounts"
      >
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i>
        {{ loading ? "統計中..." : "重新整理統計" }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.recycle-chart-panel {
  display: flex;
  height: 100%;
  flex-direction: column;
  color: #1d324b;
  background: #f7f9fc;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 26px;
  border-bottom: 1px solid #e1e7ee;
  background: #fff;
}

.chart-header p {
  margin: 0 0 4px;
  color: #2878c7;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.13em;
}

.chart-header h2 {
  margin: 0;
  font-size: 25px;
  font-weight: 800;
}

.close-button {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid #d5dee7;
  border-radius: 50%;
  color: #536779;
  background: #fff;
}

.close-button:hover {
  color: #fff;
  background: #536779;
}

.chart-body {
  padding: 24px 26px 30px;
  overflow-y: auto;
}

.summary-card {
  padding: 20px 22px;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(135deg, #244f75, #338b9d);
  box-shadow: 0 12px 28px rgb(35 100 153 / 20%);
}

.summary-card span,
.summary-card small {
  display: block;
  opacity: 0.86;
}

.summary-card strong {
  display: inline-block;
  margin: 5px 8px 0 0;
  font-size: 40px;
  line-height: 1;
}

.summary-card small {
  display: inline;
}

.status-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
  margin-top: 18px;
}

.status-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid #e1e7ee;
  border-radius: 12px;
  background: #fff;
}

.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.status-card > span:nth-child(2) {
  overflow: hidden;
  color: #647486;
  font-size: 11px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-card strong {
  color: #263f55;
  font-size: 18px;
}

.chart {
  width: 100%;
  height: 390px;
  margin-top: 8px;
}

.chart-error {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 22px;
  padding: 15px;
  border-radius: 11px;
  color: #8f3030;
  background: #fbe7e7;
}

.refresh-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  padding: 11px 16px;
  border: 1px solid #b9cadb;
  border-radius: 10px;
  color: #28689e;
  background: #fff;
  font-weight: 750;
}

.refresh-button:hover:not(:disabled) {
  color: #fff;
  background: #28689e;
}

.refresh-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

@media (max-width: 520px) {
  .chart-header,
  .chart-body {
    padding-right: 20px;
    padding-left: 20px;
  }

  .status-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .status-card {
    grid-template-columns: auto 1fr;
  }

  .status-card strong {
    grid-column: 1 / -1;
    font-size: 20px;
  }

  .chart {
    height: 350px;
  }
}
</style>
