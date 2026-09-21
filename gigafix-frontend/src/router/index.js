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
      component: ClientLayout, //待寫
      children: [
        {
          // 空字串代表父層網址「/」本身；內容會顯示在 ClientLayout 的 RouterView 內。
          path: "",
          name: "home",
          component: () => import("@/views/HomePageView.vue"),
        },
        {
          path: "about-gigafix",
          name: "about-gigafix",
          component: () => import("@/views/AboutGigafix.vue"),
        },
        ...cartClient,
        ...forumClient,
        ...productClient,
        ...repairClient,
        {
          path: "member-center",//不要加/,/是根網址
          name: "member-center",
          component: () => import("@/layouts/MemberCenterLayout.vue"),
          redirect: { name: "MemberInfo" }, //直接進/member-center時預設導向會員資料頁，不然RouterView會是空的
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
  // adminInfo只有/admin底下的頁面會用到，前台網址不用抓，避免沒登入的訪客逛前台時
  // 也打了/api/admin/account/me拿到401，被admin.js的全域攔截器誤判成「後台session過期」導去/adminLogin
  if (!to.path.startsWith('/admin')) {
    return
  }
  const fetchAdminInfoStore = useFetchAdminInfoStore()
  if (!fetchAdminInfoStore.fetched) {
    await fetchAdminInfoStore.fetchAdmin() // 真正觸發抓資料的動作
  }

  //後台網址沒登入就彈回後台登入頁，避免沒登入卻能停在 /admin 底下看到空白畫面
  if (!fetchAdminInfoStore.adminInfo) {
    return { name: 'adminLogin' }
  }

  //路由有設定meta.roles的頁面，角色不在清單內就彈回後台首頁(目前只有維修後台的路由有設定)
  const allowedRoles = to.meta.roles
  if (allowedRoles && !allowedRoles.includes(fetchAdminInfoStore.adminInfo.role)) {
    alert('你的權限不足')
    return { name: 'manager' }
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
