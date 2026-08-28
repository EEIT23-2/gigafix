export default [
    {  //陣列裡設定網頁的path
        path: 'orders',
        name: 'Admin-Order',
        component: () => import('@/features/cart/view/AdminOrderView.vue'),
        props: true
    },
    {
        path: 'orders/:orderId/edit',
        name: 'Admin-Order-Edit',
        component: () => import('@/features/cart/view/AdminOrderEditView.vue'),
        props: true
    },
    {
        path: 'orders/create',
        name: 'Admin-Order-Create',
        component: () => import('@/features/cart/view/AdminOrderCreateView.vue'),
        props: true
    },
    {
        path: 'orders/:orderId',
        name: 'Admin-Order-Detail',
        component: () => import('@/features/cart/view/AdminOrderDetailView.vue'),
        props: true
    },
    {
        path: 'orders/:orderId/ship',
        name: 'Admin-Order-Ship',
        component: () => import('@/features/cart/view/AdminOrderShipView.vue'),
        props: true
    }
]