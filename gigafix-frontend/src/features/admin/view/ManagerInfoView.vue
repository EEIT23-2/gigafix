<script setup>
import AdminInfoModel from '@/features/admin/component/AdminInfoModel.vue'
import SuperAdminModel from '@/features/admin/component/SuperAdminModel.vue'

import { useFetchAdminInfoStore } from '@/stores/admin';
import { storeToRefs } from 'pinia'

const fetchAdminInfoStore = useFetchAdminInfoStore()
const { adminInfo } = storeToRefs(fetchAdminInfoStore)

</script>

<template>
    <AdminInfoModel v-if="adminInfo"/>
    <SuperAdminModel v-if="adminInfo && adminInfo.role === 'ROLE_SUPER_ADMIN'"/>
</template>

<style scoped></style>