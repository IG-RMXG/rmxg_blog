<template>
  <div id="tags-view-container" class="tags-view-container">
    <!-- 页签滚动区：点击切换路由，中键关闭（固定页签除外），右键呼出上下文菜单 -->
    <scroll-pane ref="scrollPaneRef" class="tags-view-wrapper" @scroll="handleScroll">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :data-path="tag.path"
        :class="{ 'active': isActive(tag), 'has-icon': tagsIcon }"
        :to="{ path: tag.path, query: tag.query, fullPath: tag.fullPath }"
        class="tags-view-item"
        :style="activeStyle(tag)"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : ''"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        <svg-icon v-if="tagsIcon && tag.meta && tag.meta.icon && tag.meta.icon !== '#'" :icon-class="tag.meta.icon" />
        {{ tag.title }}
        <span v-if="!isAffix(tag)" @click.prevent.stop="closeSelectedTag(tag)">
          <close class="el-icon-close" style="width: 1em; height: 1em;vertical-align: middle;" />
        </span>
      </router-link>
    </scroll-pane>
    <!-- 右键菜单：位置由 openMenu 依鼠标坐标计算，并在靠近右边界时向左钳制 -->
    <ul v-show="visible" :style="{ left: left + 'px', top: top + 'px' }" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)">
        <refresh-right style="width: 1em; height: 1em;" /> 刷新页面
      </li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">
        <close style="width: 1em; height: 1em;" /> 关闭当前
      </li>
      <li @click="closeOthersTags">
        <circle-close style="width: 1em; height: 1em;" /> 关闭其他
      </li>
      <li v-if="!isFirstView()" @click="closeLeftTags">
        <back style="width: 1em; height: 1em;" /> 关闭左侧
      </li>
      <li v-if="!isLastView()" @click="closeRightTags">
        <right style="width: 1em; height: 1em;" /> 关闭右侧
      </li>
      <li @click="closeAllTags(selectedTag)">
        <circle-close style="width: 1em; height: 1em;" /> 全部关闭
      </li>
    </ul>
  </div>
</template>

<script setup>
// 多页签（Tags-View）：记录访问历史，支持右键刷新/关闭/关闭左右侧/全部关闭。
// 页签数据（visitedViews / cachedViews / iframeViews）全部放在 tagsView store，本组件只管交互；
// 关闭页签时若关掉的是当前页，需要回退到剩余的最后一个页签，否则路由会停在已销毁的页面上。
import ScrollPane from './ScrollPane'
import { getNormalPath } from '@/utils/ruoyi'
import useTagsViewStore from '@/store/modules/tagsView'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

// ===== 右键菜单状态：visible 控制显隐，left/top 为视口内偏移，selectedTag 为被右键命中的页签 =====
const visible = ref(false)
const top = ref(0)
const left = ref(0)
const selectedTag = ref({})
const affixTags = ref([])
const scrollPaneRef = ref(null)

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

// 页签与 keep-alive 缓存同源：新增页签即加入缓存，关闭页签即移出缓存
const visitedViews = computed(() => useTagsViewStore().visitedViews)
const routes = computed(() => usePermissionStore().routes)
const theme = computed(() => useSettingsStore().theme)
const tagsIcon = computed(() => useSettingsStore().tagsIcon)

// 路由变化后登记页签，并把当前页签滚入可视区域（页签多时容器横向滚动）
watch(route, () => {
  addTags()
  moveToCurrentTag()
})

// 菜单打开时监听全局 click 以便点空白处收起；关闭时立即解绑，避免 document 上长期挂着一个监听
watch(visible, (value) => {
  if (value) {
    document.body.addEventListener('click', closeMenu)
  } else {
    document.body.removeEventListener('click', closeMenu)
  }
})

// 固定页签（meta.affix）要先注册，保证它们始终排在页签列表最前面且不可关闭
onMounted(() => {
  initTags()
  addTags()
})

// 只比较 path：同一页面携带不同 query 视为同一个页签
function isActive(r) {
  return r.path === route.path
}

// 选中页签的底色/边框跟随主题色；用 JS 计算而不是写死样式，是因为主题色可在设置面板动态调整
function activeStyle(tag) {
  if (!isActive(tag)) return {}
  return {
    "background-color": theme.value,
    "border-color": theme.value
  }
}

