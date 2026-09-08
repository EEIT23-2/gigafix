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
    <div class="container py-5">
        <h2 class="mb-4">我的購物車</h2>

        <div v-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
        </div>

        <div v-if="loading">
            載入中...
        </div>

        <div v-else-if="cartItems.length === 0" class="text-center py-5">
            <p class="text-muted">購物車目前沒有商品</p>
        </div>

        <div v-else>

            <!-- 全選 -->
            <div class="d-flex align-items-center mb-3">
                <input id="selectAll" class="form-check-input me-2" type="checkbox" :checked="allSelected"
                    @change="handleSelectAll($event.target.checked)">

                <label for="selectAll" class="form-check-label">
                    全選
                </label>
            </div>

            <!-- 購物車商品 -->
            <CartItemCard v-for="item in cartItems" :key="item.cartItemId" :item="item"
                :selected="selectedCartItemIds.includes(item.cartItemId)"
                @update:selected="checked => handleSelect(item.cartItemId, checked)" @delete="handleDelete" />

            <!-- 購物車摘要 -->
            <CartSummary :totalAmount="selectedTotalAmount" :itemCount="selectedItems.length" :clearing="clearing"
                :checkoutDisabled="selectedItems.length === 0" :coupons="coupons"
                :selectedCouponCode="selectedCouponCode" :couponDiscount="couponDiscount" :finalAmount="finalAmount"
                @update:selectedCouponCode="selectedCouponCode = $event" @clear="handleClear"
                @checkout="handleCheckout" />
        </div>
    </div>
</template>