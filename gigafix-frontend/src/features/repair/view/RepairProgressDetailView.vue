<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getRepair, respondToQuote, submitPickupPayment } from "../api";

const props = defineProps({
  repairId: { type: [String, Number], required: true },
});

const router = useRouter();

const repair = ref(null);
const loading = ref(false);
const errorMessage = ref("");

// ===== 中文對照表：跟後台 RepairDetailView 用同一套，確保狀態顯示一致 =====
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
  PENDING: "付款中",
};
const PICKUP_LABELS = {
  SELF_PICKUP: "門市自取",
  COURIER: "宅配超商寄回",
};
function label(map, value) {
  if (!value) return "—";
  return map[value] ?? value;
}
function formatDateTime(value) {
  if (!value) return "—";
  return value.replace("T", " ").slice(0, 16);
}

// ===== 取件付款：客戶選取件方式＋付款方式（選寄件要附收件人資訊），只能送出一次 =====
const pickupPaymentForm = ref({
  pickupType: "",
  repairPay: "",
  recipientName: "",
  recipientPhone: "",
  recipientAddress: "",
});
const submittingPickupPayment = ref(false);

// 狀態要到「維修完成」或「尚未取件」、而且還沒送出過(pickupType還是null)才顯示選擇表單
const needsPickupPaymentChoice = computed(
  () =>
    repair.value &&
    ["REPAIR_COMPLETED", "AWAITING_PICKUP"].includes(repair.value.repairStatus) &&
    !repair.value.pickupType,
);

async function handleSubmitPickupPayment() {
  const form = pickupPaymentForm.value;
  if (!form.pickupType || !form.repairPay) {
    alert("請選擇取件方式與付款方式");
    return;
  }
  if (
    form.pickupType === "COURIER" &&
    (!form.recipientName || !form.recipientPhone || !form.recipientAddress)
  ) {
    alert("寄件需要填寫收件人姓名、電話、地址");
    return;
  }
  if (!window.confirm("確定送出後如需更動需聯繫技師")) return;

  submittingPickupPayment.value = true;
  errorMessage.value = "";
  try {
    await submitPickupPayment(repair.value.id, {
      pickupType: form.pickupType,
      repairPay: form.repairPay,
      recipientName: form.pickupType === "COURIER" ? form.recipientName : null,
      recipientPhone: form.pickupType === "COURIER" ? form.recipientPhone : null,
      recipientAddress: form.pickupType === "COURIER" ? form.recipientAddress : null,
    });
    // TODO(綠界金流)：這裡目前只是占位提示，還沒真的接綠界，真的要接的時候：
    // 1. 後端要先跟綠界建立訂單(MerchantTradeNo/金額/商品名稱等)，把表單參數回傳給前端
    // 2. 前端用拿到的參數組表單、整頁POST到綠界的付款頁面(測試站/正式站)，不是用ajax
    // 3. 付款完成後，真正拿來把repairPayStatus改成PAID的依據是綠界打後端的NotifyURL(伺服器對伺服器回調)，
    //    不能只靠付完款後導回來的ReturnURL，因為使用者可能付完直接關分頁、導回請求也可能被使用者取消或竄改
    // 4. NotifyURL要是外部連得到的網址(不能是localhost)、要驗證CheckMacValue、金額要傳整數、字元編碼要用UTF8
    if (form.repairPay === "ONLINE") {
      alert("（占位畫面）即將導向綠界付款頁面，尚未串接綠界，暫時以此提示代替");
    }
    await fetchRepair();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response?.data?.message
      ? error.response.data.message
      : error.response
        ? `送出失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
  } finally {
    submittingPickupPayment.value = false;
  }
}

// ===== 客戶回應報價（同意／拒絕） =====
const responding = ref(false);

async function handleRespond(approve) {
  const confirmMsg = approve
    ? "確定要同意這份報價，開始維修嗎？"
    : "確定要拒絕這份報價嗎？拒絕後將只收取檢測費用。";
  if (!window.confirm(confirmMsg)) return;

  responding.value = true;
  errorMessage.value = "";
  try {
    await respondToQuote(repair.value.id, approve);
    await fetchRepair();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response?.data?.message
      ? error.response.data.message
      : error.response
        ? `送出失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
  } finally {
    responding.value = false;
  }
}

