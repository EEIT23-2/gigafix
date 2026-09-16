import { defineStore } from 'pinia'
import axios from 'axios'

// 避免同一批並發的401請求重複跳提示、重複導頁(例如同一頁面同時打了好幾支後台API，剛好session都已過期)
let isHandlingSessionExpired = false
// 同上，避免同一批並發的403請求重複跳提示、重複導頁
let isHandlingForbidden = false

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
        },
        // 後台session過期(閒置逾時、或同帳號在別裝置登入把這裡的session踢掉)時的統一處理。
        // router的beforeEach只在「導覽到新頁面」時才會重新檢查登入狀態，如果使用者一直停在同一個
        // 後台頁面不動，session在背景過期完全不會被前端發現，直到點下某個會打API的按鈕才會收到401，
        // 所以改用這個攔截器統一處理：不管當下打的是哪支後台API，只要收到401，就清掉前端快取的登入
        // 狀態並導回登入頁。要在main.js裡、Pinia裝好之後呼叫一次，把目前用的router傳進來
        installSessionExpiredInterceptor(router) {
            axios.interceptors.response.use(
                (response) => response,
                (error) => {
                    const status = error.response?.status
                    const url = error.config?.url || ''
                    // /api/adminlogin的401是帳號密碼錯(登入頁自己的catch已經處理)，/api/adminlogout現在是
                    // permitAll不會因session過期而401，兩者都不屬於「session過期」，只攔截真正需要登入才能打的/api/admin/**
                    if (status === 403 && url.startsWith('/api/admin/')) {
                        // 「要不要吞掉這個錯誤」跟「要不要真的跳提示、導頁」分開判斷：
                        // 同一頁面常常會同時打好幾支API，全部一起403，這幾個request的錯誤都要吞掉，
                        // 只是負責跳提示、導頁的只需要第一個來做，避免重複跳好幾次alert、導頁好幾次
                        if (!isHandlingForbidden && router.currentRoute.value.name !== 'manager') {
                            isHandlingForbidden = true
                            router.push({ name: 'manager' }).finally(() => {
                                isHandlingForbidden = false
                            }).then(() => {
                                alert('你的權限不足') // 先導頁再跳提示，跟原本頁面切乾淨後才提示，避免使用者還在舊頁面上看到彈窗
                            })
                        }
                        return new Promise(() => {}) // 故意不resolve/reject，讓原本呼叫端的.then()/.catch()都不會再執行，
                        // 這樣舊頁面裡的alert/fallback文字就不會被設進去，不管是不是第一個收到403的request都一樣要吞掉
                    }

                    if (status === 401 && url.startsWith('/api/admin/') && !isHandlingSessionExpired) {
                        isHandlingSessionExpired = true
                        this.adminInfo = null
                        this.fetched = false // 讓下次導覽時guard重新抓一次，而不是繼續沿用過期的舊快取

                        if (router.currentRoute.value.name === 'adminLogin') {
                            isHandlingSessionExpired = false // 已經在登入頁了，不用再跳提示、再導頁
                        } else {
                            alert('登入已逾時或尚未登入，請重新登入')
                            router.push({ name: 'adminLogin' }).finally(() => {
                                isHandlingSessionExpired = false
                            })
                        }
                    }
                    return Promise.reject(error) // 還是要reject，讓原本呼叫端的catch/alert照常運作
                },
            )
        }
    }
})