import request from '@/utils/request'

/*
 * 博客标签接口（对外前缀 /blog/tag，网关 /blog/** -> blog-service:8010）。
 *
 * 权限分层：列表、详情、增删改仅管理员可用；all 下拉对所有登录用户开放
 * （文章编辑页多选标签用，未登录时后端返回空数组）。
 * tagName 全局唯一（更新时按 tagId 排除自身）；sort 越小越靠前。
 *
 * 删除标签会先清掉 blog_article_tag 关联再删标签本身（同一事务），文章不受影响、
 * 只是不再带这个标签。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询标签列表
// 管理员专用；非管理员后端返回空表（不是 403），前端页面对普通用户不应展示入口。
export function listTag(query) {
  return request({
    url: '/blog/tag/list',
    method: 'get',
    params: query
  })
}

// 查询标签详细
// 管理员专用；普通用户调用会拿到业务码 403「仅管理员可查看标签详情」。
export function getTag(tagId) {
  return request({
    url: '/blog/tag/' + tagId,
    method: 'get'
  })
}

// 全部标签（文章编辑页下拉）
// 登录用户即可调用；不分页，只返回 tagId / tagName / sort，不带文章计数。
export function listAllTag() {
  return request({
    url: '/blog/tag/all',
    method: 'get'
  })
}

// 新增标签
// 仅管理员；tagName 重复返回 500「标签名称已存在」，sort 不传默认 0。
export function addTag(data) {
  return request({
    url: '/blog/tag',
    method: 'post',
    data: data
  })
}

// 修改标签
// 仅管理员；必须带 tagId，重名校验按 tagId 排除自身。
export function updateTag(data) {
  return request({
    url: '/blog/tag',
    method: 'put',
    data: data
  })
}

// 删除标签
// 仅管理员；后端在同一事务里先删文章-标签关联再删标签，不会出现文章挂空标签；
// 路径变量是 Long[]，批量删除时用逗号拼 id。
export function delTag(tagId) {
  return request({
    url: '/blog/tag/' + tagId,
    method: 'delete'
  })
}
