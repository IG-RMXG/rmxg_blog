<template>
  <div class="blog-md-preview">
    <MdPreview
      :modelValue="content || ''"
      language="zh-CN"
      :previewTheme="MD_PREVIEW_THEME"
      :codeTheme="MD_CODE_THEME"
      :showCodeRowNumber="true"
    />
  </div>
</template>

<script setup>
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { MD_PREVIEW_THEME, MD_CODE_THEME } from '@/utils/md-editor'

defineProps({
  content: { type: String, default: '' }
})
</script>

<style scoped>
/* 详情页由页面整体滚动，预览容器不设固定高度、不内部滚动 */
.blog-md-preview {
  width: 100%;
}
.blog-md-preview :deep(.md-editor),
.blog-md-preview :deep(.md-editor-previewOnly) {
  height: auto;
  border: none;
  background: transparent;
  overflow: visible;
}
.blog-md-preview :deep(.md-editor-content),
.blog-md-preview :deep(.md-editor-preview-wrapper) {
  height: auto;
  overflow: visible;
}
.blog-md-preview :deep(.md-editor-preview) {
  padding-block: 0;
  padding-inline: 0;
}
</style>
