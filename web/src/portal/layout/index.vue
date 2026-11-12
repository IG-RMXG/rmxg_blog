<template>
  <div class="portal-root">
    <!-- 门户整体布局：顶栏（品牌/导航/搜索/用户区）+ 内容出口 + 页脚，所有 /portal 子页面共用这一层外壳 -->
    <header class="p-header">
      <div class="portal-container p-header__inner">
        <router-link to="/portal/home" class="p-brand">
          <span class="p-brand__logo">博</span>
          <span class="p-brand__text">
            <strong>{{ site.title || '我的博客' }}</strong>
            <em>{{ site.subtitle || '记录、思考与分享' }}</em>
          </span>
          <span v-if="isUat" class="p-brand__uat">UAT</span>
        </router-link>

        <!-- 主导航高亮只由当前路径决定；「管理后台」按角色显示，且必须是整页跳转（后台是另一套 SPA 入口） -->
        <nav class="p-nav">
          <router-link to="/portal/home" class="p-nav__item" :class="{ active: isHome }">首页</router-link>
          <router-link to="/portal/archive" class="p-nav__item" :class="{ active: isArchive }">归档</router-link>
          <a v-if="isAdmin" href="/index" class="p-nav__item">管理后台</a>
        </nav>

        <div class="p-header__actions">
          <!-- 顶栏搜索：回车即跳首页并带上 keyword，与首页自己的搜索框共用同一个 URL 参数 -->
          <div class="p-search">
            <el-input
              v-model="keyword"
              placeholder="搜索文章…"
              clearable
              @keyup.enter="doSearch"
            >
              <template #prefix>
                <el-icon><search /></el-icon>
              </template>
            </el-input>
          </div>

          <el-button type="primary" round @click="goWrite">写文章</el-button>

          <!-- 用户区只对已登录访客展示，菜单项按角色裁剪（非 admin 看不到后台入口） -->
          <el-dropdown v-if="logged" trigger="click" @command="onCommand">
            <span class="p-user">
              <el-avatar :size="30" :src="userStore.avatar || undefined">
                {{ (userStore.nickName || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="p-user__name">{{ userStore.nickName || userStore.name || '用户' }}</span>
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="me">我的文章</el-dropdown-item>
                <el-dropdown-item command="write">写文章</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin" divided>进入管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button v-else text class="p-login-btn" @click="goLogin">登录</el-button>
        </div>
      </div>
    </header>

    <!-- 内容出口：切换路由时顶栏与页脚保持挂载，加载状态由各页面自己管理 -->
    <main class="portal-main">
      <router-view />
    </main>

    <!-- 页脚：站点导航 + 版权年份 + 博主署名；博主昵称取不到时整行不渲染 -->
    <footer class="p-footer">
      <div class="portal-container">
        <div class="p-footer__links">
          <router-link to="/portal/home">首页</router-link>
          <router-link to="/portal/archive">归档</router-link>
          <router-link v-if="logged" to="/portal/me">我的文章</router-link>
          <a href="/login">管理员登录</a>
        </div>
        <p>© {{ year }} {{ site.title || '我的博客' }} · Powered by Vue 3 &amp; Spring Boot</p>
        <p v-if="authorName" class="p-footer__author">博主：{{ authorName }}</p>
      </div>
    </footer>
  </div>
</template>

<script setup name="PortalLayout">
/**
 * 门户公共布局（/portal 的父路由组件）。
 *
 * 职责：顶栏（品牌、导航高亮、站内搜索、登录态入口）+ <router-view /> 内容出口 + 页脚；
 * 门户样式由布局和登录页（独立路由，不在本布局内）各自引入，后台页面不会加载 p-* 这套类名。
 * 登录态统一以 Pinia 中的 userStore.token 为准，角色判断用 roles 里的 'admin'（若依角色标识）。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import useUserStore from '@/store/modules/user'
import { isUat } from '@/utils/uat'
import { usePortalSite } from '@/portal/composables/usePortalSite'
import '@/portal/styles/portal.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { site } = usePortalSite()

// 顶栏搜索框的输入缓冲；真正生效的搜索词在 URL 的 keyword 上（见下面的 watch）
const keyword = ref('')
// 版权年份只在布局初始化时取一次：布局常驻不重建，跨年不刷新页面不会自动变化
const year = new Date().getFullYear()

// 导航高亮只看路径、不看查询参数，所以首页换筛选条件、归档切月份都不会引起顶栏闪动
const isHome = computed(() => route.path === '/portal/home')
const isArchive = computed(() => route.path.startsWith('/portal/archive'))
// 有 token 即视为已登录（token 存在 cookie 里，刷新后仍在），不依赖昵称/角色是否已加载
const logged = computed(() => !!userStore.token)
// 后台入口只认 'admin' 角色；门户不做更细的权限判断，具体接口仍由后端再校验
const isAdmin = computed(() => (userStore.roles || []).includes('admin'))
// 站点信息还在请求中时 site.value 是空对象，此时页脚署名整行不渲染
const authorName = computed(() => (site.value.author && site.value.author.nickName) || '')

// 在首页时让顶栏搜索框回显 URL 里的关键词（从浏览器前进/后退回来也能对上）
watch(
  () => route.query.keyword,
  val => {
    if (isHome.value) {
      keyword.value = val ? String(val) : ''
    }
  },
  { immediate: true }
)

// 搜索统一落到首页：门户只有首页承载文章列表，从任何子页面搜索都跳 /portal/home
function doSearch() {
  const kw = keyword.value.trim()
  router.push({ path: '/portal/home', query: kw ? { keyword: kw } : {} })
}

// 这里不判断登录态：写文章路由带 requiresAuth，由统一的路由守卫拦截，避免两处规则不一致
function goWrite() {
  router.push('/portal/write')
}

// 带上 redirect=当前完整路径，登录成功后由登录页跳回游客原本想看的页面
function goLogin() {
  router.push({ path: '/portal/login', query: { redirect: route.fullPath } })
}

// 用户下拉的 command 分发；后台入口用 window.location 整页跳转，不能交给 vue-router
function onCommand(command) {
  if (command === 'me') {
    router.push('/portal/me')
  } else if (command === 'write') {
    router.push('/portal/write')
  } else if (command === 'admin') {
    window.location.href = '/index'
  } else if (command === 'logout') {
    doLogout()
  }
}

// 退出登录：成功后按「当前页是否需要登录」分流——需要登录的页面回首页，
// 其余页面整页刷新，清掉各视图里缓存的作者相关内容（例如「我的文章」列表）。
function doLogout() {
  userStore.logOut().then(() => {
    ElMessage.success('已退出登录')
    if (route.meta.requiresAuth) {
      router.replace('/portal/home')
    } else {
      router.go(0)
    }
  }).catch(() => {
    // 后端 token 可能已过期：本地清掉即可
    userStore.$reset()
    router.replace('/portal/home')
  })
}

onMounted(() => {
  // 已登录但刷新过页面时，store 里只有 token、没有昵称/角色，补一次（守卫也会做，这里兜底）
  if (userStore.token && userStore.roles.length === 0) {
    userStore.getInfo().catch(() => {})
  }
})
</script>

<style scoped>
.p-login-btn {
  font-size: 14px;
  color: var(--p-text-2);
}
</style>
