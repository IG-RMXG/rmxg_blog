<template>
  <div class="portal-login">
    <!-- 左侧品牌区（窄屏自动隐藏） -->
    <section class="portal-login__brand">
      <div class="portal-login__brand-inner">
        <div class="portal-login__logo">博</div>
        <h1>{{ site.title || '我的博客' }}</h1>
        <p>{{ site.subtitle || '记录、思考与分享' }}</p>
        <div class="portal-login__brand-stats">
          <div>
            <strong>{{ site.articleCount || 0 }}</strong>
            <span>篇文章</span>
          </div>
          <div>
            <strong>{{ site.categoryCount || 0 }}</strong>
            <span>个分类</span>
          </div>
          <div>
            <strong>{{ site.viewCount || 0 }}</strong>
            <span>次阅读</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 右侧登录表单 -->
    <section class="portal-login__panel">
      <div class="portal-login__card">
        <h2>欢迎回来</h2>
        <p>登录后即可评论、写文章、管理自己的文章</p>

        <!-- 用原生 form + submit：回车与点按钮共用同一条提交路径，@submit.prevent 阻止浏览器整页刷新 -->
        <form @submit.prevent="submit">
          <div class="portal-login__field">
            <label for="portal-username">账号</label>
            <el-input
              id="portal-username"
              v-model="form.username"
              size="large"
              placeholder="请输入用户名"
              autocomplete="username"
              clearable
            />
          </div>
          <div class="portal-login__field">
            <label for="portal-password">密码</label>
            <el-input
              id="portal-password"
              v-model="form.password"
              type="password"
              size="large"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="submit"
            />
          </div>
          <el-button class="portal-login__submit" type="primary" native-type="submit" :loading="loading">
            登 录
          </el-button>
        </form>

        <div class="portal-login__extra">
          <router-link to="/portal/home">← 返回博客</router-link>
          <router-link to="/login">管理员入口</router-link>
        </div>

        <!-- UAT 专用免密入口：isUat 是编译期常量，生产构建里恒为 false，这段 UI 不会出现 -->
        <div v-if="isUat" class="portal-login__uat">
          <p>UAT 测试环境：可跳过密码直接以指定角色进入</p>
          <div class="portal-login__uat-btns">
            <el-button size="small" type="warning" plain :loading="uatLoading === 'admin'" @click="uatEnter('admin')">
              以管理员进入
            </el-button>
            <el-button size="small" type="warning" plain :loading="uatLoading === 'user'" @click="uatEnter('user')">
              以普通用户进入
            </el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup name="PortalLogin">
/**
 * 门户登录页（/portal/login）。
 *
 * 账号密码走若依的统一登录接口，成功后补一次 getInfo 再跳 redirect 指定的页面；
 * 管理员也先落在门户而不是 /index——顶栏和页脚都留了进后台的入口，
 * 避免登录后直接闪进后台、让只想评论的读者迷路。
 * UAT 环境额外提供「以某角色进入」的免密按钮（详见 @/utils/uat）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import useUserStore from '@/store/modules/user'
import { usePortalSite } from '@/portal/composables/usePortalSite'
import { enterAs, isUat } from '@/utils/uat'
import '@/portal/styles/portal.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { site } = usePortalSite()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
// 记录正在免密进入的角色（'' 表示空闲），让被点击的那个按钮单独转圈
const uatLoading = ref('')

// 只接受门户内部地址作为回跳目标，避免 ?redirect=//evil.com 这类开放重定向
// （必须用 startsWith('/portal')：只判断 startsWith('/') 会放过协议相对地址 //evil.com）
const redirect = computed(() => {
  const target = route.query.redirect ? String(route.query.redirect) : ''
  return target.startsWith('/portal') ? target : '/portal/home'
})

// 先换 token、再取用户信息，两步必须串行：门户顶栏的昵称/角色、路由守卫都依赖 getInfo 的结果。
// 这里只做「非空」这种最低限度校验，账号是否存在、是否停用交给后端判断。
function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  userStore.login({ username: form.username, password: form.password })
    .then(() => userStore.getInfo())
    .then(() => {
      // 管理员也先落在门户：顶栏/页脚都有「进入管理后台」入口
      router.replace(redirect.value)
    })
    .catch(() => {
      // 失败原因（用户名或密码错误、账号停用等）拦截器已经弹过，这里不再重复提示
      loading.value = false
    })
}

// UAT 免密进入：enterAs 成功后会整页跳转（页面即将卸载，无需复位 uatLoading）；
// 只有失败时才会走 catch，需要在这里收尾并给用户一个可操作的提示。
function uatEnter(role) {
  uatLoading.value = role
  enterAs(role, '/portal/home').catch(e => {
    // handled=true 表示 request.js 已给出可操作提示（UAT 网关未启动等）
    if (!e || !e.handled) {
      ElMessage.error('UAT 快捷进入失败，请确认 UAT 后端已启动')
    }
    uatLoading.value = ''
  })
}

onMounted(() => {
  if (userStore.token && userStore.roles.length === 0) {
    // 已有 token（可能从若依后台过来）：补一次用户信息，登录页会由守卫重定向回门户
    userStore.getInfo().catch(() => {})
  }
})
</script>
