<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useMemberAuth } from '../../composables/useMemberAuth'
import { useRouter, useRoute } from 'vue-router'
import CheckoutForm from '../../components/CheckoutForm.vue'
import { getCartItems } from '../../api/cartApi'
import { createOrder } from '../../api/memberOrderApi'
import { getAvailableCoupons } from '../../api/couponApi'
import { redirectToEcpayStoreMap } from '../../api/ecpayLogisticsApi'
import { storeToRefs } from 'pinia'
import { useFetchMemberInfoStore } from '@/stores/member'

const router = useRouter()
const route = useRoute()
const memberStore = useFetchMemberInfoStore()
const { memberInfo } = storeToRefs(memberStore)
const sameAsMember = ref(false)
const CHECKOUT_DRAFT_KEY = 'gigafix_checkout_draft'
const cartItems = ref([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const coupons = ref([])
const failedImageIds = ref(new Set())
// 處理購物車商品圖片載入失敗的邏輯
const imageLoadFailed = (cartItemId) => {
    return failedImageIds.value.has(cartItemId)
}
// 處理購物車商品圖片載入失敗的邏輯
const handleImageError = (cartItemId) => {
    failedImageIds.value.add(cartItemId)
}
// 格式化價格
const formatPrice = (value) => {
    return `NT$ ${Number(value || 0)
        .toLocaleString('zh-TW')}`
}
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
// 將會員註冊資料帶入收件人欄位
const applyMemberReceiverData = () => {
    const member = memberInfo.value

    if (!member) {
        sameAsMember.value = false
        errorMessage.value =
            '目前無法取得會員資料'

        return
    }

    if (!member.realName || !member.phone) {
        sameAsMember.value = false
        errorMessage.value =
            '會員姓名或電話資料不完整，請先更新會員資料'

        return
    }

    if (
        form.value.shippingMethod === 'HOME'
        && !member.address
    ) {
        sameAsMember.value = false
        errorMessage.value =
            '會員地址資料不完整，請先更新會員資料'

        return
    }

    form.value.receiverName = member.realName
    form.value.receiverPhone = member.phone

    // 宅配才帶入會員註冊地址
    if (form.value.shippingMethod === 'HOME') {
        form.value.receiverAddress = member.address
    }

    errorMessage.value = ''
    saveCheckoutDraft()
}

// 處理勾選狀態
const handleSameAsMemberChange = (checked) => {
    sameAsMember.value = checked

    if (checked) {
        applyMemberReceiverData()
    } else {
        saveCheckoutDraft()
    }
}

// 已勾選時，切換回宅配要自動補上會員地址
watch(
    () => form.value.shippingMethod,
    () => {
        if (sameAsMember.value) {
            applyMemberReceiverData()
        }
    }
)
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

        sameAsMember: sameAsMember.value,

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

        sameAsMember.value =
            draft.sameAsMember === true

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
            storeType: isStore ? selectedStore.value.cvsType : null,
            storeId: isStore ? selectedStore.value.storeId : null,
            storeName: isStore ? selectedStore.value.storeName : null,
            storeAddress: isStore ? selectedStore.value.storeAddress : null

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
    <main class="checkout-page">
        <!-- 頁面標題 -->
        <header class="checkout-page-header">
            <RouterLink :to="{ name: 'cart' }" class="back-link">
                <i class="bi bi-arrow-left"></i>
                返回購物車
            </RouterLink>

            <div>
                <h1>結帳</h1>

                <p>
                    確認收件資料、配送方式與訂單金額
                </p>
            </div>
        </header>

        <div v-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
        </div>

        <!-- 載入狀態 -->
        <div v-if="loading" class="checkout-message">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">
                    載入中
                </span>
            </div>

            <p>正在載入結帳資料...</p>
        </div>

        <div v-else-if="selectedItems.length > 0" class="checkout-layout">
            <!-- 左側表單 -->
            <section class="checkout-form-panel">
                <header class="panel-header">
                    <div>
                        <h2>收件與付款資訊</h2>

                        <p>
                            請確認以下資料正確無誤
                        </p>
                    </div>
                </header>

                <div class="checkout-form-body">
                    <CheckoutForm :form="form" :selected-store="selectedStore" :submitting="submitting" :disabled="selectedItems.length === 0
                        " :same-as-member="sameAsMember" @submit="handleSubmit" @select-store="handleSelectStore"
                        @store-type-change="
                            handleStoreTypeChange
                        " @same-as-member-change="
                            handleSameAsMemberChange
                        " />
                </div>
            </section>

            <!-- 右側訂單摘要 -->
            <aside class="order-summary-card">
                <header class="summary-header">
                    <h2>訂單摘要</h2>

                    <span>
                        {{ selectedItems.length }} 件商品
                    </span>
                </header>

                <!-- 商品 -->
                <div class="summary-products">
                    <article v-for="item in selectedItems" :key="item.cartItemId" class="summary-product">
                        <div class="product-image-wrapper">
                            <img v-if="
                                item.imageUrl
                                && !imageLoadFailed(
                                    item.cartItemId
                                )
                            " :src="item.imageUrl" :alt="item.productName" class="product-image" loading="lazy" @error="
                                handleImageError(
                                    item.cartItemId
                                )
                                ">

                            <div v-else class="product-placeholder" aria-label="商品暫無圖片">
                                <i class="bi bi-image"></i>
                            </div>
                        </div>

                        <div class="product-information">
                            <RouterLink class="product-name" :to="{
                                name: 'mall-detail',
                                params: {
                                    productId:
                                        item.productId
                                }
                            }">
                                {{ item.productName }}
                            </RouterLink>

                            <strong class="product-price">
                                {{ formatPrice(item.price) }}
                            </strong>
                        </div>
                    </article>
                </div>

                <!-- 優惠券 -->
                <div class="coupon-section">
                    <label for="checkout-coupon" class="form-label">
                        優惠券
                    </label>

                    <select id="checkout-coupon" v-model="selectedCouponCode" class="form-select">
                        <option value="">
                            不使用優惠券
                        </option>

                        <option v-for="coupon in coupons" :key="coupon.couponCode" :value="coupon.couponCode">
                            {{ coupon.couponName }}
                            －折
                            {{
                                formatPrice(
                                    coupon.discountAmount
                                )
                            }}
                        </option>
                    </select>
                </div>

                <!-- 金額 -->
                <div class="amount-section">
                    <div class="amount-row">
                        <span>商品小計</span>

                        <strong>
                            {{
                                formatPrice(
                                    selectedTotalAmount
                                )
                            }}
                        </strong>
                    </div>

                    <div v-if="couponDiscount > 0" class="amount-row discount">
                        <span>優惠折抵</span>

                        <strong>
                            − {{
                                formatPrice(
                                    couponDiscount
                                )
                            }}
                        </strong>
                    </div>

                    <div class="amount-row">
                        <span>運費</span>
                        <strong>免運</strong>
                    </div>

                    <div class="total-row">
                        <span>應付金額</span>

                        <strong>
                            {{ formatPrice(finalAmount) }}
                        </strong>
                    </div>
                </div>

                <p class="payment-notice">
                    <i class="bi bi-shield-check"></i>
                    下單後將前往綠界完成信用卡付款
                </p>
            </aside>
        </div>

        <!-- 無法結帳 -->
        <div v-else class="checkout-message">
            <i class="bi bi-cart-x"></i>

            <h2>目前沒有可結帳商品</h2>

            <RouterLink :to="{ name: 'cart' }" class="btn btn-outline-primary">
                返回購物車
            </RouterLink>
        </div>
    </main>
</template>

<style scoped>
.checkout-page {
    width: 100%;
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px 24px 56px;
}

.checkout-page-header {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 24px;
}

.checkout-page-header h1 {
    margin: 0 0 5px;
    color: #1f2937;
    font-size: 1.75rem;
    font-weight: 700;
}

.checkout-page-header p {
    margin: 0;
    color: #6b7280;
}

.back-link {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
    gap: 7px;
    min-height: 40px;
    padding: 8px 13px;
    border: 1px solid #9ca3af;
    border-radius: 6px;
    color: #4b5563;
    text-decoration: none;
}

.back-link:hover {
    border-color: #4169e1;
    color: #4169e1;
}

.checkout-layout {
    display: grid;
    grid-template-columns:
        minmax(0, 1fr) 390px;
    align-items: start;
    gap: 22px;
}

.checkout-form-panel,
.order-summary-card {
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #ffffff;
}

.panel-header,
.summary-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 17px 20px;
    border-bottom: 1px solid #d8dde6;
    background: #f7f8fa;
}

