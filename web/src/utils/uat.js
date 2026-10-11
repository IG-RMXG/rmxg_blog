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
 */
export function enterAs(role) {
  removeToken()
  return fakeLogin(role).then(res => {
    const data = res.data
    if (!data || !data.token) {
      throw new Error('假登录返回异常')
    }
    setToken(data.token)
    window.location.href = window.location.origin + '/index'
    return data
  })
}
