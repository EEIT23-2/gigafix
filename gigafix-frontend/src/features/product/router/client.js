export default [
  {
    path: "mall",
    name: "mall-list",
    component: () => import("../view/client/MallListView.vue"),
  },
  {
    path: "mall/:productId",
    name: "mall-detail",
    component: () => import("../view/client/MallDetailView.vue"),
    props: true,
  },
  {
    path: "recycle",
    component: () => import("../components/client/RecycleLayout.vue"),
    redirect: { name: "recycle-form" },
    children: [
      {
        path: "application",
        name: "recycle-form",
        component: () => import("../view/client/RecycleEnterView.vue"),
      },
      {
        path: "iphone-prices",
        name: "recycle-iphone-prices",
        component: () => import("../view/client/OldiPhonePricePriceList.vue"),
      },
      {
        path: "ipad-prices",
        name: "recycle-ipad-prices",
        component: () => import("../view/client/OldiPadPriceListView.vue"),
      },
      {
        path: "watch-prices",
        name: "recycle-watch-prices",
        component: () => import("../view/client/OldWatchPriceListView.vue"),
      },
    ],
  },
  // {  //陣列裡設定網頁的path
  //     path:,
  //     name:,
  //     component:,
  //     props: true
  // }
];
