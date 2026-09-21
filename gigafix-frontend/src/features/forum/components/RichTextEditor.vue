<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
// Placeholder 來自 @tiptap/extensions（StarterKit 的相依，已隨它裝好），
// 它負責在空段落掛上 .is-editor-empty，下方的 placeholder 樣式才會生效
import { Placeholder } from '@tiptap/extensions'
// 字級與顏色。兩者都是以 <span style="..."> 實作，所以前後端的消毒允許清單
// 必須放行 span 與 style，否則使用者選了卻會被默默清掉
import { TextStyle, Color, FontSize, BackgroundColor } from '@tiptap/extension-text-style'
import ColorPopover from './ColorPopover.vue'
import { IMAGE_ACCEPT, uploadImageFile } from '../imageUpload'

// 字級上下限。後端 HtmlSanitizer 的 font-size 比對也收斂在同一個範圍，
// 兩邊要一起改，否則會出現「前端選得到、存進去卻被濾掉」
const MIN_FONT_SIZE = 8
const MAX_FONT_SIZE = 32
// 沒有套用字級時輸入框要顯示的值。不能留空——空值時原生微調鈕的第一下會直接跳到 min
const BASE_FONT_SIZE = 16

const TEXT_COLORS = [
  '#212529', '#c0392b', '#a15c00', '#1e7e34', '#2b77c5',
  '#6f42c1', '#d63384', '#0f766e', '#6c757d', '#adb5bd',
]

// 背景色偏淺，深色當底會讓文字讀不到
const BACKGROUND_COLORS = [
  '#fff3cd', '#f8d7da', '#d1e7dd', '#cfe2ff', '#e2d9f3',
  '#fce4ec', '#e0f2f1', '#fff0e6', '#e9ecef', '#ffffff',
]

// 色票面板沒選過任何顏色時預設顯示這組（黑字白底），「清除」按鈕也是回到這組，
// 而不是單純移除顏色標記——移除標記後其實是繼承 .tiptap 的 #333333，顏色會比黑色淺
const DEFAULT_TEXT_COLOR = '#000000'
const DEFAULT_BACKGROUND_COLOR = '#ffffff'

function pickRandom(array) {
  return array[Math.floor(Math.random() * array.length)]
}

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '開始撰寫內文...' },
})

// error：上傳圖片失敗時往上丟，由使用這個編輯器的頁面決定要顯示在哪裡
// （ArticleFormView 有自己的 errorMessage 欄位，編輯器不該自己決定用 alert 還是別的）
const emit = defineEmits(['update:modelValue', 'error'])

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    // StarterKit v3 已內含 Link 與 Underline，不要再另外註冊，會重複
    StarterKit.configure({
      link: {
        openOnClick: false, // 編輯中點連結應該是把游標放進去，不是跳走
        autolink: true,
      },
      // 手機維修/二手交易的討論區用不到程式碼，關掉可以少一顆按鈕、也少一種誤按。
      // 設成 false 會連帶關掉它們的 Markdown 輸入規則（例如三個反引號）
      codeBlock: false,
      code: false,
    }),
    Image,
    Placeholder.configure({ placeholder: props.placeholder }),
    TextStyle,
    Color,
    FontSize,
    BackgroundColor,
  ],
  onUpdate: ({ editor: instance, transaction }) => {
    if (randomStyleEnabled.value) {
      applyRandomStyleToInsertedText(transaction, instance)
    }
    emit('update:modelValue', instance.getHTML())
    syncEditorState()
  },
  // 游標移到別段時，字級輸入框要跟著顯示該處的實際字級；有沒有選取文字也要跟著更新，
  // 「複製格式」「套用格式」按鈕能不能按取決於此
  onSelectionUpdate: syncEditorState,
  editorProps: {
    // 直接貼上或拖曳圖片是論壇最順手的貼圖方式，攔下來自己走上傳。
    // 不攔的話 ProseMirror 會把圖片塞成 base64 的 data URI，而前後端兩層消毒都只放行
    // http/https，結果是「當下看得到、存檔後整張消失」
    handlePaste: (view, event) => uploadFromDataTransfer(event.clipboardData),
    handleDrop: (view, event) => uploadFromDataTransfer(event.dataTransfer),
  },
})

