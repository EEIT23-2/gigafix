import { defineStore } from "pinia";
import { ref } from "vue";

// 保存商品列表頁的查詢與顯示條件。
// Pinia store 在 SPA 導頁期間不會被銷毀，因此從詳情／編輯頁返回時仍能保留狀態。
export const useProductStore = defineStore("product", () => {
  // Spring Data 使用從 0 開始的頁碼。
  const page = ref(0);
  const size = ref(48);

  const keyword = ref("");
  const category = ref("");
  const saleStatus = ref("");
  const modelName = ref("");
  const color = ref("");
  const storage = ref("");
  const sortOption = ref("");
  const minPrice = ref(null);
  const maxPrice = ref(null);
  const currency = ref("TWD");

  // 需要讓使用者手動清除條件時可呼叫。
  function resetListState() {
    page.value = 0;
    size.value = 48;
    keyword.value = "";
    category.value = "";
    saleStatus.value = "";
    modelName.value = "";
    color.value = "";
    storage.value = "";
    sortOption.value = "";
    minPrice.value = null;
    maxPrice.value = null;
    currency.value = "TWD";
  }

  return {
    page,
    size,
    keyword,
    category,
    saleStatus,
    modelName,
    color,
    storage,
    sortOption,
    minPrice,
    maxPrice,
    currency,
    resetListState,
  };
});
