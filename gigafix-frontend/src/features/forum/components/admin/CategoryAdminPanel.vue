<script setup>
import { onMounted, ref } from 'vue'
import { getCategories } from '../../api'
import { createCategory, updateCategory, deleteCategory } from '../../adminApi'

// 對齊後端 CreateCategoryRequest/UpdateCategoryRequest 的 @Size(max = 60)
// 資料表欄位是 NVARCHAR(60)，這個 60 是「字元數」不是位元組數，中英文都一樣算 1 個字
const MAX_LENGTH = 60

const categories = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 新增
const newName = ref('')
const creating = ref(false)

// 編輯：同時間只有一列處於編輯狀態（與 CommentSection 的 reportingCommentId 同一種模式）
const editingCategoryId = ref(null)
const editingName = ref('')
const saving = ref(false)

// 刪除：展開二次確認的那一列
const deletingCategoryId = ref(null)
const deleting = ref(false)

// 錯誤訊息優先取參數驗證的 errors 陣列（400），其次是 ForumExceptionHandler 回傳的 message（409/404），
// 兩者都沒有才退回通用訊息
function resolveErrorMessage(error, fallbackPrefix) {
  const data = error.response?.data
  return (
    data?.errors?.[0]?.message ||
    data?.message ||
    (error.response ? `${fallbackPrefix}：HTTP ${error.response.status}` : '無法連線到後端伺服器')
  )
}

function resetMessages() {
  errorMessage.value = ''
  successMessage.value = ''
}

async function fetchCategories() {
  loading.value = true
  try {
    categories.value = await getCategories()
  } catch (error) {
    console.error(error)
    errorMessage.value = resolveErrorMessage(error, '讀取失敗')
  } finally {
    loading.value = false
  }
}

onMounted(fetchCategories)

async function handleCreate() {
  resetMessages()
  if (!newName.value.trim()) {
    errorMessage.value = '分類名稱不能為空'
    return
  }
  creating.value = true
  try {
    await createCategory(newName.value.trim())
    newName.value = ''
    successMessage.value = '已新增分類'
    await fetchCategories()
  } catch (error) {
    console.error(error)
    errorMessage.value = resolveErrorMessage(error, '新增失敗')
  } finally {
    creating.value = false
  }
}

function startEdit(category) {
  resetMessages()
  deletingCategoryId.value = null
  editingCategoryId.value = category.categoryId
  editingName.value = category.name
}

function cancelEdit() {
  editingCategoryId.value = null
  editingName.value = ''
}

async function handleSaveEdit(category) {
  resetMessages()
  if (!editingName.value.trim()) {
    errorMessage.value = '分類名稱不能為空'
    return
  }
  saving.value = true
  try {
    await updateCategory(category.categoryId, editingName.value.trim())
    cancelEdit()
    successMessage.value = '已更新分類名稱'
    await fetchCategories()
  } catch (error) {
    console.error(error)
    errorMessage.value = resolveErrorMessage(error, '更新失敗')
  } finally {
    saving.value = false
  }
}

function startDelete(category) {
  resetMessages()
  cancelEdit()
  deletingCategoryId.value = category.categoryId
}

function cancelDelete() {
  deletingCategoryId.value = null
}

