import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, removeToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

// 免登录白名单。
// 注意：不要把 '/index' 放进来。/index 是 Layout 的子路由，属于登录后可见的页面；
// 一旦放行，守卫在有 token 时也会直接 next()，从不调用 generateRoutes()，
// 于是 sidebarRouters 始终为空 —— 表现就是"页面能开但左侧没有任何菜单"。
// 未登录访问 /index 会被下面的 else 分支重定向到 /login?redirect=/index，登录后再跳回。
const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

// ==================== 博客门户（/portal）====================
// 门户是「独立站点」形态，和若依后台共用同一个前端、同一个 token，但：
//   - 公开页（首页/详情/归档）游客直接看，不登录
//   - 需要登录的页（写作/我的文章）由 meta.requiresAuth 标记，未登录跳 /portal/login
//   - 登录后只调 getInfo() 拿昵称和角色，**不调 generateRoutes()**：
//     门户不使用若依的动态菜单，多拉一次菜单既慢又可能污染后台的 sidebarRouters
const isPortalPath = (path) => path === '/portal' || path.startsWith('/portal/')

router.beforeEach((to, from, next) => {
  NProgress.start()

  if (isPortalPath(to.path)) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    const userStore = useUserStore()
    if (!getToken()) {
      // 游客：公开页放行；受保护页去登录并带回跳地址
      userStore.roles = []
      if (to.meta.requiresAuth) {
        next(`/portal/login?redirect=${encodeURIComponent(to.fullPath)}`)
        NProgress.done()
        return
      }
      next()
      return
    }
    // 已登录还点登录页 → 回门户首页
    if (to.path === '/portal/login') {
      next('/portal/home')
      NProgress.done()
      return
    }
    if (userStore.roles.length === 0) {
      userStore.getInfo().then(() => {
        next({ ...to, replace: true })
      }).catch(() => {
        // token 失效：本地清理后按游客处理。
        // 这里不再调 logOut()——token 已经无效，再请求一次只会又吃一个 401。
        removeToken()
        userStore.$reset()
        if (to.meta.requiresAuth) {
          next(`/portal/login?redirect=${encodeURIComponent(to.fullPath)}`)
        } else {
          next({ ...to, replace: true })
        }
        NProgress.done()
      })
      return
    }
    next()
    return
  }

  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else {
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        useUserStore().getInfo().then(() => {
          isRelogin.show = false
          usePermissionStore().generateRoutes().then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            accessRoutes.forEach(route => {
              if (!isHttp(route.path)) {
                router.addRoute(route) // 动态添加可访问路由表
              }
            })
            next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
          })
        }).catch(err => {
          useUserStore().logOut().then(() => {
            ElMessage.error(err)
            next({ path: '/' })
          })
        })
      } else {
        next()
      }
    }
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
