import axios from 'axios'

const CART_BASE_URL = '/api/gigafix/members/me/cart'
// 購物車 API
export const getCartItems = () => {
    return axios.get(`${CART_BASE_URL}/items`)
}
// 新增商品到購物車
export const addCartItem = (productId) => {
    return axios.post(`${CART_BASE_URL}/items`, {
        productId
    })
}
// 刪除購物車中的商品
export const deleteCartItem = (cartItemId) => {
    return axios.delete(`${CART_BASE_URL}/items/${cartItemId}`)
}
// 清空購物車
export const clearCart = () => {
    return axios.delete(`${CART_BASE_URL}/items`)
}