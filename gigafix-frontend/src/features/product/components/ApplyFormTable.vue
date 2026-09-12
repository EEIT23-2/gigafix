<script setup>
defineProps({
  applications: {
    type: Array,
    default: () => [],
  },
  deletingId: {
    type: [Number, String],
    default: null,
  },
});

const emit = defineEmits(["view", "edit", "delete"]);

const categoryLabels = {
  IPHONE: "iPhone",
  WATCH: "Apple Watch",
  IPAD: "iPad",
};

const statusLabels = {
  CANCELLED: "已取消",
  APPLIED: "已提出申請",
  INSPECTING: "檢查估價中",
  WAITING_FOR_AGREEMENT: "等待客戶同意",
  WIPING: "資料清除中",
  COMPLETED: "回收完成",
};

function categoryLabel(category) {
  return categoryLabels[category] ?? category ?? "—";
}

function statusLabel(status) {
  return statusLabels[status] ?? status ?? "—";
}

function statusClass(status) {
  return (
    {
      CANCELLED: "text-bg-secondary",
      APPLIED: "text-bg-primary",
      INSPECTING: "text-bg-warning",
      WAITING_FOR_AGREEMENT: "text-bg-info",
      WIPING: "text-bg-dark",
      COMPLETED: "text-bg-success",
    }[status] ?? "text-bg-light"
  );
}

function getAppearance(application) {
  return application?.appearance ?? application?.appreance ?? "—";
}

function formatPrice(price) {
  if (price == null) {
    return "尚未估價";
  }

  return `NT$ ${Number(price).toLocaleString()}`;
}

function formatDateTime(dateTime) {
  if (!dateTime) {
    return "—";
  }

  return dateTime.replace("T", " ");
}
</script>

<template>
  <div class="application-table-container">
    <table class="table application-table align-middle mb-0">
      <colgroup>
        <col class="column-id" />
        <col class="column-member" />
        <col class="column-product" />
        <col class="column-appearance" />
        <col class="column-category" />
        <col class="column-price" />
        <col class="column-status" />
        <col class="column-store" />
        <col class="column-date" />
        <col class="column-actions" />
      </colgroup>

      <thead class="table-light">
        <tr>
          <th>編號</th>
          <th>會員</th>
          <th>商品</th>
          <th>商品外觀</th>
          <th>分類</th>
          <th>預估價格</th>
          <th>狀態</th>
          <th>回收門市</th>
          <th>申請時間</th>
          <th class="text-center">操作</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="application in applications"
          :key="application.applyId"
          class="application-row"
          @click="emit('view', application)"
        >
          <td data-label="申請編號">
            <span class="fw-semibold text-primary">
              #{{ application.applyId }}
            </span>
          </td>

          <td data-label="會員">
            <div class="cell-content">
              <div class="member-name">
                {{ application.memberName || "—" }}
              </div>

              <small class="text-secondary">
                ID：{{ application.memberId ?? "—" }}
              </small>
            </div>
          </td>

          <td data-label="商品">
            <div class="product-content">
              <img
                v-if="application.imageUrl"
                :src="application.imageUrl"
                :alt="application.productName"
                class="product-thumbnail"
              />

              <div v-else class="product-thumbnail product-placeholder">
                <i class="bi bi-phone"></i>
              </div>

              <span class="product-name" :title="application.productName">
                {{ application.productName || "—" }}
              </span>
            </div>
          </td>

          <td data-label="商品外觀">
            <span class="appearance-text" :title="getAppearance(application)">
              {{ getAppearance(application) }}
            </span>
          </td>

          <td data-label="分類">
            <span class="category-text">
              {{ categoryLabel(application.category) }}
            </span>
          </td>

          <td data-label="預估價格">
            <span class="price-text">
              {{ formatPrice(application.estimatedPrice) }}
            </span>
          </td>

          <td data-label="狀態">
            <span
              class="badge rounded-pill status-badge"
              :class="statusClass(application.recycleStatus)"
            >
              {{ statusLabel(application.recycleStatus) }}
            </span>
          </td>

          <td data-label="回收門市">
            <span class="store-text">
              {{ application.storeName || "未指定" }}
            </span>
          </td>

          <td data-label="申請時間">
            <span class="date-text">
              {{ formatDateTime(application.createdTime) }}
            </span>
          </td>

          <td data-label="操作" class="actions-cell" @click.stop>
            <div class="action-buttons">
              <button
                type="button"
                class="btn btn-sm btn-outline-primary"
                title="編輯"
                aria-label="編輯"
                @click="emit('edit', application)"
              >
                <i class="bi bi-pencil-square"></i>
              </button>

              <button
                type="button"
                class="btn btn-sm btn-outline-danger"
                :title="
                  application.recycleStatus === 'CANCELLED'
                    ? '刪除'
                    : '只有已取消的回收單可以刪除'
                "
                aria-label="刪除"
                :disabled="
                  application.recycleStatus !== 'CANCELLED' ||
                  String(deletingId) === String(application.applyId)
                "
                @click="emit('delete', application)"
              >
                <span
                  v-if="String(deletingId) === String(application.applyId)"
                  class="spinner-border spinner-border-sm"
                ></span>

                <i v-else class="bi bi-trash3"></i>
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="applications.length === 0" class="empty-row">
          <td colspan="10" class="text-center text-secondary py-5">
            <i class="bi bi-inbox fs-2 d-block mb-2"></i>

            找不到符合條件的回收申請
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.application-table-container {
  width: 100%;
  overflow: visible;
}

