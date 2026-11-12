<template>
  <footer v-if="visible" class="copyright">
    <span>{{ content }}</span>
  </footer>
</template>

<script setup>
// 底部版权条：显隐与文案都读 settings store，由设置面板的「底部版权」开关控制。
// 组件自身固定定位在视口底部，AppMain.vue 用 .app-main:has(.copyright) 为它预留了 36px。
import useSettingsStore from '@/store/modules/settings'

const settingsStore = useSettingsStore()

// 只读 store、不存本地副本，设置面板一改就即时反映到页脚
const visible = computed(() => settingsStore.footerVisible)
const content = computed(() => settingsStore.footerContent)
</script>

<style scoped>
/* 固定铺满视口底部，高度与 AppMain 中预留的 36px 一一对应 */
.copyright {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 36px;
  padding: 10px 20px;
  text-align: right;
  background-color: #f8f8f8;
  color: #666;
  font-size: 14px;
  border-top: 1px solid #e7e7e7;
  z-index: 999;
}
</style>