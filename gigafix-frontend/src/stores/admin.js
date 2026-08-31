import { defineStore } from 'pinia'
import axios from 'axios'


export const useFetchAdminInfoStore = defineStore('adminInfo', {
    state: () => ({
        adminInfo: null,
        loading: false,
        fetched: false
    }),
    actions: {
        async fetchAdmin() {
            if (this.fetched || this.loading) return //避免同一份資料被重複抓取
                this.loading = true
            try {
                const res = await axios('/api/admin/account/me')
                this.adminInfo = res.data
            } catch (error) {
                this.adminInfo = null //清空避免前端null exception
            } finally {
                this.loading = false
                this.fetched = true //因為不管是否取得資料都嘗試過一次，所以設為true避免沒拿到資料就無窮迴圈(因為沒有擋未登入就不能進入管理頁面)
            }
        },
        async logoutAdmin() {
            await axios.post("/api/adminlogout")//登出的後端api不需要body
            this.adminInfo = null
            this.fetched = false
        }
    }
})