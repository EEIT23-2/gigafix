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
    <p class="text-muted">
      僅供參考，實際報價以到店/技師檢測後為準。
    </p>

    <div class="card card-body">
      <label class="form-label">選擇系列</label>
      <select v-model="selectedSeries" class="form-select mb-4" style="max-width: 260px">
        <option v-for="s in SERIES_LIST" :key="s.id" :value="s.id">
          {{ s.label }}
        </option>
      </select>

      <div class="table-responsive">
        <table class="table table-bordered align-middle">
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
  </main>
</template>

<style scoped></style>