// 外部值變動時才回寫（例如編輯模式載入既有文章）。
// 一定要先比對內容，否則自己 emit 出去的值又被寫回來，setContent 會把游標打回開頭。
watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value) return
    if (value === editor.value.getHTML()) return
    // v3 的第二參數是 options 物件（v2 是布林），emitUpdate: false 避免回寫又觸發一次 onUpdate
    editor.value.commands.setContent(value ?? '', { emitUpdate: false })
  },
)

onBeforeUnmount(() => editor.value?.destroy())

// 使用者常常只打 example.com。那會變成相對路徑的 href，而前端 DOMPurify 與後端 jsoup
// 都只放行 http/https/mailto，結果是「當下看起來有連結、存檔後變純文字」。這裡先補上協定。
function normalizeUrl(url) {
  const trimmed = url.trim()
  if (/^(https?:\/\/|mailto:)/i.test(trimmed)) return trimmed
  // 站內路徑與錨點維持原樣，消毒層允許它們
  if (/^[/#]/.test(trimmed)) return trimmed
  return `https://${trimmed}`
}

function setLink() {
  const previous = editor.value.getAttributes('link').href ?? ''
  const input = window.prompt('連結網址', previous)
  if (input === null) return // 使用者按取消

  if (input.trim() === '') {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }

  const href = normalizeUrl(input)
  const { from, to } = editor.value.state.selection

  if (from === to) {
    // 游標是收合的，沒有選取任何文字。
    // setLink 套用的是「標記」，標記必須附著在文字上——這時候直接呼叫它會什麼都不發生，
    // 所以改成插入一段「文字是網址、並帶 link 標記」的內容
    editor.value
      .chain()
      .focus()
      .insertContent({
        type: 'text',
        text: href,
        marks: [{ type: 'link', attrs: { href } }],
      })
      .run()
    return
  }

  editor.value.chain().focus().extendMarkRange('link').setLink({ href }).run()
}

// 「目前顏色」指示只認使用者上一次實際點選的顏色，不跟著游標位置的既有樣式走——
// 用本地 ref 而不是讀 ProseMirror 狀態的 computed，否則點到哪段文字，指示就會變成那段的顏色
const currentColor = ref('')
const currentBackground = ref('')

// 「隨機樣式」核取方塊：勾選後，接下來每次輸入動作打的字都會套用隨機文字顏色/背景色，
// 只在打字當下生效，不回頭改已經打好的文字；本地狀態，不持久化，每次開編輯器預設關閉
const randomStyleEnabled = ref(false)

// 字級用「本地 ref + 明確同步」而不是 computed：
// computed 讀的是 ProseMirror 的內部狀態，Vue 追蹤不到它的變化，不會可靠地重算，
// 結果是輸入框顯示的值與編輯器實際狀態各走各的（要點兩下才生效就是這樣來的）
const fontSize = ref(BASE_FONT_SIZE)

function syncFontSize() {
  const raw = editor.value?.getAttributes('textStyle')?.fontSize
  fontSize.value = raw ? Number.parseInt(raw, 10) : BASE_FONT_SIZE
}

// 目前有沒有選取文字，決定「複製格式」「套用格式」能不能按
const hasSelection = ref(false)

// 顏色／背景色是「筆刷」概念：選過一次之後，游標移到別處（沒有選取文字）繼續打字，
// 也該沿用這個顏色，而不是被 ProseMirror 的預設行為蓋掉——游標移動到新位置時，
// 會重新從當地文字推導接下來要打的字用什麼樣式，等於選過的顏色只是好看，打字時完全沒用到。
// 只在游標收合（沒有選取範圍）時介入；有選取文字時套色是靠點色票，不需要這裡處理
function syncStickyColor() {
  if (!editor.value) return
  // IME 組字（例如注音）進行中如果在這裡多丟一個 transaction，會打斷正在進行的組字，
  // 導致還沒選字完成的符號被強制送出成文字（實測：打「你好」會變成「ㄋ你好」）。
  // 組字結束後 compositionend 會觸發一次新的 onUpdate，屆時再正常同步即可，這裡先讓組字跑完
  if (editor.value.view.composing) return
  const { from, to } = editor.value.state.selection
  if (from !== to) return

  const attrs = editor.value.getAttributes('textStyle')
  const colorMismatch = currentColor.value && attrs.color !== currentColor.value
  const backgroundMismatch = currentBackground.value && attrs.backgroundColor !== currentBackground.value
  if (!colorMismatch && !backgroundMismatch) return

  // 兩個屬性要在同一個 chain 裡一起設定，不能分兩次各自呼叫 setColor/setBackgroundColor——
  // 游標收合時 setMark 是拿「目前游標位置既有的樣式」跟新屬性合併，分兩次呼叫的話，
  // 第二次修的那個屬性會拿「第一次剛設完、還沒套用這次屬性」的樣式去合併，等於白修
  const chain = editor.value.chain()
  if (currentColor.value) chain.setColor(currentColor.value)
  if (currentBackground.value) chain.setBackgroundColor(currentBackground.value)
  chain.run()
}

function syncEditorState() {
  syncFontSize()
  const { from, to } = editor.value?.state.selection ?? { from: 0, to: 0 }
  hasSelection.value = from !== to
  syncStickyColor()
}

function applyFontSize(size) {
  const value = Number.parseInt(size, 10)
  // 超出範圍就夾回邊界，不要靜靜地不做事——使用者才知道上下限在哪
  const clamped = Number.isFinite(value)
    ? Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, value))
    : BASE_FONT_SIZE
  fontSize.value = clamped
  editor.value.chain().focus().setFontSize(`${clamped}px`).run()
}

