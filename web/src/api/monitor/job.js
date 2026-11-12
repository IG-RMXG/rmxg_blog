import request from '@/utils/request'

/*
 * 定时任务（sys_job）接口。
 *
 * 路径前缀是 /schedule/**：网关会先补上 system-service 的 context-path /system
 * 再转发，因此这里写成 /system/schedule/... 反而会 404。
 *
 * ★ 现状（改动前务必知悉）：系统还没有接入 Quartz 调度器（qrtz_* 表为空），所以
 *   - 增删改查、状态切换都只是改数据库，不会注册/移除触发器；
 *   - 「立即执行一次」只会写一条调度日志留痕，不会真的调用 invokeTarget。
 *
 * 字段约定：status '0'=正常 '1'=暂停（新增默认 '1'，即新建任务默认是停用状态）；
 * misfirePolicy 1立即执行 2执行一次 3放弃执行；concurrent '0'=允许 '1'=禁止；
 * jobName 只在同一个 jobGroup 内唯一（唯一性校验带 jobGroup，不同组可以重名）。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询定时任务调度列表
export function listJob(query) {
  return request({
    url: '/schedule/job/list',
    method: 'get',
    params: query
  })
}

// 查询定时任务调度详细
export function getJob(jobId) {
  return request({
    url: '/schedule/job/' + jobId,
    method: 'get'
  })
}

// 新增定时任务调度
// 同一 jobGroup 内任务名重复会返回 500；不传 status 时后端置为 '1'（暂停），需要手动启用。
export function addJob(data) {
  return request({
    url: '/schedule/job',
    method: 'post',
    data: data
  })
}

// 修改定时任务调度
export function updateJob(data) {
  return request({
    url: '/schedule/job',
    method: 'put',
    data: data
  })
}

// 删除定时任务调度
// 路径变量是 Long[]，多个 id 用逗号拼接；只删任务定义，历史调度日志仍保留在 jobLog 里。
export function delJob(jobId) {
  return request({
    url: '/schedule/job/' + jobId,
    method: 'delete'
  })
}

// 任务状态修改
// status：'0'=正常 '1'=暂停。当前仅更新数据库字段，不会真的启停调度器。
export function changeJobStatus(jobId, status) {
  const data = {
    jobId,
    status
  }
  return request({
    url: '/schedule/job/changeStatus',
    method: 'put',
    data: data
  })
}


// 定时任务立即执行一次
// 当前实现只写一条「手动触发成功」的调度日志，不会执行 invokeTarget ——
// 验收时看到日志但业务无反应属预期，不是接口失败。
// jobGroup 为空时后端回退用任务自身的 jobGroup。
export function runJob(jobId, jobGroup) {
  const data = {
    jobId,
    jobGroup
  }
  return request({
    url: '/schedule/job/run',
    method: 'put',
    data: data
  })
}