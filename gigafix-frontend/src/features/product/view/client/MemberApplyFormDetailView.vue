<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  confirmMemberRecycleAgreement,
  getMemberRecycleApplication,
} from "../../api";

const props = defineProps({
  applyId: {
    type: [String, Number],
    required: true,
  },
});

const router = useRouter();
const application = ref(null);
const loading = ref(false);
const errorMessage = ref("");
const agreementOtp = ref("");
const signatureCanvas = ref(null);
const hasSignature = ref(false);
const confirmingAgreement = ref(false);
const agreementError = ref("");
const agreementSuccess = ref("");

let drawingSignature = false;

// 後台完成估價並寄出 OTP 後，資料庫狀態仍維持 INSPECTING，直到會員簽署成功。
const canSignAgreement = computed(
  () =>
    application.value?.recycleStatus === "INSPECTING" &&
    application.value?.estimatedPrice != null,
);

const categoryLabels = {
  IPHONE: "iPhone",
  IPAD: "iPad",
  WATCH: "Apple Watch",
};

const statusLabels = {
  CANCELLED: "已取消",
  APPLIED: "已預約交件",
  INSPECTING: "現場檢測評估中",
  WAITING_FOR_AGREEMENT: "待簽署同意",
  WIPING: "資料清除中",
  COMPLETED: "完成回收",
};

function formatPrice(price) {
  if (price == null) return "尚未估價";
  return `NT$ ${Number(price).toLocaleString("zh-TW")}`;
}

function formatDateTime(dateTime) {
  if (!dateTime) return "—";
  return String(dateTime).replace("T", " ");
}

async function fetchApplication() {
  const applyId = Number(props.applyId);

  if (!Number.isInteger(applyId) || applyId <= 0) {
    errorMessage.value = "回收申請編號格式不正確。";
    return;
  }

  loading.value = true;
  errorMessage.value = "";

  try {
    application.value = await getMemberRecycleApplication(applyId);
    if (canSignAgreement.value) {
      // 簽名板由 v-if 建立，需等待 DOM 更新後再設定畫筆。
      await nextTick();
      initializeSignaturePad();
    }
  } catch (error) {
    console.error(error);
    application.value = null;
    errorMessage.value =
      error?.response?.status === 404
        ? "找不到這筆回收申請，或這筆申請不屬於目前登入的會員。"
        : error?.response?.status === 401
          ? "登入狀態已失效，請重新登入。"
          : "目前無法載入回收申請明細，請稍後再試。";
  } finally {
    loading.value = false;
  }
}

function initializeSignaturePad() {
  const canvas = signatureCanvas.value;
  if (!canvas) return;

  const context = canvas.getContext("2d");
  context.clearRect(0, 0, canvas.width, canvas.height);
  context.lineWidth = 3;
  context.lineCap = "round";
  context.lineJoin = "round";
  context.strokeStyle = "#1d324b";
}

function signaturePoint(event) {
  const canvas = signatureCanvas.value;
  const rect = canvas.getBoundingClientRect();
  return {
    x: (event.clientX - rect.left) * (canvas.width / rect.width),
    y: (event.clientY - rect.top) * (canvas.height / rect.height),
  };
}

// Pointer Events 可同時支援桌機滑鼠、觸控筆與手機觸控簽名。
function startSignature(event) {
  const canvas = signatureCanvas.value;
  const context = canvas.getContext("2d");
  const point = signaturePoint(event);

  drawingSignature = true;
  canvas.setPointerCapture(event.pointerId);
  context.beginPath();
  context.moveTo(point.x, point.y);
}

function drawSignature(event) {
  if (!drawingSignature) return;

  const context = signatureCanvas.value.getContext("2d");
  const point = signaturePoint(event);
  context.lineTo(point.x, point.y);
  context.stroke();
  hasSignature.value = true;
}

function stopSignature() {
  drawingSignature = false;
}

function clearSignature() {
  initializeSignaturePad();
  hasSignature.value = false;
  agreementError.value = "";
}

