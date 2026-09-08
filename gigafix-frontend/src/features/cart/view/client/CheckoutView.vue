<script setup>
import { computed, onMounted, ref } from 'vue'
import { useMemberAuth } from '../../composables/useMemberAuth'
import { useRouter, useRoute } from 'vue-router'
import CheckoutForm from '../../components/CheckoutForm.vue'
import { getCartItems } from '../../api/cartApi'
import { createOrder } from '../../api/memberOrderApi'
import { getAvailableCoupons } from '../../api/couponApi'

const router = useRouter()
const route = useRoute()

const cartItems = ref([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const coupons = ref([])
// 選取的優惠券代碼 (從路由查詢參數中取得)
const selectedCouponCode = ref(
    typeof route.query.couponCode === 'string'
        ? route.query.couponCode
        : ''
)
// 選取的取貨門市資訊
const selectedStore = ref({
    cvsType: '',
    storeId: '',
    storeName: '',
    storeAddress: ''
})
// 結帳表單資料
const form = ref({
    paymentMethod: 'CREDIT_CARD',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    shippingMethod: 'HOME',
    customerRemark: ''
})
// 載入購物車商品列表
const loadCart = async () => {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await getCartItems()
        cartItems.value = response.data

        if (cartItems.value.length === 0) {
            errorMessage.value = '購物車是空的，無法結帳'
            return
        }
        if (selectedItems.value.length === 0) {
            errorMessage.value = '找不到可結帳的商品'
        }
    } catch (error) {
        console.error(error)
        errorMessage.value = '購物車載入失敗'
    } finally {
        loading.value = false
    }
}
// 載入可用的優惠券
const loadCoupons = async () => {
    try {
        const response = await getAvailableCoupons()
        coupons.value = response.data
    } catch (error) {
        console.error('優惠券載入失敗', error)
        coupons.value = []
    }
}
// 選取的優惠券
const selectedCoupon = computed(() => {
    return coupons.value.find(
        coupon => coupon.couponCode === selectedCouponCode.value
    ) || null
})
// 選取的優惠券折扣金額
const couponDiscount = computed(() => {
    if (selectedTotalAmount.value <= 0) {
        return 0
    }

    return selectedCoupon.value?.discountAmount || 0
})
// 最終結帳金額 (扣除優惠券折扣後)
const finalAmount = computed(() => {
    return Math.max(
        selectedTotalAmount.value - couponDiscount.value,
        0
    )
})
//選取的購物車商品ID表
const selectedCartItemIds = computed(() => {
    const raw = route.query.cartItemIds

    if (!raw) {
        return []
    }

    return String(raw)
        .split(',')
        .map(id => Number(id))
        .filter(id => Number.isInteger(id) && id > 0)
})
// 選取的購物車商品列表
const selectedItems = computed(() => {
    return cartItems.value.filter(item =>
        selectedCartItemIds.value.includes(item.cartItemId)
    )
})
const selectedTotalAmount = computed(() => {
    return selectedItems.value.reduce((total, item) => {
        return total + (item.price || 0)
    }, 0)
})
// 提交結帳表單
const handleSubmit = async () => {
    if (selectedCartItemIds.value.length === 0) {
        errorMessage.value = '請至少選擇一項商品進行結帳'
        return
    }

    submitting.value = true
    errorMessage.value = ''

    try {
        const response = await createOrder({
            ...form.value,
            cartItemIds: selectedCartItemIds.value,
            couponCode: selectedCouponCode.value || null

        })
        const orderId = response.data.orderId

        router.push(`/member-center/orders/${orderId}`)
    } catch (error) {
        console.error(error)
        errorMessage.value =
            error.response?.data?.message || '建立訂單失敗'
    } finally {
        submitting.value = false
    }
}

// 會員驗證與載入購物車及優惠券
const { checkMemberLogin } = useMemberAuth()

onMounted(async () => {
    const loggedIn = await checkMemberLogin()

    if (!loggedIn) {
        return
    }

    if (selectedCartItemIds.value.length === 0) {
        alert('請先選擇要結帳的商品')
        router.push('/cart')
        return
    }

    await loadCart()
    await loadCoupons()
})
</script>

<template>
    <div class="container py-5">
        <h2 class="mb-4">結帳</h2>

        <div v-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
        </div>

        <div v-if="loading">
            載入中...
        </div>
        <div v-if="!loading && selectedItems.length > 0" class="card mb-4">
            <div class="card-body">
                <h5 class="card-title mb-3">本次結帳商品</h5>

                <div v-for="item in selectedItems" :key="item.cartItemId"
                    class="d-flex justify-content-between border-bottom py-2">
                    <span>
                        {{ item.productName }}
                    </span>

                    <strong>
                        NT$ {{ item.price?.toLocaleString() }}
                    </strong>
                </div>

                <div class="d-flex justify-content-between mt-3 mb-2">
                    <span>已選商品</span>
                    <strong>{{ selectedItems.length }} 件</strong>
                </div>

                <div class="d-flex justify-content-between mb-2">
                    <span>商品小計</span>
                    <strong>
                        NT$ {{ selectedTotalAmount.toLocaleString() }}
                    </strong>
                </div>

                <div class="d-flex justify-content-between">
                    <span>運費</span>
                    <strong>免運</strong>
                </div>

                <div class="mt-4">
                    <label class="form-label fw-bold">
                        優惠券
                    </label>

                    <select v-model="selectedCouponCode" class="form-select">
                        <option value="">
                            不使用優惠券
                        </option>

                        <option v-for="coupon in coupons" :key="coupon.couponCode" :value="coupon.couponCode">
                            {{ coupon.couponName }}
                            - 折 NT$ {{ coupon.discountAmount.toLocaleString() }}
                        </option>
                    </select>
                </div>

                <hr>

                <div class="d-flex justify-content-between">
                    <strong>應付金額</strong>

                    <strong class="fs-5">
                        NT$ {{ finalAmount.toLocaleString() }}
                    </strong>
                </div>

            </div>
        </div>
        <CheckoutForm :form="form" :submitting="submitting" :disabled="selectedItems.length === 0"
            @submit="handleSubmit" />
    </div>
</template>