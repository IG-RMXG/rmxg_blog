<template>
  <el-menu
    :default-active="activeMenu"
    mode="horizontal"
    @select="handleSelect"
    :ellipsis="false"
  >
    <!-- 前 visibleNumber 项直接展示，超出部分在下方「更多菜单」里再渲染一遍 -->
    <template v-for="(item, index) in topMenus">
      <el-menu-item :style="{'--theme': theme}" :index="item.path" :key="index" v-if="index < visibleNumber">
        <svg-icon
        v-if="item.meta && item.meta.icon && item.meta.icon !== '#'"
        :icon-class="item.meta.icon"/>
        {{ item.meta.title }}
      </el-menu-item>
    </template>

    <!-- 顶部菜单超出数量折叠 -->
    <el-sub-menu :style="{'--theme': theme}" index="more" v-if="topMenus.length > visibleNumber">
      <template #title>更多菜单</template>
      <template v-for="(item, index) in topMenus">
        <el-menu-item
          :index="item.path"
          :key="index"
          v-if="index >= visibleNumber">
        <svg-icon
          v-if="item.meta && item.meta.icon && item.meta.icon !== '#'"
          :icon-class="item.meta.icon"/>
        {{ item.meta.title }}
        </el-menu-item>
      </template>
    </el-sub-menu>
  </el-menu>
</template>

<script setup>
/**
 * 顶部横向菜单（topbar 布局下替代侧边栏）：数据来自 permissionStore.topbarRouters，
 * 即登录后按权限生成的菜单树；点击一级菜单时联动左侧二级菜单与侧边栏显隐。
 *
 * 超出宽度的菜单不交给 Element Plus 的 ellipsis，而是自己按 visibleNumber 截断，
 * 多出来的收进「更多菜单」。模板里 meta.icon 为 "#" 或空表示该项不显示图标。
 */
import { constantRoutes } from "@/router"
import { isHttp } from '@/utils/validate'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

// 顶部栏初始数
// 顶部最多显示几项，由屏宽估算（见 setVisibleNumber），窗口 resize 时重算，不写死。
const visibleNumber = ref(null)
// 当前激活菜单的 index
const currentIndex = ref(null)
// 隐藏侧边栏路由
// 首页与个人中心是独立单页，命中时不展开左侧菜单（直接整页展示）。
const hideList = ['/index', '/user/profile']

const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const router = useRouter()

// 主题颜色
const theme = computed(() => settingsStore.theme)
// 所有的路由信息
const routers = computed(() => permissionStore.topbarRouters)

// 顶部显示菜单
// 过滤 hidden 后取一级菜单；path 为 "/" 且带 children 的父级只是容器（用于内部跳转），
// 用它的第一个子项代表它，避免顶栏出现一个点不动的空菜单。
const topMenus = computed(() => {
  let topMenus = []
  routers.value.map((menu) => {
    if (menu.hidden !== true) {
      // 兼容顶部栏一级菜单内部跳转
      if (menu.path === '/' && menu.children) {
          topMenus.push(menu.children[0])
      } else {
          topMenus.push(menu)
      }
    }
  })
  return topMenus
})

// 设置子路由
// 把二级路由摊平并补全 parentPath：子路由 path 常是相对写法，需要按父级 path 拼成绝对路径，
// 供 activeMenu 高亮与 handleSelect 匹配；最后并上 constantRoutes（登录前就存在的静态路由）。
// 注意这里会直接改写 router 上的 path/parentPath，属于一次性缓存，后续不再重复计算。
const childrenMenus = computed(() => {
  let childrenMenus = []
  routers.value.map((router) => {
    for (let item in router.children) {
      if (router.children[item].parentPath === undefined) {
        if(router.path === "/") {
          router.children[item].path = "/" + router.children[item].path
        } else {
          if(!isHttp(router.children[item].path)) {
            router.children[item].path = router.path + "/" + router.children[item].path
          }
        }
        router.children[item].parentPath = router.path
      }
      childrenMenus.push(router.children[item])
    }
  })
  return constantRoutes.concat(childrenMenus)
})

