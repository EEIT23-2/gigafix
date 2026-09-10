import axios from 'axios'

const ORDER_BASE_URL = '/api/gigafix/members/me/orders'
// 會員訂單 API
export const createOrder = (request) => {
    return axios.post(ORDER_BASE_URL, request)
}
// 取得會員的所有訂單
export const getMemberOrders = () => {
    return axios.get(ORDER_BASE_URL)
}
// 取得會員的單一訂單
export const getMemberOrder = (orderId) => {
    return axios.get(`${ORDER_BASE_URL}/${orderId}`)
}
// 取消會員訂單
export const cancelMemberOrder = (orderId) => {
    return axios.post(`${ORDER_BASE_URL}/${orderId}/cancel`)
}