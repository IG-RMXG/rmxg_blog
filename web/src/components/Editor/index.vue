<template>
  <div>
    <!-- 隐藏的上传宿主：仅 type=url 时渲染；工具栏"图片"按钮通过 uploadRef 触发其内部 i 元素的 click -->
    <el-upload
      :action="uploadUrl"
      :before-upload="handleBeforeUpload"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      name="file"
      :show-file-list="false"
      :headers="headers"
      class="editor-img-uploader"
      v-if="type == 'url'"
    >
      <i ref="uploadRef" class="editor-img-uploader"></i>
    </el-upload>
  </div>
  <!-- 富文本编辑区：v-model:content 双向绑定 HTML 字符串，内容变化时再对外 emit update:modelValue -->
  <div class="editor">
    <quill-editor
      ref="quillEditorRef"
      v-model:content="content"
      contentType="html"
      @textChange="(e) => $emit('update:modelValue', content)"
      :options="options"
      :style="styles"
    />
  </div>
</template>

<script setup>
/**
 * 富文本编辑器组件（基于 @vueup/vue-quill，Quill 2）
 * - 受控：v-model（modelValue）为 HTML 字符串；编辑后通过 @textChange 回写
 * - type="url"（默认）：图片先上传到 VITE_APP_BASE_API + /file/upload，再把返回的地址插入正文；
 *   type 取其他值时退回 Quill 默认行为（图片以 base64 内联到 HTML）
 * - 后端契约：上传成功返回 { code, msg, data: { url } }，code == 200 才算成功
 */
import axios from 'axios'
import { QuillEditor } from "@vueup/vue-quill"
import "@vueup/vue-quill/dist/vue-quill.snow.css"
import { getToken } from "@/utils/auth"

const { proxy } = getCurrentInstance()

const quillEditorRef = ref()
const uploadUrl = ref(import.meta.env.VITE_APP_BASE_API + "/file/upload") // 上传的图片服务器地址
const headers = ref({
  Authorization: "Bearer " + getToken()
})

const props = defineProps({
  /* 编辑器的内容 */
  modelValue: {
    type: String,
  },
  /* 高度 */
  height: {
    type: Number,
    default: null,
  },
  /* 最小高度 */
  minHeight: {
    type: Number,
    default: null,
  },
  /* 只读 */
  readOnly: {
    type: Boolean,
    default: false,
  },
  /* 上传文件大小限制(MB) */
  fileSize: {
    type: Number,
    default: 5,
  },
  /* 类型（base64格式、url格式） */
  type: {
    type: String,
    default: "url",
  }
})

// 工具栏与主题配置；bounds 指向 body，避免工具栏/浮层在弹窗等有 overflow 的容器里被裁切
const options = ref({
  theme: "snow",
  bounds: document.body,
  debug: "warn",
  modules: {
    // 工具栏配置
    toolbar: [
      ["bold", "italic", "underline", "strike"],      // 加粗 斜体 下划线 删除线
      ["blockquote", "code-block"],                   // 引用  代码块
      [{ list: "ordered" }, { list: "bullet" }],      // 有序、无序列表
      [{ indent: "-1" }, { indent: "+1" }],           // 缩进
      [{ size: ["small", false, "large", "huge"] }],  // 字体大小
      [{ header: [1, 2, 3, 4, 5, 6, false] }],        // 标题
      [{ color: [] }, { background: [] }],            // 字体颜色、字体背景颜色
      [{ align: [] }],                                // 对齐方式
      ["clean"],                                      // 清除文本格式
      ["link", "image", "video"]                      // 链接、图片、视频
    ],
  },
  placeholder: "请输入内容",
  readOnly: props.readOnly
})

// 高度换算成内联样式：props 传的是数字（px），minHeight 与 height 可分别设置
const styles = computed(() => {
  let style = {}
  if (props.minHeight) {
    style.minHeight = `${props.minHeight}px`
  }
  if (props.height) {
    style.height = `${props.height}px`
  }
  return style
})

// 外部内容 → 编辑器：先比对值再赋值，避免把用户正在输入的内容用旧值覆盖（防抖/循环回写）；
// 空值统一补成 <p></p>，否则 Quill 会拿到 null 导致工具栏与光标状态异常
const content = ref("")
watch(() => props.modelValue, (v) => {
  if (v !== content.value) {
    content.value = v == undefined ? "<p></p>" : v
  }
}, { immediate: true })

