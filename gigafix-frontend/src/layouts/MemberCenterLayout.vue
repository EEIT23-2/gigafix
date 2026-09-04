<script setup>
import { RouterView } from 'vue-router';
import MemberCenterNavBar from '@/components/MemberCenterNavBar.vue';

</script>

<template>
  <div class="member-center-wrapper">
    <main class="member-center-main">
      <RouterView />
    </main>
    <MemberCenterNavBar />
  </div>
</template>

<style scoped>
.member-center-wrapper {
  display: flex;
  width: 100%;
  min-height: calc(100vh - 140px);
}

.member-center-main {
  flex: 1;
  padding: 32px 24px 32px 40px; /* 上 右 下 左，左邊留適當間距即可，不用太大 */
}
</style>