/**
 * UAT 环境标识与假登录入口。
 *
 * isUat 是编译期常量：Vite 构建时会把 import.meta.env.VITE_APP_ENV 静态替换，
 * 生产构建（build:prod）中它恒为 false，所有 UAT 专属 UI 会被 tree-shaking
 * 掉引用路径（组件仍以懒加载 chunk 存在，但永远不会被注册/渲染）。
 */
import { setToken, removeToken } from '@/utils/auth'
import { fakeLogin } from '@/api/uat'

export const isUat = import.meta.env.VITE_APP_ENV === 'uat'

/**
 * 以指定角色进入系统（未登录时用）或切换身份（已登录时用）。
 *
 * 实现：调 /uat/fake-login 拿新 token 替换本地会话，
 * 然后整页跳转——让 permission 守卫用新 token 重新走 getInfo + generateRoutes，
 * 菜单/权限随角色整体重建，比局部刷新状态可靠得多。
 *
 * @param {'admin'|'user'} role 目标角色
 * @param {string} target 登录后落地页：若依后台（默认 /index）或门户（/portal/home）
 */
export function enterAs(role, target = '/index') {
  // 先清旧 token，避免上一个身份的会话残留，再写入假登录返回的新 token
  removeToken()
  return fakeLogin(role).then(res => {
    const data = res.data
    if (!data || !data.token) {
      throw new Error('假登录返回异常')
    }
    setToken(data.token)
    // 用 origin 拼绝对地址做整页跳转，子路径部署时才不会被当成前端路由
    window.location.href = window.location.origin + (target || '/index')
    return data
  })
}