function stepFontSize(delta) {
  applyFontSize(fontSize.value + delta)
}

function resetFontSize() {
  fontSize.value = BASE_FONT_SIZE
  editor.value.chain().focus().unsetFontSize().run()
}

// 自訂顏色（<input type="color">）點下去會跳出瀏覽器原生色盤，那是跟網頁脫勾的原生 UI，
// 關閉後編輯器的選取常常會跑掉（實測會跳到之前編輯過的別行），導致套色套錯地方、或要點回原位置重選一次才生效。
// 對策：面板一打開就先記下當時的選取，套色時強制還原成記下來的範圍，不管當下的選取實際上是什麼
let savedColorSelection = null

// 套用隨機樣式本身也會觸發一次新的 onUpdate（chain().run() 會 dispatch 一次新 transaction），
// 用這個旗標擋住那次遞迴呼叫，避免無窮迴圈——HTML 照常 emit、工具列狀態照常同步，只是不會再疊一次隨機樣式
let applyingRandomStyle = false

function captureColorSelection() {
  if (!editor.value) return
  const { from, to } = editor.value.state.selection
  savedColorSelection = { from, to }
}

function restoreColorSelection(chain) {
  return savedColorSelection ? chain.setTextSelection(savedColorSelection) : chain
}

// 套色時，如果目標範圍是收合的游標（還沒選字，屬於「筆刷」情境，例如換行後先選色再打字），
// 文字色跟背景色要在同一個 chain 裡一起明確設定——理由跟 syncStickyColor 一樣：
// 剛換行的空段落通常什麼樣式都沒有，分開呼叫的話，後設的那個屬性會把先設的那個蓋掉。
// 有實際選取既有文字時就不受影響，只改這次要改的那個屬性，其餘沿用選取範圍原本的樣式
function applyColorLikeStyle(attribute, value) {
  const target = savedColorSelection ?? editor.value.state.selection
  const chain = restoreColorSelection(editor.value.chain().focus())
  if (target.from === target.to) {
    if (currentColor.value) chain.setColor(currentColor.value)
    if (currentBackground.value) chain.setBackgroundColor(currentBackground.value)
  } else if (attribute === 'color') {
    chain.setColor(value)
  } else {
    chain.setBackgroundColor(value)
  }
  chain.run()
}

function applyColor(value) {
  currentColor.value = value
  applyColorLikeStyle('color', value)
}

function clearColor() {
  currentColor.value = DEFAULT_TEXT_COLOR
  applyColorLikeStyle('color', DEFAULT_TEXT_COLOR)
}