async function handleConfirmDelete(category) {
  resetMessages()
  deleting.value = true
  try {
    await deleteCategory(category.categoryId)
    deletingCategoryId.value = null
    successMessage.value = '已刪除分類'
    await fetchCategories()
  } catch (error) {
    console.error(error)
    errorMessage.value = resolveErrorMessage(error, '刪除失敗')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div>
    <section class="card mb-4">
      <div class="card-body">
        <div class="d-flex flex-column flex-md-row gap-3 align-items-md-center">
          <label class="fw-semibold text-nowrap mb-0" for="new-category-name">新增分類</label>
          <input
            id="new-category-name"
            v-model.trim="newName"
            class="form-control name-input"
            type="text"
            :maxlength="MAX_LENGTH"
            placeholder="輸入分類名稱..."
            @keyup.enter="handleCreate"
          />
          <button
            class="btn btn-primary text-nowrap"
            type="button"
            :disabled="creating"
            @click="handleCreate"
          >
            {{ creating ? '新增中...' : '新增' }}
          </button>
        </div>
        <div class="form-text mt-2">最多 {{ MAX_LENGTH }} 字 · {{ newName.length }}/{{ MAX_LENGTH }}</div>
      </div>
    </section>

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
    <div v-if="successMessage" class="alert alert-success alert-dismissible" role="alert">
      {{ successMessage }}
      <button
        class="btn-close"
        type="button"
        aria-label="關閉成功訊息"
        @click="successMessage = ''"
      ></button>
    </div>

    <section class="card overflow-hidden">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <div class="mt-2">載入中...</div>
      </div>

      <div v-else class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>分類名稱</th>
              <th>文章數</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="category in categories" :key="category.categoryId">
              <tr>
                <td>{{ category.categoryId }}</td>
                <td>
                  <div
                    v-if="editingCategoryId === category.categoryId"
                    class="d-flex flex-column name-input"
                  >
                    <input
                      v-model.trim="editingName"
                      class="form-control form-control-sm"
                      type="text"
                      :maxlength="MAX_LENGTH"
                      @keyup.enter="handleSaveEdit(category)"
                    />
                    <span class="text-secondary small mt-1">
                      {{ editingName.length }}/{{ MAX_LENGTH }}
                    </span>
                  </div>
                  <span v-else class="fw-semibold">{{ category.name }}</span>
                </td>
                <td>{{ category.articleCount }}</td>
                <td class="text-end text-nowrap">
                  <template v-if="editingCategoryId === category.categoryId">
                    <button
                      class="btn btn-sm btn-primary me-1"
                      type="button"
                      :disabled="saving"
                      @click="handleSaveEdit(category)"
                    >
                      {{ saving ? '儲存中...' : '儲存' }}
                    </button>
                    <button
                      class="btn btn-sm btn-outline-secondary"
                      type="button"
                      @click="cancelEdit"
                    >
                      取消
                    </button>
                  </template>
                  <template v-else>
                    <button
                      class="btn btn-sm btn-outline-primary me-1"
                      type="button"
                      @click="startEdit(category)"
                    >
                      編輯
                    </button>
                    <!-- 分類被文章使用中就不能刪除（articles.category_id 是 NOT NULL 外鍵）。
                         這裡先在前端擋掉，後端 deleteCategory 仍有同樣的防護 -->
                    <button
                      class="btn btn-sm btn-outline-danger"
                      type="button"
                      :disabled="category.articleCount > 0"
                      :title="
                        category.articleCount > 0
                          ? `還有 ${category.articleCount} 篇文章使用此分類，無法刪除`
                          : '刪除此分類'
                      "
                      @click="startDelete(category)"
                    >
                      刪除
                    </button>
                  </template>
                </td>
              </tr>

              <tr v-if="deletingCategoryId === category.categoryId">
                <td colspan="4" class="p-0">
                  <div
                    class="alert alert-danger border-danger rounded-0 mb-0 d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-2"
                  >
                    <span>確定要刪除分類「{{ category.name }}」嗎？此動作無法復原。</span>
                    <span class="text-nowrap">
                      <button
                        class="btn btn-sm btn-outline-secondary me-1"
                        type="button"
                        @click="cancelDelete"
                      >
                        取消
                      </button>
                      <button
                        class="btn btn-sm btn-danger"
                        type="button"
                        :disabled="deleting"
                        @click="handleConfirmDelete(category)"
                      >
                        {{ deleting ? '刪除中...' : '確認刪除' }}
                      </button>
                    </span>
                  </div>
                </td>
              </tr>
            </template>

            <tr v-if="categories.length === 0">
              <td colspan="4" class="text-center text-secondary py-5">沒有分類資料</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.name-input {
  max-width: 320px;
}
th {
  white-space: nowrap;
  font-size: 0.8rem;
  letter-spacing: 0.03em;
}
td {
  font-size: 0.875rem;
}
</style>
