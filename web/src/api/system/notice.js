import request from '@/utils/request'

/*
 * 通知公告（sys_notice）接口，前缀 /system/notice。
 *
 * 字段约定：noticeType '1'=通知 '2'=公告；status '0'=正常 '1'=关闭（新增默认 '0'）。
 * noticeContent 在库里是 longblob，但接口里仍是普通字符串，富文本 HTML 原样往返。
 *
 * 后端另有 GET /system/notice/listTop（最近 5 条正常公告，首页用），本文件没有封装。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询公告列表
// 分页，支持 noticeTitle 模糊与 noticeType / status 精确过滤。
export function listNotice(query) {
  return request({
    url: '/system/notice/list',
    method: 'get',
    params: query
  })
}

// 查询公告详细
export function getNotice(noticeId) {
  return request({
    url: '/system/notice/' + noticeId,
    method: 'get'
  })
}

// 新增公告
// status 不传时后端补 '0'（正常）；公告列表按 noticeId 倒序，新公告自然排在最前。
export function addNotice(data) {
  return request({
    url: '/system/notice',
    method: 'post',
    data: data
  })
}

// 修改公告
// 必带 noticeId；把 status 改成 '1' 相当于下架，首页 listTop 立刻查不到它。
export function updateNotice(data) {
  return request({
    url: '/system/notice',
    method: 'put',
    data: data
  })
}

// 删除公告
// 路径变量是 Long[]，多个 noticeId 用逗号拼接；物理删除，没有回收站。
export function delNotice(noticeId) {
  return request({
    url: '/system/notice/' + noticeId,
    method: 'delete'
  })
}