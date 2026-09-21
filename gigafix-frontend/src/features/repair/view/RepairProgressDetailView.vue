<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  cancelRepair,
  getRepair,
  redirectToEcpayPayment,
  respondToQuote,
  submitPickupPayment,
} from "../api";
import RepairStatusStepper from "../components/RepairStatusStepper.vue";
import { swalConfirm, swalWarn, useSwalMessages } from "../../../utils/swal";

const props = defineProps({
  repairId: { type: [String, Number], required: true },
});

const router = useRouter();

const repair = ref(null);
const loading = ref(false);
const errorMessage = ref("");
useSwalMessages(errorMessage, null);

// ===== 中文對照表：跟後台 RepairDetailView 用同一套，確保狀態顯示一致 =====
// (狀態流程列已抽成 RepairStatusStepper 元件自己管理，這裡不用再放 STATUS_LABELS/STATUS_BADGE_CLASS)
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
  COURIER: "寄件",
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
    swalWarn("請選擇取件方式與付款方式");
    return;
  }
  if (
    form.pickupType === "COURIER" &&
    (!form.recipientName || !form.recipientPhone || !form.recipientAddress)
  ) {
    swalWarn("寄件需要填寫收件人姓名、電話、地址");
    return;
  }
  if (!(await swalConfirm("確定送出後如需更動需聯繫技師"))) return;

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
    // 選線上付款：後端已經把repairPayStatus標記PENDING，整頁導去綠界付款頁面(不是ajax)，
    // 付款完成的狀態更新依據是綠界打後端的ReturnURL，不是這裡的導頁
    if (form.repairPay === "ONLINE") {
      redirectToEcpayPayment(repair.value.id);
      return;
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
    : "確定要拒絕這份報價嗎？拒絕後技師會再跟你聯繫確認費用。";
  if (!(await swalConfirm(confirmMsg, { danger: !approve }))) return;

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

// ===== 客戶自行取消（僅限待估價、尚未被技師認領） =====
const cancelling = ref(false);

const canCancel = computed(
  () =>
    repair.value &&
    repair.value.repairStatus === "PENDING_QUOTE" &&
    !repair.value.technicianId,
);

async function handleCancel() {
  if (!(await swalConfirm("確定要取消這張維修預約嗎？取消後無法復原。", { danger: true }))) return;

  cancelling.value = true;
  errorMessage.value = "";
  try {
    await cancelRepair(repair.value.id);
    await fetchRepair();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response?.data?.message
      ? error.response.data.message
      : error.response
        ? `取消失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
  } finally {
    cancelling.value = false;
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
      <button class="btn btn-outline-secondary" @click="router.back()">
        ← 返回列表
      </button>
    </div>

    <div v-if="loading && !repair" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="repair">
      <div class="d-flex align-items-center gap-3 mb-4 flex-wrap">
        <h1 class="fw-bold mb-0">維修單 #{{ repair.id }}</h1>
        <RepairStatusStepper
          :repair-status="repair.repairStatus"
          :approval-status="repair.approvalStatus"
        />
        <button
          v-if="canCancel"
          class="btn btn-outline-danger btn-sm ms-auto"
          :disabled="cancelling"
          @click="handleCancel"
        >
          取消預約
        </button>
      </div>


      <!-- 已報價、尚未回應：提示客戶要確認是否維修 -->
      <div
        v-if="repair.repairStatus === 'QUOTED' && repair.approvalStatus === 'PENDING'"
        class="alert alert-info"
      >
        店家已完成報價，請確認下方「檢測/報價」內容，決定是否進行維修。
      </div>

      <div class="row g-4">
        <!-- 2x2排列：每列依自己的內容高度分配，避免一欄過長、另一欄留白過多 -->
        <div class="col-lg-6">
          <!-- 1. 基本資料 -->
          <section class="card section-card mb-4">
            <div class="card-header fw-bold section-card-header">
              <i class="bi bi-person-vcard"></i> 基本資料
            </div>
            <div class="card-body row g-3">
              <div class="col-sm-6 info-field">
                <div class="info-label">聯絡姓名</div>
                <div class="info-value">{{ repair.contactName }}</div>
              </div>
              <div class="col-sm-6 info-field">
                <div class="info-label">聯絡電話</div>
                <div class="info-value">{{ repair.contactPhone }}</div>
              </div>
              <div class="col-sm-6 info-field">
                <div class="info-label">品牌</div>
                <div class="info-value">{{ repair.repairBrand }}</div>
              </div>
              <div class="col-sm-6 info-field">
                <div class="info-label">機型</div>
                <div class="info-value">{{ repair.repairModel }}</div>
              </div>
              <div class="col-sm-6 info-field">
                <div class="info-label">送修方式</div>
                <div class="info-value">{{ label(DROPOFF_LABELS, repair.dropoffType) }}</div>
              </div>
              <div class="col-sm-6 info-field">
                <div class="info-label">分店</div>
                <div class="info-value">{{ repair.storeName }}</div>
              </div>
              <div class="col-sm-6 info-field">
                <div class="info-label">預約日期</div>
                <div class="info-value">{{ repair.bookingDate ?? "—" }}</div>
              </div>
              <div class="col-sm-6 info-field">
                <div class="info-label">預約時段</div>
                <div class="info-value">{{ repair.timeSlot ?? "—" }}</div>
              </div>
              <div class="col-12 info-field">
                <div class="info-label">問題描述</div>
                <div class="info-value">{{ repair.issueDescription }}</div>
              </div>
            </div>
          </section>
        </div>

        <div class="col-lg-6">
          <!-- 2. 檢測報價 -->
          <section class="card section-card mb-4">
            <div class="card-header fw-bold section-card-header">
              <i class="bi bi-tools"></i> 檢測/報價
            </div>
            <div class="card-body row g-3">
              <!-- 第1排(3欄)：技師/技師電話/手機序號 -->
              <div class="col-md-4 info-field">
                <div class="info-label">技師</div>
                <div class="info-value">{{ repair.technicianName ?? "尚未認領" }}</div>
              </div>
              <div class="col-md-4 info-field">
                <div class="info-label">技師電話</div>
                <div class="info-value">{{ repair.technicianPhone ?? "—" }}</div>
              </div>
              <div class="col-md-4 info-field">
                <div class="info-label">手機序號</div>
                <div class="info-value">{{ repair.serialNumber ?? "—" }}</div>
              </div>
              <!-- 第2排：客戶確認狀態對齊第1排「技師」欄位、估價金額對齊第1排「技師電話」欄位 -->
              <div class="col-md-4 info-field">
                <div class="info-label">客戶確認狀態</div>
                <div class="info-value">{{ label(APPROVAL_LABELS, repair.approvalStatus) }}</div>
              </div>
              <div class="col-md-4 info-field">
                <div class="info-label">估價金額</div>
                <div class="info-value">{{ repair.estimatedCost ?? "—" }}元</div>
              </div>
              <div class="col-12 info-field">
                <div class="info-label">報價項目</div>
                <div class="info-value">{{ repair.repairItems ?? "—" }}</div>
              </div>
              <div class="col-12 info-field">
                <div class="info-label">檢測結果</div>
                <div class="info-value result-box">{{ repair.inspectionResult ?? "—" }}</div>
              </div>

              <!-- 已報價、尚未回應：同意／拒絕維修 -->
              <div
                class="col-12"
                v-if="repair.repairStatus === 'QUOTED' && repair.approvalStatus === 'PENDING'"
              >
                <button
                  class="btn btn-primary me-2 rounded-pill px-4"
                  :disabled="responding"
                  @click="handleRespond(true)"
                >
                  <i class="bi bi-check-lg me-1"></i>同意維修
                </button>
                <button
                  class="btn btn-outline-danger rounded-pill px-4"
                  :disabled="responding"
                  @click="handleRespond(false)"
                >
                  <i class="bi bi-x-lg me-1"></i>拒絕維修
                </button>
              </div>
            </div>
          </section>
        </div>

        <div class="col-lg-6">
          <!-- 3. 維修完成 -->
          <section class="card section-card mb-4">
            <div class="card-header fw-bold section-card-header">
              <i class="bi bi-check-circle"></i> 維修完成
            </div>
            <div class="card-body row g-3">
              <div class="col-12 info-field">
                <div class="info-label">最終金額</div>
                <div class="info-value">{{ repair.finalCost ?? "—" }}</div>
              </div>
            </div>
          </section>
        </div>

        <div class="col-lg-6">
          <!-- 4. 取件付款 -->
          <section class="card section-card mb-4">
            <div class="card-header fw-bold section-card-header">
              <i class="bi bi-box-seam"></i> 取件付款
            </div>
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
                  <div class="col-12">
                    <label class="form-label">收件人姓名</label>
                    <input v-model="pickupPaymentForm.recipientName" type="text" class="form-control" />
                  </div>
                  <div class="col-12">
                    <label class="form-label">收件人電話</label>
                    <input v-model="pickupPaymentForm.recipientPhone" type="text" class="form-control" />
                  </div>
                  <div class="col-12">
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
                <div class="col-6 info-field">
                  <div class="info-label">取件方式</div>
                  <div class="info-value">{{ label(PICKUP_LABELS, repair.pickupType) }}</div>
                </div>
                <div class="col-6 info-field">
                  <div class="info-label">付款方式</div>
                  <div class="info-value">{{ label(PAY_LABELS, repair.repairPay) }}</div>
                </div>
                <div class="col-12 info-field">
                  <div class="info-label">付款狀態</div>
                  <div class="info-value">
                    {{ label(PAY_STATUS_LABELS, repair.repairPayStatus) }}
                    <button
                      v-if="repair.repairPay === 'ONLINE' && repair.repairPayStatus === 'PENDING'"
                      class="btn btn-outline-primary btn-sm ms-2"
                      :disabled="submittingPickupPayment"
                      @click="redirectToEcpayPayment(repair.id)"
                    >
                      重新付款
                    </button>
                  </div>
                </div>
                <div class="col-12 info-field" v-if="repair.pickupType === 'COURIER'">
                  <div class="info-label">收件資訊</div>
                  <div class="info-value">
                    {{ repair.recipientName }}（{{ repair.recipientPhone }}）　{{
                      repair.recipientAddress
                    }}
                  </div>
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
        </div>
      </div>

      <div class="text-secondary">
        建立時間：{{ formatDateTime(repair.repairCreatedTime) }}　更新時間：{{
          formatDateTime(repair.repairUpdatedTime)
        }}
      </div>
    </template>
  </main>
</template>

<style scoped>
/* 區塊卡片：圓角+柔和陰影，取代原本方正的預設 Bootstrap card */
.section-card {
  border: none;
  border-radius: 1rem;
  box-shadow: 0 2px 10px rgba(30, 53, 87, 0.08);
  overflow: hidden;
}

/* 區塊標題色塊：跟站內品牌藍(首頁/預約頁同一套色)統一風格，取代原本純白預設樣式 */
.section-card-header {
  background-color: #a8cdf0;
  color: #14263d;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
}

/* 資訊欄位：label在上、值在下，比原本「label：值」同一行更好掃視 */
.info-label {
  font-size: 16px;
  color: var(--bs-secondary-color, #6c757d);
  margin-bottom: 2px;
}

.info-value {
  font-size: 18px;
  font-weight: 700;
  color: #1d324b;
  word-break: break-word;
}

/* 檢測結果內容可能很長，限制高度用垂直捲軸取代無限撐高卡片，寬度已經被欄位本身限制住了 */
.result-box {
  max-height: 120px;
  overflow-y: auto;
}
</style>
