<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRoute } from "vue-router";
import { addCartItem } from "@/features/cart/api/cartApi.js";
import { useFetchMemberInfoStore } from "@/stores/member";
import { getProduct } from "../../api.js";
import ProductCartDrawer from "../../components/client/ProductCartDrawer.vue";

const route = useRoute();
const fetchMemberInfoStore = useFetchMemberInfoStore();
const { memberInfo } = storeToRefs(fetchMemberInfoStore);

const product = ref(null);
const loading = ref(false);
const addingToCart = ref(false);
const errorMessage = ref("");
const cartMessage = ref("");
const cartMessageType = ref("success");
const cartDrawerOpen = ref(false);
const cartRefreshKey = ref(0);
let cartMessageTimer;

const formatter = new Intl.NumberFormat("zh-TW");

const productName = computed(
  () =>
    product.value?.product_name ??
    product.value?.productName ??
    "未命名商品",
);
const productImage = computed(
  () => product.value?.image_url ?? product.value?.imageUrl ?? "",
);
const saleStatus = computed(
  () => product.value?.sale_status ?? product.value?.saleStatus,
);
const isAvailable = computed(() => saleStatus.value === "AVAILABLE");

const categoryLabel = (category) =>
  ({ IPHONE: "iPhone", IPAD: "iPad", WATCH: "Apple Watch" })[category] ??
  category ??
  "精選商品";

const fetchProduct = async () => {
  const productId = Number(route.params.productId);
  if (!Number.isInteger(productId) || productId <= 0) {
    errorMessage.value = "商品編號格式不正確。";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    product.value = await getProduct(productId);
  } catch (error) {
    product.value = null;
    errorMessage.value =
      error?.response?.status === 404
        ? "找不到這項商品，商品可能已下架。"
        : error?.response?.data?.message ?? "商品資料載入失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

const openLoginModal = () => {
  const loginButton = document.querySelector(
    ".user-actions button.action-item",
  );
  loginButton?.click();
};

const showCartMessage = (message, type) => {
  clearTimeout(cartMessageTimer);
  cartMessage.value = message;
  cartMessageType.value = type;
  cartMessageTimer = setTimeout(() => {
    cartMessage.value = "";
  }, 3000);
};

const addProductToCart = async () => {
  if (!memberInfo.value) {
    openLoginModal();
    return;
  }

  if (!product.value || !isAvailable.value || addingToCart.value) return;

  addingToCart.value = true;
  try {
    await addCartItem(product.value.productId);
    showCartMessage(`${productName.value} 已加入購物車`, "success");
    cartRefreshKey.value += 1;
  } catch (error) {
    if (error?.response?.status === 401) {
      showCartMessage("登入狀態已失效，請重新登入。", "danger");
      openLoginModal();
    } else {
      showCartMessage(
        error?.response?.data?.message ?? "加入購物車失敗，請稍後再試。",
        "danger",
      );
    }
  } finally {
    addingToCart.value = false;
  }
};

const openCartDrawer = () => {
  cartRefreshKey.value += 1;
  cartDrawerOpen.value = true;
};

onMounted(fetchProduct);
onBeforeUnmount(() => clearTimeout(cartMessageTimer));
</script>

<template>
  <ProductCartDrawer
    :open="cartDrawerOpen"
    :refresh-key="cartRefreshKey"
    :show-trigger="Boolean(memberInfo)"
    @open="openCartDrawer"
    @close="cartDrawerOpen = false"
  />

  <main class="detail-page">
    <div class="detail-container">
      <RouterLink class="back-link" :to="{ name: 'mall-list' }">
        <i class="bi bi-arrow-left" aria-hidden="true"></i>
        返回商城
      </RouterLink>

      <div v-if="loading" class="state-card" aria-live="polite">
        <div class="spinner-border" role="status">
          <span class="visually-hidden">載入中</span>
        </div>
        <p>正在載入商品資料…</p>
      </div>

      <div v-else-if="errorMessage" class="state-card">
        <i class="bi bi-exclamation-circle state-icon" aria-hidden="true"></i>
        <h1>{{ errorMessage }}</h1>
        <button class="secondary-button" type="button" @click="fetchProduct">
          重新載入
        </button>
      </div>

      <article v-else-if="product" class="product-detail">
        <div class="image-panel">
          <img v-if="productImage" :src="productImage" :alt="productName" />
          <i v-else class="bi bi-image image-placeholder" aria-hidden="true"></i>
        </div>

        <section class="product-info">
          <p class="eyebrow">{{ categoryLabel(product.category) }}</p>
          <h1>{{ productName }}</h1>

          <dl class="product-specs">
            <div>
              <dt>商品等級</dt>
              <dd>{{ product.grade || "嚴選" }} 級品</dd>
            </div>
            <div>
              <dt>外觀狀況</dt>
              <dd>{{ product.appearance || "未標示" }}</dd>
            </div>
            <div>
              <dt>其他描述</dt>
              <dd>{{ product.description || "未標示" }}</dd>
            </div>
          </dl>

          <div class="purchase-panel">
            <span class="price-label">售價</span>
            <strong>NT$ {{ formatter.format(product.price ?? 0) }}</strong>

            <div
              v-if="cartMessage"
              class="cart-message"
              :class="`cart-message-${cartMessageType}`"
              role="status"
              aria-live="polite"
            >
              <i
                class="bi"
                :class="
                  cartMessageType === 'success'
                    ? 'bi-check-circle'
                    : 'bi-exclamation-circle'
                "
                aria-hidden="true"
              ></i>
              {{ cartMessage }}
            </div>

            <button
              class="cart-button"
              type="button"
              :disabled="addingToCart || !isAvailable"
              @click="addProductToCart"
            >
              <span
                v-if="addingToCart"
                class="spinner-border spinner-border-sm"
                aria-hidden="true"
              ></span>
              <i v-else class="bi bi-cart-plus" aria-hidden="true"></i>
              {{
                addingToCart
                  ? "加入中..."
                  : isAvailable
                    ? "加入購物車"
                    : "目前無法購買"
              }}
            </button>
          </div>
        </section>
      </article>
    </div>
  </main>
</template>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;500;600;700&family=Noto+Sans+TC:wght@400;500;600;700&display=swap");

.detail-page {
  --ink: #1b1b1b;
  --muted: #635d5e;
  --soft: #f9f9f9;
  --blue: #005ab7;
  min-height: 70vh;
  padding: 56px 5rem 96px;
  color: var(--ink);
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
}

.detail-container {
  width: min(100%, 1280px);
  margin: 0 auto;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 40px;
  color: var(--muted);
  font-weight: 600;
  text-decoration: none;
}

.back-link:hover,
.back-link:focus-visible {
  color: var(--ink);
}

.product-detail {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.95fr);
  gap: clamp(48px, 7vw, 96px);
  align-items: center;
}

.image-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 560px;
  overflow: hidden;
  border-radius: 20px;
  background: var(--soft);
}

