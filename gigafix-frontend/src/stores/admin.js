import { defineStore } from 'pinia'
import axios from 'axios'


const fetchAdminInfo = async () =>{
    const response =await axios.get("/api/admin/account/me")
    adminInfo.value = response.data
}

export const useFetchAdminInfoStore = defineStore('adminInfo', {
    state: () => ({
        adminInfo: null,
        loading: false,
        fetched: false
    }),
    actions: {
        async fetchAdmin() {
            if (this.fetched || this.loading) return
            this.loading = true
            try {
                const res = await axios('/api/admin/account/me')
                this.adminInfo = res.data
                this.fetched = true
            } catch (error) {
                console.error('抓取使用者資料失敗', error)
            } finally {
                this.loading = false
            }
        }
    }
})