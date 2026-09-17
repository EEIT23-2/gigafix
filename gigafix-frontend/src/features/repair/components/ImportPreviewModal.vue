<script setup>
import { computed, onMounted, ref } from "vue";
import { Modal } from "bootstrap";

const props = defineProps({
  preview: { type: Object, default: null },
  confirming: { type: Boolean, default: false },
});
const emit = defineEmits(["confirm"]);

const modalRef = ref(null);
let modalInstance = null;

onMounted(() => {
  modalInstance = new Modal(modalRef.value);
});

function show() {
  modalInstance.show();
}
function hide() {
  modalInstance.hide();
}

defineExpose({ show, hide });

const ACTION_LABEL = { INSERT: "新增", UPDATE: "更新", ERROR: "錯誤" };
const ACTION_BADGE = {
  INSERT: "text-bg-success",
  UPDATE: "text-bg-primary",
  ERROR: "text-bg-danger",
};

// 沒有變動的資料只算數量、不逐筆列出，避免整個清單都是沒用的資訊
const displayRows = computed(() =>
  (props.preview?.rows ?? []).filter((r) => r.action !== "UNCHANGED"),
);

const canConfirm = computed(
  () =>
    !!props.preview &&
    (props.preview.insertCount > 0 || props.preview.updateCount > 0),
);

// 新增／查無對應id的錯誤列，直接顯示使用者填的欄位內容，比「第9筆」這種編號好對照
function summaryText(data) {
  if (!data) return "";
  return Object.entries(data)
    .filter(([key, value]) => key !== "id" && value)
    .map(([, value]) => value)
    .join(" / ");
}

function primaryLabel(row) {
  if (row.action === "UPDATE") return row.rowLabel;
  return summaryText(row.data) || row.rowLabel;
}
</script>

<template>
  <div class="modal fade" ref="modalRef" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg">
      <div class="modal-content border-0 shadow-lg rounded-4">
        <div class="modal-header border-0 pb-0">
          <h5 class="modal-title">匯入預覽</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
          ></button>
        </div>
        <div class="modal-body">
          <p v-if="preview" class="mb-2">
            新增
            <strong class="text-success">{{ preview.insertCount }}</strong> 筆、更新
            <strong class="text-primary">{{ preview.updateCount }}</strong> 筆、無異動
            <strong>{{ preview.unchangedCount }}</strong> 筆、錯誤
            <strong class="text-danger">{{ preview.errorCount }}</strong> 筆
          </p>

          <div v-if="preview?.errorCount > 0" class="alert alert-warning py-2 small mb-3">
            有錯誤的資料不會被匯入，資料庫不會受影響；只有「新增」「更新」的部分，在你按下「確認套用」後才會真的寫入。
          </div>

          <div
            v-if="displayRows.length === 0"
            class="text-center text-secondary py-3"
          >
            所有資料都沒有變動，不用套用
          </div>

          <ul v-else class="list-group">
            <li
              v-for="(row, i) in displayRows"
              :key="i"
              class="list-group-item"
            >
              <div class="d-flex align-items-center gap-2 mb-1">
                <span class="badge" :class="ACTION_BADGE[row.action]">
                  {{ ACTION_LABEL[row.action] }}
                </span>
                <strong>{{ primaryLabel(row) }}</strong>
              </div>
              <div v-if="row.action === 'ERROR'" class="text-danger small">
                {{ row.error }}
              </div>
              <ul v-else-if="row.changes?.length" class="small text-secondary mb-0 ps-3">
                <li v-for="(c, j) in row.changes" :key="j">{{ c }}</li>
              </ul>
            </li>
          </ul>
        </div>
        <div class="modal-footer border-0">
          <button
            type="button"
            class="btn btn-secondary"
            data-bs-dismiss="modal"
            :disabled="confirming"
          >
            取消
          </button>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="!canConfirm || confirming"
            @click="emit('confirm')"
          >
            {{ confirming ? "套用中..." : "確認套用" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped></style>