async function fetchRepair() {
  loading.value = true;
  errorMessage.value = "";
  try {
    repair.value = await getRepair(props.repairId);
    pickupPaymentForm.value = {
      pickupType: "",
      repairPay: "",
      recipientName: "",
      recipientPhone: "",
      recipientAddress: "",
    };
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `讀取失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchRepair();
});
</script>

<template>
  <main>
    <div class="mb-3">
      <button class="btn btn-outline-secondary btn-sm" @click="router.back()">
        ← 返回列表
      </button>
    </div>

    <div v-if="loading && !repair" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="repair">
      <div class="d-flex align-items-center gap-3 mb-4">
        <h1 class="fw-bold mb-0">維修單 #{{ repair.id }}</h1>
        <span
          class="badge fs-6"
          :class="STATUS_BADGE_CLASS[repair.repairStatus] ?? 'text-bg-secondary'"
          >{{ label(STATUS_LABELS, repair.repairStatus) }}</span
        >
      </div>

      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>

      <!-- 已報價、尚未回應：提示客戶要確認是否維修 -->
      <div
        v-if="repair.repairStatus === 'QUOTED' && repair.approvalStatus === 'PENDING'"
        class="alert alert-info"
      >
        店家已完成報價，請確認下方「檢測/報價」內容，決定是否進行維修。
      </div>

      <!-- 1. 基本資料 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">基本資料</div>
        <div class="card-body row g-3">
          <div class="col-md-6">
            <span class="text-secondary">聯絡姓名：</span>{{ repair.contactName }}
          </div>
          <div class="col-md-6">
            <span class="text-secondary">聯絡電話：</span>{{ repair.contactPhone }}
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
          <div class="col-md-4">
            <span class="text-secondary">手機序號：</span
            >{{ repair.serialNumber ?? "—" }}
          </div>
          <div class="col-md-4">
            <span class="text-secondary">客戶確認狀態：</span
            >{{ label(APPROVAL_LABELS, repair.approvalStatus) }}
          </div>
          <div class="col-12">
            <span class="text-secondary">報價項目：</span
            >{{ repair.repairItems ?? "—" }}
          </div>
          <div class="col-md-4">
            <span class="text-secondary">估價金額：</span
            >{{ repair.estimatedCost ?? "—" }}元
          </div>
          <div class="col-12">
            <span class="text-secondary">檢測結果：</span
            >{{ repair.inspectionResult ?? "—" }}
          </div>

          <!-- 已報價、尚未回應：同意／拒絕維修 -->
          <div
            class="col-12"
            v-if="repair.repairStatus === 'QUOTED' && repair.approvalStatus === 'PENDING'"
          >
            <button
              class="btn btn-primary me-2"
              :disabled="responding"
              @click="handleRespond(true)"
            >
              同意維修
            </button>
            <button
              class="btn btn-outline-danger"
              :disabled="responding"
              @click="handleRespond(false)"
            >
              拒絕維修
            </button>
          </div>
        </div>
      </section>

      <!-- 3. 維修完成 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">維修完成</div>
        <div class="card-body row g-3">
          <div class="col-md-6">
            <span class="text-secondary">最終金額：</span
            >{{ repair.finalCost ?? "—" }}
          </div>
        </div>
      </section>

      <!-- 4. 取件付款 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">取件付款</div>
        <div class="card-body row g-3">
          <!-- 維修完成/尚未取件、且還沒送出過取件付款方式：顯示選擇表單，送出後就鎖住 -->
          <template v-if="needsPickupPaymentChoice">
            <div class="col-12">
              <label class="form-label text-secondary d-block">取件方式</label>
              <div class="form-check form-check-inline">
                <input
                  id="pickupSelf"
                  v-model="pickupPaymentForm.pickupType"
                  class="form-check-input"
                  type="radio"
                  value="SELF_PICKUP"
                />
                <label class="form-check-label" for="pickupSelf">門市取件</label>
              </div>
              <div class="form-check form-check-inline">
                <input
                  id="pickupCourier"
                  v-model="pickupPaymentForm.pickupType"
                  class="form-check-input"
                  type="radio"
                  value="COURIER"
                />
                <label class="form-check-label" for="pickupCourier">寄件</label>
              </div>
            </div>

            <template v-if="pickupPaymentForm.pickupType === 'COURIER'">
              <div class="col-md-4">
                <label class="form-label">收件人姓名</label>
                <input v-model="pickupPaymentForm.recipientName" type="text" class="form-control" />
              </div>
              <div class="col-md-4">
                <label class="form-label">收件人電話</label>
                <input v-model="pickupPaymentForm.recipientPhone" type="text" class="form-control" />
              </div>
              <div class="col-md-4">
                <label class="form-label">收件地址</label>
                <input v-model="pickupPaymentForm.recipientAddress" type="text" class="form-control" />
              </div>
            </template>

            <div class="col-12">
              <label class="form-label text-secondary d-block">付款方式</label>
              <!-- 寄件沒辦法到門市付款，只能線上付款 -->
              <div class="form-check form-check-inline" v-if="pickupPaymentForm.pickupType !== 'COURIER'">
                <input
                  id="payInStore"
                  v-model="pickupPaymentForm.repairPay"
                  class="form-check-input"
                  type="radio"
                  value="IN_STORE"
                />
                <label class="form-check-label" for="payInStore">門市付款</label>
              </div>
              <div class="form-check form-check-inline">
                <input
                  id="payOnline"
                  v-model="pickupPaymentForm.repairPay"
                  class="form-check-input"
                  type="radio"
                  value="ONLINE"
                />
                <label class="form-check-label" for="payOnline">線上付款</label>
              </div>
            </div>

            <div class="col-12">
              <button
                class="btn btn-primary"
                :disabled="submittingPickupPayment"
                @click="handleSubmitPickupPayment"
              >
                送出
              </button>
            </div>
          </template>

          <!-- 已經送出過(顯示實際值)、或還沒到可以選的階段(label遇到null自動顯示—) -->
          <template v-else>
            <div class="col-md-4">
              <span class="text-secondary">取件方式：</span
              >{{ label(PICKUP_LABELS, repair.pickupType) }}
            </div>
            <div class="col-md-4">
              <span class="text-secondary">付款方式：</span
              >{{ label(PAY_LABELS, repair.repairPay) }}
            </div>
            <div class="col-md-4">
              <span class="text-secondary">付款狀態：</span
              >{{ label(PAY_STATUS_LABELS, repair.repairPayStatus) }}
            </div>
            <div class="col-12" v-if="repair.pickupType === 'COURIER'">
              <span class="text-secondary">收件資訊：</span
              >{{ repair.recipientName }}（{{ repair.recipientPhone }}）　{{
                repair.recipientAddress
              }}
            </div>
            <div
              class="col-12 text-muted small"
              v-if="repair.pickupType && repair.repairStatus !== 'CLOSED'"
            >
              如需更動取件/付款/收件資訊，請聯繫負責的技師。
            </div>
          </template>
        </div>
      </section>

      <div class="text-secondary small">
        建立時間：{{ formatDateTime(repair.repairCreatedTime) }}　更新時間：{{
          formatDateTime(repair.repairUpdatedTime)
        }}
      </div>
    </template>
  </main>
</template>
