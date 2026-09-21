import { uploadForumImage } from './api'

// 討論區的圖片上傳：內文插圖（RichTextEditor）與封面圖（ArticleFormView）共用這一份，
// 兩邊要的東西一模一樣——先擋掉明顯不合格的檔案，再上傳，最後拿到一串可以直接用的網址。

// 後端 common/util/ImageValidator 是用檔頭特徵碼（magic number）認這四種格式，兩邊要一致。
// 這裡只是給 <input type="file"> 的 accept 與第一層過濾用，真正的把關在後端——
// accept 與 file.type 都來自瀏覽器，改個副檔名就能繞過
export const IMAGE_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp'

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

// 對齊 application.properties 的 spring.servlet.multipart.max-file-size=10MB。
// 先在前端擋是為了不要讓使用者傳完一整個大檔才收到 413
const MAX_BYTES = 10 * 1024 * 1024

// 通過驗證回空字串，否則回可以直接顯示給使用者看的原因
export function validateImageFile(file) {
  if (!file) return '請選擇圖片檔案'
  if (!ALLOWED_TYPES.includes(file.type)) return '只支援 JPG／PNG／GIF／WebP 圖片'
  if (file.size > MAX_BYTES) return '圖片檔案過大，請上傳 10MB 以內的圖片'
  return ''
}

// 上傳並回傳網址。失敗一律丟出 Error，訊息已經是中文、可以直接顯示，
// 呼叫端只要 catch 之後把 error.message 放進自己的錯誤欄位就好
export async function uploadImageFile(file) {
  const invalidReason = validateImageFile(file)
  if (invalidReason) throw new Error(invalidReason)

  try {
    return await uploadForumImage(file)
  } catch (error) {
    // 後端不論是 forum 的 ForumExceptionHandler、common 的 GlobalExceptionHandler（413 檔案過大）
    // 還是 Spring Security 的 RestAuthEntryPoint（401），回的都是 ErrorResp { errorCode, message }，
    // 所以一律讀 message；只有連不到後端時才會落到預設訊息
    throw new Error(error.response?.data?.message || '圖片上傳失敗，請稍後再試')
  }
}
