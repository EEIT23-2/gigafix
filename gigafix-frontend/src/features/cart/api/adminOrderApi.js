import axios from 'axios'

const ADMIN_ORDER_API = '/api/admin/orders'

//

// ==================== 訂單查詢 ====================

// 查詢全部訂單
export const getOrders = () => {
    return axios.get(ADMIN_ORDER_API)
}

// 查詢單筆訂單
export const getOrder = (orderId) => {
    return axios.get(`${ADMIN_ORDER_API}/${orderId}`)
}

// 依會員查詢訂單
export const getOrdersByMember = (memberId) => {
    return axios.get(`${ADMIN_ORDER_API}/member/${memberId}`)
}

// ========== 訂單新增 / 修改 / 刪除 =============

// 取得新增訂單需要的會員、商品選項
export const getCreateOptions = () => {
    return axios.get(`${ADMIN_ORDER_API}/create-options`)
}

// 新增訂單
export const createOrder = (data) => {
    return axios.post(ADMIN_ORDER_API, data)
}

// 修改訂單
export const updateOrder = (orderId, data) => {
    return axios.put(`${ADMIN_ORDER_API}/${orderId}`, data)
}

// 刪除訂單
export const deleteOrder = (orderId) => {
    return axios.delete(`${ADMIN_ORDER_API}/${orderId}`)
}

// ==================== 訂單狀態操作 ====================

// 訂單出貨
export const shipOrder = (orderId, data) => {
    return axios.post(`${ADMIN_ORDER_API}/${orderId}/ship`, data)
}

// 訂單送達
export const deliverOrder = (orderId) => {
    return axios.post(`${ADMIN_ORDER_API}/${orderId}/deliver`)
}

// 取消訂單
export const cancelOrder = (orderId) => {
    return axios.post(`${ADMIN_ORDER_API}/${orderId}/cancel`)
}

// ==================== Demo 付款 ====================

// 模擬會員付款
// 注意：這是會員端 API，目前後台僅用於 Demo
export const payOrder = (memberId, orderId, data) => {
    return axios.post(
        `/api/members/${memberId}/orders/${orderId}/payment`,
        data
    )
}