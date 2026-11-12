<template>
  <div :style="'height:' + height" v-loading="loading" element-loading-text="正在加载页面，请稍候！">
    <iframe
      :id="iframeId"
      style="width: 100%; height: 100%"
      :src="src"
      ref="iframeRef"
      frameborder="no"
    ></iframe>
  </div>
</template>

<script setup>
// iframe 宿主：加载期间显示遮罩，iframe 触发 load 后撤掉。
// 外层容器没有固定高度，只能按视口高度减去顶部固定区域算出高度，iframe 的 height:100% 才有参照。
// src：iframe 地址（外链菜单的 meta.link，query 已由 IframeToggle 拼好）；iframeId：唯一 id，便于按 id 定位
const props = defineProps({
  src: {
    type: String,
    default: "/"
  },
  iframeId: {
    type: String
  }
})

// loading 默认为 true，等 iframe 的 load 事件回来才撤销遮罩
const loading = ref(true)
// 94.5 是顶部固定区域的估算值：顶栏 50 + 页签栏 34，其余为边框与间隙余量
const height = ref(document.documentElement.clientHeight - 94.5 + 'px')
const iframeRef = ref(null)

// 必须在 onMounted 后绑定 onload：此时 iframe 的真实 DOM 才存在，模板里写内联 onload 拿不到这个 ref
onMounted(() => {
  if (iframeRef.value) {
    iframeRef.value.onload = () => {
      loading.value = false
    }
  }
})
</script>