async function confirmAgreement() {
  if (!/^\d{6}$/.test(agreementOtp.value)) {
    agreementError.value = "請輸入 Email 中的 6 位數驗證碼。";
    return;
  }
  if (!hasSignature.value) {
    agreementError.value = "請先在簽名板完成電子簽名。";
    return;
  }

  confirmingAgreement.value = true;
  agreementError.value = "";
  agreementSuccess.value = "";

  try {
    // Canvas 轉成 PNG 後連同 OTP 送到會員專用 API，由後端核對回收單歸屬。
    const signatureDataUrl = signatureCanvas.value.toDataURL("image/png");
    application.value = await confirmMemberRecycleAgreement(
      application.value.applyId,
      agreementOtp.value,
      signatureDataUrl,
    );
    agreementOtp.value = "";
    hasSignature.value = false;
    agreementSuccess.value = "驗證與電子簽名已完成，回收單已進入待簽署同意狀態。";
  } catch (error) {
    console.error(error);
    agreementError.value =
      error?.response?.status === 400
        ? "驗證碼錯誤或已逾期，請確認後重試；連續錯誤 5 次需請門市重新寄送。"
        : error?.response?.status === 404
          ? "找不到這筆回收申請，或這筆申請不屬於目前登入的會員。"
          : error?.response?.status === 409
            ? "目前回收單狀態已變更，請重新載入。"
            : "簽署確認失敗，請稍後再試。";
  } finally {
    confirmingAgreement.value = false;
  }
}

function goBack() {
  router.push({ name: "member-recycle-applications" });
}

onMounted(fetchApplication);
</script>

<template>
  <section class="member-application-detail">
    <button class="back-button" type="button" @click="goBack">
      <i class="bi bi-arrow-left" aria-hidden="true"></i>
      返回回收手機紀錄
    </button>

    <div v-if="errorMessage" class="state-card error-state" role="alert">
      <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
      <p>{{ errorMessage }}</p>
      <div class="state-actions">
        <button type="button" @click="goBack">返回列表</button>
        <button type="button" @click="fetchApplication">重新載入</button>
      </div>
    </div>

    <div v-else-if="loading" class="state-card" aria-live="polite">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">載入中</span>
      </div>
      <p>正在載入回收申請明細...</p>
    </div>

    <article v-else-if="application" class="detail-card">
      <header class="detail-header">
        <div>
          <p class="eyebrow">RECYCLE APPLICATION</p>
          <h1>回收申請明細</h1>
          <p class="application-number">申請編號 #{{ application.applyId }}</p>
        </div>
        <span
          class="status-badge"
          :class="`status-${application.recycleStatus?.toLowerCase()}`"
        >
          {{
            statusLabels[application.recycleStatus] ??
            application.recycleStatus ??
            "狀態未設定"
          }}
        </span>
      </header>

      <div class="detail-body">
        <div class="image-panel">
          <img
            v-if="application.imageUrl"
            :src="application.imageUrl"
            :alt="application.productName"
          />
          <div v-else class="image-placeholder">
            <i class="bi bi-phone" aria-hidden="true"></i>
            <span>沒有商品圖片</span>
          </div>
        </div>

        <dl class="detail-list">
          <div>
            <dt>裝置名稱</dt>
            <dd>{{ application.productName || "—" }}</dd>
          </div>
          <div>
            <dt>裝置類型</dt>
            <dd>
              {{
                categoryLabels[application.category] ??
                application.category ??
                "—"
              }}
            </dd>
          </div>
          <div>
            <dt>外觀狀況</dt>
            <dd>{{ application.appearance || "—" }}</dd>
          </div>
          <div>
            <dt>其他描述</dt>
            <dd>{{ application.description || "—" }}</dd>
          </div>
          <div>
            <dt>預估價格</dt>
            <dd class="price">{{ formatPrice(application.estimatedPrice) }}</dd>
          </div>
          <div>
            <dt>回收門市</dt>
            <dd>{{ application.storeName || "未指定" }}</dd>
          </div>
          <div>
            <dt>申請時間</dt>
            <dd>{{ formatDateTime(application.createdTime) }}</dd>
          </div>
          <div>
            <dt>最後更新時間</dt>
            <dd>{{ formatDateTime(application.lastModifiedTime) }}</dd>
          </div>
        </dl>
      </div>

      <!-- 會員收到門市寄出的 OTP 後，在自己的回收單明細完成驗證與電子簽名。 -->
      <section
        v-if="canSignAgreement"
        class="agreement-panel"
        aria-labelledby="member-agreement-title"
      >
        <div class="agreement-heading">
          <div>
            <p class="agreement-step">估價確認</p>
            <h2 id="member-agreement-title">客戶估價同意確認</h2>
          </div>
          <span>驗證碼有效期限為 5 分鐘</span>
        </div>

        <p class="agreement-description">
          請輸入 Email 中的 6 位數驗證碼，確認上方估價後完成電子簽名。
        </p>

        <div class="agreement-content">
          <div class="otp-panel">
            <label for="member-agreement-otp">6 位數驗證碼</label>
            <input
              id="member-agreement-otp"
              v-model.trim="agreementOtp"
              type="text"
              inputmode="numeric"
              maxlength="6"
              autocomplete="one-time-code"
              placeholder="000000"
              :disabled="confirmingAgreement"
            />
            <small>請輸入門市寄到會員信箱的驗證碼。</small>
          </div>

          <div class="signature-panel">
            <div class="signature-heading">
              <label>電子簽名</label>
              <button
                type="button"
                :disabled="confirmingAgreement"
                @click="clearSignature"
              >
                清除簽名
              </button>
            </div>
            <canvas
              ref="signatureCanvas"
              class="signature-canvas"
              width="720"
              height="220"
              aria-label="電子簽名板"
              @pointerdown.prevent="startSignature"
              @pointermove.prevent="drawSignature"
              @pointerup="stopSignature"
              @pointercancel="stopSignature"
              @pointerleave="stopSignature"
            ></canvas>
          </div>
        </div>

        <p v-if="agreementError" class="agreement-message error-message" role="alert">
          {{ agreementError }}
        </p>

        <div class="agreement-actions">
          <button
            type="button"
            class="confirm-agreement-button"
            :disabled="confirmingAgreement"
            @click="confirmAgreement"
          >
            <span
              v-if="confirmingAgreement"
              class="spinner-border spinner-border-sm"
              aria-hidden="true"
            ></span>
            {{ confirmingAgreement ? "驗證中..." : "驗證並完成簽署" }}
          </button>
        </div>
      </section>

      <p
        v-if="agreementSuccess"
        class="agreement-message success-message"
        role="status"
      >
        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
        {{ agreementSuccess }}
      </p>
    </article>
  </section>
