import { createRouter, createWebHistory } from "vue-router";

import ClientLayout from "@/layouts/ClientLayout.vue";

//載入member的前後台views陣列
import managerAdminRoutes from "@/features/admin/adminRoutes";
import memberInfoMembercenter from "@/features/member/router/membercenter";
import memberAdminRoutes from "@/features/member/router/adminRoutes";
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
      path: "/",
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
        },
        //前台404，放在children最後一筆，前面都比對不到才會落到這裡
        {
          path: ":pathMatch(.*)*",
          name: "clientNotFound",
          component: () => import("@/views/ClientNotFound.vue"),
        },
      ],
    },
    {
      path: "/admin",
      name: "gigafixadmin",
      component: () => import("@/layouts/AdminLayout.vue"),
      redirect: { name: "manager" }, //單獨打 /admin 時，導去管理員資訊頁當後台首頁
      children: [
        ...managerAdminRoutes,
        ...memberAdminRoutes,
        ...cartAdminRoutes,
        ...productAdminRoutes,
        ...forumAdminRoutes,
        ...repairAdminRoutes,
        //後台404，放在children最後一筆，前面都比對不到才會落到這裡
        {
          path: ":pathMatch(.*)*",
          name: "adminNotFound",
          component: () => import("@/views/AdminNotFound.vue"),
        },
      ],
    },
    {
      path: "/adminLogin",
      name: "adminLogin",
      component: () => import("@/features/admin/view/AdminLogin.vue")
    },
    {
      path: "/creatSuperAdminView",
      name: "creatSuperAdminView",
      component: () => import("@/features/admin/view/CreatSuperAdminView.vue")
    },
  ],
});

const fetchAdminExcludedPaths = ['/adminLogin', '/', '/creatSuperAdminView']
router.beforeEach(async (to) => {
  if (fetchAdminExcludedPaths.includes(to.path)) {
    return // 排除的路徑直接放行，不觸發抓取使用者資料
  }
  const fetchAdminInfoStore = useFetchAdminInfoStore()
  if (!fetchAdminInfoStore.fetched) {
    await fetchAdminInfoStore.fetchAdmin() // 真正觸發抓資料的動作
  }

  //後台網址沒登入就彈回後台登入頁，避免沒登入卻能停在 /admin 底下看到空白畫面
  if (to.path.startsWith('/admin') && !fetchAdminInfoStore.adminInfo) {
    return { name: 'adminLogin' }
  }
})

const fetchMemberExcludedPaths = ['/admin']
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
