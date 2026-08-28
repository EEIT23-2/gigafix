<script setup>
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

/* ===== 套用網站配色風格 ===== */

.modal-content {
  border-radius: 1rem !important;
  overflow: hidden;
}

.modal-header {
  background-color: #1e3557;
  padding: 1.25rem 1.5rem;
}

.modal-header .modal-title {
  color: #ffffff;
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.modal-header .btn-close {
  filter: brightness(0) invert(1); /* 讓預設黑色 X 變成白色，配合深藍底色 */
  opacity: 0.85;
}

.modal-header .btn-close:hover {
  opacity: 1;
}

.modal-body {
  padding: 1.75rem 1.5rem;
}

.modal-footer {
  border-top: 1px solid #eaeaea;
  padding: 1rem 1.5rem;
}

.modal-footer .btn-primary {
  background-color: #2b77c5;
  border-color: #2b77c5;
  border-radius: 0.6rem;
  font-weight: 600;
  padding: 0.5rem 1.5rem;
}

.modal-footer .btn-primary:hover {
  background-color: #1d324b;
  border-color: #1d324b;
}

.modal-footer .btn-secondary,
.modal-footer .btn-outline-secondary {
  border-radius: 0.6rem;
  font-weight: 500;
}

.admin-info-card {
  background-color: #eef4fb;
  border: 1px solid #d9e6f5;
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
  font-weight: 400;
}

.info-value {
  font-size: 1rem;
  font-weight: 500;
  color: #1d324b;
  letter-spacing: 0.01em;
}
</style>