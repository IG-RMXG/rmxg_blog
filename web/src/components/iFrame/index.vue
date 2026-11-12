<template>
  <!-- 通用 iframe 容器：src 指向外部页面，高度随视口重算。当前未被任何路由或组件引用，
       后台内链菜单实际渲染的是 layout/components/InnerLink（见脚本顶部说明）。 -->
  <div v-loading="loading" :style="'height:' + height">
    <iframe 
      :src="url" 
      frameborder="no" 
      style="width: 100%; height: 100%" 
      scrolling="auto" />
  </div>
</template>

<script setup>
/*
 * 通用 iframe 容器。
 *
 * 现状：后台「内链」菜单实际渲染的是 layout/components/InnerLink（permission.js 里按
 * component === 'InnerLink' 做映射），本组件既没有被 import、也没在 main.js 注册，属脚手架遗留件。
 *
 * 实现要点：高度 = 视口高度 - 顶部固定区域（navbar 50px + tags-view 等，约 94.5px），
 * 算出的值直接拼进 style，使 iframe 恰好占满内容区、页面级不出现滚动条。
 * 若将来启用，需确认目标站点允许被 iframe 嵌套，否则会被 X-Frame-Options 拦成空白。
 */

// 必填：被嵌入页面的完整地址
const props = defineProps({
  src: {
    type: String,
    required: true
  }
})

// 值末尾自带 "px;"，拼进 style 后形如 height:800px;，多出的分号会被浏览器忽略（历史写法，保留）
const height = ref(document.documentElement.clientHeight - 94.5 + "px;")
// 首帧遮罩：用固定 300ms 代替 iframe 的 load 事件，只负责盖住打开瞬间的白屏，
// 不会因为目标站点加载慢而一直转圈
const loading = ref(true)
// 包一层 computed：父组件切换 src 时 iframe 会随之重新加载
const url = computed(() => props.src)

onMounted(() => {
  setTimeout(() => {
    loading.value = false
  }, 300)
  // 这里用 onresize 赋值而非 addEventListener：会覆盖其他注册者，且组件卸载后不会解绑。
  // 目前后台仅本组件这样写（其余组件均用 addEventListener），保持与 RuoYi 原实现一致。
  window.onresize = function temp() {
    height.value = document.documentElement.clientHeight - 94.5 + "px;"
  }
})
</script>
