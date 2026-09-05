<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  closeRepair,
  completeRepair,
  getRepair,
  markNotified,
  markUndelivered,
  notifyRejected,
  submitQuote,
  updateInspectionResult,
  updateQuote,
} from "../api";

const props = defineProps({
  repairId: { type: [String, Number], required: true },
});

const router = useRouter();

const repair = ref(null);
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

// ===== 中文對照表 =====
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
// 狀態對應的徽章顏色（Bootstrap badge 顏色），確保同狀態永遠同顏色
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
const APPROVAL_LABELS = {
  PENDING: "待確認",
  APPROVED: "同意維修",
  REJECTED: "拒絕維修",
};
const DROPOFF_LABELS = {
  SHIPPING: "寄送門市",
  IN_STORE: "親臨門市",
};
const PAY_LABELS = {
  IN_STORE: "門市付款",
  ONLINE: "線上付款",
};
const PAY_STATUS_LABELS = {
  UNPAID: "未付款",
  PAID: "已付款",
  REFUNDED: "已退款",
};
const PICKUP_LABELS = {
  SELF_PICKUP: "門市自取",
  COURIER: "宅配超商寄回",
};
function label(map, value) {
  if (!value) return "—";
  return map[value] ?? value;
}
// 2026-08-25T12:55:03.xxx -> 2026-08-25 12:55
function formatDateTime(value) {
  if (!value) return "—";
  return value.replace("T", " ").slice(0, 16);
}

// 取件方式/付款方式/付款狀態：只有走到「尚未取件」或「已結案」才有實際值可看
const HAS_PICKUP_INFO = ["AWAITING_PICKUP", "CLOSED"];

// ===== 檢測報價區：手機序號／維修項目／估價金額（待估價+已認領時可編輯） =====
const quoteForm = ref({
  serialNumber: "",
  repairItems: "",
  estimatedCost: null,
  inspectionResult: "",
});

function loadQuoteForm(r) {
  quoteForm.value = {
    serialNumber: r.serialNumber ?? "",
    repairItems: r.repairItems ?? "",
    estimatedCost: r.estimatedCost ?? null,
    inspectionResult: r.inspectionResult ?? "",
  };
}

// 送出報價：先存最新輸入內容，再正式送出，兩步合成一個按鈕
async function handleSubmitQuote() {
  if (
    !quoteForm.value.serialNumber ||
    !quoteForm.value.repairItems ||
    quoteForm.value.estimatedCost === null ||
    quoteForm.value.estimatedCost === ""
  ) {
    alert("手機序號、維修項目、估價金額都要填寫完才能送出報價");
    return;
  }
  if (!window.confirm("確定要送出嗎？送出即無法修改報價")) return;
  await runAction(async () => {
    await updateQuote(repair.value.id, {
      technicianId: repair.value.technicianId,
      serialNumber: quoteForm.value.serialNumber,
      repairItems: quoteForm.value.repairItems,
      estimatedCost: quoteForm.value.estimatedCost,
      inspectionResult: quoteForm.value.inspectionResult || null,
    });
    await submitQuote(repair.value.id, repair.value.technicianId);
  });
}

// ===== 未送檢（僅限待估價、已認領時，按鈕放在最上方狀態旁邊） =====
async function handleUndelivered() {
  if (!window.confirm("確定要標記這張維修單為「未送檢」嗎？")) return;
  await runAction(() =>
    markUndelivered(repair.value.id, repair.value.technicianId),
  );
}

// ===== 檢測結果（維修中、報價後不維修時可編輯） =====
const inspectionResultForm = ref("");
const inspectionResultSnapshot = ref("");

function loadInspectionResultForm(r) {
  inspectionResultForm.value = r.inspectionResult ?? "";
  inspectionResultSnapshot.value = inspectionResultForm.value;
}

const inspectionResultChanged = computed(
  () => inspectionResultForm.value !== inspectionResultSnapshot.value,
);

