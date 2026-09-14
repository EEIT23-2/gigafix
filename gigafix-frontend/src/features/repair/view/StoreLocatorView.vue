<script setup>
import { onMounted, ref } from "vue";
import { setOptions, importLibrary } from "@googlemaps/js-api-loader";
import { getStores } from "../api";
import { STORE_COORDINATES } from "../storeCoordinates";

const stores = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const selectedStore = ref(null);

const mapContainer = ref(null);
let map = null;
let marker = null;

setOptions({
  key: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
  v: "weekly",
});

//點左邊列表的分店，地圖飛過去該分店座標並移動marker
function selectStore(store) {
  selectedStore.value = store;
  const coord = STORE_COORDINATES[store.name];
  if (!coord || !map) return;
  map.panTo(coord);
  marker.setPosition(coord);
}

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

onMounted(async () => {
  await fetchStores();

  const { Map } = await importLibrary("maps");
  const { Marker } = await importLibrary("marker");

  //預設選第一間查得到座標的分店，一進頁面地圖就有東西可以看
  const defaultStore = stores.value.find((s) => STORE_COORDINATES[s.name]);
  const initialCenter = defaultStore
    ? STORE_COORDINATES[defaultStore.name]
    : { lat: 23.9739, lng: 120.982 }; //找不到座標時退回台灣中心點

  map = new Map(mapContainer.value, {
    center: initialCenter,
    zoom: 16,
    gestureHandling: "greedy", //讓使用者滑鼠滾輪直接縮放，不用按Ctrl
  });
  marker = new Marker({ map, position: initialCenter });

  if (defaultStore) {
    selectedStore.value = defaultStore;
  }
});
</script>

<template>
  <main class="container-fluid py-4" style="max-width: 1200px">
    <h1 class="fw-bold mb-3">據點查詢</h1>
    <p class="text-muted">全台Gigafix分店資訊，歡迎就近前往維修/收購/取件。</p>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <div v-else class="row g-3">
      <!-- 左邊：分店卡片列表，每張卡片由上到下是分店名稱/電話/地址，點卡片可切換右邊地圖 -->
      <div class="col-lg-5 d-flex flex-column gap-3">
        <div
          v-for="s in stores"
          :key="s.id"
          class="store-card"
          :class="{ 'store-card-active': selectedStore?.id === s.id }"
          role="button"
          @click="selectStore(s)"
        >
          <div class="store-card-name">{{ s.name }}</div>
          <div class="store-card-phone">{{ s.phone }}</div>
          <div class="store-card-address">{{ s.address }}</div>
        </div>
        <p v-if="stores.length === 0" class="text-center text-secondary py-4">
          目前沒有分店資料
        </p>
      </div>

      <!-- 右邊：Google地圖，可縮放/拖移，並可連結到Google地圖 -->
      <div class="col-lg-7">
        <div ref="mapContainer" class="store-map"></div>
        <a
          v-if="selectedStore && STORE_COORDINATES[selectedStore.name]"
          class="d-inline-block mt-2"
          :href="`https://www.google.com/maps/search/?api=1&query=${STORE_COORDINATES[selectedStore.name].lat},${STORE_COORDINATES[selectedStore.name].lng}`"
          target="_blank"
          rel="noopener"
        >
          在 Google 地圖中開啟 →
        </a>
      </div>
    </div>
  </main>
</template>

<style scoped>
.store-card {
  cursor: pointer;
  padding: 0.9rem 1.1rem;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
  background-color: #ffffff;
}

.store-card:hover {
  background-color: #f5f8fc;
}

.store-card-active {
  background-color: #eef4fb;
  border-color: #2b77c5;
}

.store-card-name {
  font-weight: 700;
  font-size: 1.1rem;
}

.store-card-phone,
.store-card-address {
  color: #555555;
  margin-top: 0.2rem;
}

.store-map {
  width: 100%;
  height: 420px;
  border-radius: 0.5rem;
  border: 1px solid #dee2e6;
}
</style>
