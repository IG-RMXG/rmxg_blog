// 路由总表与 router 实例。
// constantRoutes 无需权限，任何访客都能访问（登录/注册/404/401/首页/个人中心/博客门户），
//                在 createRouter 时一次性注册；
// dynamicRoutes  是隐藏的二级功能页（分配角色、字典数据、调度日志、生成配置、文章编辑/详情），
//                需要 permissions 或 roles，由 permission store 过滤后再 addRoute。
// history 使用 createWebHistory，部署时必须保证所有前端路由回退到 index.html，否则刷新会 404。
import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  // 空白中转页：$tab.refreshPage 会先跳到 /redirect/<原路径> 再 replace 回来，
  // 借「重新挂载」清掉 keep-alive 缓存实现强制刷新。它不是给用户看的页面，因此 hidden。
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  // 通配兜底：所有未命中地址都落到 404。vue-router 4 按路径评分匹配、
  // 不依赖声明顺序，因此它不需要写在数组末尾。
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  // 个人中心：activeTab 是可选路径段（profile/:activeTab?），
  // 用户信息里提示「初始密码 / 密码过期」时会带 resetPwd，直接定位到重置密码 tab。
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  // ==================== 博客门户（/portal）====================
  // 面向读者的独立站点：自带 layout（没有若依的侧边栏/标签页），游客可直接浏览。
  // meta.portal 让 permission.js 走门户专用守卫分支；meta.requiresAuth 的页面
  // （写作 / 我的文章）未登录时会被引导到 /portal/login。
  {
    path: '/portal',
    component: () => import('@/portal/layout/index.vue'),
    redirect: '/portal/home',
    children: [
      {
        path: 'home',
        name: 'PortalHome',
        component: () => import('@/portal/views/home.vue'),
        meta: { title: '首页', portal: true }
      },
      {
        path: 'article/:articleId(\\d+)',
        name: 'PortalArticle',
        component: () => import('@/portal/views/article.vue'),
        meta: { title: '文章详情', portal: true }
      },
      {
        path: 'archive',
        name: 'PortalArchive',
        component: () => import('@/portal/views/archive.vue'),
        meta: { title: '归档', portal: true }
      },
      {
        path: 'write/:articleId(\\d+)?',
        name: 'PortalWrite',
        component: () => import('@/portal/views/write.vue'),
        meta: { title: '写文章', portal: true, requiresAuth: true }
      },
      {
        path: 'me',
        name: 'PortalMe',
        component: () => import('@/portal/views/me.vue'),
        meta: { title: '我的文章', portal: true, requiresAuth: true }
      }
    ]
  },
  {
    path: '/portal/login',
    name: 'PortalLogin',
    component: () => import('@/portal/views/login.vue'),
    meta: { title: '登录', portal: true }
  },
]

// 动态路由，基于用户权限动态去加载
// 这批都是 hidden 的二级功能页：不进菜单，只能由有权限的页面内部跳转；
// permissions 与 roles 二选一，在 filterDynamicRoutes 中 permissions 优先。
export const dynamicRoutes = [
  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  },
  {
    path: '/blog/article-edit',
    component: Layout,
    hidden: true,
    permissions: ['blog:article:list'],
    children: [
      {
        path: ':articleId?',
        component: () => import('@/views/blog/article/edit'),
        name: 'BlogArticleEdit',
        meta: { title: '文章编辑', activeMenu: '/blog/article' }
      }
    ]
  },
  {
    path: '/blog/article-detail',
    component: Layout,
    hidden: true,
    permissions: ['blog:article:list'],
    children: [
      {
        path: ':articleId',
        component: () => import('@/views/blog/article/detail'),
        name: 'BlogArticleDetail',
        meta: { title: '文章详情', activeMenu: '/blog/article' }
      }
    ]
  }
]

// 滚动行为：浏览器前进/后退时恢复离开时的位置（savedPosition），
// 其余跳转一律回到顶部，避免从长列表点进详情后仍停在页面中间。
const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

export default router
