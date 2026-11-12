import request from '@/utils/request'

/*
 * 定时任务调度日志（sys_job_log）接口，前缀 /schedule/** 由网关补 /system 转发。
 *
 * 日志字段约定：status '0'=正常 '1'=失败；失败时 exceptionInfo 才有值。
 * 列表按 jobLogId 倒序（最新的在前），分页参数由后端 PageHelper 读取。
 * 注意当前「立即执行一次」也会写一条成功日志，但任务并未真正运行。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询调度日志列表
export function listJobLog(query) {
  return request({
    url: '/schedule/job/log/list',
    method: 'get',
    params: query
  })
}

// 删除调度日志
// 路径变量是 Long[]，多个 jobLogId 用逗号拼接；物理删除，不可恢复。
export function delJobLog(jobLogId) {
  return request({
    url: '/schedule/job/log/' + jobLogId,
    method: 'delete'
  })
}

// 清空调度日志
// 用 DELETE 语义清空整表，前端必须先二次确认；调用后无法按任务恢复历史记录。
export function cleanJobLog() {
  return request({
    url: '/schedule/job/log/clean',
    method: 'delete'
  })
}
