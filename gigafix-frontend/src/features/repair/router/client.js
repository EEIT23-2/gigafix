export default[
    {
        path: "repair-appointment", //客戶預約維修單，查時段、送出預約都需要登入
        name: "repair-appointment",
        component: () => import("../view/RepairAppointmentView.vue"),
        meta: {
            requiresMember: true
        }
    },
    {
        path: "repair-price-reference", //維修報價參考表，不用登入就能看
        name: "repair-price-reference",
        component: () => import("../view/PriceReferenceView.vue"),
    },
    {
        path: "store-locator", //據點查詢，不用登入就能看
        name: "store-locator",
        component: () => import("../view/StoreLocatorView.vue"),
    }
]