<template>
  <div v-if="!item.hidden">
    <!-- 可见子路由唯一时收敛为叶子菜单，多个子路由才走下面的 el-sub-menu 分支 -->
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{ 'submenu-title-noDropdown': !isNest }">
          <svg-icon :icon-class="onlyOneChild.meta.icon || (item.meta && item.meta.icon)"/>
          <template #title><span class="menu-title" :title="hasTitle(onlyOneChild.meta.title)">{{ onlyOneChild.meta.title }}</span></template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" teleported>
      <template v-if="item.meta" #title>
        <svg-icon :icon-class="item.meta && item.meta.icon" />
        <span class="menu-title" :title="hasTitle(item.meta.title)">{{ item.meta.title }}</span>
      </template>

      <sidebar-item
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup>
// 菜单项递归组件：拿到权限过滤后路由树上的一个节点，决定渲染成「可直接点击的叶子菜单」
// 还是「可展开的 el-sub-menu」，并把 path 逐层拼成绝对路径；hidden 的节点整棵都不渲染。
import { isExternal } from '@/utils/validate'
import AppLink from './Link'
import { getNormalPath } from '@/utils/ruoyi'

// item：路由节点，读 meta.title / meta.icon / meta.hidden / alwaysShow / children
// isNest：是否处于子菜单层级（父级通过 class 传入），用于收敛叶子项的缩进样式
// basePath：父级已经解析好的路径前缀，本组件据此拼出当前节点的绝对路径
const props = defineProps({
  // route object
  item: {
    type: Object,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
})

// 缓存「唯一可见子路由」：由 hasOneShowingChild 在过滤过程中写入，
// 因为模板既需要布尔判定结果，也需要那个子路由本身来渲染图标与标题
const onlyOneChild = ref({})

// 三种结果：可见子路由恰好 1 个 → 父级不展开，直接把该子路由提升为菜单项；
// 0 个 → 用父级自身占位（path 置空，点击仍回到父级路径）；多于 1 个才返回 false 走 el-sub-menu
function hasOneShowingChild(children = [], parent) {
  if (!children) {
    children = []
  }
  const showingChildren = children.filter(item => {
    if (item.hidden) {
      return false
    }
    onlyOneChild.value = item
    return true
  })

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }

  return false
}

// 拼绝对路径：子路径是外链就原样返回；父路径是外链则整个菜单都指向父级地址；
// 只有站内路径才拼接并交给 getNormalPath 去掉重复的 '/'。
// routeQuery 是 meta 中以 JSON 字符串存放的 query，解析后作为 router-link 的对象参数
function resolvePath(routePath, routeQuery) {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  if (routeQuery) {
    let query = JSON.parse(routeQuery)
    return { path: getNormalPath(props.basePath + '/' + routePath), query: query }
  }
  return getNormalPath(props.basePath + '/' + routePath)
}

// 标题超过 5 个字才输出原生 title 属性（悬浮提示），短标题返回空串避免多余 tooltip
function hasTitle(title){
  if (title.length > 5) {
    return title
  } else {
    return ""
  }
}
</script>
