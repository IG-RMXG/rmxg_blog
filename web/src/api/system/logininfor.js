import request from '@/utils/request'

/*
 * 登录日志（sys_logininfor）接口，前缀 /system/logininfor。
 *
 * 查询条件：userName / ipaddr（都是 LIKE 模糊）+ status（'0'成功 '1'失败），
 * 时间范围要用 RuoYi 约定的 params[beginTime] / params[endTime]，平级的 beginTime 无效。
 * 列表按 infoId 倒序，分页参数 pageNum/pageSize 由后端 PageHelper 读取。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询登录日志列表
export function list(query) {
  return request({
    url: '/system/logininfor/list',
    method: 'get',
    params: query
  })
}

// 删除登录日志
// 路径变量是 Long[]，多个 infoId 用逗号拼接；物理删除，不可恢复。
export function delLogininfor(infoId) {
  return request({
    url: '/system/logininfor/' + infoId,
    method: 'delete'
  })
}

// 解锁用户登录状态
// 后端并没有账户锁定机制，这个接口只做「返回成功」以兼容页面按钮；
// 当前方法也不一致：前端是 GET，后端是 @PutMapping，实际调用会拿到 405。
export function unlockLogininfor(userName) {
  return request({
    url: '/system/logininfor/unlock/' + userName,
    method: 'get'
  })
}
// 清空登录日志
// DELETE 语义清空整表（后端是 DELETE FROM sys_logininfor，不可回滚），前端必须二次确认。
export function cleanLogininfor() {
  return request({
    url: '/system/logininfor/clean',
    method: 'delete'
  })
}
