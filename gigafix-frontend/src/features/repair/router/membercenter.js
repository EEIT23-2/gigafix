export default[
    {  //會員中心「維修進度」：客戶自己的維修單列表
        path: "repair",
        name: "membercenter-repair-list",
        component: () => import("../view/RepairProgressListView.vue"),
    },
    {  //會員中心「維修進度」：客戶自己的維修單明細（唯讀，可回應報價）
        path: "repair/:repairId",
        name: "membercenter-repair-detail",
        component: () => import("../view/RepairProgressDetailView.vue"),
        props: true,
    }
]