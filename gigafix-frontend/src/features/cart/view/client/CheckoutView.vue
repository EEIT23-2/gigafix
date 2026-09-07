<script setup>
import { computed, onMounted, ref } from 'vue'
import { useMemberAuth } from '../../composables/useMemberAuth'
import { useRouter, useRoute } from 'vue-router'
import CheckoutForm from '../../components/CheckoutForm.vue'
import { getCartItems } from '../../api/cartApi'
import { createOrder } from '../../api/memberOrderApi'

const router = useRouter()
const route = useRoute()

const cartItems = ref([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

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
            cartItemIds: selectedCartItemIds.value
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
// 會員驗證與清空購物車
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

                <div class="d-flex justify-content-between mt-3">
                    <span>
                        已選 {{ selectedItems.length }} 件
                    </span>

                    <strong>
                        商品小計：
                        NT$ {{ selectedTotalAmount.toLocaleString() }}
                    </strong>
                </div>
            </div>
        </div>
        <CheckoutForm :form="form" :submitting="submitting" :disabled="selectedItems.length === 0"
            @submit="handleSubmit" />
    </div>
</template>