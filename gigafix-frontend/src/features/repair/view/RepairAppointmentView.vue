<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useFetchMemberInfoStore } from "@/stores/member";
import { createAppointment, getStores } from "../api";

// 取得目前登入的會員資料，聯絡姓名/電話會預設帶入這裡的值
const fetchMemberInfoStore = useFetchMemberInfoStore();
const { memberInfo } = storeToRefs(fetchMemberInfoStore);

const stores = ref([]);
const submitting = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

// 表單欄位，對照後端 AppointmentRequest
const form = ref({
  storeId: "",
  contactName: "",
  contactPhone: "",
  repairBrand: "",
  repairModel: "",
  issueDescription: "",
  bookingDate: "",
  timeSlot: "",
  dropoffType: "",
});

// 今天的日期字串(yyyy-MM-dd)，讓日期欄位不能選過去的日期
const todayStr = new Date().toISOString().slice(0, 10);

// 會員資料抓到之後，把聯絡姓名/電話預設帶入表單(使用者還是可以自己改成不同的聯絡資訊)
watch(
  memberInfo,
  (info) => {
    if (info) {
      form.value.contactName = info.realName ?? "";
      form.value.contactPhone = info.phone ?? "";
    }
  },
  { immediate: true },
);

async function fetchStores() {
  try {
    stores.value = await getStores();
  } catch (error) {
    console.error(error);
  }
}

function validateForm() {
  if (
    !form.value.storeId ||
    !form.value.contactName.trim() ||
    !form.value.contactPhone.trim() ||
    !form.value.repairBrand.trim() ||
    !form.value.repairModel.trim() ||
    !form.value.issueDescription.trim() ||
    !form.value.bookingDate ||
    !form.value.timeSlot ||
    !form.value.dropoffType
  ) {
    return "請把表單欄位都填寫完整";
  }
  return "";
}

async function handleSubmit() {
  errorMessage.value = "";
  successMessage.value = "";

  const validateMsg = validateForm();
  if (validateMsg) {
    errorMessage.value = validateMsg;
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
      repairBrand: "",
      repairModel: "",
      issueDescription: "",
      bookingDate: "",
      timeSlot: "",
      dropoffType: "",
    };
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
      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>
      <div v-if="successMessage" class="alert alert-success">
        {{ successMessage }}
      </div>

      <form class="card card-body" @submit.prevent="handleSubmit">
        <h2 class="h5 mb-3">聯絡資訊</h2>
        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label">聯絡姓名</label>
            <input
              v-model.trim="form.contactName"
              type="text"
              class="form-control"
              :disabled="submitting"
            />
          </div>
          <div class="col-md-6">
            <label class="form-label">聯絡電話</label>
            <input
              v-model.trim="form.contactPhone"
              type="text"
              class="form-control"
              placeholder="預設帶入會員電話，也可以改成其他電話"
              :disabled="submitting"
            />
          </div>
        </div>

        <h2 class="h5 mb-3">維修資訊</h2>
        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label">分店</label>
            <select v-model="form.storeId" class="form-select" :disabled="submitting">
              <option value="">請選擇分店</option>
              <option v-for="s in stores" :key="s.id" :value="s.id">
                {{ s.name }}
              </option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">送修方式</label>
            <select v-model="form.dropoffType" class="form-select" :disabled="submitting">
              <option value="">請選擇送修方式</option>
              <option value="IN_STORE">親臨門市</option>
              <option value="SHIPPING">寄送門市</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">手機品牌</label>
            <input
              v-model.trim="form.repairBrand"
              type="text"
              class="form-control"
              placeholder="例如：Apple"
              :disabled="submitting"
            />
          </div>
          <div class="col-md-6">
            <label class="form-label">手機型號</label>
            <input
              v-model.trim="form.repairModel"
              type="text"
              class="form-control"
              placeholder="例如：iPhone 13"
              :disabled="submitting"
            />
          </div>
          <div class="col-12">
            <label class="form-label">故障狀況描述</label>
            <textarea
              v-model.trim="form.issueDescription"
              class="form-control"
              rows="3"
              maxlength="200"
              :disabled="submitting"
            ></textarea>
          </div>
          <div class="col-md-6">
            <label class="form-label">預約日期</label>
            <input
              v-model="form.bookingDate"
              type="date"
              class="form-control"
              :min="todayStr"
              :disabled="submitting"
            />
          </div>
          <div class="col-md-6">
            <label class="form-label">預約時段</label>
            <input
              v-model="form.timeSlot"
              type="time"
              class="form-control"
              :disabled="submitting"
            />
          </div>
        </div>

        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? "送出中..." : "送出預約" }}
        </button>
      </form>
    </template>
  </main>
</template>

<style scoped></style>
