import { useFetchMemberInfoStore } from "@/stores/member";

// ★改：沒登入(例如直接點通知信連結)就導去首頁並自動開登入視窗，登入成功後由ClientNavBar導回原網址
// 此時全域beforeEach已經抓完會員資料，直接看memberInfo即可
const requireMemberLogin = (to) => {
    if (!useFetchMemberInfoStore().memberInfo) {
        return { path: "/", query: { login: "1", redirect: to.fullPath } };
    }
};

export default[
    {  //會員中心「維修進度」：客戶自己的維修單列表
        path: "repair",
        name: "membercenter-repair-list",
        component: () => import("../view/RepairProgressListView.vue"),
        beforeEnter: requireMemberLogin, // ★改
    },
    {  //會員中心「維修進度」：客戶自己的維修單明細（唯讀，可回應報價）
        path: "repair/:repairId",
        name: "membercenter-repair-detail",
        component: () => import("../view/RepairProgressDetailView.vue"),
        props: true,
        beforeEnter: requireMemberLogin, // ★改
    }
]