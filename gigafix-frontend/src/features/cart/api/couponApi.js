import axios from 'axios'

export const getAvailableCoupons = () => {
    return axios.get('/api/gigafix/members/me/coupons')
}