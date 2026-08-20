<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const props = defineProps({
    orderId: String
})

const router = useRouter()

// 物流單號
const trackingNumber = ref(
    `GF-${Date.now()}`
)

// 確認出貨
const submitShip = async () => {

    if (!trackingNumber.value.trim()) {
        alert('請輸入物流單號')
        return
    }

    const confirmed = confirm(
        `確定要將訂單 ${props.orderId} 設為已出貨嗎？`
    )

    if (!confirmed) {
        return
    }

    try {

        const response = await axios.post(
            `/api/admin/orders/${props.orderId}/ship`,
            {
                trackingNumber: trackingNumber.value
            }
        )

        console.log('出貨成功：', response.data)

        alert('出貨成功')

        router.push('/admin/orders')

    } catch (error) {

        console.error('出貨失敗：', error)

        alert('出貨失敗')
    }
}

// 返回訂單列表
const backToOrders = () => {
    router.push('/admin/orders')
}
</script>

<template>
    <div>
        <h1>訂單出貨</h1>

        <p>訂單 ID：{{ orderId }}</p>

        <div>
            <label>物流單號：</label>

            <input v-model="trackingNumber" type="text" readonly />
        </div>

        <br>

        <button @click="submitShip">
            確認出貨
        </button>

        <button @click="backToOrders">
            返回訂單列表
        </button>
    </div>
</template>

<style scoped></style>