
//狀態顯示轉換中文
// ==================== 訂單狀態 ====================

export const orderStatusText = (status) => {
    const statusMap = {
        PENDING: '待處理',
        CANCELLED: '已取消',
        COMPLETED: '已完成'
    }

    return statusMap[status] || status
}

// ==================== 付款狀態 ====================

export const paymentStatusText = (status) => {
    const statusMap = {
        UNPAID: '未付款',
        PAID: '已付款',
        FAILED: '付款失敗',
        REFUNDED: '已退款'
    }

    return statusMap[status] || status
}

// ==================== 物流狀態 ====================

export const shippingStatusText = (status) => {
    const statusMap = {
        PENDING: '待出貨',
        SHIPPED: '已出貨',
        DELIVERED: '已送達',
        CANCELLED: '已取消'
    }

    return statusMap[status] || status
}

// ==================== 付款方式 ====================

export const paymentMethodText = (method) => {
    const methodMap = {
        CREDIT_CARD: '信用卡',
        CASH_ON_DELIVERY: '貨到付款'
    }

    return methodMap[method] || method
}

// ==================== 配送方式 ====================

export const shippingMethodText = (method) => {
    const methodMap = {
        HOME: '宅配',
        STORE: '超商取貨'
    }

    return methodMap[method] || method
}

// ==================== Badge 樣式 ====================

// 訂單狀態樣式
export const orderStatusClass = (status) => {
    const classMap = {
        PENDING: 'text-bg-warning',
        COMPLETED: 'text-bg-success',
        CANCELLED: 'text-bg-danger'
    }

    return classMap[status] || 'text-bg-secondary'
}
// 付款狀態樣式
export const paymentStatusClass = (status) => {
    const classMap = {
        UNPAID: 'text-bg-secondary',
        PAID: 'text-bg-success',
        FAILED: 'text-bg-danger',
        REFUNDED: 'text-bg-info'
    }

    return classMap[status] || 'text-bg-secondary'
}

// 物流狀態樣式
export const shippingStatusClass = (status) => {
    const classMap = {
        PENDING: 'text-bg-secondary',
        SHIPPED: 'text-bg-primary',
        DELIVERED: 'text-bg-success',
        CANCELLED: 'text-bg-danger'
    }

    return classMap[status] || 'text-bg-secondary'
}