// 如果设置了上传地址则自定义图片上传事件
// type=url 时接管图片插入：把工具栏的"图片"按钮改造成触发隐藏的上传控件，而不是弹出地址输入框；
// 同时在 Quill 根节点的捕获阶段挂粘贴监听，抢在编辑器把截图写成 base64 之前拦下来走上传
onMounted(() => {
  if (props.type == 'url') {
    let quill = quillEditorRef.value.getQuill()
    let toolbar = quill.getModule("toolbar")
    toolbar.addHandler("image", (value) => {
      if (value) {
        proxy.$refs.uploadRef.click()
      } else {
        quill.format("image", false)
      }
    })
    quill.root.addEventListener('paste', handlePasteCapture, true)
  }
})

// 上传前校检格式和大小
// 仅允许位图与 SVG：校验不通过返回 false 阻断上传，通过时返回 true 交由 el-upload 继续
function handleBeforeUpload(file) {
  const type = ["image/jpeg", "image/jpg", "image/png", "image/svg"]
  const isJPG = type.includes(file.type)
  //检验文件格式
  if (!isJPG) {
    proxy.$modal.msgError(`图片格式错误!`)
    return false
  }
  // 校检文件大小
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  return true
}

// 上传成功处理
// 把图片插入到当前光标处；toRaw 用于从响应式代理还原真实的 Quill 实例
function handleUploadSuccess(res, file) {
  // 如果上传成功
  if (res.code == 200) {
    // 获取富文本实例
    let quill = toRaw(quillEditorRef.value).getQuill()
    // 获取光标位置
    let length = quill.selection.savedRange.index
    // 插入图片，res.url为服务器返回的图片链接地址
    quill.insertEmbed(length, "image", res.data.url)
    // 调整光标到最后
    quill.setSelection(length + 1)
  } else {
    proxy.$modal.msgError("图片插入失败")
  }
}

// 上传失败处理
function handleUploadError() {
  proxy.$modal.msgError("图片插入失败")
}

// 复制粘贴图片处理
// 只拦截剪贴板里的 image 项，文本粘贴保持原样；命中图片时必须 preventDefault，
// 否则 Quill 会把图片以 base64 直接塞进正文
function handlePasteCapture(e) {
  const clipboard = e.clipboardData || window.clipboardData
  if (clipboard && clipboard.items) {
    for (let i = 0; i < clipboard.items.length; i++) {
      const item = clipboard.items[i]
      if (item.type.indexOf('image') !== -1) {
        e.preventDefault()
        const file = item.getAsFile()
        insertImage(file)
      }
    }
  }
}

// 粘贴图片走 axios 直传（不经过 el-upload 控件），请求成功后复用 handleUploadSuccess 插入正文
function insertImage(file) {
  const formData = new FormData()
  formData.append("file", file)
  axios.post(uploadUrl.value, formData, { headers: { "Content-Type": "multipart/form-data", Authorization: headers.value.Authorization } }).then(res => {
    handleUploadSuccess(res.data)
  })
}
</script>

<style>
/* 隐藏上传宿主，只把它当作图片按钮的触发入口 */
.editor-img-uploader {
  display: none;
}
.editor, .ql-toolbar {
  white-space: pre-wrap !important;
  line-height: normal !important;
}
.quill-img {
  display: none;
}
/* 以下整段是 Quill 默认英文文案的中文化：用 ::before 的 content 覆盖链接/视频提示，
   以及字号、标题、字体三个下拉项的显示文字 */
.ql-snow .ql-tooltip[data-mode="link"]::before {
  content: "请输入链接地址:";
}
.ql-snow .ql-tooltip.ql-editing a.ql-action::after {
  border-right: 0px;
  content: "保存";
  padding-right: 0px;
}
.ql-snow .ql-tooltip[data-mode="video"]::before {
  content: "请输入视频地址:";
}
.ql-snow .ql-picker.ql-size .ql-picker-label::before,
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  content: "14px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="small"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="small"]::before {
  content: "10px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="large"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="large"]::before {
  content: "18px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="huge"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="huge"]::before {
  content: "32px";
}
.ql-snow .ql-picker.ql-header .ql-picker-label::before,
.ql-snow .ql-picker.ql-header .ql-picker-item::before {
  content: "文本";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="1"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="1"]::before {
  content: "标题1";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="2"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="2"]::before {
  content: "标题2";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="3"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="3"]::before {
  content: "标题3";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="4"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="4"]::before {
  content: "标题4";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="5"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="5"]::before {
  content: "标题5";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="6"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="6"]::before {
  content: "标题6";
}
.ql-snow .ql-picker.ql-font .ql-picker-label::before,
.ql-snow .ql-picker.ql-font .ql-picker-item::before {
  content: "标准字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="serif"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="serif"]::before {
  content: "衬线字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="monospace"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="monospace"]::before {
  content: "等宽字体";
}
</style>
