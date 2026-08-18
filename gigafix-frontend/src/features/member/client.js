export default[
    {  //陣列裡設定網頁的path
        path:'member',
        name:'membertest',
        component: () => import('@/features/member/view/MemberTest.vue')
    }
]