</template>

<style scoped>
.member-application-detail {
  --blue: #1769aa;
  --ink: #1d324b;
  --muted: #6b7785;
  width: min(100%, 1060px);
  margin: 0 auto;
  color: var(--ink);
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 0;
  border: 0;
  color: var(--blue);
  background: transparent;
  font-weight: 700;
}

.detail-card {
  overflow: hidden;
  border: 1px solid #e1e8ef;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 16px 42px rgb(37 64 91 / 10%);
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 30px 34px;
  border-bottom: 1px solid #e8edf2;
  background: linear-gradient(135deg, #f7fbff 0%, #eef5fb 100%);
}

.eyebrow {
  margin: 0 0 7px;
  color: var(--blue);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.15em;
}

h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 44px);
  font-weight: 750;
  letter-spacing: -0.03em;
}

.application-number {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.status-badge {
  flex: 0 0 auto;
  padding: 9px 14px;
  border-radius: 999px;
  color: #23527c;
  background: #dfeffc;
  font-size: 13px;
  font-weight: 750;
}

.status-cancelled {
  color: #646b73;
  background: #e5e8eb;
}

.status-inspecting,
.status-waiting_for_agreement {
  color: #8a5b00;
  background: #ffebba;
}

.status-wiping {
  color: #6651a3;
  background: #e9e0ff;
}

.status-completed {
  color: #24704a;
  background: #d9f2e4;
}

.detail-body {
  display: grid;
  grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.5fr);
  gap: 36px;
  padding: 34px;
}

.image-panel {
  min-height: 320px;
  overflow: hidden;
  border: 1px solid #e2e8ee;
  border-radius: 16px;
  background: #f5f8fa;
}

.image-panel img {
  width: 100%;
  height: 100%;
  max-height: 460px;
  object-fit: contain;
}

.image-placeholder {
  display: flex;
  height: 100%;
  min-height: 320px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: #8ca3b8;
}