// meta.affix 的页签（通常是首页）固定存在，不渲染关闭按钮
function isAffix(tag) {
  return tag.meta && tag.meta.affix
}

// 首/末页签不再提供「关闭左侧 / 关闭右侧」；try/catch 兜住页签不足两个时的数组越界
function isFirstView() {
  try {
    return selectedTag.value.fullPath === '/index' || selectedTag.value.fullPath === visitedViews.value[1].fullPath
  } catch (err) {
    return false
  }
}

function isLastView() {
  try {
    return selectedTag.value.fullPath === visitedViews.value[visitedViews.value.length - 1].fullPath
  } catch (err) {
    return false
  }
}

// 递归收集整棵路由树里 meta.affix 的节点；basePath 逐层拼接后规范化，保证 fullPath 是绝对路径
function filterAffixTags(routes, basePath = '') {
  let tags = []
  routes.forEach(route => {
    if (route.meta && route.meta.affix) {
      const tagPath = getNormalPath(basePath + '/' + route.path)
      tags.push({
        fullPath: tagPath,
        path: tagPath,
        name: route.name,
        meta: { ...route.meta }
      })
    }
    if (route.children) {
      const tempTags = filterAffixTags(route.children, route.path)
      if (tempTags.length >= 1) {
        tags = [...tags, ...tempTags]
      }
    }
  })
  return tags
}

// 固定页签必须有 name：keep-alive 的 include 按组件名匹配，没有 name 的页签缓存不到
function initTags() {
  const res = filterAffixTags(routes.value)
  affixTags.value = res
  for (const tag of res) {
    // Must have tag name
    if (tag.name) {
       useTagsViewStore().addVisitedView(tag)
    }
  }
}

// 匿名路由（没有 name）不生成页签，这类多为未配置 meta 的兜底页
function addTags() {
  const { name } = route
  if (name) {
    useTagsViewStore().addView(route)
  }
}

// 等 DOM 更新后再滚动定位；同一 path 但 query 变化时，顺带把页签上的 fullPath 同步成最新值
function moveToCurrentTag() {
  nextTick(() => {
    for (const r of visitedViews.value) {
      if (r.path === route.path) {
        scrollPaneRef.value.moveToTarget(r)
        // when query is different then update
        if (r.fullPath !== route.fullPath) {
          useTagsViewStore().updateVisitedView(route)
        }
      }
    }
  })
}

// 刷新走 /redirect 中转页重新挂载组件；iframe 页签还要先移除旧实例，否则新地址不会重新加载
function refreshSelectedTag(view) {
  proxy.$tab.refreshPage(view)
  if (route.meta.link) {
    useTagsViewStore().delIframeView(route)
  }
}

// 关掉的正是当前页时才需要跳转，否则停留原页面继续操作
function closeSelectedTag(view) {
  proxy.$tab.closePage(view).then(({ visitedViews }) => {
    if (isActive(view)) {
      toLastView(visitedViews, view)
    }
  })
}

// 同理：当前页被一并关掉才切走，find 用 fullPath 判断（含 query）更精确
function closeRightTags() {
  proxy.$tab.closeRightPage(selectedTag.value).then(visitedViews => {
    if (!visitedViews.find(i => i.fullPath === route.fullPath)) {
      toLastView(visitedViews)
    }
  })
}

// 与关闭右侧同一套判断：当前页是否还在剩余页签里
function closeLeftTags() {
  proxy.$tab.closeLeftPage(selectedTag.value).then(visitedViews => {
    if (!visitedViews.find(i => i.fullPath === route.fullPath)) {
      toLastView(visitedViews)
    }
  })
}

// 先切到被右键的页签再清理其他页签，避免当前路由指向一个已被移出缓存的页面
function closeOthersTags() {
  router.push(selectedTag.value).catch(() => { })
  proxy.$tab.closeOtherPage(selectedTag.value).then(() => {
    moveToCurrentTag()
  })
}

// 当前页本身是固定页签时不跳转（它会被保留在列表里），其余情况回退到剩余页签
function closeAllTags(view) {
  proxy.$tab.closeAllPage().then(({ visitedViews }) => {
    if (affixTags.value.some(tag => tag.path === route.path)) {
      return
    }
    toLastView(visitedViews, view)
  })
}