function applyBackground(value) {
  currentBackground.value = value
  applyColorLikeStyle('background', value)
}

function clearBackground() {
  currentBackground.value = DEFAULT_BACKGROUND_COLOR
  applyColorLikeStyle('background', DEFAULT_BACKGROUND_COLOR)
}

// 插入外部圖片網址。上傳功能做好之後仍然保留這條路——既有文章與種子資料用的都是
// 外部網址（Unsplash 等），而且有時候使用者手上就只有一個網址
function addImage() {
  const url = window.prompt('圖片網址')
  if (!url) return
  editor.value.chain().focus().setImage({ src: url }).run()
}

// ── 上傳圖片 ──
// 檔案上傳到 Cloudinary 換成 HTTPS 網址後，插進來的一樣是 <img src="https://...">，
// 跟貼網址的結果同一種形狀，所以消毒層與渲染端都不用改

const imageInput = ref(null)
const uploading = ref(false)

function pickImage() {
  imageInput.value?.click()
}

async function handleImageSelected(event) {
  const file = event.target.files?.[0]
  // 不論成功失敗都要清空，否則同一張圖第二次選不會觸發 change（value 沒變）
  event.target.value = ''
  if (file) await uploadAndInsert(file)
}

async function uploadAndInsert(file) {
  if (uploading.value) return
  uploading.value = true
  try {
    const url = await uploadImageFile(file)
    editor.value.chain().focus().setImage({ src: url }).run()
  } catch (error) {
    emit('error', error.message)
  } finally {
    uploading.value = false
  }
}

// 從剪貼簿／拖放資料裡撈圖片檔。有撈到就回 true，告訴 ProseMirror 這個事件我們處理掉了，
// 它才不會再用預設行為插入一次
function uploadFromDataTransfer(dataTransfer) {
  const images = [...(dataTransfer?.files ?? [])].filter((file) => file.type.startsWith('image/'))
  if (images.length === 0) return false

  // 一次丟多張時依序上傳，插入順序才會跟使用者選的順序一致（同時發的話回來的順序不保證）
  images.reduce((queue, file) => queue.then(() => uploadAndInsert(file)), Promise.resolve())
  return true
}

// 複製/套用格式：只認字元級的行內樣式，不含標題/清單/引言等區塊層級樣式——
// 那些本來就是選字後直接點工具列切換，不需要透過格式刷複製
const FORMAT_MARKS = {
  bold: { set: 'setBold', unset: 'unsetBold' },
  italic: { set: 'setItalic', unset: 'unsetItalic' },
  strike: { set: 'setStrike', unset: 'unsetStrike' },
}

// 記錄勾選「隨機樣式」當下游標的樣式，取消勾選時要復原成這樣，
// 否則取消勾選後接著打的字會沿用最後一個字剛好套到的隨機樣式，而不是原本的樣式
let preRandomStyleFormat = null

watch(randomStyleEnabled, (enabled) => {
  if (!editor.value) return

  if (enabled) {
    const attrs = editor.value.getAttributes('textStyle')
    preRandomStyleFormat = {
      color: attrs.color ?? null,
      backgroundColor: attrs.backgroundColor ?? null,
      fontSize: attrs.fontSize ?? null,
    }
    return
  }

  if (!preRandomStyleFormat) return
  const format = preRandomStyleFormat
  const chain = editor.value.chain().focus()
  format.color ? chain.setColor(format.color) : chain.unsetColor()
  format.backgroundColor ? chain.setBackgroundColor(format.backgroundColor) : chain.unsetBackgroundColor()
  format.fontSize ? chain.setFontSize(format.fontSize) : chain.unsetFontSize()
  chain.run()
  preRandomStyleFormat = null
})

const copiedFormat = ref(null)

function copyFormat() {
  if (!editor.value || !hasSelection.value) return
  const attrs = editor.value.getAttributes('textStyle')
  copiedFormat.value = {
    color: attrs.color ?? null,
    backgroundColor: attrs.backgroundColor ?? null,
    fontSize: attrs.fontSize ?? null,
    marks: Object.fromEntries(
      Object.keys(FORMAT_MARKS).map((key) => [key, editor.value.isActive(key)]),
    ),
  }
}

