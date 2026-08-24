<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  assignRepair,
  closeRejectedRepair,
  closeRepair,
  completeRepair,
  getRepair,
  markNotified,
  markUndelivered,
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

// 所有需要技師身分的操作都共用這個欄位（後端每支API都要驗證是不是本人負責的維修單）
const technicianId = ref("");

// ===== 認領 =====
async function handleAssign() {
  if (!technicianId.value) {
    alert("請先填寫技師id才能認領");
    return;
  }
  await runAction(() => assignRepair(repair.value.id, technicianId.value));
}

// ===== 報價表單（待估價、已認領時使用） =====
const quoteForm = ref({
  serialNumber: "",
  inspectionResult: "",
  repairItems: "",
  estimatedCost: null,
});
// 記住從後端載入當下的內容，用來判斷有沒有真的改過
const quoteSnapshot = ref({});

function loadQuoteForm(r) {
  quoteForm.value = {
    serialNumber: r.serialNumber ?? "",
    inspectionResult: r.inspectionResult ?? "",
    repairItems: r.repairItems ?? "",
    estimatedCost: r.estimatedCost ?? null,
  };
  quoteSnapshot.value = { ...quoteForm.value };
}

const quoteChanged = computed(
  () => JSON.stringify(quoteForm.value) !== JSON.stringify(quoteSnapshot.value),
);

async function handleSaveQuote() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  if (!quoteChanged.value) {
    alert("內容沒有變更，不用儲存");
    return;
  }
  await runAction(() =>
    updateQuote(repair.value.id, {
      technicianId: technicianId.value,
      ...quoteForm.value,
    }),
  );
}

async function handleSubmitQuote() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  if (
    !quoteForm.value.serialNumber ||
    !quoteForm.value.inspectionResult ||
    !quoteForm.value.repairItems ||
    quoteForm.value.estimatedCost === null ||
    quoteForm.value.estimatedCost === ""
  ) {
    alert("序號、檢測結果、維修項目、估價金額都要填寫完才能送出報價");
    return;
  }
  if (quoteChanged.value) {
    alert("有欄位還沒儲存，請先按「儲存報價」再送出");
    return;
  }
  await runAction(() => submitQuote(repair.value.id, technicianId.value));
}

// ===== 未送修（僅限待估價、已認領時） =====
async function handleUndelivered() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  if (!window.confirm("確定要標記這張維修單為「未送修」嗎？")) return;
  await runAction(() => markUndelivered(repair.value.id, technicianId.value));
}

// ===== 補充檢測備註（維修中、報價後不維修都可用） =====
const noteForm = ref({ inspectionResult: "" });
const noteSnapshot = ref({ inspectionResult: "" });

function loadNoteForm(r) {
  noteForm.value = { inspectionResult: r.inspectionResult ?? "" };
  noteSnapshot.value = { ...noteForm.value };
}

const noteChanged = computed(
  () => noteForm.value.inspectionResult !== noteSnapshot.value.inspectionResult,
);

async function handleSaveNote() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  if (!noteForm.value.inspectionResult) {
    alert("請填寫要更新的內容");
    return;
  }
  if (!noteChanged.value) {
    alert("內容沒有變更，不用儲存");
    return;
  }
  await runAction(() =>
    updateInspectionResult(repair.value.id, {
      technicianId: technicianId.value,
      inspectionResult: noteForm.value.inspectionResult,
    }),
  );
}

// ===== 維修完成 =====
const completeForm = ref({ finalCost: null, adjustmentNote: "" });

async function handleComplete() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  const costChanged =
    completeForm.value.finalCost !== null &&
    completeForm.value.finalCost !== "" &&
    Number(completeForm.value.finalCost) !== repair.value.estimatedCost;
  if (costChanged && !completeForm.value.adjustmentNote) {
    alert("最終金額跟原本估價不同，請填寫調整原因");
    return;
  }
  await runAction(() =>
    completeRepair(repair.value.id, {
      technicianId: technicianId.value,
      finalCost:
        completeForm.value.finalCost === "" ? null : completeForm.value.finalCost,
      adjustmentNote: completeForm.value.adjustmentNote || null,
    }),
  );
}

