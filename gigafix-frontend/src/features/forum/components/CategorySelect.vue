<script setup>
import { ref, onMounted } from 'vue'
import { getCategories } from '../api'

const props = defineProps({
  modelValue: { type: [Number, String, null], default: null },
  includeAllOption: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue'])

const categories = ref([])
const errorMessage = ref('')

onMounted(async () => {
  try {
    categories.value = await getCategories()
  } catch {
    errorMessage.value = '分類載入失敗'
  }
})

function handleChange(event) {
  const value = event.target.value
  emit('update:modelValue', value === '' ? null : Number(value))
}
</script>

<template>
  <select class="category-select" :value="modelValue ?? ''" @change="handleChange">
    <option v-if="includeAllOption" value="">全部分類</option>
    <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
      {{ category.name }}
    </option>
  </select>
  <span v-if="errorMessage" class="error">{{ errorMessage }}</span>
</template>

<style scoped>
.category-select {
  padding: 6px 10px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-size: 14px;
}

.error {
  margin-left: 8px;
  font-size: 12px;
  color: #c0392b;
}
</style>
