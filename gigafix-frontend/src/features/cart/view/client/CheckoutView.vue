<script setup>
import { computed, onMounted, ref } from 'vue'
import { useMemberAuth } from '../../composables/useMemberAuth'
import { useRouter, useRoute } from 'vue-router'
import CheckoutForm from '../../components/CheckoutForm.vue'
import { getCartItems } from '../../api/cartApi'
import { createOrder } from '../../api/memberOrderApi'
import { getAvailableCoupons } from '../../api/couponApi'
import { redirectToEcpayStoreMap } from '../../api/ecpayLogisticsApi'

const router = useRouter()
const route = useRoute()
const CHECKOUT_DRAFT_KEY = 'gigafix_checkout_draft'
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
// 處理超商品牌變更
const handleStoreTypeChange = () => {
    selectedStore.value = {
        cvsType: selectedStore.value.cvsType,
        storeId: '',
        storeName: '',
        storeAddress: ''
    }

    saveCheckoutDraft()

}
const handleSelectStore = () => {
    if (!selectedStore.value.cvsType) {
        errorMessage.value =
            '請先選擇超商品牌'
        return
    }

    errorMessage.value = ''

    saveCheckoutDraft()

    redirectToEcpayStoreMap(
        selectedStore.value.cvsType
    )
}
const applyStoreCallbackFromRoute = async () => {
    const storeType = route.query.storeType
    const storeId = route.query.storeId
    const storeName = route.query.storeName
    const storeAddress = route.query.storeAddress

    const hasStoreCallback =
        typeof storeType === 'string'
        || typeof storeId === 'string'
        || typeof storeName === 'string'
        || typeof storeAddress === 'string'

    if (!hasStoreCallback) {
        return
    }

    if (
        typeof storeType !== 'string'
        || typeof storeId !== 'string'
        || typeof storeName !== 'string'
        || typeof storeAddress !== 'string'
    ) {
        errorMessage.value =
            'ECPay 回傳的門市資料不完整'

        return
    }

    const allowedStoreTypes = [
        'UNIMART',
        'FAMI',
        'HILIFE'
    ]

    if (!allowedStoreTypes.includes(storeType)) {
        errorMessage.value =
            'ECPay 回傳了不支援的超商品牌'

        return
    }

    selectedStore.value = {
        cvsType: storeType,
        storeId,
        storeName,
        storeAddress
    }

    form.value.shippingMethod = 'STORE'

    // 把已選門市也寫回 Draft
    // 等一下清掉 URL 後，F5 仍然可以恢復
    saveCheckoutDraft()

    // 清除 Callback query
    // 避免重新整理一直重複處理
    const {
        storeType: _storeType,
        storeId: _storeId,
        storeName: _storeName,
        storeAddress: _storeAddress,
        ...remainingQuery
    } = route.query

    await router.replace({
        path: route.path,
        query: remainingQuery
    })
}
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
// 解析路由中的購物車商品ID列表
const parseRouteCartItemIds = () => {
    const raw = route.query.cartItemIds

    if (!raw) {
        return []
    }

    return String(raw)
        .split(',')
        .map(id => Number(id))
        .filter(id => Number.isInteger(id) && id > 0)
}
//選擇的購物車商品ID列表
const selectedCartItemIds = ref(
    parseRouteCartItemIds()
)
// 比較兩個購物車商品ID列表是否相同
const hasSameCartItemIds = (left, right) => {
    const a = [...left].sort((x, y) => x - y)
    const b = [...right].sort((x, y) => x - y)

    return (
        a.length === b.length
        && a.every((id, index) => id === b[index])
    )
}
//新增保存 Checkout Draft
const saveCheckoutDraft = () => {
    const draft = {
        cartItemIds: [
            ...selectedCartItemIds.value
        ],

        couponCode:
            selectedCouponCode.value || '',

        form: {
            ...form.value
        },

        selectedStore: {
            ...selectedStore.value
        }
    }

    sessionStorage.setItem(
        CHECKOUT_DRAFT_KEY,
        JSON.stringify(draft)
    )
}
const restoreCheckoutDraft = () => {
    const raw = sessionStorage.getItem(
        CHECKOUT_DRAFT_KEY
    )

    if (!raw) {
        return
    }

    try {
        const draft = JSON.parse(raw)

        const draftCartItemIds = Array.isArray(
            draft.cartItemIds
        )
            ? draft.cartItemIds
                .map(id => Number(id))
                .filter(
                    id =>
                        Number.isInteger(id)
                        && id > 0
                )
            : []

        const routeCartItemIds =
            parseRouteCartItemIds()

        // 正常從購物車進 Checkout
        if (routeCartItemIds.length > 0) {
            selectedCartItemIds.value =
                routeCartItemIds

            // URL 商品和舊草稿不同
            // 代表這是新的結帳流程
            if (
                !hasSameCartItemIds(
                    routeCartItemIds,
                    draftCartItemIds
                )
            ) {
                sessionStorage.removeItem(
                    CHECKOUT_DRAFT_KEY
                )

                return
            }
        } else {
            // ECPay 回來時 URL 可能沒有 cartItemIds
            selectedCartItemIds.value =
                draftCartItemIds
        }

        // URL 有 couponCode 時，以 URL 為主
        // 沒有才使用 Draft
        if (
            typeof route.query.couponCode
            !== 'string'
        ) {
            selectedCouponCode.value =
                draft.couponCode || ''
        }

        if (
            draft.form
            && typeof draft.form === 'object'
        ) {
            form.value = {
                ...form.value,
                ...draft.form
            }
        }

        if (
            draft.selectedStore
            && typeof draft.selectedStore
            === 'object'
        ) {
            selectedStore.value = {
                ...selectedStore.value,
                ...draft.selectedStore
            }
        }
    } catch (error) {
        console.error(
            'Checkout Draft 讀取失敗',
            error
        )

        sessionStorage.removeItem(
            CHECKOUT_DRAFT_KEY
        )
    }
}
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
    if (
        form.value.shippingMethod === 'STORE'
        && (
            !selectedStore.value.cvsType
            || !selectedStore.value.storeId
            || !selectedStore.value.storeName
            || !selectedStore.value.storeAddress
        )
    ) {
        errorMessage.value =
            '請先選擇完整的超商取貨門市'

        return
    }
    submitting.value = true
    errorMessage.value = ''

    try {
        const isStore =
            form.value.shippingMethod === 'STORE'
        const response = await createOrder({
            ...form.value,
            cartItemIds: selectedCartItemIds.value,
            couponCode: selectedCouponCode.value || null,
            storeType:isStore? selectedStore.value.cvsType: null,
            storeId:isStore? selectedStore.value.storeId: null,
            storeName:isStore? selectedStore.value.storeName: null,
            storeAddress:isStore? selectedStore.value.storeAddress: null

        })
        const orderId = response.data.orderId

        sessionStorage.removeItem(
            CHECKOUT_DRAFT_KEY
        )

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
    //復原 Checkout Draft
    restoreCheckoutDraft()

    // 套用 ECPay 超商選店回傳
    await applyStoreCallbackFromRoute()

    //再檢查是否有選取的購物車商品
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
        <CheckoutForm :form="form" :selected-store="selectedStore" :submitting="submitting"
            :disabled="selectedItems.length === 0" @submit="handleSubmit" @select-store="handleSelectStore"
            @store-type-change="handleStoreTypeChange" />
    </div>
</template>