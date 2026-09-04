<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

// 文字顏色與文字背景共用這個面板，避免同一段 UI 在工具列裡寫兩次
const props = defineProps({
  // 目前套用的色值，用來標示色票的作用中狀態；沒有套用時為空字串
  modelValue: { type: String, default: '' },
  swatches: { type: Array, default: () => [] },
  title: { type: String, default: '顏色' },
})

const emit = defineEmits(['select', 'clear'])

const open = ref(false)
const rootRef = ref(null)
// 取色器的暫存值。不直接綁 modelValue，否則沒套用顏色時原生取色器會顯示成黑色
const customColor = ref('#000000')

function toggle() {
  open.value = !open.value
}

function close() {
  open.value = false
}

// 點面板以外的地方就收合。觸發鈕本身在 rootRef 內，點它不會被這裡誤關
// （與 MoreActionsMenu 相同的作法）
function handleDocumentClick(event) {
  if (rootRef.value && !rootRef.value.contains(event.target)) {
    close()
  }
}

onMounted(() => document.addEventListener('click', handleDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', handleDocumentClick))

function pick(color) {
  emit('select', color)
  close()
}

function clear() {
  emit('clear')
  close()
}
</script>

<template>
  <span ref="rootRef" class="color-popover">
    <button type="button" class="trigger" :title="title" @click="toggle">
      <slot name="icon" />
      <!-- 目前色值直接顯示在按鈕下緣，不用打開面板也看得到 -->
      <span class="current-bar" :style="{ background: props.modelValue || 'transparent' }"></span>
    </button>

    <div v-if="open" class="panel">
      <div class="swatches">
        <button
          v-for="color in props.swatches"
          :key="color"
          type="button"
          class="swatch"
          :class="{ active: color === props.modelValue }"
          :style="{ background: color }"
          :title="color"
          @click="pick(color)"
        ></button>
      </div>

      <label class="custom-row">
        <span>自訂</span>
        <input v-model="customColor" type="color" @change="pick(customColor)" />
      </label>

      <button type="button" class="clear-btn" @click="clear">清除</button>
    </div>
  </span>
</template>

<style scoped>
.color-popover {
  position: relative;
  display: inline-block;
}

.trigger {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  width: 30px;
  height: 30px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #5a5c69;
  font-size: 14px;
  cursor: pointer;
}

.trigger:hover {
  background: #eaeef5;
}

/* 沒有套用顏色時是透明的，用一條淺色底墊著才看得出這裡有東西 */
.current-bar {
  width: 16px;
  height: 3px;
  border-radius: 1px;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
  background-color: transparent;
}

.panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  z-index: 10;
  width: 176px;
  padding: 8px;
  background: #ffffff;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.swatches {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 4px;
}

.swatch {
  width: 100%;
  aspect-ratio: 1;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  padding: 0;
  cursor: pointer;
}

.swatch.active {
  box-shadow: 0 0 0 2px #ffffff, 0 0 0 3px #1f5fa8;
}

.custom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #5a5c69;
  margin: 0;
}

.custom-row input[type='color'] {
  width: 40px;
  height: 24px;
  padding: 0;
  border: 1px solid #d7dce5;
  border-radius: 4px;
  background: none;
  cursor: pointer;
}

.clear-btn {
  border: 1px solid #d7dce5;
  border-radius: 4px;
  background: #ffffff;
  color: #5a5c69;
  font-size: 12px;
  padding: 4px 0;
  cursor: pointer;
}

.clear-btn:hover {
  background: #f2f6fb;
}
</style>
