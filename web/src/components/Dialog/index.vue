<template>
  <!--
    ⚠️ 必须用本地导入的原始 Element Plus 弹窗（ElDialogBase），不能写 <el-dialog>：
    本组件在 main.js 里覆盖注册成了全局 ElDialog，模板里再写 <el-dialog> 会解析到自己，
    造成无限自我递归 → Maximum call stack size exceeded → 整个应用白屏。
    本地 import 的绑定优先级高于全局注册，因此这里拿到的始终是原生组件。
  -->
  <ElDialogBase
    v-bind="forwardAttrs()"
    :width="responsiveWidth()"
    :fullscreen="isFullscreen"
    :class="['app-dialog', { 'app-dialog--no-close': !hasClose() }]"
  >
    <!--
      自定义头部：标题（或视图自己的 header 插槽）+ 最大化/还原按钮。
      headerProps 是 Element Plus 传给 header 插槽的 { close, titleId, titleClass }，原样转发给视图，
      保证对视图完全透明（如 IconsDialog 会解构这些字段）。
    -->
    <template v-if="$attrs.title || $slots.header" #header="headerProps">
      <div class="app-dialog__header">
        <div class="app-dialog__title" :class="{ 'app-dialog__title--text': !$slots.header }">
          <slot name="header" v-bind="headerProps">
            <span class="el-dialog__title">{{ $attrs.title }}</span>
          </slot>
        </div>
        <span
          v-if="isMaximizable()"
          class="app-dialog__max"
          :title="isFullscreen ? '还原' : '最大化'"
          @click="toggleFullscreen"
        >
          <svg-icon :icon-class="isFullscreen ? 'exit-fullscreen' : 'fullscreen'" />
        </span>
      </div>
    </template>

    <!-- 默认插槽 / 底部插槽原样透传 -->
    <slot />
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </ElDialogBase>
</template>

<script setup>
import { ElDialog as ElDialogBase } from 'element-plus'
/**
 * 全局弹窗包装（在 main.js 里覆盖注册为 ElDialog，全站生效）。
 *
 * 提供两件事：
 *   1. 每个弹窗右上角多一个「最大化 / 还原」按钮（用项目自带的 fullscreen / exit-fullscreen 图标，
 *      与 Screenfull 组件风格一致）
 *   2. 最大化后的整页布局适配：头部/底部固定、内容区自适应滚动（见下方非 scoped 样式块）
 *
 * 对视图完全透明：props / 事件（含 v-model）/ 插槽 / class / style 全部原样透传，
 * 因此现有 29 处 <el-dialog> 一行都不用改。
 *
 * 视图可用开关：
 *   :maximizable="false"   关掉最大化按钮（例如带 !important 内联定位的窄弹窗）
 *
 * ⚠️ 实现注意：Vue 3.5 起 attrs 不是响应式对象（runtime-core 里 createInternalObject 返回空原型对象，
 * updateProps 直接改属性）。父组件修改 attrs 会让本组件重渲染（shouldUpdateComponent 比较 vnode props），
 * 但 computed 的缓存不会失效——所以下面所有基于 attrs 的判断都写成「方法调用 / 模板表达式」，
 * 每次渲染重新求值。**不要把 attrs 塞进 computed**。
 */
defineOptions({ name: 'AppDialog', inheritAttrs: false })

const attrs = useAttrs()

/** 这三个由本组件自己消费（width 做自适应、fullscreen 做切换、maximizable 是开关），不转发给 el-dialog */
const NOT_FORWARDED = ['width', 'fullscreen', 'maximizable']

function forwardAttrs() {
  const out = {}
  for (const key in attrs) {
    if (!NOT_FORWARDED.includes(key)) {
      out[key] = attrs[key]
    }
  }
  return out
}

function isMaximizable() {
  return attrs.maximizable !== false && attrs.maximizable !== 'false'
}

function hasClose() {
  return attrs.showClose !== false && attrs.showClose !== 'false'
}

/**
 * 宽度自适应：
 *   - 数字 / px 值 → min(原值, 94vw)：窄屏不溢出，宽屏保持原尺寸（与改前完全一致）
 *   - 百分比等其它写法 → 原样交给 Element Plus
 */
function responsiveWidth() {
  const w = attrs.width
  if (w === undefined || w === null || w === '') {
    return undefined
  }
  const s = String(w).trim()
  if (/^\d+(\.\d+)?$/.test(s)) {
    return `min(${s}px, 94vw)`
  }
  if (/^\d+(\.\d+)?px$/i.test(s)) {
    return `min(${s}, 94vw)`
  }
  return s
}

/** 初始值尊重视图传入的 fullscreen，之后由按钮接管 */
const isFullscreen = ref(
  attrs.fullscreen === '' || attrs.fullscreen === true || attrs.fullscreen === 'true'
)

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
}
</script>

<style scoped>
.app-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  /* 给右上角两个按钮让位：最大化(28) + 间距(4) + 关闭(48) + 余量 */
  padding-right: 84px;
}
.app-dialog--no-close .app-dialog__header {
  padding-right: 48px;
}
.app-dialog__title {
  flex: 1;
  min-width: 0;
}
/* 只有默认标题文本才需要单行省略；视图自带 header 插槽（可能含输入框等）时不做裁剪 */
.app-dialog__title--text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/*
 * 最大化按钮与 Element Plus 的关闭按钮对齐：
 * .el-dialog 是 position:relative；EP 的 .el-dialog__headerbtn 是 absolute right:0/top:0
 * 的 48×48 方块（视觉中心在距右上角 24px 处）。这里用同一坐标系定位，
 * 右移 52px 留出 4px 间距、top:10px 让两者中心齐平。
 */
.app-dialog__max {
  position: absolute;
  top: 10px;
  right: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  cursor: pointer;
  color: #909399;
  font-size: 15px;
  transition: color 0.2s, background-color 0.2s;
}
/* 没有关闭按钮时（:show-close="false"）贴到对话框内边距处 */
.app-dialog--no-close .app-dialog__max {
  right: 16px;
}
.app-dialog__max:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}
</style>

<style>
/*
 * 非 scoped 块：需要作用于 el-dialog 自身的内部结构。
 * Element Plus 的 is-fullscreen 只给了 height:100% + overflow:auto（整块一起滚），
 * 这里改成标准的「头部固定 + 内容滚动 + 底部固定」整页布局，让内部表格/树/表单自适应。
 * 选择器都带 .app-dialog 前缀，不会外泄到其它组件。
 */
.el-dialog.is-fullscreen.app-dialog {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.el-dialog.is-fullscreen.app-dialog .el-dialog__header,
.el-dialog.is-fullscreen.app-dialog .el-dialog__footer {
  flex: none;
}
.el-dialog.is-fullscreen.app-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
</style>