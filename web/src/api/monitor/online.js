import request from '@/utils/request'

/*
 * 在线用户监控（/system/online/**）。
 *
 * 数据源是 Redis 里的 login_tokens:*，不落库：会话过期或被删，列表立刻消失。
 * 后端先全量扫描会话再在内存里过滤/切片，所以 pageNum/pageSize 仍可用，
 * 但不存在“下一页更实时”这回事 —— 每次都是重新取快照。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），强退是 ResponseVO。
 */

// 查询在线用户列表
// 可选参数 ipaddr / userName 走的是 contains 模糊匹配（不是精确等值），
// 默认 pageNum=1、pageSize=10；每行含 tokenId / userId / userName / ipaddr / loginTime。
export function list(query) {
  return request({
    url: '/system/online/list',
    method: 'get',
    params: query
  })
}

// 强退用户
// 传的是列表里的 tokenId（会话 id），不是 userId —— 一个用户多处登录会有多个会话，
// 这里只踢掉指定的那一份。会话已不存在时后端返回 500「该会话已失效」，刷新列表即可。
export function forceLogout(tokenId) {
  return request({
    url: '/system/online/' + tokenId,
    method: 'delete'
  })
}