.panel-header h2,
.summary-header h2 {
    margin: 0;
    color: #1f2937;
    font-size: 1.1rem;
    font-weight: 700;
}

.panel-header p {
    margin: 4px 0 0;
    color: #6b7280;
    font-size: 0.88rem;
}

.summary-header span {
    color: #6b7280;
    font-size: 0.88rem;
}

.checkout-form-body {
    padding: 20px;
}

.checkout-form-body :deep(form > .btn-primary) {
    width: 100%;
    min-height: 46px;
    font-weight: 600;
}

.order-summary-card {
    position: sticky;
    top: 110px;
}

.summary-products {
    max-height: 330px;
    overflow-y: auto;
    padding: 0 18px;
}

.summary-product {
    display: grid;
    grid-template-columns:
        68px minmax(0, 1fr);
    align-items: center;
    gap: 12px;
    padding: 15px 0;
}

.summary-product+.summary-product {
    border-top: 1px solid #e5e7eb;
}

.product-image-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 68px;
    height: 68px;
    overflow: hidden;
    border: 1px solid #d8dde6;
    border-radius: 7px;
    background: #f7f8fa;
}

.product-image {
    width: 100%;
    height: 100%;
    padding: 4px;
    object-fit: contain;
}

.product-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: #9ca3af;
    font-size: 23px;
}

