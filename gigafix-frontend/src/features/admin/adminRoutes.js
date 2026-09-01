export default[
    {  //陣列裡設定網頁的path
        path:'manager',
        name:'manager',
        component:() => import('@/features/admin/view/ManagerInfoView.vue')
    }
]