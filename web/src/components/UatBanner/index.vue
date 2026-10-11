<template>
  <div ref="bannerRef" class="uat-banner">
    <div class="uat-banner-left">
      <span class="uat-badge">UAT</span>
      <span>验收环境 · ry-cloud-uat 库 · 与 DEV 完全隔离</span>
    </div>
    <div class="uat-banner-right">
      <span class="uat-current" v-if="userStore.name">当前：{{ userStore.nickName || userStore.name }}（{{ currentRole }}）</span>
      <el-button size="small" type="warning" plain :loading="switching === 'admin'" @click="switchTo('admin')">
        切换为管理员
      </el-button>
      <el-button size="small" type="warning" plain :loading="switching === 'user'" @click="switchTo('user')">
        切换为普通用户
      </el-button>
    </div>
  </div>
</template>

<script setup>
// UAT 专属组件：仅 import.meta.env.VITE_APP_ENV === 'uat' 时才被渲染
// （见 Navbar.vue 里的 v-if="isUat"，生产构建恒不渲染）
import useUserStore from '@/store/modules/user'
import { enterAs } from '@/utils/uat'

const userStore = useUserStore()
const switching = ref('')
const bannerRef = ref()

const currentRole = computed(() => {
  const roles = userStore.roles || []
  return roles.includes('admin') ? '管理员' : (roles.length ? roles.join(',') : '未登录')
})

function switchTo(role) {
  switching.value = role
  enterAs(role).catch(() => {
    switching.value = ''
  })
  // 成功时页面整页跳转，无需复位 loading
}

/*
 * 把横幅真实高度发布成根元素 CSS 变量 --uat-banner-h，
 * 供 AppMain.vue 把内容区顶部下移同样的高度（否则固定头部会盖住页面顶部）。
 * 用 ResizeObserver 而不是写死像素：窗口变窄时横幅会换行变高，写死就失准了。
 * 卸载时清掉变量，避免影响后续页面（DEV 下本组件根本不渲染，此逻辑不执行）。
 */
let observer = null
onMounted(() => {
  const root = document.documentElement
  const publish = () => {
    const h = bannerRef.value ? bannerRef.value.offsetHeight : 0
    root.style.setProperty('--uat-banner-h', h + 'px')
  }
  publish()
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(publish)
    observer.observe(bannerRef.value)
  } else {
    window.addEventListener('resize', publish)
  }
})
onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  document.documentElement.style.removeProperty('--uat-banner-h')
})
</script>

<style scoped>
.uat-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 16px;
  background: #fdf6ec;
  border-bottom: 1px solid #f3d19e;
  color: #b88230;
  font-size: 13px;
  flex-wrap: wrap;
}
.uat-banner-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.uat-badge {
  background: #e6a23c;
  color: #fff;
  border-radius: 3px;
  padding: 1px 6px;
  font-weight: bold;
  font-size: 12px;
  letter-spacing: 1px;
}
.uat-banner-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.uat-current {
  color: #909399;
}
</style>
