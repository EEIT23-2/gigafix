<script setup>
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { useFetchMemberInfoStore } from "@/stores/member";
import howToRecycleImage from "@/assets/jack/howToRecycle.jpg";
import whatWeRecycleImage from "@/assets/jack/whatWeRecycle.jpg";
import RecycleApplyForm from "../../components/client/RecycleApplyForm.vue";

const fetchMemberInfoStore = useFetchMemberInfoStore();
const { memberInfo } = storeToRefs(fetchMemberInfoStore);
const showApplyForm = ref(false);

const openLoginModal = () => {
  const loginButton = document.querySelector(
    ".user-actions button.action-item",
  );
  loginButton?.click();
};

const goToApplyForm = () => {
  if (!memberInfo.value) {
    openLoginModal();
    return;
  }

  showApplyForm.value = true;
  window.scrollTo({ top: 0, behavior: "smooth" });
};
</script>

<template>
  <RecycleApplyForm v-if="showApplyForm" @back="showApplyForm = false" />

  <main v-else class="recycle-page">
    <section class="recycle-content" aria-label="二手裝置回收資訊">
      <figure class="recycle-card">
        <img
          class="recycle-image"
          :src="howToRecycleImage"
          alt="二手手機回收流程"
        />
      </figure>

      <figure class="recycle-card">
        <img
          class="recycle-image"
          :src="whatWeRecycleImage"
          alt="收購項目：iPhone、iPad 與 Apple Watch"
        />
      </figure>

      <button class="apply-button" type="button" @click="goToApplyForm">
        點我填寫回收申請單
        <i class="bi bi-arrow-right" aria-hidden="true"></i>
      </button>
    </section>
  </main>
</template>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;500;600;700&family=Noto+Sans+TC:wght@400;500;600;700&display=swap");

.recycle-page {
  --ink: #1b1b1b;
  --muted: #635d5e;
  --soft: #f9f9f9;
  min-height: 100%;
  padding: 0 0 32px;
  color: var(--ink);
  background: #fff;
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
}

.recycle-content {
  width: min(100%, 1320px);
  margin: 0 auto;
}

.recycle-card {
  margin: 0 0 32px;
  overflow: hidden;
  border: 1px solid #eeeeee;
  border-radius: 20px;
  background: var(--soft);
  box-shadow: 0 24px 60px -36px rgba(0, 0, 0, 0.28);
}

.recycle-image {
  display: block;
  width: 100%;
  height: auto;
}

.apply-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  width: 100%;
  margin-top: 48px;
  padding: 22px 32px;
  border: 2px solid var(--ink);
  border-radius: 14px;
  color: #fff;
  background: var(--ink);
  font-family: inherit;
  font-size: clamp(18px, 2vw, 24px);
  font-weight: 700;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.apply-button:hover,
.apply-button:focus-visible {
  color: var(--ink);
  background: #fff;
  box-shadow: 0 18px 36px -20px rgba(0, 0, 0, 0.45);
  transform: translateY(-2px);
  outline: none;
}

.apply-button:active {
  transform: translateY(0);
}

.apply-button i {
  font-size: 1.1em;
  transition: transform 0.2s ease;
}

.apply-button:hover i,
.apply-button:focus-visible i {
  transform: translateX(4px);
}

@media (max-width: 767.98px) {
  .recycle-card {
    margin-bottom: 20px;
    border-radius: 14px;
  }

  .apply-button {
    margin-top: 32px;
    padding: 18px 24px;
    border-radius: 12px;
  }
}
</style>
