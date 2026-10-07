import request from '@/utils/request'

/*
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
export function refreshToken() {
  return request({
    url: '/system/auth/refresh',
    method: 'post'
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/system/user/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/system/auth/logout',
    method: 'delete'
  })
}

// 获取验证码（注意：这个由 gateway-service 自己提供，不带 /system 前缀）
export function getCaptcha() {
  return request({
    url: '/captcha',
    method: 'get'
  })
}

export function checkCaptcha(data) {
  return request({
    url: '/captcha',
    method: 'post',
    data: data
  })
}