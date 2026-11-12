<template>
  <div style="padding: 0 15px;" @click="toggleClick">
    <svg
      :class="{'is-active':isActive}"
      class="hamburger"
      viewBox="0 0 1024 1024"
      xmlns="http://www.w3.org/2000/svg"
      width="64"
      height="64"
      fill="currentColor"
    >
      <path d="M408 442h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8zm-8 204c0 4.4 3.6 8 8 8h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56zm504-486H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zm0 632H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zM142.4 642.1L298.7 519a8.84 8.84 0 0 0 0-13.9L142.4 381.9c-5.8-4.6-14.4-.5-14.4 6.9v246.3a8.9 8.9 0 0 0 14.4 7z" />
    </svg>
  </div>
</template>

<script setup>
/**
 * 侧边栏折叠开关（汉堡按钮）：只负责展示与派发点击，自身不维护折叠状态。
 * 父组件收到 toggleClick 后修改 app store 的 sidebar.opened，再通过 isActive 回传，
 * 图标据此翻转方向。SVG 的 path 是图标绘制数据，不要改动。
 */
defineProps({
  // true 表示侧边栏当前已折叠，图标旋转 180° 指向另一侧
  isActive: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits()
// 无参事件：折叠与否由父组件决定，本组件不关心结果
const toggleClick = () => {
  emit('toggleClick')
}
</script>

<style scoped>
.hamburger {
  display: inline-block;
  vertical-align: middle;
  width: 20px;
  height: 20px;
}

/* 折叠态整体翻转，形成「展开 / 收起」的方向指示 */
.hamburger.is-active {
  transform: rotate(180deg);
}
</style>