// 兜底跳转：还有页签就去最后一个；一个都不剩时回首页，
// 若被关掉的本身就是首页，则经 /redirect 中转一次，强制重新加载
function toLastView(visitedViews, view) {
  const latestView = visitedViews.slice(-1)[0]
  if (latestView) {
    router.push(latestView.fullPath)
  } else {
    // now the default is to redirect to the home page if there is no tags-view,
    // you can adjust it according to your needs.
    if (view.name === 'Dashboard') {
      // to reload home page
      router.replace({ path: '/redirect' + view.fullPath })
    } else {
      router.push('/')
    }
  }
}

// 菜单最小宽度 105px：靠右时把 left 钳制到 maxLeft，避免菜单被视口右边截断；
// 15 是鼠标与菜单之间的横向留白
function openMenu(tag, e) {
  const menuMinWidth = 105
  const offsetLeft = proxy.$el.getBoundingClientRect().left // container margin left
  const offsetWidth = proxy.$el.offsetWidth // container width
  const maxLeft = offsetWidth - menuMinWidth // left boundary
  const l = e.clientX - offsetLeft + 15 // 15: margin right

  if (l > maxLeft) {
    left.value = maxLeft
  } else {
    left.value = l
  }

  top.value = e.clientY
  visible.value = true
  selectedTag.value = tag
}

function closeMenu() {
  visible.value = false
}

// 页签区横向滚动时收起菜单，否则菜单会停在已经滚走的页签位置上
function handleScroll() {
  closeMenu()
}
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: 34px;
  width: 100%;
  background: var(--tags-bg, #fff);
  border-bottom: 1px solid var(--tags-item-border, #d8dce5);
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, .12), 0 0 3px 0 rgba(0, 0, 0, .04);

  .tags-view-wrapper {
    .tags-view-item {
      display: inline-block;
      position: relative;
      cursor: pointer;
      height: 26px;
      line-height: 26px;
      border: 1px solid var(--tags-item-border, #d8dce5);
      color: var(--tags-item-text, #495060);
      background: var(--tags-item-bg, #fff);
      padding: 0 8px;
      font-size: 12px;
      margin-left: 5px;
      margin-top: 4px;

      &:first-of-type {
        margin-left: 15px;
      }

      &:last-of-type {
        margin-right: 15px;
      }

      &.active {
        background-color: #42b983;
        color: #fff;
        border-color: #42b983;

        /* 选中页签前的小白点；开启页签图标时由下面 .has-icon 规则去掉，避免和图标重复 */
        &::before {
          content: '';
          background: #fff;
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          position: relative;
          margin-right: 5px;
        }
      }
    }
  }

  .tags-view-item.active.has-icon::before {
    content: none !important;
  }

  /* z-index 3000：需高于 Element Plus 弹层（2000 档），否则右键菜单会被下拉/对话框盖住 */
  .contextmenu {
    margin: 0;
    background: var(--el-bg-color-overlay, #fff);
    z-index: 3000;
    position: absolute;
    list-style-type: none;
    padding: 5px 0;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 400;
    color: var(--tags-item-text, #333);
    box-shadow: 2px 2px 3px 0 rgba(0, 0, 0, .3);
    border: 1px solid var(--el-border-color-light, #e4e7ed);

    li {
      margin: 0;
      padding: 7px 16px;
      cursor: pointer;

      &:hover {
        background: var(--tags-item-hover, #eee);
      }
    }
  }
}
</style>

<style lang="scss">
//reset element css of el-icon-close
.tags-view-wrapper {
  .tags-view-item {
    .el-icon-close {
      width: 16px;
      height: 16px;
      vertical-align: 2px;
      border-radius: 50%;
      text-align: center;
      transition: all .3s cubic-bezier(.645, .045, .355, 1);
      transform-origin: 100% 50%;

      &:before {
        transform: scale(.6);
        display: inline-block;
        vertical-align: -3px;
      }

      &:hover {
        background-color: var(--tags-close-hover, #b4bccc);
        color: #fff;
        width: 12px !important;
        height: 12px !important;
      }
    }
  }
}
</style>