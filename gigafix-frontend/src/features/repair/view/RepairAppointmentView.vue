<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useFetchMemberInfoStore } from "@/stores/member";
import { createAppointment, getBookedSlots, getStores } from "../api";
import { REPAIR_ITEMS } from "../priceTable";

// 取得目前登入的會員資料，聯絡姓名/電話會預設帶入這裡的值
const fetchMemberInfoStore = useFetchMemberInfoStore();
const { memberInfo } = storeToRefs(fetchMemberInfoStore);

const stores = ref([]);
const submitting = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
// 選好分店+日期後，去後端查回來的「當天已被預約時段」清單，畫面上要把這些時段設為不可選
const bookedSlots = ref([]);

// 每個必填欄位各自的錯誤訊息，有值就代表這個欄位沒填、要顯示紅字
const fieldErrors = ref({
  contactName: "",
  contactPhone: "",
  storeId: "",
  dropoffType: "",
  repairBrand: "",
  repairModel: "",
  issueDescription: "",
  bookingDate: "",
  timeSlot: "",
});

// 表單欄位，對照後端 AppointmentRequest
const form = ref({
  storeId: "",
  contactName: "",
  contactPhone: "",
  repairBrand: "Apple",
  repairModel: "",
  issueDescription: "",
  bookingDate: "",
  timeSlot: "",
  dropoffType: "",
});

// 今天的日期字串(yyyy-MM-dd)，讓日期欄位不能選過去的日期
const todayStr = new Date().toISOString().slice(0, 10);

// 預約時段只開放 9:00~21:00 整點，共13個按鈕
const timeSlotOptions = [];
for (let hour = 9; hour <= 21; hour++) {
  // padStart(2, "0"):不足2位數，前面要補0
  timeSlotOptions.push(`${String(hour).padStart(2, "0")}:00`);
}

// 是否使用會員資料的姓名/電話：預設打勾，進頁面會自動帶入；
// 使用者清空姓名/電話後想要拿回會員資料，重新勾選這個框就能再帶回來，不用重新整理頁面
const useMemberContact = ref(true);

// 會員資料抓到之後，把聯絡姓名/電話預設帶入表單(使用者還是可以自己改成不同的聯絡資訊)
watch(
  memberInfo,
  (info) => {
    if (info && useMemberContact.value) {
      form.value.contactName = info.realName ?? "";
      form.value.contactPhone = info.phone ?? "";
    }
  },
  { immediate: true },
);

// 使用者直接點擊勾選框時呼叫：勾選=重新帶回會員資料，取消勾選=清空讓使用者自己填
function onToggleMemberContact(checked) {
  useMemberContact.value = checked;
  if (checked) {
    if (memberInfo.value) {
      form.value.contactName = memberInfo.value.realName ?? "";
      form.value.contactPhone = memberInfo.value.phone ?? "";
    }
  } else {
    form.value.contactName = "";
    form.value.contactPhone = "";
  }
}

// 姓名或電話只要被改成跟會員資料不一樣，就代表使用者自己在編輯，勾選框要自動取消勾選
// (這裡只改 useMemberContact 這個值本身，不會去動欄位內容，跟上面 onToggleMemberContact 的「取消勾選要清空」是不同情境)
watch(
  () => [form.value.contactName, form.value.contactPhone],
  ([name, phone]) => {
    if (!useMemberContact.value || !memberInfo.value) {
      return;
    }
    const matchesMember =
      name === (memberInfo.value.realName ?? "") && phone === (memberInfo.value.phone ?? "");
    if (!matchesMember) {
      useMemberContact.value = false;
    }
  },
);

async function fetchStores() {
  try {
    stores.value = await getStores();
  } catch (error) {
    console.error(error);
  }
}

// 查選定的分店+日期，當天已經被預約的時段
async function fetchBookedSlots() {
  if (!form.value.storeId || !form.value.bookingDate) {
    bookedSlots.value = [];
    return;
  }
  try {
    const result = await getBookedSlots(form.value.storeId, form.value.bookingDate);
    // 後端回傳可能帶秒數(例如"09:00:00")，這裡只取前5碼("09:00")跟按鈕格式對齊
    bookedSlots.value = result.map((slot) => slot.slice(0, 5));
  } catch (error) {
    console.error(error);
    bookedSlots.value = [];
  }
}

// 分店或日期改變時，重新查詢已被預約的時段，並清空原本選的時段(避免選到後來才發現被訂走)
watch(
  () => [form.value.storeId, form.value.bookingDate],
  () => {
    form.value.timeSlot = "";
    fetchBookedSlots();
  },
);

// 判斷這個時段是不是已經過去了(只有選「今天」才需要擋，選未來日期不會有這個問題)
function isPastSlot(slot) {
  if (!form.value.bookingDate) {
    return false;
  }
  const slotDateTime = new Date(`${form.value.bookingDate}T${slot}`);
  return slotDateTime <= new Date();
}

// 點時段按鈕：已被預約、或已經過去的時段不能選
function selectTimeSlot(slot) {
  if (submitting.value || bookedSlots.value.includes(slot) || isPastSlot(slot)) {
    return;
  }
  form.value.timeSlot = slot;
}

