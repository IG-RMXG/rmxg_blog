<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="tagsViewStore.cachedViews">
          <component v-if="!route.meta.link" :is="Component" :key="route.path"/>
        </keep-alive>
      </transition>
    </router-view>
    <iframe-toggle />
    <copyright />
  </section>
</template>

<script setup>
import copyright from "./Copyright/index"
import iframeToggle from "./IframeToggle/index"
import useTagsViewStore from '@/store/modules/tagsView'

const route = useRoute()
const tagsViewStore = useTagsViewStore()

onMounted(() => {
  addIframe()
})

watchEffect(() => {
  addIframe()
})

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