function applyFormat() {
  if (!editor.value || !copiedFormat.value || !hasSelection.value) return
  const format = copiedFormat.value
  const chain = editor.value.chain().focus()

  // 來源有值就套用，沒有值（複製當下就是「未設定」）就清掉，
  // 讓套用後兩段文字的樣式完全一致，而不是只疊加、殘留目標段落原本的舊樣式
  format.color ? chain.setColor(format.color) : chain.unsetColor()
  format.backgroundColor ? chain.setBackgroundColor(format.backgroundColor) : chain.unsetBackgroundColor()
  format.fontSize ? chain.setFontSize(format.fontSize) : chain.unsetFontSize()

  // 粗體/斜體/刪除線是布林開關，直接依來源狀態明確 set/unset，
  // 不用 toggle——選取範圍內狀態不一致時，toggle 的結果會跟預期相反
  Object.entries(FORMAT_MARKS).forEach(([key, commands]) => {
    chain[format.marks[key] ? commands.set : commands.unset]()
  })

  chain.run()
}

// 「隨機樣式」核取方塊勾選時，每次輸入動作（英打通常一個字元、中文選字後一整個詞，
// 連續快速輸入或自動化工具也可能一次送出一整串）結束後呼叫，把剛打進去的每一個字元各自套上
// 獨立隨機的文字顏色/背景色——同一次輸入動作插入多個字元時，每個字元顏色都要不一樣
function applyRandomStyleToInsertedText(transaction, instance) {
  if (applyingRandomStyle || !transaction.docChanged) return

  // 剛打完字游標會收合在插入內容後面；選取狀態的異動（例如刪除選取範圍）不是「打字」，不處理
  const { from, to } = transaction.selection
  if (from !== to) return

  // 純文字時 step.slice.content.size 等於這次插入的字數（ProseMirror 標準算法）；
  // 刪除等異動沒有 slice 或 size 為 0，insertedSize 會是 0，直接略過
  let insertedSize = 0
  transaction.steps.forEach((step) => {
    if (step.slice) insertedSize += step.slice.content.size
  })
  if (insertedSize <= 0) return

  const start = to - insertedSize

  // Enter 換行（分段）也會讓 docChanged 為真、insertedSize 算出非 0（段落分割本身佔位），
  // 但那不是「打字」，沒有插入任何新文字——這時候 start 往前算會跨進「上一段」，
  // textBetween(start, to) 撈到的其實是換行前那段文字尾端既有的字元，會被誤套上隨機樣式
  // （實測：文字尾端按 Enter，前一行最後兩個字元會被套色）。用頭尾是否同一段落擋掉這種情況
  if (transaction.doc.resolve(start).parent !== transaction.doc.resolve(to).parent) return

  // 取出剛插入的純文字，用展開運算子拆成一個個 Unicode 字元（避免 emoji 之類的 surrogate pair
  // 字元被切成半個，跟 initial() 用的手法一致），逐一各自套用隨機樣式
  const insertedText = transaction.doc.textBetween(start, to)
  const characters = [...insertedText]
  if (characters.length === 0) return

  applyingRandomStyle = true
  const chain = instance.chain()
  let cursor = start
  characters.forEach((char) => {
    const charEnd = cursor + char.length
    chain.setTextSelection({ from: cursor, to: charEnd })
    chain.setColor(pickRandom(TEXT_COLORS))
    chain.setBackgroundColor(pickRandom(BACKGROUND_COLORS))
    cursor = charEnd
  })
  chain.setTextSelection(to) // 全部套完再把游標收回打字位置後面，不留選取狀態
  chain.run()
  applyingRandomStyle = false
}
</script>