// 故障狀況描述上方的常見項目下拉選單：只帶項目名稱文字，不帶價格(客戶是自己描述症狀，不是議價)
const selectedIssueItem = ref("");

// 選了項目後把名稱插入描述框，用頓號分隔已有內容；插入後選單重置，可以連續加選多個項目
// textarea本身有maxlength="200"限制使用者手動輸入，但這裡是程式賦值不受HTML maxlength限制，所以自己做字數上限
function insertIssueItem() {
  if (!selectedIssueItem.value) return;
  const current = form.value.issueDescription;
  const combined = current ? `${current}、${selectedIssueItem.value}` : selectedIssueItem.value;
  form.value.issueDescription = combined.slice(0, 200);
  selectedIssueItem.value = "";
}

// 每個必填欄位對應的錯誤訊息，跟「判斷欄位有沒有填」共用同一份，
// 避免 validateForm 跟即時清除紅字的邏輯各寫一套、之後改欄位容易漏改
const requiredFieldMessages = {
  contactName: "請填寫聯絡姓名",
  contactPhone: "請填寫聯絡電話",
  storeId: "請選擇分店",
  dropoffType: "請選擇送修方式",
  repairBrand: "請填寫手機品牌",
  repairModel: "請填寫手機型號",
  issueDescription: "請描述故障狀況",
  bookingDate: "請選擇預約日期",
  timeSlot: "請選擇預約時段",
};

// 判斷某個欄位是否已經填寫：文字欄位要 trim 過(不能只填空白)，其他(下拉選單等)有值就算填了
function isFieldFilled(field) {
  const value = form.value[field];
  return typeof value === "string" ? value.trim() !== "" : Boolean(value);
}

// 逐欄檢查必填，把結果存進 fieldErrors 讓每個欄位旁邊都能各自顯示紅字
// 回傳 true 代表有欄位沒填，回傳 false 代表全部檢查通過
function validateForm() {
  const errors = {};
  for (const field in requiredFieldMessages) {
    errors[field] = isFieldFilled(field) ? "" : requiredFieldMessages[field];
  }
  fieldErrors.value = errors;
  return Object.values(errors).some((msg) => msg !== "");
}

// 欄位內容一有變動，只要該欄位目前有填好，就把它的紅字清掉(還沒填的欄位不會主動冒出新錯誤，只在按送出時才會檢查)
watch(
  form,
  () => {
    for (const field in fieldErrors.value) {
      if (fieldErrors.value[field] && isFieldFilled(field)) {
        fieldErrors.value[field] = "";
      }
    }
  },
  { deep: true },
);