// 默认激活的菜单
// 用路径首段（/a/b/c → /a）反推所属一级菜单，并顺带决定侧边栏显隐：
// 有二级菜单就展开侧边栏，否则收起。外链（meta.link）不截取首段，避免把地址截坏。
const activeMenu = computed(() => {
  const path = route.path
  let activePath = path
  if (path !== undefined && path.lastIndexOf("/") > 0 && hideList.indexOf(path) === -1) {
    const tmpPath = path.substring(1, path.length)
    if (!route.meta.link) {
      activePath = "/" + tmpPath.substring(0, tmpPath.indexOf("/"))
      appStore.toggleSideBarHide(false)
    }
  } else if(!route.children) {
    activePath = path
    appStore.toggleSideBarHide(true)
  }
  activeRoutes(activePath)
  return activePath
})

// 估算可见菜单数：屏宽的三分之一留给顶部菜单，每项按 85px（含图标与间距）折算。
function setVisibleNumber() {
  const width = document.body.getBoundingClientRect().width / 3
  visibleNumber.value = parseInt(width / 85)
}

// 点击一级菜单分三种情况：
//   1. http(s) 外链 → 新窗口打开；
//   2. 无子路由 → 内部跳转（路由表里带 query 时先还原成对象），并收起侧边栏；
//   3. 有子路由 → 刷新左侧联动菜单并展开侧边栏。
function handleSelect(key, keyPath) {
  currentIndex.value = key
  const route = routers.value.find(item => item.path === key)
  if (isHttp(key)) {
    // http(s):// 路径新窗口打开
    window.open(key, "_blank")
  } else if (!route || !route.children) {
    // 没有子路由路径内部打开
    const routeMenu = childrenMenus.value.find(item => item.path === key)
    if (routeMenu && routeMenu.query) {
      let query = JSON.parse(routeMenu.query)
      router.push({ path: key, query: query })
    } else {
      router.push({ path: key })
    }
    appStore.toggleSideBarHide(true)
  } else {
    // 显示左侧联动菜单
    activeRoutes(key)
    appStore.toggleSideBarHide(false)
  }
}

// 把 parentPath 等于当前一级菜单的子路由写入侧边栏 store，实现左侧菜单联动；
// 一个都没匹配到（例如静态路由的单页）就收起侧边栏，避免左侧留一个空壳。
function activeRoutes(key) {
  let routes = []
  if (childrenMenus.value && childrenMenus.value.length > 0) {
    childrenMenus.value.map((item) => {
      if (key == item.parentPath || (key == "index" && "" == item.path)) {
        routes.push(item)
      }
    })
  }
  if(routes.length > 0) {
    permissionStore.setSidebarRouters(routes)
  } else {
    appStore.toggleSideBarHide(true)
  }
  return routes
}

// 窗口变化时重算可见项数；初值由下面第二个 onMounted 计算。
onMounted(() => {
  window.addEventListener('resize', setVisibleNumber)
})

// 卸载时务必移除监听，否则顶栏切走后 resize 仍会触发并操作已销毁的组件状态。
onBeforeUnmount(() => {
  window.removeEventListener('resize', setVisibleNumber)
})

onMounted(() => {
  setVisibleNumber()
})
</script>

<style lang="scss">
.topmenu-container.el-menu--horizontal > .el-menu-item {
  float: left;
  height: 50px !important;
  line-height: 50px !important;
  color: #999093 !important;
  padding: 0 5px !important;
  margin: 0 10px !important;
}

.topmenu-container.el-menu--horizontal > .el-menu-item.is-active, .el-menu--horizontal > .el-sub-menu.is-active .el-submenu__title {
  border-bottom: 2px solid #{'var(--theme)'} !important;
  color: #303133;
}

/* sub-menu item */
.topmenu-container.el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
  float: left;
  height: 50px !important;
  line-height: 50px !important;
  color: #999093 !important;
  padding: 0 5px !important;
  margin: 0 10px !important;
}

/* 背景色隐藏 */
.topmenu-container.el-menu--horizontal>.el-menu-item:not(.is-disabled):focus, .topmenu-container.el-menu--horizontal>.el-menu-item:not(.is-disabled):hover, .topmenu-container.el-menu--horizontal>.el-submenu .el-submenu__title:hover {
  background-color: #ffffff;
}

/* 图标右间距 */
.topmenu-container .svg-icon {
  margin-right: 4px;
}

/* topmenu more arrow */
.topmenu-container .el-sub-menu .el-sub-menu__icon-arrow {
  position: static;
  vertical-align: middle;
  margin-left: 8px;
  margin-top: 0px;
}


</style>
