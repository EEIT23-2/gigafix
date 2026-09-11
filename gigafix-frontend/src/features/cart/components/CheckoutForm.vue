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
    },
    sameAsMember: {
        type: Boolean,
        default: false
    }
})

defineEmits([
    'submit',
    'select-store',
    'store-type-change',
    'same-as-member-change'
])
</script>

<template>
    <form @submit.prevent="$emit('submit')">
        <label class="same-member-option" :class="{
            active: sameAsMember
        }">
            <input type="checkbox" class="form-check-input" :checked="sameAsMember" @change="
                $emit(
                    'same-as-member-change',
                    $event.target.checked
                )
                ">

            <span>
                <strong>
                    收件人資料與會員資料相同
                </strong>

            </span>
        </label>
        <div class="mb-3">
            <label class="form-label">付款方式</label>

            <select v-model="form.paymentMethod" class="form-select" required>
                <option value="CREDIT_CARD">
                    信用卡
                </option>
            </select>
        </div>

        <div class="mb-3">
            <label class="form-label">收件人姓名</label>

            <input v-model.trim="form.receiverName" type="text" class="form-control" maxlength="40" autocomplete="name"
                placeholder="請輸入收件人姓名" :disabled="sameAsMember" required>
        </div>

        <div class="mb-3">
            <label class="form-label">收件人電話</label>

            <input v-model.trim="form.receiverPhone" type="tel" class="form-control" maxlength="10" inputmode="numeric"
                autocomplete="tel" pattern="09[0-9]{8}" placeholder="例如：0912345678" :disabled="sameAsMember" required>
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

            <input v-model.trim="form.receiverAddress" type="text" class="form-control" maxlength="255"
                autocomplete="street-address" placeholder="請輸入完整配送地址" :disabled="sameAsMember" required>
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
<style scoped>
.same-member-option {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 18px;
    padding: 13px 14px;
    border: 1px solid #d8dde6;
    border-radius: 8px;
    background: #f7f8fa;
    cursor: pointer;
}

.same-member-option.active {
    border-color: #4169e1;
    background: #f3f6ff;
}

.same-member-option .form-check-input {
    flex-shrink: 0;
    margin-top: 3px;
    cursor: pointer;
}

.same-member-option span {
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.same-member-option strong {
    color: #1f2937;
}

.same-member-option small {
    color: #6b7280;
}
</style>