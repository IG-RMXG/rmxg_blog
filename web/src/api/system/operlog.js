import request from '@/utils/request'

/*
 * 操作日志（sys_oper_log）接口，前缀 /system/operlog。
 *
 * 查询条件：title / operName（LIKE 模糊）、businessType、status，
 * 时间范围必须放 params[beginTime] / params[endTime]（RuoYi 约定），平级字段无效。
 * 字段约定：businessType 0其它 1新增 2修改 3删除（4 授权、5 导出等沿用 RuoYi 原约定）；
 * status '0'=正常 '1'=异常，异常时 errorMsg 才有内容。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询操作日志列表
// 按 operId 倒序；日志表增长很快，查询建议始终带上时间范围。
export function list(query) {
  return request({
    url: '/system/operlog/list',
    method: 'get',
    params: query
  })
}

// 删除操作日志
// 路径变量是 Long[]，多个 operId 用逗号拼接；物理删除，不可恢复。
export function delOperlog(operId) {
  return request({
    url: '/system/operlog/' + operId,
    method: 'delete'
  })
}

// 清空操作日志
// DELETE 语义清空整表（后端直接 DELETE FROM sys_oper_log），前端必须二次确认。
export function cleanOperlog() {
  return request({
    url: '/system/operlog/clean',
    method: 'delete'
  })
}
