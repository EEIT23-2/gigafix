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
                this.fetched = true //不管成功失敗都算「嘗試過一次」，避免同一輪導航重複發request；真正擋未登入的邏輯在 router 的 beforeEach（見 router/index.js）
            }
        },
        async logoutAdmin() {
            await axios.post("/api/adminlogout")//登出的後端api不需要body
            this.adminInfo = null
            this.fetched = false
        }
    }
})