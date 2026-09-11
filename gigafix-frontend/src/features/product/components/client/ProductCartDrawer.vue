<script setup>
import { computed, onBeforeUnmount, watch } from "vue";
import { ref } from "vue";
import { getCartItems } from "@/features/cart/api/cartApi.js";

const props = defineProps({
  open: { type: Boolean, default: false },
  refreshKey: { type: Number, default: 0 },
  showTrigger: { type: Boolean, default: false },
});

const emit = defineEmits(["open", "close"]);
const cartItems = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const formatter = new Intl.NumberFormat("zh-TW");

const totalAmount = computed(() =>
  cartItems.value.reduce((sum, item) => sum + (item.price ?? 0), 0),
);

const loadCart = async () => {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await getCartItems();
    cartItems.value = response.data ?? [];
  } catch (error) {
    cartItems.value = [];
    errorMessage.value =
      error?.response?.data?.message ?? "購物車明細載入失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

watch(
  () => [props.open, props.refreshKey, props.showTrigger],
  ([open, , showTrigger]) => {
    document.body.style.overflow = open ? "hidden" : "";
    if (open || showTrigger) loadCart();
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  document.body.style.overflow = "";
});
</script>

<template>
  <Teleport to="body">
    <Transition name="trigger-fade">
      <button
        v-if="showTrigger && !open"
        class="cart-floating-button"
        type="button"
        :aria-label="`查看購物車明細，目前共 ${cartItems.length} 件商品`"
        @click="emit('open')"
      >
        <i class="bi bi-cart3" aria-hidden="true"></i>
        查看購物車明細
        <span class="cart-count" aria-hidden="true">
          {{ cartItems.length > 99 ? "99+" : cartItems.length }}
        </span>
      </button>
    </Transition>

    <Transition name="drawer-fade">
      <div v-if="open" class="cart-drawer-layer" role="presentation">
        <button
          class="drawer-backdrop"
          type="button"
          aria-label="關閉購物車明細"
          @click="emit('close')"
        ></button>

        <aside
          class="cart-drawer"
          role="dialog"
          aria-modal="true"
          aria-labelledby="cart-drawer-title"
        >
          <header class="drawer-header">
            <div>
              <span class="drawer-eyebrow">YOUR CART</span>
              <h2 id="cart-drawer-title">購物車明細</h2>
            </div>
            <button
              class="close-button"
              type="button"
              aria-label="關閉購物車明細"
              @click="emit('close')"
            >
              <i class="bi bi-x-lg" aria-hidden="true"></i>
            </button>
          </header>

          <div class="drawer-content">
            <div v-if="loading" class="drawer-state" aria-live="polite">
              <div class="spinner-border" role="status">
                <span class="visually-hidden">載入中</span>
              </div>
              <p>正在更新購物車…</p>
            </div>

            <div v-else-if="errorMessage" class="drawer-state drawer-error">
              <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
              <p>{{ errorMessage }}</p>
              <button type="button" @click="loadCart">重新載入</button>
            </div>

            <div v-else-if="!cartItems.length" class="drawer-state">
              <i class="bi bi-cart" aria-hidden="true"></i>
              <p>購物車目前沒有商品</p>
            </div>

            <ul v-else class="cart-item-list">
              <li v-for="item in cartItems" :key="item.cartItemId">
                <div class="item-image">
                  <img
                    v-if="item.imageUrl"
                    :src="item.imageUrl"
                    :alt="item.productName"
                  />
                  <i v-else class="bi bi-image" aria-hidden="true"></i>
                </div>
                <div class="item-info">
                  <span>商品 #{{ item.productId }}</span>
                  <h3>{{ item.productName }}</h3>
                  <strong>NT$ {{ formatter.format(item.price ?? 0) }}</strong>
                </div>
              </li>
            </ul>
          </div>

          <footer class="drawer-footer">
            <div class="cart-total">
              <span>共 {{ cartItems.length }} 件商品</span>
              <strong>NT$ {{ formatter.format(totalAmount) }}</strong>
            </div>
            <RouterLink class="view-cart-button" to="/cart" @click="emit('close')">
              查看完整購物車
              <i class="bi bi-arrow-right" aria-hidden="true"></i>
            </RouterLink>
          </footer>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.cart-floating-button {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 1060;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 54px;
  padding: 14px 22px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  color: #fff;
  background: rgba(27, 27, 27, 0.9);
  box-shadow: 0 16px 38px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(14px);
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.cart-floating-button:hover,
.cart-floating-button:focus-visible {
  color: #1b1b1b;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.28);
  transform: translateY(-3px);
  outline: none;
}

.cart-floating-button i {
  font-size: 20px;
}

.cart-count {
  position: absolute;
  top: -8px;
  right: -8px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 25px;
  height: 25px;
  padding: 0 6px;
  border: 2px solid #fff;
  border-radius: 999px;
  color: #fff;
  background: #005ab7;
  box-shadow: 0 5px 12px rgba(0, 0, 0, 0.22);
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.cart-drawer-layer {
  position: fixed;
  inset: 0;
  z-index: 1080;
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  border: 0;
  background: rgba(20, 24, 30, 0.32);
  backdrop-filter: blur(3px);
  cursor: pointer;
}

.cart-drawer {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  width: min(440px, 92vw);
  height: 100%;
  color: #1b1b1b;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: -24px 0 70px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(18px);
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 30px 28px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.drawer-eyebrow {
  display: block;
  margin-bottom: 5px;
  color: #005ab7;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.drawer-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: 1px solid #dedede;
  border-radius: 50%;
  color: #1b1b1b;
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
}

.close-button:hover,
.close-button:focus-visible {
  color: #fff;
  background: #1b1b1b;
  outline: none;
}

.drawer-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 28px;
}

.cart-item-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.cart-item-list li {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 16px;
  padding: 20px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.item-image {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 92px;
  height: 92px;
  overflow: hidden;
  border-radius: 12px;
  color: #b5b5b5;
  background: #f3f3f3;
  font-size: 26px;
}

.item-image img {
  width: 84%;
  height: 84%;
  object-fit: contain;
}

.item-info {
  align-self: center;
  min-width: 0;
}

.item-info > span {
  color: #7e7576;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.item-info h3 {
  margin: 4px 0 8px;
  overflow: hidden;
  font-size: 17px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-info strong {
  font-size: 17px;
}

.drawer-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  color: #635d5e;
  text-align: center;
}

.drawer-state > i {
  margin-bottom: 12px;
  font-size: 38px;
}

.drawer-state p {
  margin: 14px 0 0;
}

.drawer-error button {
  margin-top: 12px;
  padding: 8px 16px;
  border: 1px solid #1b1b1b;
  border-radius: 8px;
  background: transparent;
}

.drawer-footer {
  padding: 22px 28px 28px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.cart-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
  color: #635d5e;
}

.cart-total strong {
  color: #1b1b1b;
  font-size: 22px;
}

.view-cart-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 54px;
  border: 2px solid #1b1b1b;
  border-radius: 11px;
  color: #fff;
  background: #1b1b1b;
  font-weight: 700;
  text-decoration: none;
}

.view-cart-button:hover,
.view-cart-button:focus-visible {
  color: #1b1b1b;
  background: #fff;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.22s ease;
}

.drawer-fade-enter-active .cart-drawer,
.drawer-fade-leave-active .cart-drawer {
  transition: transform 0.28s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-fade-enter-from .cart-drawer,
.drawer-fade-leave-to .cart-drawer {
  transform: translateX(100%);
}

.trigger-fade-enter-active,
.trigger-fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.trigger-fade-enter-from,
.trigger-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 575.98px) {
  .cart-floating-button {
    right: 16px;
    bottom: 16px;
    min-height: 50px;
    padding: 12px 18px;
    font-size: 14px;
  }

  .drawer-header,
  .drawer-footer {
    padding-right: 20px;
    padding-left: 20px;
  }

  .drawer-content {
    padding-right: 20px;
    padding-left: 20px;
  }
}
</style>
