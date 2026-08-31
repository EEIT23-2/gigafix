export default[
    {  //陣列裡設定網頁的path
        path:"MemberInfo",
        name:"MemberInfo",
        component: () => import("@/features/member/view/MemberInfo.vue"),
    }
]