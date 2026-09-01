<script setup>
//台灣縣市/行政區+詳細地址的共用選擇元件，註冊、修改個人資料(前台)、後台修改會員資料都共用同一顆
//單純的三個欄位：v-model:city、v-model:district、v-model:detail，是否有值、要不要組合成完整地址字串都交給外層自己判斷/處理
import { computed } from 'vue'
import { taiwanDistricts, getDistrictsByCity } from '@/static/taiwanDistricts'

const props = defineProps({
    city: { type: String, default: '' },
    district: { type: String, default: '' },
    detail: { type: String, default: '' },
    disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:city', 'update:district', 'update:detail'])

//該縣市對應的行政區選項，縣市還沒選之前是空陣列
const districtOptions = computed(() => getDistrictsByCity(props.city))

//切換縣市要清空原本選的行政區，避免殘留上一個縣市底下的行政區選項
const onCityChange = (event) => {
    emit('update:city', event.target.value)
    emit('update:district', '')
}
</script>

<template>
    <div class="address-select">
        <div class="address-select-row">
            <select class="form-select" :value="city" :disabled="disabled" @change="onCityChange($event)">
                <option value="" disabled>請選擇縣市</option>
                <option v-for="item in taiwanDistricts" :key="item.city" :value="item.city">{{ item.city }}</option>
            </select>
            <select class="form-select" :value="district" :disabled="disabled || !city" @change="emit('update:district', $event.target.value)">
                <option value="" disabled>請選擇行政區</option>
                <option v-for="d in districtOptions" :key="d" :value="d">{{ d }}</option>
            </select>
        </div>
        <input type="text" class="form-control mt-2" :value="detail" :disabled="disabled" placeholder="請輸入詳細地址(路名、門牌號等)" @input="emit('update:detail', $event.target.value)">
    </div>
</template>

<style scoped>
.address-select-row {
    display: flex;
    gap: 0.5rem;
}

.address-select-row .form-select {
    flex: 1;
    min-width: 0; /* 避免select內建的最小寬度撐爆flex，兩個select才能真的平分寬度 */
}

.address-select .form-select,
.address-select .form-control {
    border-radius: 0.5rem;
}
</style>
