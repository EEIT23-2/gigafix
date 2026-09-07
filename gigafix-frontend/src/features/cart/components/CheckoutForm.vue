<script setup>
defineProps({
    form: {
        type: Object,
        required: true
    },
    submitting: {
        type: Boolean,
        default: false
    },
    disabled: {
        type: Boolean,
        default: false
    }
})

defineEmits(['submit'])
</script>

<template>
    <form @submit.prevent="$emit('submit')">
        <div class="mb-3">
            <label class="form-label">付款方式</label>

            <select v-model="form.paymentMethod" class="form-select" required>
                <option value="CREDIT_CARD">
                    信用卡
                </option>

                <option value="CASH_ON_DELIVERY">
                    貨到付款
                </option>
            </select>
        </div>

        <div class="mb-3">
            <label class="form-label">收件人姓名</label>

            <input v-model="form.receiverName" type="text" class="form-control" required>
        </div>

        <div class="mb-3">
            <label class="form-label">收件人電話</label>

            <input v-model="form.receiverPhone" type="text" class="form-control" required>
        </div>

        <div class="mb-3">
            <label class="form-label">收件地址</label>

            <input v-model="form.receiverAddress" type="text" class="form-control" required>
        </div>

        <div class="mb-3">
            <label class="form-label">配送方式</label>

            <select v-model="form.shippingMethod" class="form-select" required>
                <option value="HOME">
                    宅配
                </option>

                <option value="STORE">
                    超商取貨
                </option>
            </select>
        </div>

        <div class="mb-4">
            <label class="form-label">訂單備註</label>

            <textarea v-model="form.customerRemark" class="form-control" rows="3"></textarea>
        </div>

        <button type="submit" class="btn btn-primary" :disabled="submitting || disabled">
            {{ submitting ? '建立訂單中...' : '確認下單' }}
        </button>
    </form>
</template>