// ===== 已通知客戶取件 =====
async function handleNotify() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  await runAction(() => markNotified(repair.value.id, technicianId.value));
}

// ===== 門市取貨付款、結案 =====
async function handleClose() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  if (!window.confirm("確定客戶已經到店取貨付款了嗎？")) return;
  await runAction(() => closeRepair(repair.value.id, technicianId.value));
}

// ===== 報價不維修、結案 =====
const rejectedFee = ref(null);

async function handleCloseRejected() {
  if (!technicianId.value) {
    alert("請先填寫技師id");
    return;
  }
  if (!window.confirm("確定客戶已經到店領回手機了嗎？")) return;
  await runAction(() =>
    closeRejectedRepair(
      repair.value.id,
      technicianId.value,
      rejectedFee.value === "" ? null : rejectedFee.value,
    ),
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
    loadNoteForm(data);
    completeForm.value = { finalCost: data.finalCost ?? null, adjustmentNote: "" };
    rejectedFee.value = null;
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
    <button class="btn btn-link px-0 mb-3" @click="router.back()">← 返回列表</button>

    <div v-if="loading && !repair" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="repair">
      <h1 class="fw-bold mb-4">維修單 #{{ repair.id }}</h1>

      <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
      <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

      <!-- 基本資料，唯讀 -->
      <section class="card mb-4">
        <div class="card-header fw-bold">基本資料</div>
        <div class="card-body row g-3">
          <div class="col-md-4"><span class="text-secondary">客戶：</span>{{ repair.memberName }}（id:{{ repair.memberId }}）</div>
          <div class="col-md-4"><span class="text-secondary">技師：</span>{{ repair.technicianName ?? "尚未認領" }}<span v-if="repair.technicianId">（id:{{ repair.technicianId }}）</span></div>
          <div class="col-md-4"><span class="text-secondary">分店：</span>{{ repair.storeName }}</div>
          <div class="col-md-4"><span class="text-secondary">機型：</span>{{ repair.repairBrand }} {{ repair.repairModel }}</div>
          <div class="col-md-4"><span class="text-secondary">送修方式：</span>{{ repair.dropoffType === "SHIPPING" ? "寄送門市" : "親臨門市" }}</div>
          <div class="col-md-4"><span class="text-secondary">狀態：</span>{{ repair.repairStatus }}</div>
          <div class="col-12"><span class="text-secondary">問題描述：</span>{{ repair.issueDescription }}</div>
        </div>
      </section>

      <!-- 操作用的技師id，下面所有動作共用 -->
      <section
        v-if="!['CLOSED', 'CANCELLED', 'NOT_DROPPED_OFF', 'QUOTED'].includes(repair.repairStatus)"
        class="card mb-4"
      >
        <div class="card-body">
          <label class="form-label">操作技師id（下面動作都要驗證是不是本人負責的維修單）</label>
          <input v-model="technicianId" type="number" class="form-control" style="max-width: 200px" />
        </div>
      </section>

      <!-- 待估價：還沒認領 -->
      <section v-if="repair.repairStatus === 'PENDING_QUOTE' && !repair.technicianId" class="card mb-4">
        <div class="card-header fw-bold">認領維修單</div>
        <div class="card-body">
          <button class="btn btn-primary" :disabled="loading" @click="handleAssign">認領</button>
        </div>
      </section>

      <!-- 待估價：已認領，填報價 -->
      <section v-if="repair.repairStatus === 'PENDING_QUOTE' && repair.technicianId" class="card mb-4">
        <div class="card-header fw-bold">檢測報價</div>
        <div class="card-body">
          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label">序號</label>
              <input v-model="quoteForm.serialNumber" type="text" class="form-control" />
            </div>
            <div class="col-md-6">
              <label class="form-label">估價金額</label>
              <input v-model.number="quoteForm.estimatedCost" type="number" class="form-control" />
            </div>
            <div class="col-12">
              <label class="form-label">檢測結果</label>
              <textarea v-model="quoteForm.inspectionResult" class="form-control" rows="2"></textarea>
            </div>
            <div class="col-12">
              <label class="form-label">維修項目</label>
              <textarea v-model="quoteForm.repairItems" class="form-control" rows="2"></textarea>
            </div>
          </div>
          <div class="mt-3 d-flex gap-2">
            <button class="btn btn-outline-primary" :disabled="loading" @click="handleSaveQuote">儲存報價</button>
            <button class="btn btn-primary" :disabled="loading" @click="handleSubmitQuote">正式送出報價</button>
            <button class="btn btn-outline-danger ms-auto" :disabled="loading" @click="handleUndelivered">標記未送修</button>
          </div>
        </div>
      </section>

      <!-- 已報價：等客戶回應，後台不能操作 -->
      <section v-if="repair.repairStatus === 'QUOTED'" class="card mb-4">
        <div class="card-body text-secondary">
          報價已送出，等待客戶在前台回應（同意／拒絕）。
        </div>
      </section>

      <!-- 維修中：補充備註 + 完成維修 -->
      <template v-if="repair.repairStatus === 'IN_REPAIR'">
        <section class="card mb-4">
          <div class="card-header fw-bold">補充檢測備註</div>
          <div class="card-body">
            <textarea v-model="noteForm.inspectionResult" class="form-control" rows="2"></textarea>
            <button class="btn btn-outline-primary mt-3" :disabled="loading" @click="handleSaveNote">儲存備註</button>
          </div>
        </section>
        <section class="card mb-4">
          <div class="card-header fw-bold">維修完成</div>
          <div class="card-body row g-3">
            <div class="col-md-6">
              <label class="form-label">最終金額（不填就沿用估價金額）</label>
              <input v-model.number="completeForm.finalCost" type="number" class="form-control" />
            </div>
            <div class="col-12">
              <label class="form-label">調整原因（金額跟估價不同時才需要填）</label>
              <textarea v-model="completeForm.adjustmentNote" class="form-control" rows="2"></textarea>
            </div>
            <div class="col-12">
              <button class="btn btn-primary" :disabled="loading" @click="handleComplete">標記維修完成</button>
            </div>
          </div>
        </section>
      </template>

      <!-- 報價後不維修：補充備註 + 結案 -->
      <template v-if="repair.repairStatus === 'QUOTE_REJECTED'">
        <section class="card mb-4">
          <div class="card-header fw-bold">補充備註（例如收檢測費原因）</div>
          <div class="card-body">
            <textarea v-model="noteForm.inspectionResult" class="form-control" rows="2"></textarea>
            <button class="btn btn-outline-primary mt-3" :disabled="loading" @click="handleSaveNote">儲存備註</button>
          </div>
        </section>
        <section class="card mb-4">
          <div class="card-header fw-bold">客戶到店領回、結案</div>
          <div class="card-body">
            <label class="form-label">檢測費（不填就是0元）</label>
            <input v-model.number="rejectedFee" type="number" class="form-control mb-3" style="max-width: 200px" />
            <button class="btn btn-primary" :disabled="loading" @click="handleCloseRejected">結案</button>
          </div>
        </section>
      </template>

      <!-- 維修完成：通知客戶 -->
      <section v-if="repair.repairStatus === 'REPAIR_COMPLETED'" class="card mb-4">
        <div class="card-header fw-bold">通知客戶取件</div>
        <div class="card-body">
          <button class="btn btn-primary" :disabled="loading" @click="handleNotify">已通知客戶</button>
        </div>
      </section>

      <!-- 尚未取件：結案 -->
      <section v-if="repair.repairStatus === 'AWAITING_PICKUP'" class="card mb-4">
        <div class="card-header fw-bold">門市取貨付款、結案</div>
        <div class="card-body">
          <button class="btn btn-primary" :disabled="loading" @click="handleClose">結案</button>
        </div>
      </section>

      <!-- 已結案／已取消／未送檢：純顯示 -->
      <section
        v-if="['CLOSED', 'CANCELLED', 'NOT_DROPPED_OFF'].includes(repair.repairStatus)"
        class="card mb-4"
      >
        <div class="card-body row g-3">
          <div class="col-md-4"><span class="text-secondary">最終金額：</span>{{ repair.finalCost }}</div>
          <div class="col-md-4"><span class="text-secondary">付款方式：</span>{{ repair.repairPay ?? "—" }}</div>
          <div class="col-md-4"><span class="text-secondary">取件方式：</span>{{ repair.pickupType ?? "—" }}</div>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped></style>
