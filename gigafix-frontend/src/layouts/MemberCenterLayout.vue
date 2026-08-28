<script setup>
import { RouterView } from 'vue-router';
import MemberCenterNavBar from '@/components/MemberCenterNavBar.vue';

</script>

<template>
    <MemberCenterNavBar />
    <main >
        <RouterView />
    </main>
</template>

<style scoped></style>