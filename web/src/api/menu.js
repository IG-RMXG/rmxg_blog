import request from '@/utils/request'

/*
 * 动态路由数据源：登录后由 permission store 调用 getRouters() 拉取当前用户
 * 有权访问的菜单树，再在前端拼成 vue-router 路由（component 字段是相对 views 的路径）。
 * 后端按角色权限过滤（管理员走全量分支），并把 meta.title / meta.noCache / hidden 都算好；
 * visible='1' 的菜单仍会下发，只是带 hidden=true 不进侧边栏，前端不要自行再过滤一遍。
 */

// 获取路由
// 响应 data 为树形菜单数组；顶级菜单的 path 存的是不带前导 '/' 的目录名（RuoYi 惯例），
// permission store 会统一补上 '/' 再 addRoute —— 改这里前先看 store/modules/permission.js。
export const getRouters = () => {
  return request({
    url: '/system/menu/getRouters',
    method: 'get'
  })
}