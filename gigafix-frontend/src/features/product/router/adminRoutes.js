export default [
  {
    path: "products",
    name: "admin-products",
    component: () => import("../view/ProductsListView.vue"),
  },
  {
    path: "products/create",
    name: "admin-product-create",
    component: () => import("../view/ProductCreateView.vue"),
  },
  {
    path: "products/:productId",
    name: "admin-product-detail",
    component: () => import("../view/ProductDetailView.vue"),
    props: true,
  },
  {
    path: "products/:productId/edit",
    name: "admin-product-edit",
    component: () => import("../view/ProductEditView.vue"),
    props: true,
  },
  /////下面是回收單跳轉要用的
  {
    path: "applyForms",
    name: "admin-applyForms",
    component: () => import("../view/ApplyFormsListView.vue"),
  },
  {
    path: "applyForms/:applyId",
    name: "admin-applyForms-detail",
    component: () => import("../view/ApplyFormDetailView.vue"),
  },
  {
    path: "applyForms/:applyId/edit",
    name: "admin-applyForms-edit",
    component: () => import("../view/ApplyFormEditView.vue"),
  },
  // {  //陣列裡設定網頁的path
  //     path:,
  //     name:,
  //     component:,
  //     props: true
  // }
];