<template>
  <div v-if="editor" class="rich-text-editor">
    <div class="toolbar">
      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('heading', { level: 2 }) }"
        title="標題"
        @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
      >
        <i class="bi bi-type-h2"></i>
      </button>
      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('bold') }"
        title="粗體"
        @click="editor.chain().focus().toggleBold().run()"
      >
        <i class="bi bi-type-bold"></i>
      </button>
      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('italic') }"
        title="斜體"
        @click="editor.chain().focus().toggleItalic().run()"
      >
        <i class="bi bi-type-italic"></i>
      </button>
      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('strike') }"
        title="刪除線"
        @click="editor.chain().focus().toggleStrike().run()"
      >
        <i class="bi bi-type-strikethrough"></i>
      </button>

      <span class="divider"></span>

      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('bulletList') }"
        title="項目清單"
        @click="editor.chain().focus().toggleBulletList().run()"
      >
        <i class="bi bi-list-ul"></i>
      </button>
      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('orderedList') }"
        title="編號清單"
        @click="editor.chain().focus().toggleOrderedList().run()"
      >
        <i class="bi bi-list-ol"></i>
      </button>
      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('blockquote') }"
        title="引言"
        @click="editor.chain().focus().toggleBlockquote().run()"
      >
        <i class="bi bi-quote"></i>
      </button>
      <span class="divider"></span>

      <!-- 用自訂的 −/+ 而不是原生 number 微調鈕：原生微調鈕在空值時第一下會跳到 min，
           而且每次套用都會把焦點搶回編輯器，造成「點兩下才生效」 -->
      <span class="size-group">
        <button
          type="button"
          class="size-step"
          title="縮小字級"
          :disabled="fontSize <= MIN_FONT_SIZE"
          @click="stepFontSize(-1)"
        >
          −
        </button>
        <input
          v-model.number="fontSize"
          class="size-input"
          type="text"
          inputmode="numeric"
          :title="`字級（${MIN_FONT_SIZE}~${MAX_FONT_SIZE}px）`"
          @change="applyFontSize(fontSize)"
          @keyup.enter="applyFontSize(fontSize)"
        />
        <button
          type="button"
          class="size-step"
          title="放大字級"
          :disabled="fontSize >= MAX_FONT_SIZE"
          @click="stepFontSize(1)"
        >
          ＋
        </button>
        <button type="button" class="size-reset" title="字級恢復預設" @click="resetFontSize">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="5" y1="19" x2="19" y2="5"></line></svg>
        </button>
      </span>

      <ColorPopover
        :model-value="currentColor"
        :swatches="TEXT_COLORS"
        :default-color="DEFAULT_TEXT_COLOR"
        title="文字顏色"
        @select="applyColor"
        @clear="clearColor"
        @panel-open="captureColorSelection"
      >
        <template #icon><i class="bi bi-fonts"></i></template>
      </ColorPopover>

      <ColorPopover
        :model-value="currentBackground"
        :swatches="BACKGROUND_COLORS"
        :default-color="DEFAULT_BACKGROUND_COLOR"
        title="文字背景"
        @select="applyBackground"
        @clear="clearBackground"
        @panel-open="captureColorSelection"
      >
        <template #icon><i class="bi bi-highlighter"></i></template>
      </ColorPopover>

      <label class="random-style-toggle" title="打字時隨機套用文字顏色與背景色">
        <input v-model="randomStyleEnabled" type="checkbox">
        <span>隨機樣式</span>
      </label>

      <span class="divider"></span>

      <button
        type="button"
        class="tool"
        title="複製格式"
        :disabled="!hasSelection"
        @click="copyFormat"
      >
        <i class="bi bi-eyedropper"></i>
      </button>
      <button
        type="button"
        class="tool"
        :class="{ active: !!copiedFormat }"
        title="套用格式"
        :disabled="!copiedFormat || !hasSelection"
        @click="applyFormat"
      >
        <i class="bi bi-brush"></i>
      </button>

      <span class="divider"></span>

      <button
        type="button"
        class="tool"
        :class="{ active: editor.isActive('link') }"
        title="連結"
        @click="setLink"
      >
        <i class="bi bi-link-45deg"></i>
      </button>
      <button
        type="button"
        class="tool"
        title="上傳圖片（也可以直接貼上或拖曳）"
        :disabled="uploading"
        @click="pickImage"
      >
        <i :class="uploading ? 'bi bi-hourglass-split' : 'bi bi-image'"></i>
      </button>
      <button type="button" class="tool" title="插入圖片網址" @click="addImage">
        <i class="bi bi-globe"></i>
      </button>
      <!-- 刻意用隱藏的 input ＋ 按鈕觸發，而不是包一層 <label>：
           label 會把點擊轉發給內部第一個可標記控制項，在工具列裡會變成誤按到別顆鈕 -->
      <input
        ref="imageInput"
        class="file-input"
        type="file"
        :accept="IMAGE_ACCEPT"
        @change="handleImageSelected"
      />

      <span class="divider"></span>

      <button
        type="button"
        class="tool"
        title="復原"
        :disabled="!editor.can().undo()"
        @click="editor.chain().focus().undo().run()"
      >
        <i class="bi bi-arrow-counterclockwise"></i>
      </button>
      <button
        type="button"
        class="tool"
        title="重做"
        :disabled="!editor.can().redo()"
        @click="editor.chain().focus().redo().run()"
      >
        <i class="bi bi-arrow-clockwise"></i>
      </button>
    </div>

    <EditorContent :editor="editor" class="editor-surface" />
  </div>
