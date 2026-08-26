<script setup>
import { onMounted, ref } from "vue";
import { Modal } from "bootstrap";
import {
  createTechnician,
  deleteTechnician,
  getStores,
  getTechnicians,
  updateTechnician,
} from "../api";

const technicians = ref([]);
const stores = ref([]);
const filterStoreId = ref("");
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

// ===== 新增/修改彈窗 =====
const modalRef = ref(null);
let modalInstance = null;
const editingId = ref(null); // null 代表新增，有值代表修改
const form = ref({ name: "", phone: "", storeId: "" });
const formSnapshot = ref({ name: "", phone: "", storeId: "" });
const saving = ref(false);
const formError = ref("");

async function fetchTechnicians() {
  loading.value = true;
  errorMessage.value = "";
  try {
    technicians.value = await getTechnicians(filterStoreId.value || undefined);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `查詢失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

async function fetchStores() {
  try {
    stores.value = await getStores();
  } catch (error) {
    console.error(error);
  }
}

function openCreateModal() {
  editingId.value = null;
  form.value = { name: "", phone: "", storeId: "" };
  formSnapshot.value = { ...form.value };
  formError.value = "";
  modalInstance.show();
}

function openEditModal(t) {
  editingId.value = t.id;
  form.value = { name: t.name, phone: t.phone, storeId: t.storeId };
  formSnapshot.value = { ...form.value };
  formError.value = "";
  modalInstance.show();
}

async function handleSave() {
  formError.value = "";

  if (!form.value.name || !form.value.phone || !form.value.storeId) {
    formError.value = "姓名、電話、分店都要填寫";
    return;
  }

  if (
    editingId.value !== null &&
    JSON.stringify(form.value) === JSON.stringify(formSnapshot.value)
  ) {
    formError.value = "內容沒有變更，不用儲存";
    return;
  }

  saving.value = true;
  try {
    if (editingId.value === null) {
      await createTechnician(form.value);
      successMessage.value = "新增成功";
    } else {
      await updateTechnician(editingId.value, form.value);
      successMessage.value = "修改成功";
    }
    modalInstance.hide();
    await fetchTechnicians();
  } catch (error) {
    console.error(error);
    formError.value = error.response
      ? `儲存失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    saving.value = false;
  }
}

async function handleDelete(t) {
  if (!window.confirm(`確定要刪除技師「${t.name}」嗎？`)) return;
  errorMessage.value = "";
  try {
    await deleteTechnician(t.id);
    successMessage.value = "刪除成功";
    await fetchTechnicians();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response?.data?.message
      ? `刪除失敗：${error.response.data.message}`
      : error.response
        ? `刪除失敗：HTTP ${error.response.status}`
        : "無法連線到後端伺服器";
  }
}

onMounted(async () => {
  modalInstance = new Modal(modalRef.value);
  modalRef.value.addEventListener("hide.bs.modal", () => {
    document.activeElement?.blur();
  });
  await fetchStores();
  await fetchTechnicians();
});
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="fw-bold mb-0">技師管理</h1>
      <button class="btn btn-primary" @click="openCreateModal">
        ＋ 新增技師
      </button>
    </div>

    <section class="card mb-4">
      <div class="card-body d-flex gap-3 align-items-center">
        <label class="form-label mb-0">依分店篩選：</label>
        <select
          v-model="filterStoreId"
          class="form-select"
          style="max-width: 220px"
          @change="fetchTechnicians"
        >
          <option value="">所有分店</option>
          <option v-for="s in stores" :key="s.id" :value="s.id">
            {{ s.name }}
          </option>
        </select>
      </div>
    </section>

    <div v-if="errorMessage" class="alert alert-danger alert-dismissible">
      {{ errorMessage }}
      <button
        type="button"
        class="btn-close"
        @click="errorMessage = ''"
      ></button>
    </div>
    <div
      v-if="successMessage"
      class="alert alert-success alert-dismissible"
      role="alert"
    >
      {{ successMessage }}
      <button
        class="btn-close"
        type="button"
        @click="successMessage = ''"
      ></button>
    </div>

    <section class="card overflow-hidden">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <table v-else class="table table-hover mb-0 align-middle">
        <thead class="table-light">
          <tr>
            <th>id</th>
            <th>姓名</th>
            <th>電話</th>
            <th>分店</th>
            <th class="text-end">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in technicians" :key="t.id">
            <td>{{ t.id }}</td>
            <td>{{ t.name }}</td>
            <td>{{ t.phone }}</td>
            <td>{{ t.storeName }}</td>
            <td class="text-end">
              <button
                class="btn btn-sm btn-outline-primary me-2"
                @click="openEditModal(t)"
              >
                修改
              </button>
              <button
                class="btn btn-sm btn-outline-danger"
                @click="handleDelete(t)"
              >
                刪除
              </button>
            </td>
          </tr>
          <tr v-if="technicians.length === 0">
            <td colspan="5" class="text-center text-secondary py-4">
              目前沒有技師資料
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 新增/修改技師彈窗 -->
    <div class="modal fade" ref="modalRef" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4">
          <div class="modal-header border-0 pb-0">
            <h5 class="modal-title">
              {{ editingId === null ? "新增技師" : "修改技師" }}
            </h5>
            <button
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
            ></button>
          </div>
          <div class="modal-body">
            <label class="form-label">姓名</label>
            <input
              v-model="form.name"
              type="text"
              class="form-control mb-2"
              :disabled="saving"
            />

            <label class="form-label">電話</label>
            <input
              v-model="form.phone"
              type="text"
              class="form-control mb-2"
              :disabled="saving"
            />

            <label class="form-label">分店</label>
            <select
              v-model="form.storeId"
              class="form-select mb-2"
              :disabled="saving"
            >
              <option value="">請選擇分店</option>
              <option v-for="s in stores" :key="s.id" :value="s.id">
                {{ s.name }}
              </option>
            </select>

            <p v-if="formError" class="text-danger mt-2 mb-0">
              {{ formError }}
            </p>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-secondary"
              data-bs-dismiss="modal"
              :disabled="saving"
            >
              取消
            </button>
            <button
              type="button"
              class="btn btn-primary"
              :disabled="saving"
              @click="handleSave"
            >
              {{ saving ? "儲存中..." : "儲存" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped></style>
