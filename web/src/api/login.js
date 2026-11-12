import request from '@/utils/request'

/*
 * 认证模块：登录、注册、刷新令牌、获取当前用户信息、登出、滑块验证码。
 * 这里只声明 URL 与请求方法，账号校验、令牌签发都在 system-service 的 AuthController 里；
 * 是否携带 Bearer token 由 request 拦截器读 headers.isToken 决定（false = 匿名请求）。
 *
 * 返回结构不统一（调用方容易踩到）：
 *   - login() 拿到的是 ResponseVO，token 在 res.data.access_token（另含 token_type、expires_in）；
 *   - getInfo() 的响应体本身就是 { user, roles, permissions, ... }，没有 data 包装层。
 *
 * 路径前缀说明（容易踩坑）：
 *   vite 代理 /dev-api -> 网关 8889（rewrite 去掉 /dev-api）
 *   网关按首段路由：/system/** 与 /auth/** 都转发给 system-service(8000)，
 *   system-service 的 context-path 是 /system，所以必须以 /system 开头。
 *
 * 本文件里 getInfo/logout 本来就是 '/system/user/...'，但 login/logout 曾经写成
 * '/auth/login'，少了 /system 段 —— 网关匹配不到转发规则，Tomcat 直接 404。
 * 新增接口请统一带 /system 前缀。
 */

// 登录方法
// isToken: false —— 登录时还不能带旧 token；repeatSubmit: false —— 不参与 1 秒内同参
// 防重复校验，否则用户密码输错后立刻重试会被前端自己拦成「数据正在处理」。
// code/uuid 是滑块验证码校验通过后拿到的凭证，两者一起交给后端二次核对。
export function login(username, password, code, uuid) {
  return request({
    url: '/system/auth/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: { username, password, code, uuid }
  })
}

// 注册方法
// 游客可调用（isToken: false）；账号唯一性、密码强度等规则都在后端校验。
export function register(data) {
  return request({
    url: '/system/auth/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 刷新方法
// 注意：system-service 的 /auth/refresh 目前直接返回 501「暂未实现」，
// 且本函数暂无调用方 —— 令牌过期实际走响应拦截器的 401 -> 重新登录流程。
export function refreshToken() {
  return request({
    url: '/system/auth/refresh',
    method: 'post'
  })
}

// 获取用户详细信息
// 响应体直接是 { user, roles, permissions, isDefaultModifyPwd, isPasswordExpired }，
// 没有统一的 data 包装，所以 store/modules/user.js 里取的是 res.user 而非 res.data.user。
export function getInfo() {
  return request({
    url: '/system/user/getInfo',
    method: 'get'
  })
}

// 退出方法
// 后端用 DELETE /system/auth/logout 注销服务端会话（token 在 Redis 里的记录）。
// 前端本地 token 由 store 的 logOut() 在请求成功后才清除，所以此接口失败会挡住退出跳转。
export function logout() {
  return request({
    url: '/system/auth/logout',
    method: 'delete'
  })
}

// 获取验证码（注意：这个由 gateway-service 自己提供，不带 /system 前缀）
// 返回 { originalImageBase64, jigsawImageBase64, jigsawY, token }；
// token 是本次验证码的唯一标识，校验时必须原样回传。
export function getCaptcha() {
  return request({
    url: '/captcha',
    method: 'get'
  })
}

// 校验滑块验证码：与 GET 同一路径，body 为 { token, x }，
// 由 gateway 按容差比对滑块横向坐标；仅当返回 200 才允许继续登录。
export function checkCaptcha(data) {
  return request({
    url: '/captcha',
    method: 'post',
    data: data
  })
}