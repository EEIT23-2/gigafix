<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMemberAuth } from '../../composables/useMemberAuth'
import CartItemCard from '../../components/CartItemCard.vue'
import CartSummary from '../../components/CartSummary.vue'
import {
    getCartItems,
    deleteCartItem,
    clearCart
} from '../../api/cartApi'
import { getAvailableCoupons } from '../../api/couponApi'
//Router
const router = useRouter()

// 購物車相關狀態與計算
const cartItems = ref([])
const selectedCartItemIds = ref([])
const loading = ref(false)
const errorMessage = ref('')
const clearing = ref(false)
const coupons = ref([])
const selectedCouponCode = ref('')
// 載入購物車商品列表
const loadCart = async () => {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await getCartItems()
        cartItems.value = response.data
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
// 選取的購物車商品列表
const selectedItems = computed(() => {
    return cartItems.value.filter(item =>
        selectedCartItemIds.value.includes(item.cartItemId)
    )
})
// 選取的購物車商品總金額
const selectedTotalAmount = computed(() => {
    return selectedItems.value.reduce((total, item) => {
        return total + (item.price || 0)
    }, 0)
})
// 目前選擇的優惠券
const selectedCoupon = computed(() => {
    return coupons.value.find(
        coupon => coupon.couponCode === selectedCouponCode.value
    ) || null
})

// 優惠券折扣金額
const couponDiscount = computed(() => {
    if (selectedTotalAmount.value <= 0) {
        return 0
    }
    return selectedCoupon.value?.discountAmount || 0
})

// 預估應付金額
const finalAmount = computed(() => {
    return Math.max(
        selectedTotalAmount.value - couponDiscount.value,
        0
    )
})
// 是否全選
const allSelected = computed(() => {
    return (
        cartItems.value.length > 0 &&
        selectedCartItemIds.value.length === cartItems.value.length
    )
})
// 全選與單選處理
const handleSelectAll = (checked) => {
    if (checked) {
        selectedCartItemIds.value =
            cartItems.value.map(item => item.cartItemId)
    } else {
        selectedCartItemIds.value = []
    }
}
// 購物車商品選取
const handleSelect = (cartItemId, checked) => {
    if (checked) {
        if (!selectedCartItemIds.value.includes(cartItemId)) {
            selectedCartItemIds.value.push(cartItemId)
        }
    } else {
        selectedCartItemIds.value =
            selectedCartItemIds.value.filter(id => id !== cartItemId)
    }
}
// 刪除購物車中的商品
const handleDelete = async (cartItemId) => {
    try {
        await deleteCartItem(cartItemId)

        selectedCartItemIds.value =
            selectedCartItemIds.value.filter(id => id !== cartItemId)

        await loadCart()
    } catch (error) {
        console.error(error)
        errorMessage.value = '刪除商品失敗'
    }
}
// 清空購物車
const handleClear = async () => {
    if (!confirm('確定要清空購物車嗎？')) return

    clearing.value = true
    errorMessage.value = ''

    try {
        await clearCart()

        selectedCartItemIds.value = []

        await loadCart()
    } catch (error) {
        console.error(error)
        errorMessage.value = '清空購物車失敗'
    } finally {
        clearing.value = false
    }
}
// 前往結帳
const handleCheckout = () => {
    if (selectedCartItemIds.value.length === 0) {
        alert('請至少選擇一項商品')
        return
    }

    router.push({
        path: '/checkout',
        query: {
            cartItemIds: selectedCartItemIds.value.join(','),
            couponCode: selectedCouponCode.value || ''
        }
    })
}
// 會員驗證與初始化購物車
const { checkMemberLogin } = useMemberAuth()

onMounted(async () => {
    const loggedIn = await checkMemberLogin()

    if (!loggedIn) {
        return
    }

    await loadCart()
    await loadCoupons()
})
</script>

<template>
    <main class="container py-5 cart-page">
        <!-- 頁面標題 -->
        <header class="cart-page-header">
            <div>
                <h2 class="mb-1">
                    購物車
                </h2>

                <p class="mb-0 text-muted">
                    選擇要一起結帳的商品
                </p>
            </div>

            <span v-if="!loading" class="cart-item-count">
                購物車內共 {{ cartItems.length }} 件
            </span>
        </header>

        <!-- 錯誤訊息 -->
        <div v-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
        </div>

        <!-- 載入狀態 -->
        <div v-if="loading" class="text-center py-5 text-muted">
            <div class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></div>

            載入購物車中...
        </div>

        <!-- 空購物車 -->
        <div v-else-if="cartItems.length === 0" class="empty-cart">
            <i class="bi bi-cart-x" aria-hidden="true"></i>

            <p class="mb-0">
                購物車目前沒有商品
            </p>
        </div>

        <!-- 購物車內容 -->
        <section v-else>
            <!-- 商品欄位標頭 -->
            <div class="cart-list-header">
                <label for="selectAll" class="cart-header-product">
                    <input id="selectAll" class="form-check-input" type="checkbox" :checked="allSelected" @change="
                        handleSelectAll(
                            $event.target.checked
                        )
                        ">

                    <span>勾選要結帳的商品</span>
                </label>

                <span class="text-end">
                    單價
                </span>

                <span class="text-center">
                    操作
                </span>
            </div>

            <!-- 購物車商品 -->
            <div class="cart-list">
                <CartItemCard v-for="item in cartItems" :key="item.cartItemId" :item="item" :selected="selectedCartItemIds.includes(
                    item.cartItemId
                )
                    " @update:selected="
                        checked =>
                            handleSelect(
                                item.cartItemId,
                                checked
                            )
                    " @delete="handleDelete" />
            </div>

            <!-- 購物車摘要 -->
            <CartSummary :totalAmount="selectedTotalAmount" :itemCount="selectedItems.length" :clearing="clearing"
                :checkoutDisabled="selectedItems.length === 0
                    " :coupons="coupons" :selectedCouponCode="selectedCouponCode" :couponDiscount="couponDiscount"
                :finalAmount="finalAmount" @update:selectedCouponCode="
                    selectedCouponCode = $event
                    " @clear="handleClear" @checkout="handleCheckout" />
        </section>
    </main>
</template>

<style scoped>
.cart-page {
    max-width: 1200px;
}

.cart-page-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 22px;
}

.cart-item-count {
    color: #6b7280;
    white-space: nowrap;
}

.cart-list-header {
    display: grid;
    grid-template-columns:
        minmax(360px, 1fr) 140px 80px;
    align-items: center;
    gap: 18px;
    min-height: 48px;
    margin-bottom: 12px;
    padding: 10px 18px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    color: #6b7280;
    background: #f7f8fa;
    font-size: 0.9rem;
}

.cart-header-product {
    display: flex;
    align-items: center;
    gap: 20px;
    cursor: pointer;
}

.cart-header-product .form-check-input {
    width: 18px;
    height: 18px;
    margin: 0;
    cursor: pointer;
}

.empty-cart {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 14px;
    min-height: 260px;
    color: #6b7280;
    text-align: center;
}

.empty-cart i {
    font-size: 2.5rem;
}

@media (max-width: 767.98px) {
    .cart-page-header {
        align-items: flex-start;
        flex-direction: column;
        gap: 8px;
    }

    .cart-list-header {
        display: none;
    }
}
</style>