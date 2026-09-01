<script setup>
//會員分布圖：後端只回傳所有會員(含地址)，縣市的比對/計數完全在前端做，
//反正畫地圖時本來就要把taiwan-atlas的縣市都跑一遍組成series.data，順便查表把對應的會員數帶進去即可，不需要後端另外維護一份縣市清單
import { computed } from 'vue'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { MapChart } from 'echarts/charts'
import { TooltipComponent, VisualMapComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { feature } from 'topojson-client'
import countiesTopoJson from 'taiwan-atlas/counties-10t.json' //用一般經緯度版本，mercator投影版拿來畫地圖會整個上下顛倒
import { parseTaiwanAddress } from '@/static/taiwanDistricts'

echarts.use([CanvasRenderer, MapChart, TooltipComponent, VisualMapComponent])

const props = defineProps({
    members: { type: Array, default: () => [] } //後台會員列表(含address欄位)，跟會員列表表格共用同一份資料，不重複打API
})

//離島(澎湖/金門/連江)地理位置離本島很遠，畫成實際地圖形狀不是形狀被拉開、就是跟圖例位置打架，
//改成不畫地圖形狀，直接用色塊卡片顯示在旁邊的空白處，底色跟地圖熱點同一套深淺，一樣看得出人數多寡
const outlyingCounties = ['澎湖縣', '金門縣', '連江縣']

//把taiwan-atlas的TopoJSON轉成geoJSON，每個縣市的name統一成COUNTYNAME，
//這個轉換跟地圖資料一樣是固定不變的，只需要算一次，不用放進computed
const countiesGeoJson = feature(countiesTopoJson, countiesTopoJson.objects.counties)
countiesGeoJson.features.forEach(f => {
    f.properties.name = f.properties.COUNTYNAME
})

//地圖只畫本島(排除3個離島)，本島的邊界框才不會被離島的地理位置拉遠、拉小
const mainlandGeoJson = { ...countiesGeoJson, features: countiesGeoJson.features.filter(f => !outlyingCounties.includes(f.properties.name)) }
echarts.registerMap('taiwan-mainland', mainlandGeoJson)

//依會員地址統計每個縣市有幾位會員：地址格式不符或縣市還沒選(city是空的)就不列入統計
const cityCounts = computed(() => {
    const counts = {}
    for (const member of props.members) {
        const { city } = parseTaiwanAddress(member.address)
        if (!city) continue
        counts[city] = (counts[city] || 0) + 1
    }
    return counts
})

//畫地圖一定要把本島的縣市都跑一遍組成series.data，這裡就順便查cityCounts把對應的會員數帶進去，沒有會員的縣市數量是0
const mainlandSeriesData = computed(() =>
    mainlandGeoJson.features.map(f => ({
        name: f.properties.name,
        value: cityCounts.value[f.properties.name] || 0
    }))
)

//maxCount要含離島一起算，色階/色塊卡片顏色才會是同一套基準
const maxCount = computed(() => Math.max(1, ...countiesGeoJson.features.map(f => cityCounts.value[f.properties.name] || 0)))

//色塊卡片跟地圖visualMap共用同一組色階(淺到深)，數值0是最淺色，maxCount是最深色
const heatColorStops = ['#eef4fb', '#7fb1e0', '#2b77c5', '#1d324b']
const hexToRgb = (hex) => {
    const clean = hex.replace('#', '')
    return [0, 2, 4].map(i => parseInt(clean.slice(i, i + 2), 16))
}
const mixHexColors = (hexA, hexB, ratio) => {
    const [r1, g1, b1] = hexToRgb(hexA)
    const [r2, g2, b2] = hexToRgb(hexB)
    const mix = (a, b) => Math.round(a + (b - a) * ratio)
    return `rgb(${mix(r1, r2)}, ${mix(g1, g2)}, ${mix(b1, b2)})`
}
const getHeatColor = (value) => {
    const ratio = Math.min(1, value / maxCount.value)
    const scaledIndex = ratio * (heatColorStops.length - 1)
    const lowerIndex = Math.floor(scaledIndex)
    const upperIndex = Math.min(heatColorStops.length - 1, lowerIndex + 1)
    return mixHexColors(heatColorStops[lowerIndex], heatColorStops[upperIndex], scaledIndex - lowerIndex)
}
//底色深的時候文字要換白色才看得清楚
const getTextColor = (value) => (value / maxCount.value > 0.55 ? '#ffffff' : '#1d324b')

//依會員人數由多到少排序，人數多的排前面
const outlyingCards = computed(() =>
    outlyingCounties
        .map(county => ({ name: county, count: cityCounts.value[county] || 0 }))
        .sort((a, b) => b.count - a.count)
)

const chartOption = computed(() => ({
    tooltip: {
        trigger: 'item',
        formatter: (params) => `${params.name}<br/>會員人數：${params.value ?? 0} 人`
    },
    visualMap: {
        min: 0,
        max: maxCount.value,
        left: 'left',
        bottom: 20,
        text: ['多', '少'],
        calculable: true,
        inRange: { color: heatColorStops }
    },
    series: [{
        type: 'map',
        map: 'taiwan-mainland',
        roam: false, //不開放縮放/拖曳，避免使用者滑一滑把地圖滑出視窗或跑版
        layoutCenter: ['50%', '50%'],
        layoutSize: '92%', //依卡片可用空間自動縮放置中
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12 } },
        itemStyle: { borderColor: '#8fa8c4', borderWidth: 1.1 }, //輪廓顏色夠明顯，不會跟淺色底混在一起看不出海岸線
        data: mainlandSeriesData.value
    }]
}))
</script>

<template>
    <div class="card shadow mb-4">
        <div class="card-header py-3">
            <h6 class="m-0 fw-bold text-primary">會員分布圖</h6>
        </div>
        <div class="card-body d-flex gap-3">
            <v-chart class="chart" :option="chartOption" autoresize />
            <!-- 離島不畫地圖形狀，改用色塊卡片顯示在右側空白處，依人數多到少由上往下排 -->
            <div class="outlying-panel">
                <div
                    v-for="card in outlyingCards"
                    :key="card.name"
                    class="outlying-card"
                    :style="{ backgroundColor: getHeatColor(card.count), color: getTextColor(card.count) }"
                >
                    <div class="outlying-name">{{ card.name }}</div>
                    <div class="outlying-count">{{ card.count }} 人</div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.outlying-panel {
    flex: 0 0 120px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 14px;
}

.outlying-card {
    border-radius: 0.5rem;
    padding: 0.75rem 0.5rem;
    text-align: center;
    border: 1px solid #d9e2ec;
}

.outlying-name {
    font-size: 0.85rem;
    font-weight: 600;
}

.outlying-count {
    font-size: 1.1rem;
    font-weight: 700;
    margin-top: 0.2rem;
}

.chart {
    flex: 1;
    min-width: 0;
    height: 420px;
}
</style>
