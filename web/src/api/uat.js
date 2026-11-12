import request from '@/utils/request'

/*
 * UAT 验收专用接口（由 uat-service:8020 提供）。
 *
 * 环境约束：只有 UAT 模式（yarn dev:uat / --mode uat）下 VITE_APP_BASE_API=/uat-api 指向
 * UAT 网关 8890；DEV 网关没有 /uat/** 转发规则，请求会直接 404。
 * 两个接口都不需要登录（headers.isToken=false），因为假登录本身就是给未登录的验收人用的。
 *
 * 安全边界：拿到 token 就等于拿到对应角色的全部权限。生产构建里 utils/uat.js 的
 * isUat 恒为 false、相关入口不会被渲染，但这些接口本身只应部署在 UAT 环境。
 */

// UAT 假登录：按角色签发真实可用的 token（仅 UAT 环境，DEV 网关不路由该路径）
// role 只接受 'admin'（uat_admin）/ 'user'（uat_user），其他值后端返回 400。
// 响应 data 为 { token, expiresIn, userId, userName, nickName, roles }，有效期 30 分钟；
// 注意字段名是 token（不是登录接口的 access_token）。
export function fakeLogin(role) {
  return request({
    url: '/uat/fake-login',
    method: 'post',
    data: { role },
    headers: {
      isToken: false,
      repeatSubmit: false
    }
  })
}

// UAT 环境自检（库 / Redis / 测试账号）
// 返回 { service, db, redis, accounts }，UAT 起不来时先看这里判断是哪个依赖没就绪。
export function uatStatus() {
  return request({
    url: '/uat/status',
    method: 'get',
    headers: { isToken: false }
  })
}
