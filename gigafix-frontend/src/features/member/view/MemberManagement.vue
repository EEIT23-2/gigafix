<script setup>
import axios from 'axios'
import { ref, onMounted, watch } from 'vue'
import BaseModal from '../component/BaseModal.vue'
import AddressSelect from '@/components/AddressSelect.vue'
import { parseTaiwanAddress } from '@/static/taiwanDistricts'
import MemberGrowthChart from '../component/MemberGrowthChart.vue'
import MemberDistrictHeatmap from '../component/MemberDistrictHeatmap.vue'

const allMembers = ref([]) //目前這一頁的會員(給表格用)
const allMembersForStats = ref([]) //全部會員(不分頁，給上面兩個圖表統計用)
const errorMsg = ref('')

// ====分頁相關====
const currentPage = ref(0) //跟後端一樣從0開始算
const pageSize = 20
const totalPages = ref(0)
const totalElements = ref(0)

//性別代碼轉中文顯示用的對照表
const genderLabelMap = {
    MALE: '男',
    FEMALE: '女',
}
const genderLabel = (gender) => genderLabelMap[gender] ?? '未提供'

//後端LocalDateTime格式化成yyyy/MM/dd HH:mm:ss顯示
const formatDateTime = (ldtString) => {
    if (!ldtString) return ''

    // 拆出日期跟時間兩部分
    const [datePart, timePart] = ldtString.split('T')
    if (!datePart || !timePart) return ldtString // 格式不符預期時,原樣顯示,不讓畫面壞掉

    const [year, month, day] = datePart.split('-')

    // 時間部分可能帶奈秒(.8387595),只取到秒
    const [hour, minute, secondWithNano] = timePart.split(':')
    const second = secondWithNano.split('.')[0]

    return `${year}/${month}/${day} ${hour}:${minute}:${second}`
}

//====取得會員列表(分頁，一次20筆，給表格用)====
const fetchAllMembers = async (page = 0) => {
    errorMsg.value = ''
    try {
        const rep = await axios.get('/api/admin/members', { params: { page, size: pageSize } })
        allMembers.value = rep.data.content
        currentPage.value = rep.data.number
        totalPages.value = rep.data.totalPages
        totalElements.value = rep.data.totalElements
    } catch (err) {
        alert(`會員列表讀取失敗，原因: ${err.response.data.message}`)
        errorMsg.value = '無法載入會員列表,請稍後再試'
    }
}

//====取得全部會員(不分頁，只給上面兩個統計圖表用，跟表格的分頁資料分開抓，避免圖表只統計到當頁20筆)====
const fetchAllMembersForStats = async () => {
    try {
        const rep = await axios.get('/api/admin/members', { params: { page: 0, size: 1000 } }) //size給大一點，一次撈全部會員
        allMembersForStats.value = rep.data.content
    } catch (err) {
        //統計圖表載入失敗不影響會員列表的主要功能，不特別跳alert打擾使用者
    }
}

//切換分頁頁碼，超出範圍或點目前這頁就不動作
const goToPage = (page) => {
    if (page < 0 || page >= totalPages.value || page === currentPage.value) {
        return
    }
    fetchAllMembers(page)
}

// ====修改會員資料的相關宣告====
const showEditModal = ref(false)
const editMemberId = ref(null)
const editRealName = ref('')
const editNickName = ref('')
const editEmail = ref('')
const editPhone = ref('')
const editAddressCity = ref('')
const editAddressDistrict = ref('')
const editAddressDetail = ref('')
const editGender = ref('')
const editSubmitting = ref(false)
const editErrorMsg = ref('')

//開啟彈窗前，把表單值同步成該會員目前的資料
const openEditModal = (member) => {
    editMemberId.value = member.id
    editRealName.value = member.realName
    editNickName.value = member.nickName
    editEmail.value = member.email
    editPhone.value = member.phone
    const parsedAddress = parseTaiwanAddress(member.address) //把既有地址字串拆回縣市/行政區/詳細地址，帶回下拉選單
    editAddressCity.value = parsedAddress.city
    editAddressDistrict.value = parsedAddress.district
    editAddressDetail.value = parsedAddress.detail
    editGender.value = member.gender
    checkEditError() //資料是既有的會員資料，理論上都合法，這裡順便算一次讓按鈕正確顯示成可送出
    showEditModal.value = true
}