</template>

<style scoped>
.rich-text-editor {
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  overflow: hidden;
  background: #ffffff;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 6px 8px;
  background: #f8f9fc;
  border-bottom: 1px solid #e3e6f0;
}

.tool {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #5a5c69;
  font-size: 15px;
  cursor: pointer;
}

.tool:hover:not(:disabled) {
  background: #eaeef5;
}

.tool.active {
  background: #e7f0fb;
  border-color: #9dc1ec;
  color: #1f5fa8;
}

.tool:disabled {
  opacity: 0.4;
  cursor: default;
}

.divider {
  width: 1px;
  height: 20px;
  background: #e3e6f0;
  margin: 0 4px;
}

/* 只負責開啟檔案選擇視窗，不該佔工具列的版面 */
.file-input {
  display: none;
}

.random-style-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 8px;
  color: #5a5c69;
  font-size: 14px;
  cursor: pointer;
  user-select: none;
}

.random-style-toggle input {
  cursor: pointer;
}

.size-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.size-input {
  width: 34px;
  height: 30px;
  border: 1px solid #d7dce5;
  border-radius: 4px;
  background: #ffffff;
  color: #5a5c69;
  font-size: 14px;
  padding: 0 2px;
  text-align: center;
}

.size-step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 30px;
  border: 1px solid #d7dce5;
  border-radius: 4px;
  background: #ffffff;
  color: #5a5c69;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}

.size-step:hover:not(:disabled) {
  background: #eaeef5;
}

.size-step:disabled {
  opacity: 0.4;
  cursor: default;
}

.size-reset {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 30px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #6c757d;
  cursor: pointer;
}

.size-reset:hover {
  background: #eaeef5;
}

/* EditorContent 會在內部再包一層 .tiptap，樣式要穿透下去 */
.editor-surface :deep(.tiptap) {
  min-height: 280px;
  padding: 14px 16px;
  outline: none;
  /* 沒有空白的長字串（網址、連續英數）要能斷行，否則會把容器撐寬 */
  overflow-wrap: break-word;
  word-break: break-word;
  line-height: 1.75;
  color: #333333;
}

.editor-surface :deep(.tiptap p) {
  margin: 0 0 0.75em;
}

.editor-surface :deep(.tiptap > *:last-child) {
  margin-bottom: 0;
}

.editor-surface :deep(.tiptap h2) {
  font-size: 1.4em;
  font-weight: 700;
  margin: 1em 0 0.5em;
}

.editor-surface :deep(.tiptap blockquote) {
  border-left: 3px solid #d0d7e2;
  margin: 0 0 0.75em;
  padding-left: 12px;
  color: #666666;
}


.editor-surface :deep(.tiptap img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

.editor-surface :deep(.tiptap a) {
  color: #2b77c5;
}

/* 空編輯器時給一點視覺提示，避免看起來像壞掉。
   文字由 Placeholder 擴充寫進 data-placeholder，不要在這裡寫死 */
.editor-surface :deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  color: #adb5bd;
  float: left;
  height: 0;
  pointer-events: none;
}
</style>
