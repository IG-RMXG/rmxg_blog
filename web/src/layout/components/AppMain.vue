<template>
  <section class="app-main">
    <!-- 路由出口：切换过渡 + keep-alive 缓存；缓存名单来自 tagsView store 的 cachedViews -->
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="tagsViewStore.cachedViews">
          <component v-if="!route.meta.link" :is="Component" :key="route.path"/>
        </keep-alive>
      </transition>
    </router-view>
    <!-- iframe 页签容器：同名路由下用 v-show 切换，切走再切回不会重建 iframe -->
    <iframe-toggle />
    <!-- 底部版权条，显隐与文案由设置面板的「底部版权」开关和 settings 里的文案决定 -->
    <copyright />
  </section>
</template>

<script setup>
// 内容区容器：路由出口（含 keep-alive 缓存）、iframe 页签挂载点、底部版权条。
// cachedViews 由 tagsView store 维护，关闭页签即移出缓存，所以「关掉再打开」会重新创建组件；
// 路由 meta.noCache 为 true 的页面不会进入缓存。
import copyright from "./Copyright/index"
import iframeToggle from "./IframeToggle/index"
import useTagsViewStore from '@/store/modules/tagsView'

const route = useRoute()
const tagsViewStore = useTagsViewStore()

// 首屏由 onMounted 登记一次，后续切换交给 watchEffect；两处都调用同一函数，靠 store 按 path 去重
onMounted(() => {
  addIframe()
})

watchEffect(() => {
  addIframe()
})

// 只处理外链菜单（meta.link）：这类路由没有本地组件，需要单独登记到 iframeViews 才有页面可显示
function addIframe() {
  if (route.meta.link) {
    useTagsViewStore().addIframeView(route)
  }
}
</script>

<style lang="scss" scoped>
/*
 * 高度补偿说明：
 * 固定头部 = 导航栏(50) + 标签栏(34) + UAT 横幅。
 * UAT 横幅只在验收环境渲染，它的高度由 UatBanner 组件用 ResizeObserver
 * 写到根元素的 CSS 变量 --uat-banner-h 上（DEV 下该变量不存在）。
 * 这里所有高度计算都减去该变量，默认 0px 时与原值完全等价，
 * 横幅出现时内容自动下移，避免被固定头部盖住顶部。
 */
.app-main {
  /* 50= navbar  50  */
  min-height: calc(100vh - 50px - var(--uat-banner-h, 0px));
  width: 100%;
  position: relative;
  overflow: hidden;
}

.fixed-header + .app-main {
  overflow-y: auto;
  scrollbar-gutter: auto;
  height: calc(100vh - 50px - var(--uat-banner-h, 0px));
  min-height: 0px;
}

/* 出现底部版权条时给内容区预留 36px，避免页面最后一段内容被固定页脚盖住 */
.app-main:has(.copyright) {
  padding-bottom: 36px;
}

.fixed-header + .app-main {
  margin-top: calc(50px + var(--uat-banner-h, 0px));
}

.hasTagsView {
  .app-main {
    /* 84 = navbar + tags-view = 50 + 34 */
    min-height: calc(100vh - 84px - var(--uat-banner-h, 0px));
  }

  .fixed-header + .app-main {
    margin-top: calc(84px + var(--uat-banner-h, 0px));
    height: calc(100vh - 84px - var(--uat-banner-h, 0px));
    min-height: 0px;
  }
}
</style>

<style lang="scss">
/* 全局滚动条样式：本块不加 scoped，直接作用于整个应用；宽度压到 6px 以适配紧凑的管理端布局 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background-color: #c0c0c0;
  border-radius: 3px;
}
</style>
