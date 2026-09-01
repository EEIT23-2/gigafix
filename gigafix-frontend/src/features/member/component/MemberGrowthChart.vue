<script setup>
//每月會員註冊數量：X軸是月份(yyyy-MM)，Y軸是當月新增的會員數(不是累計總數)，資料來自後端GET /api/admin/members/stats/monthly-registrations
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const loading = ref(true)
const errorMsg = ref('')
const chartOption = ref({
    tooltip: { trigger: 'axis', valueFormatter: (value) => `${value} 人` },
    grid: { left: 48, right: 24, top: 24, bottom: 32 },
    xAxis: { type: 'category', data: [], boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1, name: '註冊人數' },
    series: [{
        type: 'line',
        name: '當月新增註冊人數',
        data: [],
        smooth: true,
        showSymbol: false,
        areaStyle: { color: 'rgba(43, 119, 197, 0.15)' },
        lineStyle: { color: '#2b77c5', width: 2 },
        itemStyle: { color: '#2b77c5' }
    }]
})

//====取得每月會員註冊數量資料====
const fetchMonthlyRegistrations = async () => {
    loading.value = true
    errorMsg.value = ''
    try {
        const rep = await axios.get('/api/admin/members/stats/monthly-registrations')
        chartOption.value.xAxis.data = rep.data.map(point => point.month)
        chartOption.value.series[0].data = rep.data.map(point => point.count)
    } catch (err) {
        errorMsg.value = '每月註冊數量讀取失敗，請稍後再試'
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchMonthlyRegistrations()
})
</script>

<template>
    <div class="card shadow mb-4">
        <div class="card-header py-3">
            <h6 class="m-0 fw-bold text-primary">每月會員註冊數量</h6>
        </div>
        <div class="card-body">
            <p v-if="errorMsg" class="text-danger mb-0">{{ errorMsg }}</p>
            <v-chart v-else class="chart" :option="chartOption" :loading="loading" autoresize />
        </div>
    </div>
</template>

<style scoped>
.chart {
    height: 420px;
}
</style>
