<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
    getCreateOptions,
    createOrder as createOrderApi
} from '../api'

//******新增訂單頁面******

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
        const response = await getCreateOptions()

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

        await createOrderApi(form.value)

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
// 套用目前選擇會員的註冊資料到收件資訊
const applyMemberInfo = () => {

    // 找到目前選擇的會員
    const member = members.value.find(
        member => member.memberId === Number(form.value.memberId)
    )

    if (!member) {
        alert('找不到會員資料')
        return
    }

    // 將會員資料填入訂單收件資訊
    form.value.receiverName = member.memberName
    form.value.receiverPhone = member.phone
    form.value.receiverAddress = member.address
}
</script>



<template>
    <main class="container-fluid px-3 px-lg-4 py-4 order-admin-page">
        <div class="mx-auto order-form-width">

            <!-- 頁面標題 -->
            <header class="mb-4">
                <h1 class="fw-bold mb-1">新增訂單</h1>
                <p class="text-secondary mb-0">
                    Create a new customer order.
                </p>
            </header>

            <!-- 基本資料 -->
            <section class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white border-bottom py-3">
                    <h2 class="h5 fw-bold mb-0">
                        訂單基本資料
                    </h2>
                </div>

                <div class="card-body">
                    <div class="row g-4">

                        <!-- 會員 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                會員
                                <span class="text-danger">*</span>
                            </label>

                            <select v-model="form.memberId" class="form-select">
                                <option value="">
                                    請選擇會員
                                </option>

                                <option v-for="member in members" :key="member.memberId" :value="member.memberId">
                                    {{ member.memberName }}
                                    （ID：{{ member.memberId }}）
                                </option>
                            </select>
                        </div>

                        <!-- 商品 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                商品
                                <span class="text-danger">*</span>
                            </label>

                            <select v-model="productId" class="form-select">
                                <option value="">
                                    請選擇商品
                                </option>

                                <option v-for="product in products" :key="product.productId" :value="product.productId">
                                    {{ product.productName }}
                                    - NT$ {{ formatPrice(product.price) }}
                                </option>
                            </select>
                        </div>

                        <!-- 付款方式 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                付款方式
                                <span class="text-danger">*</span>
                            </label>

                            <select v-model="form.paymentMethod" class="form-select">
                                <option value="">
                                    請選擇付款方式
                                </option>

                                <option value="CREDIT_CARD">
                                    信用卡
                                </option>

                                <option value="CASH_ON_DELIVERY">
                                    貨到付款
                                </option>
                            </select>
                        </div>

                        <!-- 配送方式 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                配送方式
                                <span class="text-danger">*</span>
                            </label>

                            <select v-model="form.shippingMethod" class="form-select">
                                <option value="">
                                    請選擇配送方式
                                </option>

                                <option value="HOME">
                                    宅配
                                </option>

                                <option value="STORE">
                                    超商取貨
                                </option>
                            </select>
                        </div>

                    </div>
                </div>
            </section>

            <!-- 收件資訊 -->
            <section class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
                    <h2 class="h5 fw-bold mb-0">
                        收件資訊
                    </h2>

                    <button class="btn btn-sm btn-outline-primary" type="button" :disabled="!form.memberId"
                        @click="applyMemberInfo">
                        套用會員資料
                    </button>
                </div>

                <div class="card-body">
                    <div class="row g-4">

                        <!-- 收件人 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                收件人
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="form.receiverName" type="text" class="form-control"
                                placeholder="請輸入收件人姓名" />
                        </div>

                        <!-- 電話 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                聯絡電話
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="form.receiverPhone" type="tel" class="form-control"
                                placeholder="例如：0912345678" />
                        </div>

                        <!-- 地址 -->
                        <div class="col-12">
                            <label class="form-label fw-semibold">
                                收件地址
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="form.receiverAddress" type="text" class="form-control"
                                placeholder="請輸入完整收件地址" />
                        </div>

                        <!-- 備註 -->
                        <div class="col-12">
                            <label class="form-label fw-semibold">
                                訂單備註
                            </label>

                            <textarea v-model="form.customerRemark" class="form-control" rows="4"
                                placeholder="可填寫配送或訂單相關備註"></textarea>

                            <div class="form-text">
                                此欄位可留空。
                            </div>
                        </div>

                    </div>
                </div>
            </section>

            <!-- 操作按鈕 -->
            <div class="d-flex flex-column-reverse flex-sm-row justify-content-end gap-2">
                <button class="btn btn-outline-secondary" type="button" @click="goBack">
                    返回訂單列表
                </button>

                <button class="btn btn-primary px-4" type="button" @click="createOrder">
                    建立訂單
                </button>
            </div>

        </div>
    </main>
</template>

<style scoped>
.order-admin-page {
    min-height: 100vh;
    background: #f8f9ff;
}

.order-form-width {
    max-width: 1200px;
}

.card {
    border-radius: 0.75rem;
}

.form-label {
    font-size: 0.9rem;
}

.form-control,
.form-select {
    min-height: 42px;
}
</style>