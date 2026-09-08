import { useRoute, useRouter } from 'vue-router'
import { useFetchMemberInfoStore } from '@/stores/member'

export const useMemberAuth = () => {
    // 會員驗證相關邏輯
    const router = useRouter()
    const route = useRoute()
    const memberStore = useFetchMemberInfoStore()
    // 檢查會員是否已登入
    const checkMemberLogin = async () => {
        if (!memberStore.fetched) {
            await memberStore.fetchMember()
        }

        if (!memberStore.memberInfo) {
            alert('請先登入會員')

            router.push({
                path: '/',
                query: {
                    redirect: route.fullPath
                }
            })

            return false
        }

        return true
    }

    return {
        checkMemberLogin
    }
}