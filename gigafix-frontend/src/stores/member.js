import { defineStore } from "pinia";
import axios from "axios";


export const useFetchMemberInfoStore = defineStore('testMemberInfo123', {
    state: () => ({
        memberInfo: null,
        loading: false,
        fetched: false
    }),
    actions: {
        async fetchMember(force = false) {
            if (this.loading) return
            //如果已經抓取過個人資料(而且不是強制用force抓取的話)，就強制跳出函數
            if (this.fetched && !force) return
                this.loading = true
            try {
                const res = await axios('/api/gigafix/members/me')
                this.memberInfo = res.data
            } catch (error) {
                this.memberInfo = null
            }
            this.fetched = true
            this.loading = false
        },
        async logoutMember(){
            await axios.post("/api/gigafix/members/logout")
            this.memberInfo = null
            this.fetched = false
        }
    }
})