//每次欄位變動就重新檢查一次
const checkEditError = () => {
    if (!editRealName.value.trim()) {
        editErrorMsg.value = '真實姓名不可為空'
    } else if (editRealName.value.length > 40) {
        editErrorMsg.value = '真實姓名字數上限為40'
    } else if (!editNickName.value.trim()) {
        editErrorMsg.value = '暱稱不可為空'
    } else if (editNickName.value.length > 40) {
        editErrorMsg.value = '暱稱字數上限為40'
    } else if (!editEmail.value.trim()) {
        editErrorMsg.value = 'Email不可為空'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editEmail.value)) {
        editErrorMsg.value = 'Email格式錯誤'
    } else if (!editPhone.value.trim()) {
        editErrorMsg.value = '手機號碼不可為空'
    } else if (!/^09\d{8}$/.test(editPhone.value)) {
        editErrorMsg.value = '手機號碼格式錯誤，需為09開頭的10碼數字'
    } else if (!editAddressCity.value) {
        editErrorMsg.value = '請選擇縣市'
    } else if (!editAddressDistrict.value) {
        editErrorMsg.value = '請選擇行政區'
    } else if (!editAddressDetail.value.trim()) {
        editErrorMsg.value = '請輸入詳細地址'
    } else if (!editGender.value) {
        editErrorMsg.value = '請選擇性別'
    } else {
        editErrorMsg.value = ''
    }
}
//AddressSelect選縣市/行政區或打詳細地址都會更新這三個值，一併重新驗證讓送出鈕即時反映
watch([editAddressCity, editAddressDistrict, editAddressDetail], () => checkEditError())

//送出修改會員資料表單(整包資料送給後端PUT /api/admin/members/{memberId})
const submitEdit = async () => {
    editSubmitting.value = true
    try {
        await axios.put(`/api/admin/members/${editMemberId.value}`, {
            realName: editRealName.value,
            nickName: editNickName.value,
            email: editEmail.value,
            phone: editPhone.value,
            address: `${editAddressCity.value}${editAddressDistrict.value}${editAddressDetail.value}`,
            gender: editGender.value
        })
        await fetchAllMembers(currentPage.value) //留在原本那一頁重新整理
        await fetchAllMembersForStats() //地址可能改了，圖表的統計也要跟著更新
        showEditModal.value = false
        alert('會員資料修改成功！')
    } catch (err) { //回傳4xx,5xx，後端Bean Validation的錯誤訊息會放在message
        const message = err.response?.data?.message || '請稍後再試'
        alert(`修改失敗，原因: ${message}`)
    } finally {
        editSubmitting.value = false
    }
}

// ====刪除會員的相關函式====
//管理員刪除會員不需要驗證該會員密碼，用瀏覽器原生confirm再次確認即可(跟AdminOrderView刪除訂單的作法一致)
const deleteMember = async (member) => {
    const confirmed = confirm(`確定要刪除會員「${member.nickName}」嗎？此操作無法復原。`)
    if (!confirmed) {
        return
    }
    try {
        await axios.delete(`/api/admin/members/${member.id}`)
        //如果刪掉的是目前這頁最後一筆(例如最後一頁只剩1筆)，留在原頁會變成空頁，往回一頁比較合理
        const pageToShow = (allMembers.value.length === 1 && currentPage.value > 0) ? currentPage.value - 1 : currentPage.value
        await fetchAllMembers(pageToShow)
        await fetchAllMembersForStats() //會員總數變了，圖表的統計也要跟著更新
        alert('刪除成功')
    } catch (err) { //回傳4xx,5xx
        const message = err.response?.data?.message || '請稍後再試'
        alert(`刪除會員失敗，原因: ${message}`)
    }
}

//只要元件掛載就去抓會員列表(第一頁)跟圖表用的全部會員
onMounted(() => {
    fetchAllMembers()
    fetchAllMembersForStats()
})
</script>

