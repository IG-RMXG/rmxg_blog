// 动态路由 store：把后端菜单（getRouters）转换成 vue-router 路由。
// 关键点「一份数据、三份深拷贝」：filterAsyncRouter 会就地改写 path/component/children，
// 而侧边栏要保留嵌套层级、router.addRoute 需要拍平 ParentView、顶部导航又需要原始结构，
// 三者必须各自转换，否则会互相污染（典型现象：菜单多了一层或少了一级）。
// addRoute 用的是 rewriteRoutes（type=true）那一份，ParentView 的子路由会被提上来，
// 从而让 /system/user 这类地址可以直接访问，而不依赖父级的 redirect。
import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'
import { isHttp } from '@/utils/validate'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue')

const usePermissionStore = defineStore(
  'permission',
  {
    // 五个字段都是路由数组，分别给：完整路由表、本次动态添加部分、默认布局、
    // 顶部导航、侧边栏菜单使用；切换账号重新登录时应整体重建，不要增量合并。
    state: () => ({
      routes: [],
      addRoutes: [],
      defaultRoutes: [],
      topbarRouters: [],
      sidebarRouters: []
    }),
    actions: {
      setRoutes(routes) {
        this.addRoutes = routes
        this.routes = constantRoutes.concat(routes)
      },
      setDefaultRoutes(routes) {
        this.defaultRoutes = constantRoutes.concat(routes)
      },
      setTopbarRoutes(routes) {
        this.topbarRouters = routes
      },
      setSidebarRouters(routes) {
        this.sidebarRouters = routes
      },
      // 拉取后端菜单并生成可访问路由。roles 参数当前未参与过滤：
      // 菜单权限由后端 getRouters 按当前用户裁剪，前端只做格式转换。
      // 这里只负责 generateRoutes 的返回值，addRoute 由 permission.js 守卫执行。
      generateRoutes(roles) {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(res => {
            const sdata = JSON.parse(JSON.stringify(res.data))
            const rdata = JSON.parse(JSON.stringify(res.data))
            const defaultData = JSON.parse(JSON.stringify(res.data))
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            const defaultRoutes = filterAsyncRouter(defaultData)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            asyncRoutes.forEach(route => { router.addRoute(route) })
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(constantRoutes.concat(sidebarRoutes))
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(defaultRoutes)
            resolve(rewriteRoutes)
          })
        })
      }
    }
  })

// 遍历后台传来的路由字符串，转换为组件对象
// 参数约定：lastRouter 为父路由（用来判断是否顶级、以及拼接子路径）；
// type=true 表示这份结果要交给 router.addRoute，需要把 ParentView 的子路由拍平到同一层；
// component 是后端下发的字符串，Layout/ParentView/InnerLink 换成真实组件，其余交给 loadView 懒加载。
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    // 后台顶级菜单的 path 存的是 'system' / 'monitor' / 'tool'（不带前导斜杠，
    // 这是 RuoYi 的标准存法）。但这些会被 router.addRoute() 注册为顶级路由，
    // 而 vue-router 4 要求顶级 path 必须以 '/' 开头，否则抛出
    //   Route paths should start with a "/": "system" should be "/system"
    // 导致路由注册中断、登录后无法跳转。这里统一补上前导斜杠。
    // 子路由保持相对路径（由 filterChildren 拼接），不做处理。
    if (!lastRouter && route.path && !route.path.startsWith('/') && !isHttp(route.path)) {
      route.path = '/' + route.path
    }
    // 只有交给 addRoute 的那份需要拍平：ParentView 本身只是个中转组件，
    // 把它的子路由提上一层并拼出完整 path，才能直接用地址访问这些页面。
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    // 叶子路由必须删掉空的 children / redirect：留着会被注册成父路由，
    // 却没有默认子路由可匹配，访问时直接空白或报「找不到子路由」。
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

// 拼接 ParentView 子路由的完整路径（父 path + '/' + 子 path）。
// 结果直接交给 router.addRoute，所以必须是能被 vue-router 解析的完整地址；
// lastRouter 在递归时传入当前节点，用于多层 ParentView 的逐级拼接。
function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 动态路由遍历，验证是否具备权限
// 有 permissions 就只按权限码判断（忽略 roles），没有 permissions 时才看 roles；
// 两者都没声明的路由不会被返回，这类页面必须写进 constantRoutes 才能访问。
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

// 按 views 下的相对路径（不含 .vue）反查已登记的模块，返回懒加载函数给 vue-router。
// 找不到时返回 undefined：路由仍会注册，但组件为空，表现为白屏 + 控制台警告，
// 通常意味着后端下发的 component 字符串与文件路径不一致。
export const loadView = (view) => {
  let res
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0]
    if (dir === view) {
      res = () => modules[path]()
    }
  }
  return res
}

export default usePermissionStore
