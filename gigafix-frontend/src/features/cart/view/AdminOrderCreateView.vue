<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// 新增訂單表單
const form = ref({
    memberId: '',
    productIds: [],
    paymentMethod: '',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    shippingMethod: '',
    customerRemark: ''
})

// 下拉選單資料
const members = ref([])
const products = ref([])

// 使用者目前選到的商品 ID
const productId = ref('')

// 取得新增訂單需要的選項
const loadCreateOptions = async () => {
    try {
        const response = await axios.get(
            '/api/admin/orders/create-options'
        )

        members.value = response.data.members
        products.value = response.data.products

        console.log('會員選項：', members.value)
        console.log('商品選項：', products.value)

    } catch (error) {
        console.error('取得新增訂單選項失敗：', error)
    }
}
const createOrder = async () => {

    // 基本防呆
    if (!form.value.memberId) {
        alert('請選擇會員')
        return
    }

    if (!productId.value) {
        alert('請選擇商品')
        return
    }

    if (!form.value.paymentMethod) {
        alert('請選擇付款方式')
        return
    }

    if (!form.value.receiverName) {
        alert('請輸入收件人')
        return
    }

    if (!form.value.receiverPhone) {
        alert('請輸入電話')
        return
    }

    if (!form.value.receiverAddress) {
        alert('請輸入地址')
        return
    }

    if (!form.value.shippingMethod) {
        alert('請選擇配送方式')
        return
    }

    try {

        // 後端需要 productIds 陣列
        form.value.productIds = [
            Number(productId.value)
        ]

        console.log('建立訂單 Request：', form.value)

        await axios.post(
            '/api/admin/orders',
            form.value
        )

        alert('新增訂單成功')

        router.push('/admin/orders')

    } catch (error) {
        console.error('新增訂單失敗：', error)

        alert('新增訂單失敗')
    }
}
onMounted(() => {
    loadCreateOptions()
})

const goBack = () => {
    router.push('/admin/orders')
}
const formatPrice = (price) => {
    if (price == null) {
        return '0'
    }

    return Number(price).toLocaleString('zh-TW')
}
</script>



<template>
    <div>
        <h1>新增訂單</h1>

        <div>
            <label>會員：</label>

            <select v-model="form.memberId">
                <option value="">請選擇會員</option>

                <option v-for="member in members" :key="member.memberId" :value="member.memberId">
                    {{ member.memberName }}（ID：{{ member.memberId }}）
                </option>
            </select>
        </div>

        <div>
            <label>商品：</label>

            <select v-model="productId">
                <option value="">請選擇商品</option>

                <option v-for="product in products" :key="product.productId" :value="product.productId">
                    {{ product.productName }}
                    - ${{ formatPrice(product.price) }}
                </option>
            </select>
        </div>
        <div>
            <label>付款方式：</label>

            <select v-model="form.paymentMethod">
                <option value="">請選擇付款方式</option>
                <option value="CREDIT_CARD">信用卡</option>
                <option value="CASH_ON_DELIVERY">貨到付款</option>
            </select>
        </div>

        <div>
            <label>收件人：</label>
            <input v-model="form.receiverName">
        </div>

        <div>
            <label>電話：</label>
            <input v-model="form.receiverPhone">
        </div>

        <div>
            <label>地址：</label>
            <input v-model="form.receiverAddress">
        </div>

        <div>
            <label>配送方式：</label>

            <select v-model="form.shippingMethod">
                <option value="">請選擇配送方式</option>
                <option value="HOME">宅配</option>
                <option value="STORE">超商取貨</option>
            </select>
        </div>

        <div>
            <label>備註：</label>
            <input v-model="form.customerRemark">
        </div>

        <button @click="createOrder">
            新增訂單
        </button>

        <button @click="goBack">
            返回訂單列表
        </button>
    </div>
</template>