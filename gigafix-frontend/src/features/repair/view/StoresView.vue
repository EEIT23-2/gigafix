<script setup>
import { onMounted, ref } from "vue";
import { Modal } from "bootstrap";
import {
  confirmImportStores,
  createStore,
  deleteStore,
  downloadBlob,
  exportStores,
  formatFromFileName,
  getStores,
  getTodayString, // ★改：新增
  previewImportStores,
  updateStore,
} from "../api";
import { useExportMenu } from "../useExportMenu";
import ImportPreviewModal from "../components/ImportPreviewModal.vue";
import { swalConfirm, useSwalMessages } from "../../../utils/swal";

const stores = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
useSwalMessages(errorMessage, successMessage);

// ===== 匯出/匯入 =====
const exportMenu = useExportMenu();
const exporting = ref(false);
const importing = ref(false);
const importFileInput = ref(null);

async function handleExport(format) {
  exportMenu.close();
  exporting.value = true;
  errorMessage.value = "";
  try {
    const blob = await exportStores(format);
    downloadBlob(blob, `stores-${getTodayString()}.${format}`);
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `匯出失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    exporting.value = false;
  }
}

function openImportFilePicker() {
  importFileInput.value.click();
}

const previewModalRef = ref(null);
const importPreview = ref(null);
const confirmingImport = ref(false);

async function handleImportFileChange(event) {
  const file = event.target.files[0];
  event.target.value = ""; // 清空，這樣同一個檔案要重選也會觸發change
  if (!file) return;

  const format = formatFromFileName(file.name);
  if (!format) {
    errorMessage.value = "不支援的檔案格式，請選擇 .xlsx、.json 或 .xml 檔";
    return;
  }

  importing.value = true;
  errorMessage.value = "";
  try {
    importPreview.value = await previewImportStores(file, format);
    previewModalRef.value.show();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `匯入預覽失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    importing.value = false;
  }
}

async function handleConfirmImport() {
  confirmingImport.value = true;
  errorMessage.value = "";
  successMessage.value = "";
  try {
    const rows = importPreview.value.rows
      .filter((r) => r.action === "INSERT" || r.action === "UPDATE")
      .map((r) => r.data);
    const result = await confirmImportStores(rows);
    successMessage.value = `匯入完成：新增 ${result.inserted} 筆、更新 ${result.updated} 筆、失敗 ${result.failed} 筆${
      result.errors?.length ? "（" + result.errors.join("；") + "）" : ""
    }`;
    previewModalRef.value.hide();
    await fetchStores();
  } catch (error) {
    console.error(error);
    errorMessage.value = error.response
      ? `匯入失敗：HTTP ${error.response.status}`
      : "無法連線到後端伺服器";
  } finally {
    confirmingImport.value = false;
  }
}

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
  if (!(await swalConfirm(`確定要刪除分店「${s.name}」嗎？`, { danger: true }))) return;
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
      <div class="d-flex gap-2">
        <button class="btn btn-primary rounded-pill px-4" @click="openCreateModal">
          <i class="bi bi-plus-lg me-1"></i>新增分店
        </button>

        <!-- 匯出：下拉選格式 -->
        <div class="dropdown" :ref="(el) => (exportMenu.containerRef.value = el)">
          <button
            class="btn btn-outline-secondary dropdown-toggle rounded-pill px-3"
            type="button"
            :disabled="exporting"
            @click="exportMenu.toggle"
          >
            {{ exporting ? "匯出中..." : "匯出" }}
          </button>
          <ul class="dropdown-menu dropdown-menu-end" :class="{ show: exportMenu.open.value }">
            <li>
              <button class="dropdown-item" @click="handleExport('xlsx')">
                Excel
              </button>
            </li>
            <li>
              <button class="dropdown-item" @click="handleExport('json')">
                JSON
              </button>
            </li>
            <li>
              <button class="dropdown-item" @click="handleExport('xml')">
                XML
              </button>
            </li>
          </ul>
        </div>

        <!-- 匯入：直接跳檔案選擇，格式從副檔名判斷 -->
        <button
          class="btn btn-outline-secondary rounded-pill px-3"
          type="button"
          :disabled="importing"
          @click="openImportFilePicker"
        >
          {{ importing ? "匯入中..." : "匯入" }}
        </button>
        <input
          ref="importFileInput"
          type="file"
          accept=".xlsx,.json,.xml"
          class="d-none"
          @change="handleImportFileChange"
        />
      </div>
    </div>


    <section class="card section-card overflow-hidden">
      <div class="card-header fw-bold section-card-header">
        <i class="bi bi-shop"></i> 分店列表
      </div>
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <table v-else class="table table-hover mb-0 align-middle data-table">
        <colgroup>
          <col style="width: 8%">
          <col style="width: 23%">
          <col style="width: 23%">
          <col style="width: 23%">
          <col style="width: 23%">
        </colgroup>
        <thead class="table-light">
          <tr>
            <th>id</th>
            <th>分店名稱</th>
            <th>地址</th>
            <th>電話</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in stores" :key="s.id">
            <td>{{ s.id }}</td>
            <td>{{ s.name }}</td>
            <td>{{ s.address }}</td>
            <td>{{ s.phone }}</td>
            <td>
              <button
                class="btn btn-sm btn-outline-primary me-2 rounded-pill px-3"
                @click="openEditModal(s)"
              >
                修改
              </button>
              <button
                class="btn btn-sm btn-outline-danger rounded-pill px-3"
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
              class="btn btn-secondary rounded-pill px-4"
              data-bs-dismiss="modal"
              :disabled="saving"
            >
              取消
            </button>
            <button
              type="button"
              class="btn btn-primary rounded-pill px-4"
              :disabled="saving"
              @click="handleSave"
            >
              <i class="bi bi-save me-1"></i>{{ saving ? "儲存中..." : "儲存" }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 匯入預覽視窗 -->
    <ImportPreviewModal
      ref="previewModalRef"
      :preview="importPreview"
      :confirming="confirmingImport"
      @confirm="handleConfirmImport"
    />
  </main>
</template>

<style scoped>
/* 匯出選單不靠 Bootstrap JS(Popper)定位，改用固定的向右對齊 */
.dropdown-menu {
  right: 0;
  left: auto;
}

/* 區塊卡片：圓角+柔和陰影，跟維修單管理同一套風格 */
.section-card {
  border: none;
  border-radius: 1rem;
  box-shadow: 0 2px 10px rgba(30, 53, 87, 0.08);
}

/* 區塊標題色塊：跟站內品牌藍統一風格 */
.section-card-header {
  background-color: #a8cdf0;
  color: #14263d;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
}

/* 欄寬固定(搭配上面的 colgroup)，除了id欄，其他欄位平均分配寬度 */
.data-table {
  table-layout: fixed;
}
.data-table td {
  word-break: break-word;
}

/* 表格文字放大、加粗，跟維修單管理同一個級距 */
.data-table th {
  font-size: 16px;
}
.data-table td {
  font-size: 18px;
  font-weight: 700;
  color: #1d324b;
}
</style>
