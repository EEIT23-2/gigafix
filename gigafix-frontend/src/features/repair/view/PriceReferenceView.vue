<script setup>
import { ref, computed } from "vue";
import { REPAIR_ITEMS, SERIES_LIST, getModelsForSeries, getItemPrice, formatPrice } from "../priceTable";

// 預設選第一個系列(SE)，一進頁面就有表格可以看
const selectedSeries = ref(SERIES_LIST[0].id);

const models = computed(() => getModelsForSeries(selectedSeries.value));
</script>

<template>
  <main class="container py-4" style="max-width: 900px">
    <h1 class="fw-bold mb-3">維修報價參考表</h1>
    <p class="text-muted fs-5">
      僅供參考，實際報價以到店/技師檢測後為準。
    </p>

    <section class="card section-card">
      <div class="card-header fw-bold section-card-header">
        <i class="bi bi-tags"></i> 選擇系列
      </div>
      <div class="card-body">
        <select v-model="selectedSeries" class="form-select mb-4" style="max-width: 260px">
          <option v-for="s in SERIES_LIST" :key="s.id" :value="s.id">
            {{ s.label }}
          </option>
        </select>

        <div class="table-responsive">
          <table class="table table-bordered align-middle price-table">
            <thead class="table-light">
              <tr>
                <th>維修項目</th>
                <th v-for="model in models" :key="model">{{ model }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in REPAIR_ITEMS" :key="item.key">
                <td class="fw-bold">{{ item.label }}</td>
                <td v-for="(model, index) in models" :key="model">
                  {{ formatPrice(getItemPrice(selectedSeries, index, item.key)) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
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

/* 表格文字放大 */
.price-table th,
.price-table td {
  font-size: 16px;
}
.price-table td {
  font-weight: 600;
  color: #1d324b;
}
</style>
