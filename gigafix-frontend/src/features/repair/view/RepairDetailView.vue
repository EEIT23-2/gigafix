<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { Modal } from "bootstrap";
import {
  closeRepair,
  completeRepair,
  getRepair,
  markNotified,
  markUndelivered,
  notifyRejected,
  submitQuote,
  updateInspectionResult,
  updatePayStatus,
  updateQuote,
  updateRecipient,
} from "../api";
import {
  SERIES_LIST,
  REPAIR_ITEMS,
  getModelsForSeries,
  getItemPrice,
  formatPrice,
} from "../priceTable";

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
// 2026-08-25T12:55:03.xxx -> 2026-08-25 12:55
function formatDateTime(value) {
  if (!value) return "—";
  return value.replace("T", " ").slice(0, 16);
}

// ===== 檢測報價區：手機序號／報價項目購物車／估價金額（待估價+已認領時可編輯） =====
const quoteForm = ref({
  serialNumber: "",
  inspectionResult: "",
});

// 報價項目改成購物車：每一列是{id, label, price}，可能是從下面的參考小工具選的標準項目，
// 也可能是技師自己手動輸入的自訂項目(金額可以打負數，代表折扣/優惠)
const quoteItems = ref([]);
let quoteItemSeq = 0; // 給每一列一個遞增id當:key用，不需要真的全域唯一

// 這張單之前如果已經存過報價，購物車一律從空的開始(不解析舊字串)，這裡只存來顯示參考用
const previousQuote = ref({ repairItems: "", estimatedCost: null });

// 送出報價按下去，手機序號沒填、或購物車一個項目都沒有，就要標示錯誤
const quoteFieldErrors = ref({
  serialNumber: false,
  quoteItems: false,
});

function loadQuoteForm(r) {
  quoteForm.value = {
    serialNumber: r.serialNumber ?? "",
    inspectionResult: r.inspectionResult ?? "",
  };
  quoteItems.value = [];
  previousQuote.value = {
    repairItems: r.repairItems ?? "",
    estimatedCost: r.estimatedCost ?? null,
  };
  quoteFieldErrors.value = { serialNumber: false, quoteItems: false };
}

// 手機序號補填之後，即時把紅框拿掉，不用等下一次按送出才清除
watch(
  () => quoteForm.value.serialNumber,
  (value) => {
    if (quoteFieldErrors.value.serialNumber && value) {
      quoteFieldErrors.value.serialNumber = false;
    }
  },
);
// 購物車只要有項目了，就把「至少要有一個項目」的錯誤拿掉
watch(
  () => quoteItems.value.length,
  (length) => {
    if (quoteFieldErrors.value.quoteItems && length > 0) {
      quoteFieldErrors.value.quoteItems = false;
    }
  },
);

// ===== 報價參考小工具：選系列/機型，下拉選項目直接加進購物車(不會清掉購物車裡原本的項目) =====
const pickerSeries = ref(SERIES_LIST[0].id);
const pickerModelIndex = ref(0);
const pickerItemKey = ref(""); // 選完就加入購物車、重置成空字串，可以連續加選

const pickerModels = computed(() => getModelsForSeries(pickerSeries.value));

// 換系列時，原本選的機型index可能超出新系列的機型數量，重置成第一個機型避免對不到價格
watch(pickerSeries, () => {
  pickerModelIndex.value = 0;
});

// 下拉選單只列「這個機型查得到價格、購物車裡還沒加過」的項目：
// 已經在購物車的不會重複出現，尚未開放報價(詢價)的項目也不會出現，避免加進去金額變成空的
const pickerAvailableItems = computed(() => {
  const addedKeys = new Set(
    quoteItems.value.filter((i) => i.itemKey).map((i) => i.itemKey),
  );
  return REPAIR_ITEMS.filter((item) => {
    if (addedKeys.has(item.key)) return false;
    return getItemPrice(pickerSeries.value, pickerModelIndex.value, item.key) != null;
  });
});

