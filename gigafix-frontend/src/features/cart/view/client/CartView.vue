<script setup>
import { computed, onMounted, ref } from 'vue'
import { useMemberAuth } from '../../composables/useMemberAuth'
import CartItemCard from '../../components/CartItemCard.vue'
import CartSummary from '../../components/CartSummary.vue'
import {
    getCartItems,
    deleteCartItem,
    clearCart
} from '../../api/cartApi'
// 購物車相關狀態與計算
const cartItems = ref([])
const loading = ref(false)
const errorMessage = ref('')
const clearing = ref(false)

const totalAmount = computed(() => {
    return cartItems.value.reduce((total, item) => {
        return total + (item.price || 0)
    }, 0)
})
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
// 刪除購物車中的商品
const handleDelete = async (cartItemId) => {
    try {
        await deleteCartItem(cartItemId)
        await loadCart()
    } catch (error) {
        console.error(error)
        errorMessage.value = '刪除商品失敗'
    }
}
// 清空購物車
const handleClear = async () => {
    if (!confirm('確定要清空購物車嗎？')) {
        return
    }

    clearing.value = true
    errorMessage.value = ''

    try {
        await clearCart()
        await loadCart()
    } catch (error) {
        console.error(error)
        errorMessage.value = '清空購物車失敗'
    } finally {
        clearing.value = false
    }
}
// 會員驗證與初始化購物車
const { checkMemberLogin } = useMemberAuth()
onMounted(async () => {
    const loggedIn = await checkMemberLogin()

    if (!loggedIn) {
        return
    }

    loadCart()
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
            <CartItemCard v-for="item in cartItems" :key="item.cartItemId" :item="item" @delete="handleDelete" />

            <CartSummary :totalAmount="totalAmount" :itemCount="cartItems.length" :clearing="clearing"
                @clear="handleClear" />
        </div>
    </div>
</template>