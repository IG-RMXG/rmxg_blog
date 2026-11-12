<template>
  <div>
    <!-- 顶栏「帮助文档」入口：RuoYi 脚手架自带的外部链接，与本项目业务无关 -->
    <svg-icon icon-class="question" @click="goto" />
  </div>
</template>

<script setup>
// RuoYi-Cloud 官方文档地址；若要指向本项目自有文档，改这里的常量即可
const url = ref('http://doc.ruoyi.vip/ruoyi-cloud')

// 用 window.open 打开外部站点：不经过前端路由，因此不会新增 tags-view 标签、也不受权限守卫拦截
function goto() {
  window.open(url.value)
}
</script>