.image-panel img {
  width: 82%;
  height: 500px;
  object-fit: contain;
}

.image-placeholder {
  color: #c6c6c6;
  font-size: 72px;
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--blue);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.product-info h1 {
  margin: 0 0 18px;
  color: #000;
  font-size: clamp(40px, 5vw, 64px);
  line-height: 1.08;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.product-specs {
  margin: 0;
  border-top: 1px solid #e2e2e2;
}

.product-specs > div {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 16px;
  padding: 18px 0;
  border-bottom: 1px solid #e2e2e2;
}

.product-specs dt {
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
}

.product-specs dd {
  margin: 0;
  font-weight: 600;
}

.purchase-panel {
  margin-top: 36px;
}

.price-label {
  display: block;
  margin-bottom: 4px;
  color: var(--muted);
  font-size: 14px;
}

.purchase-panel > strong {
  display: block;
  margin-bottom: 24px;
  font-size: 36px;
  font-weight: 700;
}

.cart-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 2px solid var(--ink);
  border-radius: 12px;
  color: #fff;
  background: var(--ink);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease;
}

.cart-button {
  width: 100%;
  min-height: 60px;
  padding: 16px 24px;
  font-size: 18px;
}

.secondary-button {
  margin-top: 12px;
  padding: 12px 22px;
}

.cart-button:hover:not(:disabled),
.cart-button:focus-visible:not(:disabled),
.secondary-button:hover,
.secondary-button:focus-visible {
  color: var(--ink);
  background: #fff;
  transform: translateY(-2px);
}

.cart-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.cart-message {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 16px;
  padding: 13px 15px;
  border-radius: 10px;
  font-weight: 600;
}

.cart-message-success {
  color: #0f5132;
  background: #d1e7dd;
}

.cart-message-danger {
  color: #842029;
  background: #f8d7da;
}

.state-card {
  margin: 80px auto;
  color: var(--muted);
  text-align: center;
}

.state-card p {
  margin-top: 16px;
}

.state-card h1 {
  margin-top: 16px;
  color: var(--ink);
  font-size: 26px;
}

.state-icon {
  font-size: 48px;
}

@media (max-width: 991.98px) {
  .detail-page {
    padding: 48px 32px 80px;
  }

  .product-detail {
    grid-template-columns: minmax(0, 1fr);
    gap: 48px;
  }

  .image-panel {
    min-height: 440px;
  }

  .image-panel img {
    height: 380px;
  }
}

@media (max-width: 767.98px) {
  .detail-page {
    padding: 36px 20px 64px;
  }

  .back-link {
    margin-bottom: 28px;
  }

  .image-panel {
    min-height: 320px;
    border-radius: 14px;
  }

  .image-panel img {
    height: 280px;
  }

  .product-info h1 {
    font-size: 38px;
  }

}
</style>
