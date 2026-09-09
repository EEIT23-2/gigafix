<script setup>
defineProps({
    form: {
        type: Object,
        required: true
    },
    selectedStore: {
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

defineEmits([
    'submit',
    'select-store',
    'store-type-change'])
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

        <!-- 超商取貨 -->
        <div v-if="form.shippingMethod === 'STORE'" class="mb-3">
            <label class="form-label">
                超商品牌
            </label>

            <select v-model="selectedStore.cvsType" class="form-select" @change="$emit('store-type-change')">
                <option value="">
                    請選擇超商
                </option>

                <option value="UNIMART">
                    7-ELEVEN
                </option>

                <option value="FAMI">
                    全家
                </option>

                <option value="HILIFE">
                    萊爾富
                </option>
            </select>
        </div>
        <div v-if="
            form.shippingMethod === 'STORE'
            && selectedStore.cvsType
            && !selectedStore.storeId
        " class="mb-3">
            <button type="button" class="btn btn-outline-primary" @click="$emit('select-store')">
                選擇取貨門市
            </button>
        </div>
        <div v-if="
            form.shippingMethod === 'STORE'
            && selectedStore.storeId
        " class="card mb-3">
            <div class="card-body">

                <div class="fw-bold mb-2">
                    已選擇取貨門市
                </div>

                <div>
                    {{ selectedStore.storeName }}
                </div>

                <div class="text-muted">
                    門市代號：{{ selectedStore.storeId }}
                </div>

                <div class="text-muted">
                    {{ selectedStore.storeAddress }}
                </div>

                <button type="button" class="btn btn-outline-secondary btn-sm mt-3" @click="$emit('select-store')">
                    重新選擇門市
                </button>

            </div>
        </div>
        <!-- 宅配 -->
        <div v-if="form.shippingMethod === 'HOME'" class="mb-3">
            <label class="form-label">收件地址</label>

            <input v-model="form.receiverAddress" type="text" class="form-control" required>
        </div>

        <div class="mb-4">
            <label class="form-label">訂單備註</label>

            <textarea v-model="form.customerRemark" class="form-control" rows="3"></textarea>
        </div>

        <button type="submit" class="btn btn-primary" :disabled="submitting
            || disabled
            || (
                form.shippingMethod === 'STORE'
                && !selectedStore.storeId
            )
            ">
            {{ submitting ? '建立訂單中...' : '確認下單' }}
        </button>
    </form>
</template>