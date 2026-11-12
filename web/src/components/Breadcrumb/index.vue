<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <!-- 最后一层与 noRedirect 层级只展示文字，其余渲染为可点击链接 -->
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="item.redirect === 'noRedirect' || index == levelList.length - 1" class="no-redirect">{{ item.meta.title }}</span>
        <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup>
/**
 * 面包屑：把当前路由翻译成「首页 / 一级 / 二级 …」的层级链。
 *
 * 两条取数路径：
 *   - 普通页面直接用 route.matched（路由匹配链，最省事）；
 *   - 多级菜单（路径里 "/" 超过 2 个）改用 permissionStore.defaultRoutes 逐级递归匹配，
 *     因为动态路由的层级未必都体现在 matched 上。
 * 非首页时补一个「首页」节点；无 meta.title 或 meta.breadcrumb === false 的层级不显示。
 */
import usePermissionStore from '@/store/modules/permission'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
// 当前渲染的面包屑节点，每项需具备 path 与 meta.title
const levelList = ref([])

// 按路径深度决定取数方式：多级菜单把路径切段后从路由树递归找节点，否则用 matched。
function getBreadcrumb() {
  // only show routes with meta.title
  let matched = []
  const pathNum = findPathNum(route.path)
  // multi-level menu
  if (pathNum > 2) {
    const reg = /\/\w+/gi
    const pathList = route.path.match(reg).map((item, index) => {
      if (index !== 0) item = item.slice(1)
      return item
    })
    getMatched(pathList, permissionStore.defaultRoutes, matched)
  } else {
    matched = route.matched.filter((item) => item.meta && item.meta.title)
  }
  // 判断是否为首页
  if (!isDashboard(matched[0])) {
    matched = [{ path: "/index", meta: { title: "首页" } }].concat(matched)
  }
  levelList.value = matched.filter(item => item.meta && item.meta.title && item.meta.breadcrumb !== false)
}
// 统计路径里分隔符出现的次数，用作菜单层级深度（如 /a/b 为 2）。
function findPathNum(str, char = "/") {
  let index = str.indexOf(char)
  let num = 0
  while (index !== -1) {
    num++
    index = str.indexOf(char, index + 1)
  }
  return num
}
// 递归匹配路由树：先用 path 或（转成字符串的）name 定位当前层，命中后把已消费的段位移除，
// 继续在 children 里找下一段，直到路径段用完。
function getMatched(pathList, routeList, matched) {
  let data = routeList.find(item => item.path == pathList[0] || (item.name += '').toLowerCase() == pathList[0])
  if (data) {
    matched.push(data)
    if (data.children && pathList.length) {
      pathList.shift()
      getMatched(pathList, data.children, matched)
    }
  }
}
// 判断首项是否已经是首页（约定首页路由 name 为 Index），避免重复插入首页节点。
function isDashboard(route) {
  const name = route && route.name
  if (!name) {
    return false
  }
  return name.trim() === 'Index'
}
// 点击中间层级跳转：配置了 redirect 的按 redirect 走，否则用自身 path。
// 最后一级和 noRedirect 层级在模板里只渲染文字，不会触发这里。
function handleLink(item) {
  const { redirect, path } = item
  if (redirect) {
    router.push(redirect)
    return
  }
  router.push(path)
}

// 路由变化后重算；/redirect/ 是登录后跳转用的临时中转页，跳过以免面包屑闪出中转路径。
watchEffect(() => {
  // if you go to the redirect page, do not update the breadcrumbs
  if (route.path.startsWith('/redirect/')) {
    return
  }
  getBreadcrumb()
})
// 首次挂载时 watchEffect 之外的兜底：保证进入页面即有面包屑，不依赖路由是否再次变化。
getBreadcrumb()
</script>

<style lang='scss' scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;
  margin-left: 8px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }
}
</style>