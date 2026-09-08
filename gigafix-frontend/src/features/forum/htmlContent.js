import DOMPurify from 'dompurify'

// 文章內文從純文字改成 HTML（TipTap）之後，需要兩件共用的事：
// 1. 顯示前消毒——內文是使用者產生的 HTML，直接餵給 v-html 就是 stored XSS
// 2. 判斷是否為空——空的編輯器輸出是 <p></p>，用 .trim() 判斷會誤判成「有內容」
// 兩者都跟「文章內文這個 HTML」有關，放同一個模組。

// 允許清單對齊 TipTap StarterKit + Image 實際會產生的標籤。
// 後端的 jsoup 允許清單要跟這份保持一致，否則會出現「前端顯示得出來、後端存不進去」的落差。
const ALLOWED_TAGS = [
  'p', 'br', 'hr',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'strong', 'em', 's', 'u', 'code', 'pre',
  'blockquote', 'ul', 'ol', 'li',
  'a', 'img',
  // 字級與顏色是 TipTap 的 TextStyle 以 <span style="..."> 實作的
  'span',
]

// style 是為了字級與顏色而放行。DOMPurify 本身會解析並清理 CSS，
// 而後端 HtmlSanitizer 另外只保留 color/font-size 兩個宣告，那層才是權威閘門
const ALLOWED_ATTR = ['href', 'target', 'rel', 'src', 'alt', 'title', 'style']

export function sanitizeHtml(html) {
  if (!html) return ''
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    // 只放行 http/https/mailto 與站內相對路徑，擋掉 javascript: 與 data:
    ALLOWED_URI_REGEXP: /^(?:https?:|mailto:|[./#])/i,
  })
}

// 判斷文章內文是否為空。
// 不能用 content.trim() === ''——TipTap 的空編輯器輸出是 <p></p>，trim 之後仍有長度。
// 注意：只有圖片、沒有任何文字的文章「不算空」，所以要先看有沒有 img/hr。
export function isHtmlEmpty(html) {
  if (!html) return true
  if (/<(img|hr)\b/i.test(html)) return false
  const text = html
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .trim()
  return text === ''
}
