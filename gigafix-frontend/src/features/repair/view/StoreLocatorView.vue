<script setup>
import { onMounted, ref } from "vue";
import { getStores } from "../api";

const stores = ref([]);
const loading = ref(false);
const errorMessage = ref("");

async function fetchStores() {
  loading.value = true;
  errorMessage.value = "";
  try {
    stores.value = await getStores();
  } catch (error) {
    console.error(error);
    errorMessage.value = "分店資料載入失敗，請稍後再試";
  } finally {
    loading.value = false;
  }
}

onMounted(fetchStores);
</script>

<template>
  <main class="container py-4" style="max-width: 900px">
    <h1 class="fw-bold mb-3">據點查詢</h1>
    <p class="text-muted">全台Gigafix分店資訊，歡迎就近前往維修/收購/取件。</p>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>
    <div v-else class="card card-body">
      <div class="table-responsive">
        <table class="table table-bordered align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>分店名稱</th>
              <th>地址</th>
              <th>電話</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in stores" :key="s.id">
              <td class="fw-bold">{{ s.name }}</td>
              <td>{{ s.address }}</td>
              <td>{{ s.phone }}</td>
            </tr>
            <tr v-if="stores.length === 0">
              <td colspan="3" class="text-center text-secondary py-4">
                目前沒有分店資料
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </main>
</template>

<style scoped></style>
