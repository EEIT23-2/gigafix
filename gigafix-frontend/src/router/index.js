import { createRouter, createWebHistory } from "vue-router";

import ClientLayout from "@/layouts/ClientLayout.vue";

//載入member的前後台views陣列
import managerAdminRoutes from "@/features/manager/adminRoutes";
import memberInfoMembercenter from "@/features/member/router/membercenter";
//載入cart的views陣列
import cartClient from "@/features/cart/router/client";
import cartAdminRoutes from "@/features/cart/router/adminRoutes";
import cartMembercenter from "@/features/cart/router/membercenter";
//載入forum的views陣列
import forumClient from "@/features/forum/router/client";
import forumAdminRoutes from "@/features/forum/router/adminRoutes";
import forumMembercenter from "@/features/forum/router/membercenter";
//載入product的views陣列
import productClient from "@/features/product/router/client";
import productAdminRoutes from "@/features/product/router/adminRoutes";
import productMembercenter from "@/features/product/router/membercenter";
//載入repair的views陣列
import repairClient from "@/features/repair/router/client";
import repairAdminRoutes from "@/features/repair/router/adminRoutes";
import repairMembercenter from "@/features/repair/router/membercenter";

import { useFetchAdminInfoStore } from '@/stores/admin'
import { useFetchMemberInfoStore } from "@/stores/member";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/gigafix",
      name: "home",
      component: ClientLayout, //待寫
      children: [
        ...cartClient,
        ...forumClient,
        ...productClient,
        ...repairClient,
        {
          path: "member-center",//不要加/,/是根網址
          name: "member-center",
          component: () => import("@/layouts/MemberCenterLayout.vue"),
          children: [
            ...memberInfoMembercenter,
            ...cartMembercenter,
            ...forumMembercenter,
            ...productMembercenter,
            ...repairMembercenter,
          ]
        }
      ],
    },
    {
      path: "/admin",
      name: "gigafixadmin",
      component: () => import("@/layouts/AdminLayout.vue"),
      children: [
        ...managerAdminRoutes,
        ...cartAdminRoutes,
        ...productAdminRoutes,
        ...forumAdminRoutes,
        ...repairAdminRoutes,
      ],
    }, //NotFound待寫
    {
      path: "/adminLogin",
      name: "adminLogin",
      component: () => import("@/features/manager/view/AdminLogin.vue")
    },
    {
      path: "/creatSuperAdminView",
      name: "creatSuperAdminView",
      component: () => import("@/features/manager/view/CreatSuperAdminView.vue")
    },
  ],
});

const fetchAdminExcludedPaths = ['/adminLogin', '/gigafix', '/creatSuperAdminView']
router.beforeEach(async (to) => {
  if (fetchAdminExcludedPaths.includes(to.path)) {
    return // 排除的路徑直接放行，不觸發抓取使用者資料
  }
  const fetchAdminInfoStore = useFetchAdminInfoStore()
  if (!fetchAdminInfoStore.fetched) {
    await fetchAdminInfoStore.fetchAdmin() // 真正觸發抓資料的動作
  }
})

const fetchMemberExcludedPaths = ['/gigafix', '/admin']
router.beforeEach(async (to) => {
  if (fetchMemberExcludedPaths.includes(to.path)) {
    return // 排除的路徑直接放行，不觸發抓取使用者資料
  }

  const fetchMemberInfoStore = useFetchMemberInfoStore()
  if (!fetchMemberInfoStore.fetched) {
    await fetchMemberInfoStore.fetchMember() // 向後端請求member的資訊
  }
})

export default router;
