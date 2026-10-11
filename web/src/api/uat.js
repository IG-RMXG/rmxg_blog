import request from '@/utils/request'

// UAT 假登录：按角色签发真实可用的 token（仅 UAT 环境，DEV 网关不路由该路径）
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
export function uatStatus() {
  return request({
    url: '/uat/status',
    method: 'get',
    headers: { isToken: false }
  })
}
