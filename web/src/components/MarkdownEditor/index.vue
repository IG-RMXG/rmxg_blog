<template>
  <MdEditor
    ref="editorRef"
    v-model="content"
    class="blog-md-editor"
    language="zh-CN"
    :previewTheme="MD_PREVIEW_THEME"
    :codeTheme="MD_CODE_THEME"
    :toolbars="MD_TOOLBARS"
    :footers="MD_FOOTERS"
    :preview="true"
    :showCodeRowNumber="true"
    :autoDetectCode="true"
    :placeholder="placeholder"
    :onUploadImg="onUploadImg"
    :style="{ height: '100%' }"
    @onError="handleError"
  />
</template>

<script setup>
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import 'md-editor-v3/lib/preview.css'
import axios from 'axios'
import { getToken } from '@/utils/auth'
import { MD_TOOLBARS, MD_PREVIEW_THEME, MD_CODE_THEME, MD_FOOTERS } from '@/utils/md-editor'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: {
    type: String,
    default: '请输入正文… 支持 Markdown：标题 / 列表 / 表格 / 代码块 / 公式 / 流程图 / 图片上传'
  }
})
const emit = defineEmits(['update:modelValue'])

const { proxy } = getCurrentInstance()
const editorRef = ref()

const content = computed({
  get: () => props.modelValue || '',
  set: (v) => emit('update:modelValue', v)
})

/** 编辑器内置图片上传：上传到博客接口后回填 markdown 图片语法 */
async function onUploadImg(files, callback) {
  const urls = []
  proxy.$modal.loading('正在上传图片…')
  try {
    for (const file of files) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await axios.post(
        import.meta.env.VITE_APP_BASE_API + '/blog/upload',
        fd,
        { headers: { 'Content-Type': 'multipart/form-data', Authorization: 'Bearer ' + getToken() } }
      )
      if (res.data && res.data.code === 200 && res.data.data && res.data.data.url) {
        urls.push(res.data.data.url)
      }
    }
    callback(urls)
  } catch (e) {
    proxy.$modal.msgError('图片上传失败')
  } finally {
    proxy.$modal.closeLoading()
  }
}

function handleError(err) {
  // mermaid/katex 语法写错等内部异常，仅告警不打断编辑
  console.warn('[md-editor]', err)
}

/** 暴露给页面：预览/全屏/目录等切换（供"预览"按钮调用） */
defineExpose({
  togglePreview: (status) => editorRef.value && editorRef.value.togglePreview(status),
  togglePreviewOnly: (status) => editorRef.value && editorRef.value.togglePreviewOnly(status),
  togglePageFullscreen: (status) => editorRef.value && editorRef.value.togglePageFullscreen(status),
  toggleCatalog: (status) => editorRef.value && editorRef.value.toggleCatalog(status),
  focus: (option) => editorRef.value && editorRef.value.focus(option)
})
</script>

<style scoped>
.blog-md-editor {
  width: 100%;
  height: 100%;
}
</style>
