import axios from 'axios'

// 用獨立 instance 而不是全域 axios，是為了不要繼承 src/stores/admin.js 裝的那個全域 401 攔截器——
// 那支攔截器是給後台用的（401 會導去 /adminLogin），這裡的 401（工作階段過期）要在聊天面板內處理
const http = axios.create()

export function postChatMessage(message, history) {
  return http.post('/api/gigafix/support/chat', { message, history }).then((res) => res.data)
}
