import { createRouter, createWebHistory } from 'vue-router'
import ClientLayout from '@/layouts/ClientLayout.vue'

//載入member的前後台views陣列
import memberClient from '@/features/member/client'
import managerAdminRoutes from '@/features/manager/adminRoutes'
//載入cart的views陣列
import cartClient from '@/features/cart/client'
import cartAdminRoutes from '@/features/cart/adminRoutes'
//載入forum的views陣列
import forumClient from '@/features/forum/client'
import forumAdminRoutes from '@/features/forum/adminRoutes'
//載入product的views陣列
import productClient from '@/features/product/client'
import productAdminRoutes from '@/features/product/adminRoutes'
//載入repair的views陣列
import repairClient from '@/features/repair/client'
import repairAdminRoutes from '@/features/repair/adminRoutes'


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/gigafix',
      name: 'home',
      component: ClientLayout, //待寫
      children: [
        ...memberClient,
        ...cartClient,
        ...forumClient,
        ...productClient,
        ...repairClient
      ]
    },
    {
      path: '/admin',
      name: 'gigafixadmin',
      component: () => import('@/layouts/AdminLayout.vue'), //待寫
      children: [
        ...managerAdminRoutes,
        ...cartAdminRoutes,
        ...productAdminRoutes,
        ...forumAdminRoutes,
        ...repairAdminRoutes
      ]  
    },  //NotFound待寫
  ],
})

export default router
