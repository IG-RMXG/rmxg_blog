<template>
  <div class="portal-root">
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

        <nav class="p-nav">
          <router-link to="/portal/home" class="p-nav__item" :class="{ active: isHome }">首页</router-link>
          <router-link to="/portal/archive" class="p-nav__item" :class="{ active: isArchive }">归档</router-link>
          <a v-if="isAdmin" href="/index" class="p-nav__item">管理后台</a>
        </nav>

        <div class="p-header__actions">
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

    <main class="portal-main">
      <router-view />
    </main>

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

const keyword = ref('')
const year = new Date().getFullYear()

const isHome = computed(() => route.path === '/portal/home')
const isArchive = computed(() => route.path.startsWith('/portal/archive'))
const logged = computed(() => !!userStore.token)
const isAdmin = computed(() => (userStore.roles || []).includes('admin'))
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

function doSearch() {
  const kw = keyword.value.trim()
  router.push({ path: '/portal/home', query: kw ? { keyword: kw } : {} })
}

function goWrite() {
  router.push('/portal/write')
}

function goLogin() {
  router.push({ path: '/portal/login', query: { redirect: route.fullPath } })
}

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
