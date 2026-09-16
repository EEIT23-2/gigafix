import { onBeforeUnmount, onMounted, ref } from "vue";

// 匯出按鈕的下拉選單，改用 Vue 自己的狀態控制開關（不依賴 Bootstrap 的 data-bs-toggle="dropdown"）。
// 因為 Bootstrap dropdown 判斷「點擊是否在選單外」時，在 Vue 會即時重繪 DOM 的情境下容易誤判，
// 導致選單一打開就馬上被 Bootstrap 自己關掉，這裡跟專案原本處理彈窗一樣改成手動控制、不靠自動偵測。
export function useExportMenu() {
  const containerRef = ref(null);
  const open = ref(false);

  function toggle() {
    open.value = !open.value;
  }

  function close() {
    open.value = false;
  }

  function handleClickOutside(event) {
    if (open.value && containerRef.value && !containerRef.value.contains(event.target)) {
      close();
    }
  }

  onMounted(() => document.addEventListener("click", handleClickOutside));
  onBeforeUnmount(() => document.removeEventListener("click", handleClickOutside));

  return { containerRef, open, toggle, close };
}
