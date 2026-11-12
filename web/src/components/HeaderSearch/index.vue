<template>
  <div class="header-search">
    <svg-icon class-name="search-icon" icon-class="search" @click.stop="click" />
    <!-- 搜索弹窗：点击图标开合，隐藏右上角关闭按钮，点遮罩/Esc 也能关闭 -->
    <el-dialog
      v-model="show"
      width="600"
      @close="close"
      :show-close="false"
      append-to-body
    >
      <el-input
        v-model="search"
        ref="headerSearchSelectRef"
        size="large"
        @input="querySearch"
        prefix-icon="Search"
        placeholder="菜单搜索，支持标题、URL模糊查询"
        clearable
        @keyup.enter="selectActiveResult"
        @keydown.up.prevent="navigateResult('up')"
        @keydown.down.prevent="navigateResult('down')"
      >
      </el-input>

      <!-- 结果列表：图标 + 层级标题 + 绝对路径；鼠标悬停与键盘上下键共用 activeIndex -->
      <div class="result-wrap">
        <el-scrollbar>
          <div class="search-item" tabindex="1" v-for="(item, index) in options" :key="item.path" :style="activeStyle(index)" @mouseenter="activeIndex = index" @mouseleave="activeIndex = -1">
            <div class="left">
              <svg-icon class="menu-icon" :icon-class="item.icon" />
            </div>
            <div class="search-info" @click="change(item)">
              <div class="menu-title">
                {{ item.title.join(" / ") }}
              </div>
              <div class="menu-path">
                {{ item.path }}
              </div>
            </div>
            <svg-icon icon-class="enter" v-show="index === activeIndex"/>
          </div>
        </el-scrollbar>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 顶栏菜单搜索：点放大镜弹出搜索框，用 Fuse.js 对「菜单标题 + 路由路径」做模糊匹配，
 * 回车或点击后跳转到对应菜单。
 *
 * 数据来源是 permissionStore.defaultRoutes（登录后动态生成的路由表），
 * 因此结果天然与当前用户可访问的菜单一致，不需要额外接口；组件无 props / emits，
 * 主题色取自 settingsStore.theme，用于高亮键盘选中的结果项。
 */
import Fuse from 'fuse.js'
import { getNormalPath } from '@/utils/ruoyi'
import { isHttp } from '@/utils/validate'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

// ===== 搜索状态 =====
// search 输入关键字；options 当前展示结果；searchPool 全量候选池（挂载时由路由表拍平）；
// activeIndex 键盘上下键定位（-1 表示未选中）；fuse 为 Fuse 实例，候选池变化后重建。
const search = ref('')
const options = ref([])
const searchPool = ref([])
const activeIndex = ref(-1)
const show = ref(false)
const fuse = ref(undefined)
const headerSearchSelectRef = ref(null)
// 主题色与路由表直接取自 store，权限变化后搜索池来源随之改变。
const router = useRouter()
const theme = computed(() => useSettingsStore().theme)
const routes = computed(() => usePermissionStore().defaultRoutes)

// 打开弹窗时聚焦输入框（否则要再点一次才能输入），并先用全量池铺满结果区。
function click() {
  show.value = !show.value
  if (show.value) {
    headerSearchSelectRef.value && headerSearchSelectRef.value.focus()
    options.value = searchPool.value
  }
}

// 关闭时把关键字、结果、键盘选中项全部复位，避免下次打开残留上一次的高亮与列表。
function close() {
  headerSearchSelectRef.value && headerSearchSelectRef.value.blur()
  search.value = ''
  options.value = []
  show.value = false
  activeIndex.value = -1
}

// 跳转规则：
//   - http(s) 外链用新窗口打开（path 上可能带前后缀，所以先定位 "http" 起点再截取）；
//   - 内部路由若带 query，需要把路由表里存的 query 字符串还原成对象再 push；
//   - 收起弹窗放到 nextTick，等输入框失焦和列表清空生效后再关，避免关闭动画期间内容闪一下。
function change(val) {
  const path = val.path
  const query = val.query
  if (isHttp(path)) {
    // http(s):// 路径新窗口打开
    const pindex = path.indexOf("http")
    window.open(path.substr(pindex, path.length), "_blank")
  } else {
    if (query) {
      router.push({ path: path, query: JSON.parse(query) })
    } else {
      router.push(path)
    }
  }

  search.value = ''
  options.value = []
  nextTick(() => {
    show.value = false
  })
}

