import { defineStore } from "pinia";
import axios from "axios";


export const useFetchMemberInfoStore = defineStore('testMemberInfo123', {
    state: () => ({
        memberInfo: null,
        loading: false,
        fetched: false
    }),
    actions: {
        async fetchMember() {
            if (this.fetched || this.loading) return
                this.loading = true
            try {
                const res = await axios('/api/gigafix/members/me')
                this.memberInfo = res.data
                this.fetched = true
            } catch (error) {
                this.memberInfo = null
            }
            this.loading = false
        },
        async logoutMember(){
            await axios.post("/api/gigafix/members/logout")
            this.memberInfo = null
            this.fetched = false
        }
    }
})

