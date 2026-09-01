export default [
    {  //陣列裡設定網頁的path
        path: 'member-management',
        name: 'Admin-Member-Management',
        component: () => import('@/features/member/view/MemberManagement.vue')
    }
]