.application-table {
  width: 100%;
  table-layout: fixed;
  font-size: 0.9rem;
}

.column-id {
  width: 6%;
}

.column-member {
  width: 9%;
}

.column-product {
  width: 16%;
}

.column-appearance {
  width: 12%;
}

.column-category {
  width: 7%;
}

.column-price {
  width: 10%;
}

.column-status {
  width: 11%;
}

.column-store {
  width: 9%;
}

.column-date {
  width: 12%;
}

.column-actions {
  width: 8%;
}

.application-table th,
.application-table td {
  padding: 0.75rem 0.45rem;
  overflow: hidden;
  vertical-align: middle;
}

.application-table th {
  font-size: 0.88rem;
  white-space: normal;
  overflow-wrap: anywhere;
}

.application-row {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.application-row:hover {
  background-color: #f5f7ff;
}

.cell-content,
.product-content {
  min-width: 0;
}

.member-name {
  overflow: hidden;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-content {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.product-thumbnail {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border: 1px solid #dee2e6;
  border-radius: 0.4rem;
  object-fit: cover;
}

.product-placeholder {
  display: grid;
  place-items: center;
  color: #6c757d;
  background-color: #f1f3f5;
}

.product-name {
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  font-weight: 600;
  line-height: 1.3;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.appearance-text {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.4;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-clamp: 3;
}

.category-text,
.price-text,
.store-text,
.date-text {
  display: block;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.status-badge {
  max-width: 100%;
  font-size: 0.78rem;
  line-height: 1.25;
  white-space: normal;
}

.actions-cell {
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 0.3rem;
}

.action-buttons .btn {
  width: 31px;
  height: 31px;
  padding: 0;
}

@media (max-width: 1199.98px) {
  .application-table {
    font-size: 0.82rem;
  }

  .application-table th,
  .application-table td {
    padding: 0.65rem 0.3rem;
  }

  .product-thumbnail {
    width: 32px;
    height: 32px;
    flex-basis: 32px;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
}

@media (max-width: 991.98px) {
  .application-table {
    display: block;
    font-size: 0.92rem;
  }

  .application-table colgroup,
  .application-table thead {
    display: none;
  }

  .application-table tbody {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1rem;
    padding: 1rem;
  }

  .application-table tr.application-row {
    display: block;
    overflow: hidden;
    border: 1px solid #dee2e6;
    border-radius: 0.75rem;
    background-color: #fff;
    box-shadow: 0 0.125rem 0.25rem rgb(0 0 0 / 7.5%);
  }

  .application-table tr.application-row:hover {
    background-color: #f8f9ff;
  }

  .application-table td {
    display: flex;
    width: 100%;
    min-height: 44px;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 0.65rem 0.85rem;
    border-bottom: 1px solid #edf0f2;
    text-align: right;
    overflow: visible;
  }

  .application-table td:last-child {
    border-bottom: 0;
  }

  .application-table td::before {
    content: attr(data-label);
    flex: 0 0 82px;
    align-self: flex-start;
    color: #6c757d;
    font-weight: 600;
    text-align: left;
  }

  .product-content {
    max-width: calc(100% - 98px);
    justify-content: flex-end;
  }

  .product-name {
    text-align: right;
  }

  .appearance-text {
    max-width: calc(100% - 98px);
    text-align: right;
    -webkit-line-clamp: initial;
    line-clamp: initial;
  }

  .status-badge {
    max-width: calc(100% - 98px);
  }

  .action-buttons {
    flex-direction: row;
  }

  .empty-row {
    display: block;
    grid-column: 1 / -1;
  }

  .empty-row td {
    display: block;
    width: 100%;
    border: 0;
  }

  .empty-row td::before {
    display: none;
  }
}

@media (max-width: 575.98px) {
  .application-table tbody {
    grid-template-columns: 1fr;
    padding: 0.75rem;
  }
}
</style>