function addPickerItem() {
  if (!pickerItemKey.value) return;
  const item = REPAIR_ITEMS.find((i) => i.key === pickerItemKey.value);
  const price = getItemPrice(pickerSeries.value, pickerModelIndex.value, pickerItemKey.value);
  quoteItems.value.push({
    id: ++quoteItemSeq,
    label: item.label,
    type: "amount",
    price,
    itemKey: item.key,
  });
  pickerItemKey.value = "";
}

// ===== 購物車最下面：技師自己手動新增項目(不在13個標準項目裡的，或是折扣/優惠列) =====
// 類型「金額」：直接加(打負數就是扣錢)；類型「折扣」：把這一列以上已經加總的金額乘上折數(例如打8折輸入8)
const customItemLabel = ref("");
const customItemType = ref("amount"); // "amount" | "percent"
const customItemPrice = ref("");

function addCustomItem() {
  const label = customItemLabel.value.trim();
  if (!label) return;
  if (customItemType.value === "percent") {
    const percent = customItemPrice.value === "" ? 10 : Number(customItemPrice.value);
    quoteItems.value.push({ id: ++quoteItemSeq, label, type: "percent", percent, itemKey: null });
  } else {
    const price = customItemPrice.value === "" ? 0 : Number(customItemPrice.value);
    quoteItems.value.push({ id: ++quoteItemSeq, label, type: "amount", price, itemKey: null });
  }
  customItemLabel.value = "";
  customItemType.value = "amount";
  customItemPrice.value = "";
}

function removeQuoteItem(id) {
  quoteItems.value = quoteItems.value.filter((i) => i.id !== id);
}

// 目前加總：由上到下依序計算，「金額」列直接加(可打負數扣錢)，「折扣」列把目前為止的加總乘上折數，
// 所以折扣列要套用在全部品項上的話，記得放在購物車最後一列
const cartTotal = computed(() => {
  let total = 0;
  for (const item of quoteItems.value) {
    if (item.type === "percent") {
      total = total * (item.percent / 10);
    } else {
      total += Number(item.price) || 0;
    }
  }
  return total;
});

// 組成要送給後端的報價項目文字，跟畫面上購物車顯示的內容一致
const repairItemsText = computed(() =>
  quoteItems.value
    .map((i) => (i.type === "percent" ? `${i.label}(×${i.percent}折)` : `${i.label}(${formatPrice(i.price)})`))
    .join("、"),
);

// ===== 送出報價前的確認彈窗：唯讀顯示目前內容，技師再看一次確認沒填錯 =====
const quoteConfirmModalRef = ref(null);
let quoteConfirmModalInstance = null;

// 按「送出報價」先檢查必填欄位，沒填的欄位標紅框，通過才打開確認彈窗
function openQuoteConfirmModal() {
  quoteFieldErrors.value = {
    serialNumber: !quoteForm.value.serialNumber,
    quoteItems: quoteItems.value.length === 0,
  };
  if (Object.values(quoteFieldErrors.value).some(Boolean)) {
    alert("需填寫完整才能送出報價");
    return;
  }
  quoteConfirmModalInstance.show();
}

// 確認彈窗裡按「送出報價」才是真的送出，送出即無法修改
async function confirmSubmitQuote() {
  quoteConfirmModalInstance.hide();
  await runAction(async () => {
    await updateQuote(repair.value.id, {
      technicianId: repair.value.technicianId,
      serialNumber: quoteForm.value.serialNumber,
      repairItems: repairItemsText.value,
      estimatedCost: cartTotal.value,
      inspectionResult: quoteForm.value.inspectionResult || null,
    });
    await submitQuote(repair.value.id, repair.value.technicianId);
  });
}