async function handleSubmit() {
  errorMessage.value = "";
  successMessage.value = "";

  const hasError = validateForm();
  if (hasError) {
    return;
  }

  submitting.value = true;
  try {
    await createAppointment(form.value);
    successMessage.value = "預約成功！我們會盡快為您安排。";
    // 送出成功後，聯絡資訊以外的欄位清空，方便使用者再預約下一支手機
    form.value = {
      ...form.value,
      storeId: "",
      repairBrand: "Apple",
      repairModel: "",
      issueDescription: "",
      bookingDate: "",
      timeSlot: "",
      dropoffType: "",
    };
    // 把紅字錯誤歸零，不要殘留上一次的舊訊息
    fieldErrors.value = Object.fromEntries(
      Object.keys(requiredFieldMessages).map((field) => [field, ""]),
    );
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response?.data?.message
      ? error.response.data.message
      : error.response
        ? `預約失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  await fetchMemberInfoStore.fetchMember();
  await fetchStores();
});
</script>

<template>
  <main class="container py-4" style="max-width: 720px">
    <h1 class="fw-bold mb-4">預約維修單</h1>

    <!-- 沒登入不能預約 -->
    <div v-if="!memberInfo" class="alert alert-warning">
      請先登入會員才能預約維修。
    </div>

    <template v-else>
      <div v-if="errorMessage" class="alert alert-danger alert-dismissible fade show" role="alert">
        {{ errorMessage }}
        <button type="button" class="btn-close" @click="errorMessage = ''"></button>
      </div>
      <div v-if="successMessage" class="alert alert-success alert-dismissible fade show" role="alert">
        {{ successMessage }}
        <button type="button" class="btn-close" @click="successMessage = ''"></button>
      </div>

      <form class="card card-body" @submit.prevent="handleSubmit">
        <p class="text-danger small text-end mb-2">*為必填</p>

        <h2 class="h5 mb-3">聯絡資訊</h2>
        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label">聯絡姓名<span class="text-danger">*</span></label>
            <input
              v-model.trim="form.contactName"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.contactName }"
              :disabled="submitting"
            />
            <p v-if="fieldErrors.contactName" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.contactName }}
            </p>
          </div>
          <div class="col-md-6">
            <label class="form-label">聯絡電話<span class="text-danger">*</span></label>
            <input
              v-model.trim="form.contactPhone"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.contactPhone }"
              placeholder="預設帶入會員電話，也可以改成其他電話"
              :disabled="submitting"
            />
            <p v-if="fieldErrors.contactPhone" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.contactPhone }}
            </p>
          </div>
        </div>

        <div class="form-check mb-3">
          <input
            id="useMemberContact"
            :checked="useMemberContact"
            type="checkbox"
            class="form-check-input"
            :disabled="submitting"
            @change="onToggleMemberContact($event.target.checked)"
          />
          <label class="form-check-label" for="useMemberContact">
            使用會員資料的姓名/電話(自行修改姓名或電話會自動取消勾選；取消勾選會清空欄位，重新勾選可帶回會員資料)
          </label>
        </div>

        <h2 class="h5 mb-3">維修資訊</h2>
        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label">分店<span class="text-danger">*</span></label>
            <select
              v-model="form.storeId"
              class="form-select"
              :class="{ 'is-invalid': fieldErrors.storeId }"
              :disabled="submitting"
            >
              <option value="">請選擇分店</option>
              <option v-for="s in stores" :key="s.id" :value="s.id">
                {{ s.name }}
              </option>
            </select>
            <p v-if="fieldErrors.storeId" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.storeId }}
            </p>
          </div>
          <div class="col-md-6">
            <label class="form-label">送修方式<span class="text-danger">*</span></label>
            <select
              v-model="form.dropoffType"
              class="form-select"
              :class="{ 'is-invalid': fieldErrors.dropoffType }"
              :disabled="submitting"
            >
              <option value="">請選擇送修方式</option>
              <option value="IN_STORE">親臨門市</option>
              <option value="SHIPPING">寄送門市</option>
            </select>
            <p v-if="fieldErrors.dropoffType" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.dropoffType }}
            </p>
          </div>
          <div class="col-md-6">
            <label class="form-label">手機品牌<span class="text-danger">*</span></label>
            <input
              v-model.trim="form.repairBrand"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.repairBrand }"
              placeholder="Apple"
              :disabled="submitting"
            />
            <p v-if="fieldErrors.repairBrand" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.repairBrand }}
            </p>
          </div>
          <div class="col-md-6">
            <label class="form-label">手機型號<span class="text-danger">*</span></label>
            <input
              v-model.trim="form.repairModel"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.repairModel }"
              placeholder="例如：iPhone 13"
              :disabled="submitting"
            />
            <p v-if="fieldErrors.repairModel" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.repairModel }}
            </p>
          </div>
          <div class="col-12">
            <label class="form-label">故障狀況描述<span class="text-danger">*</span></label>
            <select
              v-model="selectedIssueItem"
              class="form-select mb-2"
              :disabled="submitting"
              @change="insertIssueItem"
            >
              <option value="">選擇常見故障項目，快速加入描述(可複選)</option>
              <option v-for="item in REPAIR_ITEMS" :key="item.key" :value="item.label">
                {{ item.label }}
              </option>
            </select>
            <textarea
              v-model.trim="form.issueDescription"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.issueDescription }"
              rows="3"
              maxlength="200"
              :disabled="submitting"
            ></textarea>
            <p v-if="fieldErrors.issueDescription" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.issueDescription }}
            </p>
          </div>
          <div class="col-md-6">
            <label class="form-label">預約日期<span class="text-danger">*</span></label>
            <input
              v-model="form.bookingDate"
              type="date"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.bookingDate }"
              :min="todayStr"
              :disabled="submitting"
            />
            <p v-if="fieldErrors.bookingDate" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.bookingDate }}
            </p>
          </div>
          <div class="col-12">
            <label class="form-label">預約時段<span class="text-danger">*</span></label>
            <p v-if="!form.storeId || !form.bookingDate" class="text-muted small mb-0">
              請先選擇分店與預約日期，才能選擇時段
            </p>
            <div v-else class="time-slot-grid">
              <button
                v-for="slot in timeSlotOptions"
                :key="slot"
                type="button"
                class="time-slot-btn"
                :class="{
                  selected: form.timeSlot === slot,
                  booked: bookedSlots.includes(slot) || isPastSlot(slot),
                }"
                :disabled="submitting || bookedSlots.includes(slot) || isPastSlot(slot)"
                @click="selectTimeSlot(slot)"
              >
                {{ slot }}
              </button>
            </div>
            <p v-if="fieldErrors.timeSlot" class="text-danger small mb-0 mt-1">
              {{ fieldErrors.timeSlot }}
            </p>
          </div>
        </div>

        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? "送出中..." : "送出預約" }}
        </button>
      </form>
    </template>
  </main>
</template>

<style scoped>
.time-slot-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.time-slot-btn {
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  background-color: #eaf2fb;
  color: #1d324b;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.time-slot-btn:hover:not(:disabled) {
  background-color: #cfe3f7;
}

.time-slot-btn.selected {
  background-color: #2b77c5;
  color: #ffffff;
}

.time-slot-btn.booked {
  background-color: #f0f0f0;
  color: #b0b0b0;
  text-decoration: line-through;
  cursor: not-allowed;
}
</style>
