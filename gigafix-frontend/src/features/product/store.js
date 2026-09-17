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
  const recycleApplyId = ref("");
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
    recycleApplyId.value = "";
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
    recycleApplyId,
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
// 保存回收申請列表的篩選、排序及分頁狀態。
export const useRecycleApplicationStore = defineStore(
  "recycleApplication",
  () => {
    const page = ref(0);
    const size = ref(10);
    // 保留後台回收單 ID 搜尋條件，從詳情頁返回列表時仍可維持篩選結果。
    const applyId = ref("");
    // 會員 ID 也必須存放在 Pinia，避免離開列表頁後因元件卸載而清空。
    const memberId = ref("");
    const productName = ref("");
    const appearance = ref("");
    const category = ref("");
    const recycleStatus = ref("");
    const sortOption = ref("createdTime:desc");

    function resetListState() {
      page.value = 0;
      size.value = 10;
      applyId.value = "";
      memberId.value = "";
      productName.value = "";
      appearance.value = "";
      category.value = "";
      recycleStatus.value = "";
      sortOption.value = "createdTime:desc";
    }

    return {
      page,
      size,
      applyId,
      memberId,
      productName,
      appearance,
      category,
      recycleStatus,
      sortOption,
      resetListState,
    };
  },
);