async function handleSaveInspectionResult() {
  if (!inspectionResultForm.value) {
    alert("請填寫檢測結果");
    return;
  }
  if (!inspectionResultChanged.value) {
    alert("內容沒有變更，不用儲存");
    return;
  }
  await runAction(() =>
    updateInspectionResult(repair.value.id, {
      technicianId: repair.value.technicianId,
      inspectionResult: inspectionResultForm.value,
    }),
  );
}

// ===== 維修完成區：最終金額／調整原因 =====
// 維修中：走 completeRepair，會把狀態推進到「維修完成」
const completeForm = ref({ finalCost: null, adjustmentNote: "" });

async function handleComplete() {
  if (!window.confirm("確定要送出嗎？")) return;
  await runAction(() =>
    completeRepair(repair.value.id, {
      technicianId: repair.value.technicianId,
      finalCost:
        completeForm.value.finalCost === ""
          ? null
          : completeForm.value.finalCost,
      adjustmentNote: completeForm.value.adjustmentNote || null,
    }),
  );
}

// ===== 已通知客戶取件（維修完成時） =====
async function handleNotify() {
  if (!window.confirm("確定已經通知客戶取件了嗎？")) return;
  await runAction(() =>
    markNotified(repair.value.id, repair.value.technicianId),
  );
}

// ===== 報價後不維修：技師填最終金額(檢測費)，按送出，狀態推進到「尚未取件」 =====
const rejectedFee = ref(null);

async function handleSubmitRejected() {
  if (!window.confirm("確定要送出嗎？")) return;
  await runAction(() =>
    notifyRejected(
      repair.value.id,
      repair.value.technicianId,
      rejectedFee.value === "" ? null : rejectedFee.value,
    ),
  );
}

// ===== 取件付款：付款狀態下拉選單選「已付款」後，旁邊小按鈕按下才真正結案、鎖住 =====
const closePayStatus = ref("UNPAID");

async function handleFinalClose() {
  if (closePayStatus.value !== "PAID") return;
  if (!window.confirm("確定要結案嗎？")) return;
  await runAction(() =>
    closeRepair(repair.value.id, repair.value.technicianId),
  );
}

