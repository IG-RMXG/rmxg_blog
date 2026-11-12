<template>
  <div :class="classObj" class="app-wrapper" :style="{ '--current-color': theme }">
    <!-- 移动端抽屉遮罩：仅在小屏且侧边栏展开时出现，点击即收起 -->
    <div v-if="device === 'mobile' && sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container" />
    <!-- 主体区：hasTagsView / sidebarHide 两个类名会被 AppMain.vue 的样式引用，用于高度与宽度补偿 -->
    <div :class="{ hasTagsView: needTagsView, sidebarHide: sidebar.hide }" class="main-container">
      <!-- 固定头部容器：fixedHeader 打开时用 position: fixed 吸顶 -->
      <div :class="{ 'fixed-header': fixedHeader }">
        <navbar @setLayout="setLayout" />
        <tags-view v-if="needTagsView" />
      </div>
      <!-- 路由内容区：内部自带 keep-alive 缓存、iframe 页签挂载与底部版权 -->
      <app-main />
      <!-- 布局设置抽屉：navbar 的「布局设置」经 setLayout 事件转调这里的 openSetting() -->
      <settings ref="settingRef" />
    </div>
  </div>
</template>

<script setup>
// 布局根组件：搭出「侧边栏 + 顶栏 + 标签栏 + 内容区」的整体骨架，
// 并把窗口宽度映射为 app store 中的设备类型（mobile / desktop）。
// 主题色以 CSS 变量 --current-color 挂在根节点上，供子组件样式直接引用。
import { useWindowSize } from '@vueuse/core'
import Sidebar from './components/Sidebar/index.vue'
import { AppMain, Navbar, Settings, TagsView } from './components'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'

// ===== 布局开关 =====
// 全部取自 store，组件内不保存副本，保证设置面板改动后立即生效：
// settingsStore 管主题/配色/标签栏/固定头部，appStore 管侧边栏开合与设备类型
const settingsStore = useSettingsStore()
const theme = computed(() => settingsStore.theme)
const sideTheme = computed(() => settingsStore.sideTheme)
const sidebar = computed(() => useAppStore().sidebar)
const device = computed(() => useAppStore().device)
const needTagsView = computed(() => settingsStore.tagsView)
const fixedHeader = computed(() => settingsStore.fixedHeader)

// 根节点类名驱动样式：hideSidebar/openSidebar 决定侧边栏宽度，withoutAnimation 关闭过渡动画，
// mobile 标记小屏（此时侧边栏变成覆盖式抽屉，需要配合上面的遮罩层）
const classObj = computed(() => ({
  hideSidebar: !sidebar.value.opened,
  openSidebar: sidebar.value.opened,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === 'mobile'
}))

const { width, height } = useWindowSize()
const WIDTH = 992 // refer to Bootstrap's responsive design

// 只监听设备类型本身：切到移动端时自动收起侧边栏，避免抽屉挡住内容；
// 反向回到桌面端不强制展开，保留用户手动折叠的偏好
watch(() => device.value, () => {
  if (device.value === 'mobile' && sidebar.value.opened) {
    useAppStore().closeSideBar({ withoutAnimation: false })
  }
})

// 992px 断点（沿用 Bootstrap 的 md）：宽度减 1 后比较，等价于实际宽度 ≤ 992 视为移动端。
// 移动端顺带以无动画方式收起侧边栏，避免缩放窗口时看到多余的抽屉滑动
watchEffect(() => {
  if (width.value - 1 < WIDTH) {
    useAppStore().toggleDevice('mobile')
    useAppStore().closeSideBar({ withoutAnimation: true })
  } else {
    useAppStore().toggleDevice('desktop')
  }
})

// 点击遮罩收起抽屉；这里要滑出动画，所以 withoutAnimation 为 false
function handleClickOutside() {
  useAppStore().closeSideBar({ withoutAnimation: false })
}

// 设置抽屉对外只暴露 openSetting()，由本组件通过 ref 调用，navbar 不需要知道抽屉细节
const settingRef = ref(null)
function setLayout() {
  settingRef.value.openSetting()
}
</script>

<style lang="scss" scoped>
@use "@/assets/styles/mixin.scss" as mix;
@use "@/assets/styles/variables.module.scss" as vars;

.app-wrapper {
  @include mix.clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

/* 固定头部时主体区锁定视口高度并隐藏溢出，滚动交给 AppMain 内部，头部不会随页面滚走 */
.main-container:has(.fixed-header) {
  height: 100vh;
  overflow: hidden;
}

/* 移动端抽屉遮罩：绝对定位铺满根节点，层级低于侧边栏（sidebar.scss 中为 1001） */
.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

/* 固定头部宽度要扣掉侧边栏：展开宽度取 SCSS 变量，折叠后固定为 54px（只剩图标的宽度） */
.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - #{vars.$base-sidebar-width});
  transition: width 0.28s;
}

.hideSidebar .fixed-header {
  width: calc(100% - 54px);
}

.sidebarHide .fixed-header {
  width: 100%;
}

.mobile .fixed-header {
  width: 100%;
}
</style>