.image-placeholder i {
  font-size: 54px;
}

.detail-list {
  margin: 0;
  border-top: 1px solid #e2e8ee;
}

.detail-list > div {
  display: grid;
  grid-template-columns: 125px minmax(0, 1fr);
  gap: 20px;
  padding: 16px 0;
  border-bottom: 1px solid #e2e8ee;
}

.detail-list dt {
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
}

.detail-list dd {
  margin: 0;
  overflow-wrap: anywhere;
  font-weight: 650;
  white-space: pre-wrap;
}

.detail-list .price {
  color: var(--blue);
  font-size: 18px;
}

.agreement-panel {
  padding: 32px 34px 36px;
  border-top: 1px solid #e1e8ef;
  background: #f9fbfd;
}

.agreement-heading,
.signature-heading,
.agreement-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.agreement-heading h2 {
  margin: 0;
  font-size: 25px;
  font-weight: 800;
}

.agreement-heading > span,
.agreement-description,
.otp-panel small {
  color: var(--muted);
}

.agreement-step {
  margin: 0 0 5px;
  color: var(--blue);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.agreement-description {
  margin: 12px 0 24px;
  font-size: 17px;
}

.agreement-content {
  display: grid;
  grid-template-columns: minmax(230px, 0.55fr) minmax(0, 1.45fr);
  gap: 32px;
}

.otp-panel,
.signature-panel {
  min-width: 0;
}

.otp-panel label,
.signature-heading label {
  display: block;
  margin-bottom: 10px;
  font-size: 17px;
  font-weight: 800;
}

.otp-panel input {
  width: 100%;
  max-width: 330px;
  padding: 14px 16px;
  border: 1px solid #cfd9e3;
  border-radius: 12px;
  color: var(--ink);
  background: #fff;
  font-size: 25px;
  font-weight: 800;
  letter-spacing: 0.38em;
  text-align: center;
}

.otp-panel small {
  display: block;
  margin-top: 9px;
}

.signature-heading label {
  margin-bottom: 0;
}

.signature-heading button {
  padding: 7px 13px;
  border: 1px solid #aab8c5;
  border-radius: 8px;
  color: #526579;
  background: #fff;
  font-weight: 700;
}

.signature-canvas {
  display: block;
  width: 100%;
  height: 220px;
  margin-top: 10px;
  border: 2px dashed #9aaabd;
  border-radius: 12px;
  background: #fff;
  cursor: crosshair;
  touch-action: none;
}

.agreement-actions {
  justify-content: flex-end;
  margin-top: 22px;
}

.confirm-agreement-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 13px 22px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  background: var(--blue);
  font-size: 16px;
  font-weight: 800;
}

.confirm-agreement-button:disabled,
.signature-heading button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.agreement-message {
  margin: 18px 0 0;
  padding: 13px 16px;
  border-radius: 10px;
  font-weight: 700;
}

.error-message {
  color: #8f2626;
  background: #fbe3e3;
}

.success-message {
  margin: 24px 34px 32px;
  color: #216a45;
  background: #dff3e7;
}

.state-card {
  display: grid;
  min-height: 300px;
  place-items: center;
  align-content: center;
  gap: 14px;
  padding: 34px;
  border: 1px solid #e1e8ef;
  border-radius: 20px;
  text-align: center;
  background: #fff;
}

.state-card p {
  margin: 0;
}

.state-card > i {
  font-size: 42px;
}

.error-state {
  color: #9d2e2e;
}

.state-actions {
  display: flex;
  gap: 10px;
}

.state-actions button {
  padding: 9px 16px;
  border: 1px solid #bed0e0;
  border-radius: 9px;
  color: var(--blue);
  background: #fff;
  font-weight: 700;
}

@media (max-width: 800px) {
  .detail-body {
    grid-template-columns: 1fr;
  }

  .image-panel,
  .image-placeholder {
    min-height: 240px;
  }

  .agreement-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .detail-header {
    align-items: flex-start;
    flex-direction: column;
    padding: 24px;
  }

  .detail-body {
    gap: 24px;
    padding: 24px;
  }

  .detail-list > div {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .agreement-panel {
    padding: 24px;
  }

  .agreement-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .success-message {
    margin: 20px 24px 24px;
  }
}
</style>
