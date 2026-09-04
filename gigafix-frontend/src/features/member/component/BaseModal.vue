<script setup>
//會員中心共用的彈窗殼，只負責外觀跟開關(v-model)，不放任何表單/業務邏輯，
//之後member資料夾底下其他要做彈窗的功能都可以直接複用這個殼，內容自己用slot帶進來就好
const props = defineProps({
  modelValue: Boolean,
  showBackdrop: {
    type: Boolean,
    default: true //預設自己畫背景遮罩；同畫面同時有多個Modal互切時，可傳false改由外層共用一層遮罩
  }
})
const emit = defineEmits(['update:modelValue', 'closed'])
const close = () => emit('update:modelValue', false)
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue && showBackdrop" class="modal-backdrop fade show" @click="close"></div>
    </Transition>
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal fade show d-block" tabindex="-1" @keyup.esc="close">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-0 shadow-lg rounded-4">
            <div class="modal-header border-0">
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
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-dialog,
.modal-fade-leave-active .modal-dialog {
  transition: transform 0.25s ease;
}

.modal-fade-enter-from .modal-dialog,
.modal-fade-leave-to .modal-dialog {
  transform: translateY(-30px);
}

/* ===== 套用網站前台配色風格，跟LoginRegisterModal維持同一套視覺 ===== */

.modal-content {
  border-radius: 1rem !important;
  overflow: hidden;
}

.modal-header {
  background-color: #1e3557;
  padding: 0.75rem 1.1rem;
  display: flex;
  align-items: center;
}

.modal-header .modal-title {
  margin: 0;
  color: #ffffff;
  font-size: 1.4rem;
  font-weight: 400;
  letter-spacing: 0.5px;
}

.modal-header .btn-close {
  margin: 0 0 0 auto;
  filter: brightness(0) invert(1);
  opacity: 0.85;
}

.modal-header .btn-close:hover {
  opacity: 1;
}

.modal-header .btn-close:focus {
  box-shadow: none;
  outline: none;
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
</style>
