<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
    getCreateOptions,
    createOrder as createOrderApi
} from '../../api/adminOrderApi'

//******新增訂單頁面******

const router = useRouter()

// 新增訂單表單
const form = ref({
    memberId: '',
    productIds: [],
    paymentMethod: 'CREDIT_CARD',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    shippingMethod: 'HOME',
    customerRemark: ''
})

// 下拉選單資料
const members = ref([])
const products = ref([])

// 使用者目前選到的商品 ID
const productId = ref('')

// 表單欄位錯誤
const errors = ref({})

// 避免重複送出
const submitting = ref(false)
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
const validateForm = () => {
    const newErrors = {}

    // 送出前整理文字格式
    form.value.receiverName =
        (form.value.receiverName || '').trim()

    form.value.receiverPhone =
        (form.value.receiverPhone || '')
            .replace(/[\s-]/g, '')

    form.value.receiverAddress =
        (form.value.receiverAddress || '').trim()

    form.value.customerRemark =
        (form.value.customerRemark || '').trim()

    // 管理員訂單固定使用宅配與信用卡
    form.value.paymentMethod = 'CREDIT_CARD'
    form.value.shippingMethod = 'HOME'

    if (!form.value.memberId) {
        newErrors.memberId = '請選擇會員'
    }

    if (!productId.value) {
        newErrors.productId = '請選擇商品'
    }

    if (!form.value.receiverName) {
        newErrors.receiverName = '請輸入收件人姓名'
    } else if (form.value.receiverName.length > 40) {
        newErrors.receiverName =
            '收件人姓名最多 40 個字'
    }

    if (!form.value.receiverPhone) {
        newErrors.receiverPhone = '請輸入聯絡電話'
    } else if (
        !/^09\d{8}$/.test(form.value.receiverPhone)
    ) {
        newErrors.receiverPhone =
            '手機號碼必須為 09 開頭的 10 碼數字'
    }

    if (!form.value.receiverAddress) {
        newErrors.receiverAddress = '請輸入收件地址'
    } else if (
        form.value.receiverAddress.length > 255
    ) {
        newErrors.receiverAddress =
            '收件地址最多 255 個字'
    }

    if (form.value.customerRemark.length > 255) {
        newErrors.customerRemark =
            '訂單備註最多 255 個字'
    }

    errors.value = newErrors

    return Object.keys(newErrors).length === 0
}
const createOrder = async () => {
    if (submitting.value) {
        return
    }

    if (!validateForm()) {
        return
    }

    submitting.value = true
    errors.value.submit = ''

    try {
        // 後端目前接收 productIds 陣列
        form.value.productIds = [
            Number(productId.value)
        ]

        await createOrderApi({
            ...form.value,
            memberId: Number(form.value.memberId)
        })

        alert('新增訂單成功')

        router.push('/admin/orders')
    } catch (error) {
        console.error('新增訂單失敗：', error)

        errors.value.submit =
            error.response?.data?.message
            || error.response?.data?.error
            || '新增訂單失敗，請確認資料後再試一次'
    } finally {
        submitting.value = false
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

                            <select v-model="form.memberId" class="form-select"
                                :class="{ 'is-invalid': errors.memberId }">
                                <option value="">
                                    請選擇會員
                                </option>

                                <option v-for="member in members" :key="member.memberId" :value="member.memberId">
                                    {{ member.memberName }}
                                    （ID：{{ member.memberId }}）
                                </option>
                            </select>
                            <div v-if="errors.memberId" class="invalid-feedback">
                                {{ errors.memberId }}
                            </div>
                        </div>

                        <!-- 商品 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                商品
                                <span class="text-danger">*</span>
                            </label>

                            <select v-model="productId" class="form-select" :class="{ 'is-invalid': errors.productId }">
                                <option value="">
                                    請選擇商品
                                </option>

                                <option v-for="product in products" :key="product.productId" :value="product.productId">
                                    {{ product.productName }}
                                    - NT$ {{ formatPrice(product.price) }}
                                </option>
                            </select>

                            <div v-if="errors.productId" class="invalid-feedback">
                                {{ errors.productId }}
                            </div>
                        </div>

                        <!-- 付款方式 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                付款方式
                                <span class="text-danger">*</span>
                            </label>

                            <input type="text" class="form-control bg-light" value="信用卡" readonly>

                            <div class="form-text">
                                訂單建立後，由會員至自己的訂單頁完成付款。
                            </div>
                        </div>

                        <!-- 配送方式 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                配送方式
                                <span class="text-danger">*</span>
                            </label>

                            <input type="text" class="form-control bg-light" value="宅配" readonly>

                            <div class="form-text">
                                管理員建立的訂單僅支援宅配。
                            </div>
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
                                :class="{ 'is-invalid': errors.receiverName }" maxlength="40" autocomplete="name"
                                placeholder="請輸入收件人姓名">

                            <div v-if="errors.receiverName" class="invalid-feedback">
                                {{ errors.receiverName }}
                            </div>
                        </div>

                        <!-- 電話 -->
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">
                                聯絡電話
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="form.receiverPhone" type="tel" class="form-control"
                                :class="{ 'is-invalid': errors.receiverPhone }" inputmode="numeric" maxlength="12"
                                autocomplete="tel" placeholder="例如：0912345678">

                            <div v-if="errors.receiverPhone" class="invalid-feedback">
                                {{ errors.receiverPhone }}
                            </div>

                            <div class="form-text">
                                可輸入 0912345678 或 0912-345-678。
                            </div>
                        </div>

                        <!-- 地址 -->
                        <div class="col-12">
                            <label class="form-label fw-semibold">
                                收件地址
                                <span class="text-danger">*</span>
                            </label>

                            <input v-model="form.receiverAddress" type="text" class="form-control"
                                :class="{ 'is-invalid': errors.receiverAddress }" maxlength="255"
                                autocomplete="street-address" placeholder="請輸入完整收件地址">

                            <div v-if="errors.receiverAddress" class="invalid-feedback">
                                {{ errors.receiverAddress }}
                            </div>
                        </div>

                        <!-- 備註 -->
                        <div class="col-12">
                            <label class="form-label fw-semibold">
                                訂單備註
                            </label>

                            <textarea v-model="form.customerRemark" class="form-control"
                                :class="{ 'is-invalid': errors.customerRemark }" rows="4" maxlength="255"
                                placeholder="可填寫配送或訂單相關備註"></textarea>

                            <div v-if="errors.customerRemark" class="invalid-feedback">
                                {{ errors.customerRemark }}
                            </div>

                            <div class="form-text d-flex justify-content-between">
                                <span>此欄位可留空。</span>

                                <span>
                                    {{ form.customerRemark.length }} / 255
                                </span>
                            </div>
                        </div>

                    </div>
                </div>
            </section>

            <div v-if="errors.submit" class="alert alert-danger">
                {{ errors.submit }}
            </div>
            
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
