/**
 * Markdown 编辑器（md-editor-v3）全局配置。
 *
 * 默认情况下 md-editor-v3 会从 unpkg CDN 拉取 highlight/katex/mermaid 等依赖，
 * 这里统一注入项目本地依赖实例，保证离线/内网环境也能正常高亮、渲染公式与图表。
 *
 * 说明：未注入 echarts（__echarts 代码块）—— 该项目 echarts 属未使用依赖，
 * 为一个低频功能往编辑器懒加载分块里塞约 1MB 不划算；需要时可由 CDN 兜底。
 */
import { config } from 'md-editor-v3'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import mermaid from 'mermaid'

config({
  editorExtensions: {
    highlight: { instance: hljs },
    katex: { instance: katex },
    mermaid: { instance: mermaid }
  }
})

/**
 * 工具栏：覆盖常见博客正文元素。
 * 刻意不含 prettier / fullscreen —— 这两项依赖 CDN 运行时脚本，本地不可用时会报错。
 */
export const MD_TOOLBARS = [
  'bold', 'underline', 'italic', 'strikeThrough', '-',
  'title', 'sub', 'sup', 'quote', 'unorderedList', 'orderedList', 'task', '-',
  'codeRow', 'code', 'link', 'image', 'table', '-',
  'katex', 'mermaid', '=',
  'revoke', 'next', 'pageFullscreen', 'preview', 'previewOnly', 'catalog'
]

export const MD_PREVIEW_THEME = 'github'
export const MD_CODE_THEME = 'github'
export const MD_FOOTERS = ['markdownTotal', '=', 'scrollSwitch']
