/**
 * 登录凭据（Cookie）读写工具。
 *
 * 约定：token 存放在 Cookie 的 Admin-Token 上，由登录 / UAT 假登录流程写入；
 * 请求拦截器（@/utils/request.js）读取后拼成 Bearer 头；退出登录时 token
 * 与有效期一起清理。Cookie 由前端 JS 直接读写，不防 XSS，会话安全最终依赖
 * 后端校验与 HTTPS。
 */
import Cookies from 'js-cookie'

// 两个 Cookie 键：token 用于鉴权，Expires-In 存剩余有效期（秒）供续期逻辑读取
const TokenKey = 'Admin-Token'

const ExpiresInKey = 'Admin-Expires-In'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}

// 无 Cookie 时统一返回 -1，调用方据此判定“没有有效期信息”，而不是当成 0 秒
export function getExpiresIn() {
  return Cookies.get(ExpiresInKey) || -1
}

export function setExpiresIn(time) {
  return Cookies.set(ExpiresInKey, time)
}

export function removeExpiresIn() {
  return Cookies.remove(ExpiresInKey)
}
