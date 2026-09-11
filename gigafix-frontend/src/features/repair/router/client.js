export default[
    {
        path: "repair-appointment", //客戶預約維修單
        name: "repair-appointment",
        component: () => import("../view/RepairAppointmentView.vue"),
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