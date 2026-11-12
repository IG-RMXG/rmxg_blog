<template>
  <div>
    <!-- 图标随全屏状态切换：退出全屏时用 exit-fullscreen，其余用 fullscreen -->
    <svg-icon :icon-class="isFullscreen ? 'exit-fullscreen' : 'fullscreen'" @click="toggle" />
  </div>
</template>

<script setup>
import { useFullscreen } from '@vueuse/core'

// 顶栏全屏按钮：useFullscreen 不传 target 时作用于整个文档，并自动监听状态变化，
// 因此用户按 Esc 或浏览器菜单退出全屏时 isFullscreen 会同步回 false，图标无需手动维护。
// enter / exit 这里没有直接使用，只保留 toggle。
const { isFullscreen, enter, exit, toggle } = useFullscreen()
</script>

<style lang='scss' scoped>
/* 注意：模板里并未使用 .screenfull-svg（图标的外观由 svg-icon 自带类控制），此规则目前不生效，属历史遗留 */
.screenfull-svg {
  display: inline-block;
  cursor: pointer;
  fill: #5a5e66;
  width: 20px;
  height: 20px;
  vertical-align: 10px;
}
</style>