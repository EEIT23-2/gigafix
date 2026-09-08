import MemberOrderListView from '../view/membercenter/MemberOrderListView.vue'
import MemberOrderDetailView from '../view/membercenter/MemberOrderDetailView.vue'

export default [
    {
        path: 'orders',
        name: 'member-orders',
        component: MemberOrderListView,
        meta: {
            requiresMember: true
        }
    },
    {
        path: 'orders/:orderId',
        name: 'member-order-detail',
        component: MemberOrderDetailView,
        props: true,
        meta: {
            requiresMember: true
        }
    }
]