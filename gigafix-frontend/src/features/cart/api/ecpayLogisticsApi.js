export const redirectToEcpayStoreMap = (logisticsSubType) => {
    if (!logisticsSubType) {
        throw new Error('logisticsSubType 不可為空')
    }

    const params = new URLSearchParams({
        logisticsSubType
    })

    window.location.assign(
        `/api/gigafix/ecpay/logistics/store-map?${params.toString()}`
    )
}