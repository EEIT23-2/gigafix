const ORDER_BASE_URL =
    '/api/gigafix/members/me/orders'

export const redirectToEcpayPayment = (orderId) => {
    if (!orderId) {
        throw new Error('orderId 不可為空')
    }

    window.location.assign(
        `${ORDER_BASE_URL}/${orderId}/ecpay-payment`
    )
}