<template>
    <h1>會員管理</h1>

    <!-- 會員累計成長曲線 + 行政區分布熱點圖，左右並排 -->
    <div class="row g-3">
        <div class="col-md-6">
            <MemberGrowthChart />
        </div>
        <div class="col-md-6">
            <MemberDistrictHeatmap :members="allMembersForStats" />
        </div>
    </div>

    <!-- 所有會員的資料 -->
    <div class="card shadow mb-4 overflow-hidden">
        <div class="card-header py-3">
            <h6 class="m-0 fw-bold text-primary">會員列表</h6>
        </div>
        <p v-if="errorMsg" class="text-danger px-3 pt-3 mb-0">{{ errorMsg }}</p>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead>
                    <tr class="text-muted">
                        <th>編號</th>
                        <th>真實姓名</th>
                        <th>暱稱</th>
                        <th>Email</th>
                        <th>手機號碼</th>
                        <th>地址</th>
                        <th>性別</th>
                        <th>加入時間</th>
                        <th class="text-center">操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="member in allMembers" :key="member.id">
                        <td>{{ member.id }}</td>
                        <td class="cell-truncate">{{ member.realName }}</td>
                        <td class="cell-truncate">{{ member.nickName }}</td>
                        <td class="cell-truncate">{{ member.email }}</td>
                        <td>{{ member.phone }}</td>
                        <td class="cell-truncate">{{ member.address }}</td>
                        <td>{{ genderLabel(member.gender) }}</td>
                        <td>{{ formatDateTime(member.createTime) }}</td>
                        <td>
                            <div class="d-flex gap-2 justify-content-center">
                                <button class="btn btn-sm btn-outline-warning square-btn" title="修改該會員資料" @click="openEditModal(member)">
                                    <i class="bi bi-pencil-square"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-danger square-btn" title="刪除該會員" @click="deleteMember(member)">
                                    <i class="bi bi-trash-fill"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- 分頁控制，一頁20筆 -->
        <div v-if="totalElements > 0" class="d-flex flex-column align-items-center gap-2 py-3 border-top">
            <p class="text-muted small mb-0">共 {{ totalElements }} 筆會員</p>
            <nav v-if="totalPages > 1" aria-label="會員列表分頁">
                <ul class="pagination mb-0">
                    <li class="page-item" :class="{ disabled: currentPage === 0 }">
                        <button class="page-link" @click="goToPage(currentPage - 1)">上一頁</button>
                    </li>
                    <li v-for="pageNum in totalPages" :key="pageNum" class="page-item" :class="{ active: pageNum - 1 === currentPage }">
                        <button class="page-link" @click="goToPage(pageNum - 1)">{{ pageNum }}</button>
                    </li>
                    <li class="page-item" :class="{ disabled: currentPage === totalPages - 1 }">
                        <button class="page-link" @click="goToPage(currentPage + 1)">下一頁</button>
                    </li>
                </ul>
            </nav>
        </div>
    </div>

    <!-- 修改會員資料的彈窗 -->
    <BaseModal v-model="showEditModal">
        <template #title>修改會員資料</template>

        <label class="form-label">真實姓名</label>
        <input type="text" class="form-control mb-3" v-model="editRealName" maxlength="40" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">暱稱</label>
        <input type="text" class="form-control mb-3" v-model="editNickName" maxlength="40" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">Email</label>
        <input type="email" class="form-control mb-3" v-model="editEmail" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">手機號碼</label>
        <input type="text" class="form-control mb-3" v-model="editPhone" placeholder="09xxxxxxxx" :disabled="editSubmitting" @input="checkEditError()">

        <label class="form-label">地址</label>
        <div class="mb-3">
            <AddressSelect v-model:city="editAddressCity" v-model:district="editAddressDistrict" v-model:detail="editAddressDetail" :disabled="editSubmitting" />
        </div>

        <label class="form-label">性別</label>
        <select class="form-select" v-model="editGender" :disabled="editSubmitting" @change="checkEditError()">
            <option value="" disabled>請選擇性別</option>
            <option value="MALE">男</option>
            <option value="FEMALE">女</option>
        </select>

        <template #footer>
            <p v-if="editErrorMsg" class="text-danger small mb-3">{{ editErrorMsg }}</p>
            <button class="btn btn-secondary" @click="showEditModal = false" :disabled="editSubmitting">取消</button>
            <button v-if="editErrorMsg" type="button" class="btn btn-primary" disabled>請輸入正確資訊</button>
            <button v-if="editErrorMsg == ''" class="btn btn-primary" @click="submitEdit()" :disabled="editSubmitting">
                {{ editSubmitting ? '送出中...' : '送出' }}
            </button>
        </template>
    </BaseModal>
</template>

<style scoped>
.cell-truncate {
    max-width: 200px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.square-btn {
    width: 34px;
    height: 34px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1rem;
    line-height: 1;
    flex-shrink: 0;
    border-radius: 0.5rem;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.square-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 0.25rem 0.5rem rgba(0, 0, 0, 0.1);
}
</style>
