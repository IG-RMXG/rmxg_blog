<template>
  <div class="blog-md-preview">
    <!-- 只读渲染文章正文：不引入编辑器，样式与预览端共用 @/utils/md-editor 的主题配置 -->
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
/**
 * 文章正文只读预览组件（md-editor-v3 的 MdPreview）
 * - 只做渲染，不引入编辑器实例，详情页/列表页可直接使用
 * - 主题与代码高亮配置取自 @/utils/md-editor，保证与编辑器内的预览效果一致
 * - 行内滚动与高度由外层样式接管（见下方 :deep 覆盖），详情页由整页滚动
 */
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { MD_PREVIEW_THEME, MD_CODE_THEME } from '@/utils/md-editor'

// content：markdown 原文，缺省为空串，避免把 undefined 传进 MdPreview
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
