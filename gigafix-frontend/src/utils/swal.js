import { watch } from "vue";
import Swal from "sweetalert2";

// 全站共用的 SweetAlert2 封裝，取代瀏覽器原生的 alert / confirm。
// 按鈕改用 Bootstrap 的 btn 樣式(buttonsStyling: false)，顏色才會跟網站主題一致
const BaseSwal = Swal.mixin({
  buttonsStyling: false,
  reverseButtons: true,
  confirmButtonText: "確定",
  cancelButtonText: "取消",
  customClass: {
    confirmButton: "btn btn-primary mx-1",
    cancelButton: "btn btn-outline-secondary mx-1",
  },
});

const Toast = BaseSwal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 2500,
  timerProgressBar: true,
});

// 取代 window.confirm：回傳 Promise<boolean>，用法 `if (!(await swalConfirm("..."))) return;`
export async function swalConfirm(text, { title = "請確認", confirmText = "確定", danger = false } = {}) {
  const result = await BaseSwal.fire({
    title,
    text,
    icon: danger ? "warning" : "question",
    showCancelButton: true,
    confirmButtonText: confirmText,
    customClass: {
      confirmButton: `btn ${danger ? "btn-danger" : "btn-primary"} mx-1`,
      cancelButton: "btn btn-outline-secondary mx-1",
    },
  });
  return result.isConfirmed;
}

// 取代 window.alert：欄位沒填好之類的提醒
export function swalWarn(text, title = "請留意") {
  return BaseSwal.fire({ title, text, icon: "warning" });
}

export function swalError(text, title = "發生錯誤") {
  return BaseSwal.fire({ title, text, icon: "error" });
}

export function swalInfo(text, title = "提醒") {
  return BaseSwal.fire({ title, text, icon: "info" });
}

// 成功訊息用右上角的小提示，幾秒後自己消失，不打斷操作
export function swalSuccess(text) {
  return Toast.fire({ icon: "success", title: text });
}

// 把頁面上原本「errorMessage / successMessage 兩個 ref + 頁面橫幅」的寫法，改成彈出視窗。
// ref 有值就跳窗，視窗關閉後才清成空字串(所以送出後馬上檢查 errorMessage 的判斷仍然有效)
export function useSwalMessages(errorRef, successRef) {
  if (errorRef) {
    watch(errorRef, (msg) => {
      if (msg) swalError(msg).then(() => (errorRef.value = ""));
    });
  }
  if (successRef) {
    watch(successRef, (msg) => {
      if (msg) swalSuccess(msg).then(() => (successRef.value = ""));
    });
  }
}