.product-information {
    display: flex;
    align-items: flex-start;
    min-width: 0;
    flex-direction: column;
    gap: 7px;
}

.product-name {
    overflow-wrap: anywhere;
    color: #1f2937;
    font-weight: 600;
    line-height: 1.4;
    text-decoration: none;
}

.product-name:hover {
    color: #4169e1;
    text-decoration: underline;
}

.product-price {
    color: #1f2937;
    font-size: 0.92rem;
}

.coupon-section,
.amount-section {
    padding: 18px;
    border-top: 1px solid #d8dde6;
}

.coupon-section .form-label {
    color: #374151;
    font-weight: 600;
}

.amount-row,
.total-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
}

.amount-row {
    margin-bottom: 12px;
    color: #4b5563;
}

.amount-row strong {
    color: #1f2937;
    white-space: nowrap;
}

.amount-row.discount,
.amount-row.discount strong {
    color: #dc3545;
}

.total-row {
    margin-top: 17px;
    padding-top: 17px;
    border-top: 1px solid #d8dde6;
    color: #1f2937;
    font-weight: 700;
}

.total-row strong {
    color: #4169e1;
    font-size: 1.2rem;
    white-space: nowrap;
}

.payment-notice {
    margin: 0;
    padding: 13px 18px;
    color: #6b7280;
    background: #f7f8fa;
    font-size: 0.82rem;
    text-align: center;
}

.payment-notice i {
    margin-right: 5px;
    color: #198754;
}

.checkout-message {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    min-height: 320px;
    padding: 40px 20px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    color: #6b7280;
    background: #ffffff;
    text-align: center;
}

.checkout-message>i {
    margin-bottom: 14px;
    color: #9ca3af;
    font-size: 42px;
}

.checkout-message h2 {
    margin-bottom: 18px;
    color: #1f2937;
    font-size: 1.1rem;
}

.checkout-message p {
    margin: 12px 0 0;
}

@media (max-width: 991.98px) {
    .checkout-layout {
        grid-template-columns: 1fr;
    }

    .order-summary-card {
        position: static;
    }
}

@media (max-width: 575.98px) {
    .checkout-page {
        padding: 28px 14px 44px;
    }

    .checkout-page-header {
        align-items: flex-start;
        flex-direction: column;
        gap: 14px;
    }

    .checkout-page-header h1 {
        font-size: 1.5rem;
    }

    .back-link {
        min-height: auto;
        padding: 0;
        border: 0;
    }

    .checkout-form-body {
        padding: 16px;
    }
}
</style>