// 「儲存」：先把目前填的內容存起來，不用全部欄位都填好，方便技師分次填寫、避免資料遺失。
// 購物車是空的話，報價項目/估價金額傳null(後端會跳過不覆蓋)，不然會把之前存的報價洗成空的
async function handleSaveQuoteDraft() {
  await runAction(() =>
    updateQuote(repair.value.id, {
      technicianId: repair.value.technicianId,
      serialNumber: quoteForm.value.serialNumber,
      repairItems: quoteItems.value.length > 0 ? repairItemsText.value : null,
      estimatedCost: quoteItems.value.length > 0 ? cartTotal.value : null,
      inspectionResult: quoteForm.value.inspectionResult,
    }),
  );
  if (!errorMessage.value) {
    alert("儲存成功");
  }
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

// ===== 收件資訊（客戶選寄件時才有）：結案前技師都可以編輯、儲存 =====
const recipientForm = ref({
  recipientName: "",
  recipientPhone: "",
  recipientAddress: "",
});

function loadRecipientForm(r) {
  recipientForm.value = {
    recipientName: r.recipientName ?? "",
    recipientPhone: r.recipientPhone ?? "",
    recipientAddress: r.recipientAddress ?? "",
  };
}

async function handleSaveRecipient() {
  const f = recipientForm.value;
  if (!f.recipientName || !f.recipientPhone || !f.recipientAddress) {
    alert("收件人姓名、電話、地址都要填寫");
    return;
  }
  await runAction(() =>
    updateRecipient(repair.value.id, {
      technicianId: repair.value.technicianId,
      recipientName: f.recipientName,
      recipientPhone: f.recipientPhone,
      recipientAddress: f.recipientAddress,
    }),
  );
}

// ===== 線上付款：目前尚未串接綠界，先讓技師在確認客戶已經付款後手動標記 =====
async function handleMarkOnlinePaid() {
  if (!window.confirm("確定客戶已經完成線上付款了嗎？")) return;
  await runAction(() => updatePayStatus(repair.value.id, "PAID"));
}

// ===== 取件付款：門市付款用下拉選單選「已付款」當確認手續；線上付款要repairPayStatus已經是PAID才能結案 =====
const closePayStatus = ref("UNPAID");

const canFinalClose = computed(() => {
  if (!repair.value || repair.value.repairStatus !== "AWAITING_PICKUP") return false;
  if (repair.value.repairPay === "ONLINE") {
    return repair.value.repairPayStatus === "PAID";
  }
  return closePayStatus.value === "PAID";
});

async function handleFinalClose() {
  if (!canFinalClose.value) return;
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
    loadRecipientForm(data);
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

onMounted(() => {
  quoteConfirmModalInstance = new Modal(quoteConfirmModalRef.value);
  fetchRepair();
});
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
              :class="{ 'is-invalid': quoteFieldErrors.serialNumber }"
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

          <!-- 報價參考小工具：選系列/機型，下拉選項目直接加進下面的購物車 -->
          <div
            class="col-12"
            v-if="
              repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId
            "
          >
            <label class="form-label text-secondary">報價參考小工具：</label>
            <div class="d-flex flex-wrap gap-2">
              <select v-model="pickerSeries" class="form-select" style="max-width: 160px">
                <option v-for="s in SERIES_LIST" :key="s.id" :value="s.id">
                  {{ s.label }}
                </option>
              </select>
              <select v-model.number="pickerModelIndex" class="form-select" style="max-width: 160px">
                <option v-for="(model, index) in pickerModels" :key="model" :value="index">
                  {{ model }}
                </option>
              </select>
              <select
                v-model="pickerItemKey"
                class="form-select"
                style="max-width: 260px"
                @change="addPickerItem"
              >
                <option value="">選擇項目，加入下方報價項目</option>
                <option v-for="item in pickerAvailableItems" :key="item.key" :value="item.key">
                  {{ item.label }}({{ formatPrice(getItemPrice(pickerSeries, pickerModelIndex, item.key)) }})
                </option>
              </select>
            </div>
          </div>

          <!-- 報價項目：購物車形式，選小工具的項目或自己新增都會往下加一列，可以個別刪除 -->
          <div
            class="col-12"
            v-if="
              repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId
            "
          >
            <label class="form-label text-secondary">報價項目：</label>
            <p v-if="previousQuote.repairItems" class="text-muted small mb-2">
              先前紀錄(僅供參考，購物車不會自動帶入)：{{ previousQuote.repairItems }}（{{
                previousQuote.estimatedCost ?? "—"
              }}元）
            </p>
            <div class="quote-cart">
              <div v-for="item in quoteItems" :key="item.id" class="quote-cart-row">
                <span class="quote-cart-label">{{ item.label }}</span>
                <span class="quote-cart-price">
                  {{ item.type === "percent" ? `× ${item.percent}折` : formatPrice(item.price) }}
                </span>
                <button
                  type="button"
                  class="btn-close"
                  aria-label="移除"
                  @click="removeQuoteItem(item.id)"
                ></button>
              </div>
              <p v-if="quoteItems.length === 0" class="text-muted small mb-0 py-1">
                還沒有任何報價項目，可以從上面小工具選，或在下面自己新增
              </p>
              <div class="quote-cart-row quote-cart-new">
                <input
                  v-model="customItemLabel"
                  type="text"
                  class="form-control form-control-sm"
                  placeholder="自訂項目名稱"
                  @keyup.enter="addCustomItem"
                />
                <select v-model="customItemType" class="form-select form-select-sm" style="max-width: 90px">
                  <option value="amount">金額</option>
                  <option value="percent">折扣</option>
                </select>
                <input
                  v-model="customItemPrice"
                  type="number"
                  class="form-control form-control-sm no-spinner"
                  :placeholder="customItemType === 'percent' ? '折數(例如8=8折)' : '金額(折扣可打負數)'"
                  @keyup.enter="addCustomItem"
                />
                <button
                  type="button"
                  class="btn btn-outline-primary btn-sm"
                  :disabled="!customItemLabel.trim()"
                  @click="addCustomItem"
                >
                  ＋
                </button>
              </div>
            </div>
            <p v-if="quoteFieldErrors.quoteItems" class="text-danger small mb-0 mt-1">
              請至少新增一個報價項目
            </p>
            <hr class="my-2" />
            <div class="text-end fw-bold">
              估價金額(＝目前加總)：{{ formatPrice(cartTotal) }}
            </div>
          </div>
          <div class="col-12" v-else>
            <span class="text-secondary">報價項目：</span
            >{{ repair.repairItems ?? "—" }}
          </div>

          <!-- 估價金額：非編輯狀態時顯示，編輯狀態的估價金額已經併入上面購物車底部的「目前加總」 -->
          <div
            class="col-md-4"
            v-if="
              !(repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId)
            "
          >
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
              class="btn btn-outline-secondary me-2"
              :disabled="loading"
              @click="handleSaveQuoteDraft"
            >
              儲存
            </button>
            <button
              class="btn btn-primary"
              :disabled="loading"
              @click="openQuoteConfirmModal"
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
                class="form-control no-spinner"
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
            >{{ label(PICKUP_LABELS, repair.pickupType) }}
          </div>
          <div class="col-md-4">
            <span class="text-secondary">付款方式：</span
            >{{ label(PAY_LABELS, repair.repairPay) }}
          </div>

          <!-- 門市付款：尚未取件時用下拉選單當「確認收到現金」的手續，不會真的送到後端 -->
          <div
            class="col-md-4"
            v-if="
              repair.repairStatus === 'AWAITING_PICKUP' &&
              repair.repairPay === 'IN_STORE'
            "
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
          <!-- 線上付款：尚未串接綠界，先讓技師確認後手動標記已付款 -->
          <div
            class="col-md-4"
            v-else-if="
              repair.repairStatus === 'AWAITING_PICKUP' &&
              repair.repairPay === 'ONLINE'
            "
          >
            <span class="text-secondary">付款狀態：</span
            >{{ label(PAY_STATUS_LABELS, repair.repairPayStatus) }}
            <button
              v-if="repair.repairPayStatus === 'PENDING'"
              class="btn btn-outline-primary btn-sm ms-2"
              :disabled="loading"
              @click="handleMarkOnlinePaid"
            >
              標記已收到付款
            </button>
          </div>
          <div class="col-md-4" v-else>
            <span class="text-secondary">付款狀態：</span
            >{{ label(PAY_STATUS_LABELS, repair.repairPayStatus) }}
          </div>

          <!-- 收件資訊：客戶選寄件才有，結案前技師都可以編輯儲存，結案後唯讀 -->
          <template v-if="repair.pickupType === 'COURIER'">
            <div class="col-12" v-if="repair.repairStatus !== 'CLOSED'">
              <label class="form-label text-secondary d-block">收件資訊（可編輯）</label>
              <div class="row g-2">
                <div class="col-md-4">
                  <input
                    v-model="recipientForm.recipientName"
                    type="text"
                    class="form-control"
                    placeholder="收件人姓名"
                  />
                </div>
                <div class="col-md-4">
                  <input
                    v-model="recipientForm.recipientPhone"
                    type="text"
                    class="form-control"
                    placeholder="收件人電話"
                  />
                </div>
                <div class="col-md-4">
                  <input
                    v-model="recipientForm.recipientAddress"
                    type="text"
                    class="form-control"
                    placeholder="收件地址"
                  />
                </div>
              </div>
              <button
                class="btn btn-outline-primary btn-sm mt-2"
                :disabled="loading"
                @click="handleSaveRecipient"
              >
                儲存收件資訊
              </button>
            </div>
            <div class="col-12" v-else>
              <span class="text-secondary">收件資訊：</span
              >{{ repair.recipientName }}（{{ repair.recipientPhone }}）　{{
                repair.recipientAddress
              }}
            </div>
          </template>
        </div>
      </section>

      <!-- 結案按鈕：走到尚未取件狀態才會出現，付款確認完成才能按 -->
      <div v-if="repair.repairStatus === 'AWAITING_PICKUP'" class="mb-3">
        <button
          class="btn btn-primary"
          :disabled="loading || !canFinalClose"
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

    <!-- 送出報價前的確認彈窗：唯讀顯示目前檢測/報價內容，技師確認無誤才真的送出
         (放在v-else-if區塊外面，確保onMounted初始化Modal實例時這個元素已經在畫面上) -->
    <div class="modal fade" ref="quoteConfirmModalRef" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4">
          <div class="modal-header border-0 pb-0">
            <h5 class="modal-title">確認報價內容</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <p class="text-secondary mb-3">
              送出後客戶就會看到這份報價，請再確認一次內容是否正確。
            </p>
            <div class="mb-2">
              <span class="text-secondary">手機序號：</span>{{ quoteForm.serialNumber || "—" }}
            </div>
            <div class="mb-2">
              <span class="text-secondary">報價項目：</span>{{ repairItemsText || "—" }}
            </div>
            <div class="mb-2">
              <span class="text-secondary">估價金額：</span>{{ formatPrice(cartTotal) }}
            </div>
            <div class="mb-2">
              <span class="text-secondary">檢測結果：</span>{{ quoteForm.inspectionResult || "—" }}
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
              編輯
            </button>
            <button
              type="button"
              class="btn btn-primary"
              :disabled="loading"
              @click="confirmSubmitQuote"
            >
              送出報價
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* 估價金額欄位很容易不小心點到數字輸入框旁邊的上下箭頭改到數值，這裡把箭頭藏起來，看起來就是普通文字框 */
.no-spinner::-webkit-outer-spin-button,
.no-spinner::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.no-spinner {
  -moz-appearance: textfield;
}

/* 報價項目購物車：一列一個項目，左邊項目名、右邊價格、最右邊x移除鈕 */
.quote-cart {
  border: 1px solid #dee2e6;
  border-radius: 6px;
  padding: 4px 12px;
}

.quote-cart-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.quote-cart-row:last-child {
  border-bottom: none;
}

.quote-cart-label {
  flex: 1;
}

.quote-cart-price {
  white-space: nowrap;
  color: #444;
}

.quote-cart-new .form-control {
  max-width: 220px;
}
</style>
