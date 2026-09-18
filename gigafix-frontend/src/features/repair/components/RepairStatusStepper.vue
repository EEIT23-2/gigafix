<script setup>
import { computed } from "vue";

// 維修單狀態流程列：取代原本標題旁單一顆狀態徽章，依目前狀態/客戶確認結果挑出對應的流程分支逐格顯示
// 中文標籤跟 RepairDetailView、RepairProgressDetailView 等處用同一套對照表
const props = defineProps({
  repairStatus: { type: String, required: true },
  approvalStatus: { type: String, default: null },
});

const STATUS_LABELS = {
  PENDING_QUOTE: "待估價",
  QUOTED: "已報價",
  IN_REPAIR: "維修中",
  QUOTE_REJECTED: "報價後不維修",
  REPAIR_COMPLETED: "維修完成",
  AWAITING_PICKUP: "尚未取件",
  CLOSED: "已結案",
  CANCELLED: "已取消",
  NOT_DROPPED_OFF: "未送檢",
};

// 目前所在階段要用的顏色，跟維修單列表(RepairsListView/RepairProgressListView)的徽章顏色同一套飽和色，方便一眼認出
const STATUS_BADGE_CLASS = {
  PENDING_QUOTE: "text-bg-warning",
  QUOTED: "text-bg-info",
  IN_REPAIR: "text-bg-primary",
  QUOTE_REJECTED: "text-bg-danger",
  REPAIR_COMPLETED: "text-bg-success",
  AWAITING_PICKUP: "text-bg-warning",
  CLOSED: "text-bg-dark",
  CANCELLED: "text-bg-secondary",
  NOT_DROPPED_OFF: "text-bg-secondary",
};

// 4種流程分支：正常走完整流程／報價後不維修／未送檢／客戶自行取消，各自獨立不互相混
const SEQ_NORMAL = ["PENDING_QUOTE", "QUOTED", "IN_REPAIR", "REPAIR_COMPLETED", "AWAITING_PICKUP", "CLOSED"];
const SEQ_REJECTED = ["PENDING_QUOTE", "QUOTED", "QUOTE_REJECTED", "AWAITING_PICKUP", "CLOSED"];
const SEQ_NOT_DROPPED_OFF = ["PENDING_QUOTE", "NOT_DROPPED_OFF"];
const SEQ_CANCELLED = ["PENDING_QUOTE", "CANCELLED"];

// 未送檢／已取消是獨立分支；報價後不維修看 approvalStatus 判斷
// (即使後來從 QUOTE_REJECTED 推進到尚未取件/已結案，approvalStatus 仍會維持 REJECTED，所以還是能判斷出正確分支)
const sequence = computed(() => {
  if (props.repairStatus === "NOT_DROPPED_OFF") return SEQ_NOT_DROPPED_OFF;
  if (props.repairStatus === "CANCELLED") return SEQ_CANCELLED;
  if (props.approvalStatus === "REJECTED") return SEQ_REJECTED;
  return SEQ_NORMAL;
});
</script>

<template>
  <div class="repair-status-stepper">
    <template v-for="(status, index) in sequence" :key="status">
      <span
        class="stepper-node"
        :class="
          status === repairStatus
            ? [STATUS_BADGE_CLASS[status], 'stepper-node-current']
            : 'stepper-node-inactive'
        "
      >
        {{ STATUS_LABELS[status] }}
      </span>
      <span v-if="index < sequence.length - 1" class="stepper-divider">─</span>
    </template>
  </div>
</template>

<style scoped>
.repair-status-stepper {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}

.stepper-node {
  display: inline-block;
  border-radius: 999px;
  white-space: nowrap;
  line-height: 1.4;
}

/* 非目前階段：不上底色，只留橢圓外框+文字，避免每格都上色反而看不出目前在哪 */
.stepper-node-inactive {
  padding: 3px 12px;
  font-size: 0.85rem;
  font-weight: 500;
  border: 1px solid var(--bs-border-color, #ced4da);
  color: var(--bs-secondary-color, #6c757d);
  background-color: transparent;
}

/* 目前所在階段：跟維修單列表同一套飽和色，並放大1.5倍凸顯出來 */
.stepper-node-current {
  padding: 5px 18px;
  font-size: 1.275rem;
  font-weight: 700;
}

.stepper-divider {
  color: var(--bs-secondary-color, #adb5bd);
  font-size: 0.8rem;
}
</style>
