import { compile } from "vue";

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
  // {  //陣列裡設定網頁的path
  //     path:,
  //     name:,
  //     component:,
  //     props: true
  // }
];
