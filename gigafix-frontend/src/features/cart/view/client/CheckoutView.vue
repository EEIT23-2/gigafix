<script setup>
import { onMounted, ref } from 'vue'
import { useMemberAuth } from '../../composables/useMemberAuth'
import { useRouter } from 'vue-router'
import CheckoutForm from '../../components/CheckoutForm.vue'
import { getCartItems } from '../../api/cartApi'
import { createOrder } from '../../api/memberOrderApi'

const router = useRouter()

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
        }
    } catch (error) {
        console.error(error)
        errorMessage.value = '購物車載入失敗'
    } finally {
        loading.value = false
    }
}

// 提交結帳表單
const handleSubmit = async () => {
    if (cartItems.value.length === 0) {
        errorMessage.value = '購物車是空的，無法結帳'
        return
    }

    submitting.value = true
    errorMessage.value = ''

    try {
        const response = await createOrder(form.value)

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

    loadCart()
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

        <CheckoutForm :form="form" :submitting="submitting" :disabled="cartItems.length === 0" @submit="handleSubmit" />
    </div>
</template>