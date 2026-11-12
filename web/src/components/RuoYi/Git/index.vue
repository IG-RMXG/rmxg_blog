<template>
  <div>
    <!-- 顶栏源码仓库入口：RuoYi 脚手架自带的外部链接，指向 Gitee 上的上游项目而非本项目仓库 -->
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script setup>
// 上游 RuoYi-Cloud 仓库地址；部署到生产环境前通常需要替换或移除此入口
const url = ref('https://gitee.com/y_project/RuoYi-Cloud')

// 用 window.open 打开外部站点：不经过前端路由，因此不会新增 tags-view 标签、也不受权限守卫拦截
function goto() {
  window.open(url.value)
}
</script>
