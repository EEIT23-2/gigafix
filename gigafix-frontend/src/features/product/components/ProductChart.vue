<script setup>
import { computed, onMounted, ref } from "vue";
import { use } from "echarts/core";
import { BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import VChart from "vue-echarts";
import { getAdminProducts } from "../api";

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent]);

defineEmits(["close"]);

const categoryDefinitions = [
  { value: "IPHONE", label: "iPhone", color: "#2878c7" },
  { value: "WATCH", label: "Apple Watch", color: "#7c5cc4" },
  { value: "IPAD", label: "iPad", color: "#36a178" },
];

const categoryCounts = ref(
  categoryDefinitions.map((category) => ({ ...category, count: 0 })),
);
const loading = ref(false);
const errorMessage = ref("");

const totalProducts = computed(() =>
  categoryCounts.value.reduce((total, category) => total + category.count, 0),
);

// ECharts 的設定會在統計資料更新後自動重新計算並刷新圖表。
const chartOption = computed(() => ({
  animationDuration: 650,
  tooltip: {
    trigger: "axis",
    axisPointer: { type: "shadow" },
    valueFormatter: (value) => `${value} 件`,
  },
  grid: { left: 18, right: 24, top: 24, bottom: 12, containLabel: true },
  xAxis: {
    type: "value",
    minInterval: 1,
    axisLine: { show: false },
    splitLine: { lineStyle: { color: "#e9eef4" } },
  },
  yAxis: {
    type: "category",
    data: categoryCounts.value.map((category) => category.label),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: "#34495e", fontSize: 13, fontWeight: 700 },
  },
  series: [
    {
      name: "商品數量",
      type: "bar",
      barWidth: 34,
      data: categoryCounts.value.map((category) => ({
        value: category.count,
        itemStyle: { color: category.color, borderRadius: [0, 8, 8, 0] },
      })),
      label: {
        show: true,
        position: "right",
        color: "#253b50",
        fontWeight: 800,
        formatter: "{c} 件",
      },
    },
  ],
}));

// 每個類別只取一筆內容，並使用 Page 回傳的 totalElements 取得完整庫存數量。
async function fetchCategoryCounts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const pages = await Promise.all(
      categoryDefinitions.map((category) =>
        getAdminProducts({ category: category.value, limit: 1, offset: 0 }),
      ),
    );

    categoryCounts.value = categoryDefinitions.map((category, index) => ({
      ...category,
      count: Number(pages[index]?.totalElements ?? 0),
    }));
  } catch (error) {
    console.error(error);
    errorMessage.value = error?.response
      ? `商品統計讀取失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器，請稍後再試。";
  } finally {
    loading.value = false;
  }
}

onMounted(fetchCategoryCounts);
</script>

<template>
  <div class="product-chart-panel">
    <header class="chart-header">
      <div>
        <p>PRODUCT INVENTORY</p>
        <h2 id="product-chart-title">商品類別統計</h2>
      </div>
      <button
        class="close-button"
        type="button"
        aria-label="關閉商品統計"
        @click="$emit('close')"
      >
        <i class="bi bi-x-lg" aria-hidden="true"></i>
      </button>
    </header>

    <div class="chart-body">
      <div class="summary-card">
        <span>目前商品總數</span>
        <strong>{{ totalProducts }}</strong>
        <small>件庫存商品</small>
      </div>

      <div class="category-summary" aria-label="各類別商品數量">
        <div
          v-for="category in categoryCounts"
          :key="category.value"
          class="category-card"
        >
          <span
            class="category-dot"
            :style="{ backgroundColor: category.color }"
            aria-hidden="true"
          ></span>
          <span>{{ category.label }}</span>
          <strong>{{ category.count }}</strong>
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
        @click="fetchCategoryCounts"
      >
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i>
        {{ loading ? "統計中..." : "重新整理統計" }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.product-chart-panel {
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
  background: linear-gradient(135deg, #1c5f99, #2b87c8);
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

.category-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 18px 0 8px;
}

.category-card {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 5px 7px;
  min-width: 0;
  padding: 13px;
  border: 1px solid #e1e7ee;
  border-radius: 13px;
  background: #fff;
}

.category-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.category-card > span:nth-child(2) {
  overflow: hidden;
  color: #647486;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-card strong {
  grid-column: 1 / -1;
  font-size: 24px;
}

.chart {
  width: 100%;
  height: 330px;
  margin-top: 10px;
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

  .category-summary {
    grid-template-columns: 1fr;
  }

  .category-card {
    grid-template-columns: auto 1fr auto;
  }

  .category-card strong {
    grid-column: auto;
    font-size: 20px;
  }
}
</style>
