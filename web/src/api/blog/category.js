import request from '@/utils/request'

/*
 * 博客分类接口（对外前缀 /blog/category，网关 /blog/** -> blog-service:8010）。
 *
 * 权限分层：列表、详情、增删改仅管理员可用（后端 isAdmin 校验，普通用户拿到业务码 403）；
 * enabled 下拉对所有登录用户开放，供文章编辑页选择分类。
 *
 * 字段约定：status '0'=启用 '1'=停用，新增不传默认 '0'；sort 越小越靠前，
 * 列表按 sort ASC、categoryId ASC 排序；categoryName 全局唯一。
 * 分类下还有文章时删除会被后端拒绝，不会级联删除文章。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询分类列表
// 管理员专用；每行带 articleCount（子查询统计），前端据此提示「有文章不能删」。
export function listCategory(query) {
  return request({
    url: '/blog/category/list',
    method: 'get',
    params: query
  })
}

// 查询分类详细
// 管理员专用；编辑弹窗回填用，返回单条记录而非数组。
export function getCategory(categoryId) {
  return request({
    url: '/blog/category/' + categoryId,
    method: 'get'
  })
}

// 启用的分类（文章编辑页下拉）
// 登录用户即可调用；只返回 status='0' 的分类，未登录时后端给空数组而不是报错。
export function listEnabledCategory() {
  return request({
    url: '/blog/category/enabled',
    method: 'get'
  })
}

// 新增分类
// 仅管理员；categoryName 重复会返回 500「分类名称已存在」，sort/status 由后端补默认值。
export function addCategory(data) {
  return request({
    url: '/blog/category',
    method: 'post',
    data: data
  })
}

// 修改分类
// 仅管理员；必须带 categoryId，重名校验会把自身排除，改回原名不会误报。
export function updateCategory(data) {
  return request({
    url: '/blog/category',
    method: 'put',
    data: data
  })
}

// 删除分类
// 仅管理员；后端先逐个校验分类下是否还有文章，任意一个有文章就整批失败；
// 路径变量是 Long[]，批量删除时用逗号拼 id。
export function delCategory(categoryId) {
  return request({
    url: '/blog/category/' + categoryId,
    method: 'delete'
  })
}
