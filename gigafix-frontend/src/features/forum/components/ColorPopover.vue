<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

// 文字顏色與文字背景共用這個面板，避免同一段 UI 在工具列裡寫兩次
const props = defineProps({
  // 目前套用的色值，用來標示色票的作用中狀態；沒有套用時為空字串
  modelValue: { type: String, default: '' },
  swatches: { type: Array, default: () => [] },
  title: { type: String, default: '顏色' },
})

const emit = defineEmits(['select', 'clear', 'panel-open'])

const open = ref(false)
const rootRef = ref(null)
const nativeColorInput = ref(null)
// 使用者上一次透過自訂色選過的顏色。選過一次之後才會出現在色票列裡，
// 之後點它跟點預設色票一樣直接套用，不用每次都重新彈出瀏覽器原生選色器
const lastCustomColor = ref('')

function toggle() {
  open.value = !open.value
  // 面板剛打開的這一刻，編輯器的選取範圍還沒被任何東西動過，是最後可信的一刻——
  // 稍後點自訂顏色會跳出瀏覽器原生色盤，那是跟網頁脫勾的原生 UI，關閉後編輯器的選取常常會跑掉。
  // 通知呼叫端先把這一刻的選取記下來，套色時用記下來的，而不是相信「當下」可能已經跑掉的選取
  if (open.value) emit('panel-open')
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

// 另外開瀏覽器原生選色器，選一個全新的自訂色——跟直接點下面那顆「上次自訂色」色票是兩件事
function openCustomPicker() {
  nativeColorInput.value?.click()
}

function handleCustomColorChange(event) {
  const color = event.target.value
  lastCustomColor.value = color
  pick(color)
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

        <button
          v-if="lastCustomColor"
          type="button"
          class="swatch"
          :class="{ active: lastCustomColor === props.modelValue }"
          :style="{ background: lastCustomColor }"
          :title="`上次自訂色 ${lastCustomColor}`"
          @click="pick(lastCustomColor)"
        ></button>

        <button type="button" class="swatch custom-trigger" title="選新的自訂色" @click="openCustomPicker">
          +
        </button>
      </div>

      <input
        ref="nativeColorInput"
        type="color"
        class="native-color-input"
        @change="handleCustomColorChange"
      />

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

.swatch.custom-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1px dashed #adb5bd;
  color: #6c757d;
  font-size: 14px;
  line-height: 1;
}

.swatch.custom-trigger:hover {
  border-color: #2b77c5;
  color: #2b77c5;
}

/* 保留在畫面上（不能用 display:none）才能被 .click() 觸發原生選色器，但視覺上完全隱藏 */
.native-color-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  border: 0;
  opacity: 0;
  pointer-events: none;
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
