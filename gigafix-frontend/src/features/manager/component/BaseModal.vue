<script setup>
import { getRoleLabel } from '../util/roleMap.js';
import { formatDateTime } from '../util/timeMap.js';

const props = defineProps({
  modelValue: Boolean,
  adminInfo: {
    type: Object,
    default: null
  }
})
const emit = defineEmits(['update:modelValue', 'closed'])
const close = () => emit('update:modelValue', false)
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-backdrop fade show" @click="close"></div>
    </Transition>
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal fade show d-block" tabindex="-1" @keyup.esc="close">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-0 shadow-lg rounded-4">
            <div class="modal-header border-0 pb-0">
              <h5 class="modal-title"><slot name="title" /></h5>
              <button type="button" class="btn-close" @click="close"></button>
            </div>

            <div class="modal-body">
            <div v-if="adminInfo" class="admin-info-card mb-3">
                <div class="info-row">
                <div class="info-label">管理員編號</div>
                <div class="info-value">{{ adminInfo.adminId }}</div>
                </div>
                <div class="info-row">
                <div class="info-label">管理員名稱</div>
                <div class="info-value">{{ adminInfo.adminName }}</div>
                </div>
                <div class="info-row">
                <div class="info-label">目前角色</div>
                <div class="info-value">
                    <span class="badge bg-secondary">{{ getRoleLabel(adminInfo.role) }}</span>
                </div>
                </div>
                <div class="info-row">
                <div class="info-label">創建時間</div>
                <div class="info-value">{{ formatDateTime(adminInfo.createDateTime) }}</div>
                </div>
            </div>

            <slot />
            </div>
            <div class="modal-footer">
              <slot name="footer" />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.4s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-dialog,
.modal-fade-leave-active .modal-dialog {
  transition: transform 0.4s ease;
}

.modal-fade-enter-from .modal-dialog,
.modal-fade-leave-to .modal-dialog {
  transform: translateY(-30px);
}
.admin-info-card {
  background-color: #eef1f6;
  border: 1px solid #dee2e6;
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 0.4rem 0;
}

.info-row:not(:last-child) {
  border-bottom: 1px solid #e2e5ea;
}

.info-label {
  flex: 0 0 100px;
  font-size: 0.85rem;
  color: #6c757d;
  font-weight: 400; /* 標籤維持正常字重 */
}

.info-value {
  font-size: 1rem;        /* 用字級拉開差異,而不是加粗 */
  font-weight: 500;       /* 中文字 500(介於正常跟粗體之間)通常比 600/700 好看 */
  color: #1a1d20;         /* 用更深的顏色對比取代粗體的視覺效果 */
  letter-spacing: 0.01em;
}
</style>