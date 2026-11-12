<template>
  <div></div>
</template>

<script setup>
/**
 * 空白中转页，对应路由 /redirect/:path(.*)，只被 $tab.refreshPage 使用。
 * 跳转链路：业务页 → /redirect/<原路径> → 本组件 replace 回原地址。
 * 先离开再进入会让目标组件重新挂载，从而绕过 keep-alive 缓存实现"刷新页签"；
 * 页面自身不渲染任何内容，也不做鉴权/数据请求。
 */
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
// params.path 是去掉前导斜杠的原路径（catch-all 参数，如 blog/article）；query 为刷新前的查询参数
const { params, query } = route
const { path } = params

// 用 replace 而非 push：空白页不写入历史栈，否则浏览器后退会停在空页面；
// path 前补 '/'（params.path 不含前导斜杠），刷新前的 query 一并带回
router.replace({ path: '/' + path, query })
</script>