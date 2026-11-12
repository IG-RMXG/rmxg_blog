<template>
  <!-- 根组件只是路由出口：后台 Layout、博客门户、登录页等都由 router 在这里切换挂载 -->
  <router-view />
</template>

<script setup>
/*
 * 根组件职责：除渲染 router-view 外，只做一次应用级初始化（主题色）。
 *
 * 关于 onMounted / nextTick 没有 import：本项目启用了 unplugin-auto-import
 * （vite/plugins/auto-import.js 注入了 vue / vue-router / pinia 三个预设），
 * 这些 API 在编译期自动注入，手写 import 反而会重复。
 *
 * 主题色不走静态 CSS，而是在运行时写入 document.documentElement 的 --el-color-primary
 * 系列 CSS 变量（实现见 utils/theme.js），所以必须等浏览器 DOM 就绪后再写；
 * nextTick 用来把写变量的时机推到本次挂载的渲染任务之后。此处理只执行一次，
 * 之后换肤由「布局设置」面板调用同一个 handleThemeStyle，不要另写一套。
 */
import useSettingsStore from '@/store/modules/settings'
import { handleThemeStyle } from '@/utils/theme'

onMounted(() => {
  nextTick(() => {
    // 初始化主题样式
    handleThemeStyle(useSettingsStore().theme)
  })
})
</script>
