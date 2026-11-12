<template>
  <inner-link
    v-for="(item, index) in tagsViewStore.iframeViews"
    :key="item.path"
    :iframeId="'iframe' + index"
    v-show="route.path === item.path"
    :src="iframeUrl(item.meta.link, item.query)"
  ></inner-link>
</template>

<script setup>
// iframe 页签容器：只为已打开且带 meta.link 的页签渲染一个 InnerLink。
// 用 v-show 而非 v-if 切换显隐，保证切走再切回时 iframe 内的页面状态（表单、滚动位置）不丢。
import InnerLink from "../InnerLink/index"
import useTagsViewStore from "@/store/modules/tagsView"

const route = useRoute()
const tagsViewStore = useTagsViewStore()

// 把路由 query 拼到外链地址后面：后端配置的 meta.link 本身不带业务参数。
// 注意这里手工拼串、未做 encodeURIComponent，参数值含 & = 或中文时需由调用方保证已编码
function iframeUrl(url, query) {
  if (Object.keys(query).length > 0) {
    let params = Object.keys(query).map((key) => key + "=" + query[key]).join("&")
    return url + "?" + params
  }
  return url
}
</script>
