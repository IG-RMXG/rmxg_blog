<template>
  <!-- 雪碧图图标：iconClass 对应 src/assets/icons/svg/<name>.svg，symbol id 为 icon-<name>（见 vite/plugins/svg-icon.js） -->
  <svg :class="svgClass" aria-hidden="true">
    <use :xlink:href="iconName" :fill="color" />
  </svg>
</template>

<script>
/*
 * 全局 SVG 图标组件（main.js 注册为 <svg-icon>）。
 *
 * 用法：<svg-icon icon-class="search" />，可选 color 覆盖填充色、class-name 追加样式类。
 * 依赖 vite-plugin-svg-icons 注入的 sprite（main.js 的 virtual:svg-icons-register）：
 * 新图标只需把 .svg 放进 src/assets/icons/svg/，按文件名即可使用，无需在此登记。
 * 图标名不存在时不会报错，只是渲染为空白。
 */
export default defineComponent({
  props: {
    // 图标名，不带 icon- 前缀
    iconClass: {
      type: String,
      required: true
    },
    // 追加的类名，用于覆盖尺寸/颜色等样式
    className: {
      type: String,
      default: ''
    },
    // 覆盖 <use> 的 fill；为空时不设置，图标跟随父级文字颜色（currentColor）
    color: {
      type: String,
      default: ''
    },
  },
  setup(props) {
    return {
      // vite-plugin-svg-icons 注册的 symbol id 统一带 icon- 前缀
      iconName: computed(() => `#icon-${props.iconClass}`),
      // 始终保留基础类 svg-icon（提供尺寸/对齐），再合并调用方传入的 className
      svgClass: computed(() => {
        if (props.className) {
          return `svg-icon ${props.className}`
        }
        return 'svg-icon'
      })
    }
  }
})
</script>

<style scope lang="scss">
/* 注意属性是 scope 而非 scoped，样式实际全局生效（历史写法）；改动会波及所有使用 svg-icon 的地方 */
/* 侧边栏/下拉菜单图标的排版，这两个类由菜单渲染代码直接挂到图标上 */
.sub-el-icon,
.nav-icon {
  display: inline-block;
  font-size: 15px;
  margin-right: 12px;
  position: relative;
}

/* 1em 让图标跟随字号缩放；fill: currentColor 让图标跟随文字颜色；-2px 校正 SVG 与文本的基线 */
.svg-icon {
  width: 1em;
  height: 1em;
  position: relative;
  fill: currentColor;
  vertical-align: -2px;
}
</style>
