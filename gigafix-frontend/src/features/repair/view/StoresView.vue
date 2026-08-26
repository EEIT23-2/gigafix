<script setup>
import { onMounted, ref } from "vue";
import { Modal } from "bootstrap";
import { createStore, deleteStore, getStores, updateStore } from "../api";

const stores = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

// ===== 新增/修改彈窗 =====
const modalRef = ref(null);
let modalInstance = null;
const editingId = ref(null); // null 代表新增，有值代表修改
const form = ref({ name: "", address: "", phone: "" });
const formSnapshot = ref({ name: "", address: "", phone: "" });
const saving = ref(false);
const formError = ref("");

async function fetchStores() {
  loading.value = true;
  errorMessage.value = "";
  try {
    stores.value = await getStores();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `查詢失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    loading.value = false;
  }
}

function openCreateModal() {
  editingId.value = null;
  form.value = { name: "", address: "", phone: "" };
  formSnapshot.value = { ...form.value };
  formError.value = "";
  modalInstance.show();
}

function openEditModal(s) {
  editingId.value = s.id;
  form.value = { name: s.name, address: s.address, phone: s.phone };
  formSnapshot.value = { ...form.value };
  formError.value = "";
  modalInstance.show();
}

async function handleSave() {
  formError.value = "";

  if (!form.value.name || !form.value.address || !form.value.phone) {
    formError.value = "分店名稱、地址、電話都要填寫";
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
      await createStore(form.value);
      successMessage.value = "新增成功";
    } else {
      await updateStore(editingId.value, form.value);
      successMessage.value = "修改成功";
    }
    modalInstance.hide();
    await fetchStores();
  } catch (error) {
    console.error(error);
    formError.value = error.response
      ? `儲存失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    saving.value = false;
  }
}

async function handleDelete(s) {
  if (!window.confirm(`確定要刪除分店「${s.name}」嗎？`)) return;
  errorMessage.value = "";
  try {
    await deleteStore(s.id);
    successMessage.value = "刪除成功";
    await fetchStores();
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
});
</script>

<template>
  <main class="container-fluid px-3 px-lg-4 py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="fw-bold mb-0">分店管理</h1>
      <button class="btn btn-primary" @click="openCreateModal">
        ＋ 新增分店
      </button>
    </div>

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
            <th>分店名稱</th>
            <th>地址</th>
            <th>電話</th>
            <th class="text-end">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in stores" :key="s.id">
            <td>{{ s.id }}</td>
            <td>{{ s.name }}</td>
            <td>{{ s.address }}</td>
            <td>{{ s.phone }}</td>
            <td class="text-end">
              <button
                class="btn btn-sm btn-outline-primary me-2"
                @click="openEditModal(s)"
              >
                修改
              </button>
              <button
                class="btn btn-sm btn-outline-danger"
                @click="handleDelete(s)"
              >
                刪除
              </button>
            </td>
          </tr>
          <tr v-if="stores.length === 0">
            <td colspan="5" class="text-center text-secondary py-4">
              目前沒有分店資料
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 新增/修改分店彈窗 -->
    <div class="modal fade" ref="modalRef" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4">
          <div class="modal-header border-0 pb-0">
            <h5 class="modal-title">
              {{ editingId === null ? "新增分店" : "修改分店" }}
            </h5>
            <button
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
            ></button>
          </div>
          <div class="modal-body">
            <label class="form-label">分店名稱</label>
            <input
              v-model="form.name"
              type="text"
              class="form-control mb-2"
              :disabled="saving"
            />

            <label class="form-label">地址</label>
            <input
              v-model="form.address"
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