// Fuse 配置：标题权重 0.7、路径 0.3（标题命中优先排序）；threshold 0.4 表示允许约四成字符不匹配，
// 既能容忍错字又不会把无关菜单全搜出来。
function initFuse(list) {
  fuse.value = new Fuse(list, {
    shouldSort: true,
    threshold: 0.4,
    location: 0,
    distance: 100,
    minMatchCharLength: 1,
    keys: [{
      name: 'title',
      weight: 0.7
    }, {
      name: 'path',
      weight: 0.3
    }]
  })
}

// 把树形路由拍平成搜索项 { path, title: [...层级标题], icon, query }：
//   - 跳过 hidden 菜单，保证结果与侧边栏可见项一致；
//   - title 用数组保存父级链，渲染时 join(" / ") 显示层级；
//   - redirect 为 "noRedirect" 的父级只起分组作用、本身不可点击，因此不加入结果；
//   - 递归子路由时把父级 path 作为 basePath，得到可直接 router.push 的绝对路径。
// Filter out the routes that can be displayed in the sidebar
// And generate the internationalized title
function generateRoutes(routes, basePath = '', prefixTitle = []) {
  let res = []

  for (const r of routes) {
    // skip hidden router
    if (r.hidden) { continue }
    const p = r.path.length > 0 && r.path[0] === '/' ? r.path : '/' + r.path
    const data = {
      path: !isHttp(r.path) ? getNormalPath(basePath + p) : r.path,
      title: [...prefixTitle],
      icon: ''
    }

    if (r.meta && r.meta.title) {
      data.title = [...data.title, r.meta.title]
      data.icon = r.meta.icon
      if (r.redirect !== "noRedirect") {
        // only push the routes with title
        // special case: need to exclude parent router without redirect
        res.push(data)
      }
    }
    if (r.query) {
      data.query = r.query
    }

    // recursive child routes
    if (r.children) {
      const tempRoutes = generateRoutes(r.children, data.path, data.title)
      if (tempRoutes.length >= 1) {
        res = [...res, ...tempRoutes]
      }
    }
  }
  return res
}

// 输入变化即检索并复位键盘选中项；关键字为空时回退到全量候选池。
function querySearch(query) {
  activeIndex.value = -1
  if (query !== '') {
    options.value = fuse.value.search(query).map((item) => item.item) ?? searchPool.value
  } else {
    options.value = searchPool.value
  }
}

// 键盘/悬停选中的行用主题色反白，其余行不注入内联样式，避免每次移动都重建全部样式对象。
function activeStyle(index) {
  if (index !== activeIndex.value) return {}
  return {
    "background-color": theme.value,
    "color": "#fff"
  }
}

// 上下键循环移动选中项：到首/尾时回绕到另一端，符合常见搜索框操作习惯。
function navigateResult(direction) {
  if (direction === "up") {
    activeIndex.value = activeIndex.value <= 0 ? options.value.length - 1 : activeIndex.value - 1
  } else if (direction === "down") {
    activeIndex.value = activeIndex.value >= options.value.length - 1 ? 0 : activeIndex.value + 1
  }
}

// 回车只有在「有结果且已用上下键选中某一项」时才跳转，未选中时按回车不做任何事。
function selectActiveResult() {
  if (options.value.length > 0 && activeIndex.value >= 0) {
    change(options.value[activeIndex.value])
  }
}

// 候选池只在挂载时生成一次：本组件位于布局层，动态路由在渲染布局前已就绪。
// watch 负责的是「池子变化后重建 Fuse 索引」——Fuse 实例持有数据快照，不会自动跟随。
onMounted(() => {
  searchPool.value = generateRoutes(routes.value)
})

watch(searchPool, (list) => {
  initFuse(list)
})
</script>

<style lang='scss' scoped>
.header-search {
  .search-icon {
    cursor: pointer;
    font-size: 18px;
    vertical-align: middle;
  }
}

/* 固定高度让弹窗尺寸稳定，结果多时内部滚动而不是把弹窗撑长 */
.result-wrap {	
  height: 280px;
  margin: 6px 0;

  .search-item {
    display: flex;
    height: 48px;
    align-items: center;
    padding-right: 10px;

    .left {
      width: 60px;
      text-align: center;

      .menu-icon {
        width: 18px;
        height: 18px;
      }
    }

    .search-info {
      padding-left: 5px;
      margin-top: 10px;
      width: 100%;
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
      flex: 1;

      .menu-title,
      .menu-path {
        height: 20px;
      }
      .menu-path {
        color: #ccc;
        font-size: 10px;
      }
    }
  }

  .search-item:hover {
    cursor: pointer;
  }
}
</style>
