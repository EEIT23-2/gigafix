import CartView from '../view/client/CartView.vue'
import CheckoutView from '../view/client/CheckoutView.vue'

export default [
    {
        path: '/cart',
        name: 'cart',
        component: CartView,
        meta: {
            requiresMember: true
        }
    },
    {
        path: '/checkout',
        name: 'checkout',
        component: CheckoutView,
        meta: {
            requiresMember: true
        }
    }
]