// ===== 共用：呼叫API、重抓資料、錯誤處理 =====
async function runAction(actionFn) {
  errorMessage.value = "";
  successMessage.value = "";
  loading.value = true;
  try {
    await actionFn();
    successMessage.value = "操作成功";
    await fetchRepair();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response?.data?.message
      ? error.response.data.message
      : error.response
        ? `操作失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

async function fetchRepair() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await getRepair(props.repairId);
    repair.value = data;
    loadQuoteForm(data);
    loadInspectionResultForm(data);
    completeForm.value = {
      finalCost: data.estimatedCost ?? null,
      adjustmentNote: "",
    };
    rejectedFee.value = null;
    closePayStatus.value = "UNPAID";
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `讀取失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

onMounted(() => fetchRepair());
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="mb-3">
      <button class="btn btn-outline-secondary btn-sm" @click="router.back()">
        ← 返回列表
      </button>
    </div>

    <div v-if="loading && !repair" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="repair">
      <!-- 維修單號 + 狀態 + (待估價已認領時)未送檢按鈕 -->
      <div class="d-flex align-items-center gap-3 mb-4">
        <h1 class="fw-bold mb-0">維修單 #{{ repair.id }}</h1>
        <span
          class="badge fs-6"
          :class="STATUS_BADGE_CLASS[repair.repairStatus] ?? 'text-bg-secondary'"
          >{{ label(STATUS_LABELS, repair.repairStatus) }}</span
        >
        <button
          v-if="repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId"
          class="btn btn-outline-danger btn-sm ms-auto"
          :disabled="loading"
          @click="handleUndelivered"
        >
          未送檢
        </button>
      </div>

      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>
      <div v-if="successMessage" class="alert alert-success">
        {{ successMessage }}
      </div>

      <!-- 1. 基本資料 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">基本資料</div>
        <div class="card-body row g-3">
          <div class="col-12">
            <span class="text-secondary">客戶：</span
            >{{ repair.memberName }}（id:{{ repair.memberId }}）
          </div>
          <div class="col-md-6">
            <span class="text-secondary">品牌：</span>{{ repair.repairBrand }}
          </div>
          <div class="col-md-6">
            <span class="text-secondary">機型：</span>{{ repair.repairModel }}
          </div>
          <div class="col-md-6">
            <span class="text-secondary">送修方式：</span
            >{{ label(DROPOFF_LABELS, repair.dropoffType) }}
          </div>
          <div class="col-md-6">
            <span class="text-secondary">分店：</span>{{ repair.storeName }}
          </div>
          <div class="col-md-6">
            <span class="text-secondary">預約日期：</span
            >{{ repair.bookingDate ?? "—" }}
          </div>
          <div class="col-md-6">
            <span class="text-secondary">預約時段：</span
            >{{ repair.timeSlot ?? "—" }}
          </div>
          <div class="col-12">
            <span class="text-secondary">問題描述：</span
            >{{ repair.issueDescription }}
          </div>
        </div>
      </section>

      <!-- 2. 檢測報價 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">檢測/報價</div>
        <div class="card-body row g-3">
          <div class="col-md-4">
            <span class="text-secondary">技師：</span
            >{{ repair.technicianName ?? "尚未認領" }}
          </div>

          <!-- 手機序號 -->
          <div
            class="col-md-4"
            v-if="
              repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId
            "
          >
            <span class="text-secondary">手機序號：</span>
            <input
              v-model="quoteForm.serialNumber"
              type="text"
              class="form-control"
              style="
                display: inline-block;
                width: 140px;
                vertical-align: middle;
              "
            />
          </div>
          <div class="col-md-4" v-else>
            <span class="text-secondary">手機序號：</span
            >{{ repair.serialNumber ?? "—" }}
          </div>

          <!-- 客戶確認狀態 -->
          <div class="col-md-4">
            <span class="text-secondary">客戶確認狀態：</span
            >{{ label(APPROVAL_LABELS, repair.approvalStatus) }}
          </div>

          <!-- 維修項目 -->
          <div
            class="col-12"
            v-if="
              repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId
            "
          >
            <label class="form-label text-secondary">報價項目：</label>
            <textarea
              v-model="quoteForm.repairItems"
              class="form-control"
              rows="2"
            ></textarea>
          </div>
          <div class="col-12" v-else>
            <span class="text-secondary">報價項目：</span
            >{{ repair.repairItems ?? "—" }}
          </div>

          <!-- 估價金額 -->
          <div
            class="col-md-4"
            v-if="
              repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId
            "
          >
            <label class="form-label text-secondary">估價金額：</label>
            <input
              v-model.number="quoteForm.estimatedCost"
              type="number"
              class="form-control"
            />
          </div>
          <div class="col-md-4" v-else>
            <span class="text-secondary">估價金額：</span
            >{{ repair.estimatedCost ?? "—" }}元
          </div>

          <!-- 檢測結果 -->
          <div
            class="col-12"
            v-if="
              repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId
            "
          >
            <label class="form-label text-secondary">檢測結果：</label>
            <textarea
              v-model="quoteForm.inspectionResult"
              class="form-control"
              rows="2"
            ></textarea>
          </div>
          <div
            class="col-12"
            v-else-if="
              ['IN_REPAIR', 'QUOTE_REJECTED'].includes(repair.repairStatus)
            "
          >
            <label class="form-label text-secondary">檢測結果：</label>
            <textarea
              v-model="inspectionResultForm"
              class="form-control"
              rows="2"
            ></textarea>
            <button
              class="btn btn-outline-primary mt-2"
              :disabled="loading"
              @click="handleSaveInspectionResult"
            >
              儲存檢測結果
            </button>
          </div>
          <div class="col-12" v-else>
            <span class="text-secondary">檢測結果：</span
            >{{ repair.inspectionResult ?? "—" }}
          </div>

          <!-- 待估價+已認領：送出報價 -->
          <div
            class="col-12"
            v-if="
              repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId
            "
          >
            <button
              class="btn btn-primary"
              :disabled="loading"
              @click="handleSubmitQuote"
            >
              送出報價
            </button>
          </div>
        </div>
      </section>

      <!-- 已報價：等客戶回應提示 -->
      <div
        v-if="repair.repairStatus === 'QUOTED'"
        class="alert alert-secondary"
      >
        報價已送出，等待客戶在前台回應（同意／拒絕）。
      </div>

      <!-- 3. 維修完成 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">維修完成</div>
        <div class="card-body row g-3">
          <!-- 維修中：可編輯，送出後推進到「維修完成」 -->
          <template v-if="repair.repairStatus === 'IN_REPAIR'">
            <div class="col-md-6">
              <label class="form-label">最終金額</label>
              <input
                v-model.number="completeForm.finalCost"
                type="number"
                class="form-control"
              />
            </div>
            <div class="col-12">
              <label class="form-label"
                >調整原因（金額跟估價不同時需填寫）</label
              >
              <textarea
                v-model="completeForm.adjustmentNote"
                class="form-control"
                rows="2"
              ></textarea>
            </div>
            <div class="col-12">
              <button
                class="btn btn-primary"
                :disabled="loading"
                @click="handleComplete"
              >
                維修完成
              </button>
            </div>
          </template>

          <!-- 報價後不維修：這裡填的其實是檢測費 -->
          <template v-else-if="repair.repairStatus === 'QUOTE_REJECTED'">
            <div class="col-md-6">
              <label class="form-label"
                >最終金額（不填就是0元，即檢測費）</label
              >
              <input
                v-model.number="rejectedFee"
                type="number"
                class="form-control"
              />
            </div>
            <div class="col-12">
              <button
                class="btn btn-primary"
                :disabled="loading"
                @click="handleSubmitRejected"
              >
                送出
              </button>
            </div>
          </template>

          <div class="col-md-6" v-else>
            <span class="text-secondary">最終金額：</span
            >{{ repair.finalCost ?? "—" }}
          </div>

          <!-- 維修完成：通知客戶取件 -->
          <div class="col-12" v-if="repair.repairStatus === 'REPAIR_COMPLETED'">
            <button
              class="btn btn-primary"
              :disabled="loading"
              @click="handleNotify"
            >
              已通知客戶取件
            </button>
          </div>
        </div>
      </section>

      <!-- 4. 取件付款 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">取件付款</div>
        <div class="card-body row g-3">
          <div class="col-md-4">
            <span class="text-secondary">取件方式：</span
            >{{
              HAS_PICKUP_INFO.includes(repair.repairStatus)
                ? label(PICKUP_LABELS, repair.pickupType)
                : "—"
            }}
          </div>
          <div class="col-md-4">
            <span class="text-secondary">付款方式：</span
            >{{
              HAS_PICKUP_INFO.includes(repair.repairStatus)
                ? label(PAY_LABELS, repair.repairPay)
                : "—"
            }}
          </div>

          <div
            class="col-md-4"
            v-if="repair.repairStatus === 'AWAITING_PICKUP'"
          >
            <span class="text-secondary">付款狀態：</span>
            <select
              v-model="closePayStatus"
              class="form-select d-inline-block w-auto align-middle"
            >
              <option value="UNPAID">未付款</option>
              <option value="PAID">已付款</option>
            </select>
          </div>
          <div class="col-md-4" v-else>
            <span class="text-secondary">付款狀態：</span
            >{{
              repair.repairStatus === "CLOSED"
                ? label(PAY_STATUS_LABELS, repair.repairPayStatus)
                : "—"
            }}
          </div>
        </div>
      </section>

      <!-- 結案按鈕：走到尚未取件狀態才會出現，選「已付款」才能按 -->
      <div v-if="repair.repairStatus === 'AWAITING_PICKUP'" class="mb-3">
        <button
          class="btn btn-primary"
          :disabled="loading || closePayStatus !== 'PAID'"
          @click="handleFinalClose"
        >
          結案
        </button>
      </div>

      <!-- 建立/更新時間 -->
      <div class="text-secondary small">
        建立時間：{{ formatDateTime(repair.repairCreatedTime) }}　更新時間：{{
          formatDateTime(repair.repairUpdatedTime)
        }}
      </div>
    </template>
  </main>
</template>

<style scoped></style>
