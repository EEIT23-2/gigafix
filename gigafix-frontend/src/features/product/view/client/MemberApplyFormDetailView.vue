<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getMemberRecycleApplication } from "../../api";

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
}
</style>
