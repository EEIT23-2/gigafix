<script setup>
import { computed, watch, onBeforeUnmount } from 'vue'
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

// 字級上下限。後端 HtmlSanitizer 的 font-size 比對也收斂在同一個範圍，
// 兩邊要一起改，否則會出現「前端選得到、存進去卻被濾掉」
const MIN_FONT_SIZE = 8
const MAX_FONT_SIZE = 32

const TEXT_COLORS = [
  '#212529', '#c0392b', '#a15c00', '#1e7e34', '#2b77c5',
  '#6f42c1', '#d63384', '#0f766e', '#6c757d', '#adb5bd',
]

// 背景色偏淺，深色當底會讓文字讀不到
const BACKGROUND_COLORS = [
  '#fff3cd', '#f8d7da', '#d1e7dd', '#cfe2ff', '#e2d9f3',
  '#fce4ec', '#e0f2f1', '#fff0e6', '#e9ecef', '#ffffff',
]

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '開始撰寫內文...' },
})

const emit = defineEmits(['update:modelValue'])

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
  onUpdate: ({ editor: instance }) => {
    emit('update:modelValue', instance.getHTML())
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

function setLink() {
  const previous = editor.value.getAttributes('link').href ?? ''
  const url = window.prompt('連結網址', previous)
  if (url === null) return // 使用者按取消
  if (url === '') {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

// 目前選取範圍的字級，去掉 px 只留數字給 number input 用
const currentFontSize = computed(() => {
  const raw = editor.value?.getAttributes('textStyle')?.fontSize
  return raw ? Number.parseInt(raw, 10) : null
})

const currentColor = computed(() => editor.value?.getAttributes('textStyle')?.color ?? '')
const currentBackground = computed(
  () => editor.value?.getAttributes('textStyle')?.backgroundColor ?? '',
)

function applyFontSize(event) {
  const value = Number.parseInt(event.target.value, 10)
  // 超出範圍就不套用——後端也會濾掉，這裡先擋住免得使用者以為成功了
  if (!Number.isFinite(value) || value < MIN_FONT_SIZE || value > MAX_FONT_SIZE) return
  editor.value.chain().focus().setFontSize(`${value}px`).run()
}

function resetFontSize() {
  editor.value.chain().focus().unsetFontSize().run()
}

function applyColor(value) {
  editor.value.chain().focus().setColor(value).run()
}

function clearColor() {
  editor.value.chain().focus().unsetColor().run()
}

function applyBackground(value) {
  editor.value.chain().focus().setBackgroundColor(value).run()
}

function clearBackground() {
  editor.value.chain().focus().unsetBackgroundColor().run()
}

function addImage() {
  // 專案沒有上傳機制，圖片只收網址（規劃書：圖片存 URL 不存 Base64）
  const url = window.prompt('圖片網址')
  if (!url) return
  editor.value.chain().focus().setImage({ src: url }).run()
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

      <span class="size-group">
        <input
          class="size-input"
          type="number"
          :min="MIN_FONT_SIZE"
          :max="MAX_FONT_SIZE"
          :value="currentFontSize"
          :title="`字級（${MIN_FONT_SIZE}~${MAX_FONT_SIZE}px）`"
          placeholder="16"
          @change="applyFontSize"
        />
        <button type="button" class="size-reset" title="字級恢復預設" @click="resetFontSize">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="5" y1="19" x2="19" y2="5"></line></svg>
        </button>
      </span>

      <ColorPopover
        :model-value="currentColor"
        :swatches="TEXT_COLORS"
        title="文字顏色"
        @select="applyColor"
        @clear="clearColor"
      >
        <template #icon><i class="bi bi-fonts"></i></template>
      </ColorPopover>

      <ColorPopover
        :model-value="currentBackground"
        :swatches="BACKGROUND_COLORS"
        title="文字背景"
        @select="applyBackground"
        @clear="clearBackground"
      >
        <template #icon><i class="bi bi-highlighter"></i></template>
      </ColorPopover>

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
      <button type="button" class="tool" title="插入圖片網址" @click="addImage">
        <i class="bi bi-image"></i>
      </button>

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

.size-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.size-input {
  width: 52px;
  height: 30px;
  border: 1px solid #d7dce5;
  border-radius: 4px;
  background: #ffffff;
  color: #5a5c69;
  font-size: 13px;
  padding: 0 4px;
  text-align: center;
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
