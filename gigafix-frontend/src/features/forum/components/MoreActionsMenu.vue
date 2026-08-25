<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const open = ref(false)
const rootRef = ref(null)

function toggle() {
  open.value = !open.value
}

function close() {
  open.value = false
}

// 點選單以外的地方就收合。觸發鈕本身也在 rootRef 內，所以點它不會被這裡誤關掉
function handleDocumentClick(event) {
  if (rootRef.value && !rootRef.value.contains(event.target)) {
    close()
  }
}

onMounted(() => document.addEventListener('click', handleDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', handleDocumentClick))
</script>

<template>
  <div ref="rootRef" class="more-actions">
    <button type="button" class="trigger" title="更多選項" @click="toggle">⋮</button>
    <!-- 呼叫端自己放選項，並可用 close 在點完之後收合選單 -->
    <div v-if="open" class="menu">
      <slot :close="close" />
    </div>
  </div>
</template>

<style scoped>
.more-actions {
  position: relative;
  display: inline-block;
}

.trigger {
  padding: 2px 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background-color: #ffffff;
  color: #555555;
  font-size: 14px;
  line-height: 1.2;
  cursor: pointer;
}

.trigger:hover {
  background-color: #f2f6fb;
  border-color: #2b77c5;
}

.menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  z-index: 10;
  min-width: 96px;
  padding: 4px 0;
  background-color: #ffffff;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

/* 選項是由呼叫端透過 slot 傳進來的，scoped CSS 要用 :slotted() 才吃得到 */
.menu :slotted(button),
.menu :slotted(a) {
  display: block;
  width: 100%;
  padding: 6px 14px;
  border: 0;
  background: none;
  color: inherit;
  font-size: 13px;
  text-align: left;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
}

.menu :slotted(button:hover),
.menu :slotted(a:hover) {
  background-color: #f2f6fb;
}

.menu :slotted(.danger) {
  color: #c0392b;
}
</style>
