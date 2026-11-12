import request from '@/utils/request'

/*
 * 岗位管理（sys_post）接口，前缀 /system/post。
 *
 * 字段约定：postCode 与 postName 都要求全局唯一；status '0'=正常 '1'=停用。
 * 岗位与用户是多对多（sys_user_post），已被用户引用的岗位不允许删除。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询岗位列表
// 分页，支持 postCode（模糊）、postName（模糊）、status 过滤。
export function listPost(query) {
  return request({
    url: '/system/post/list',
    method: 'get',
    params: query
  })
}

// 查询岗位详细
export function getPost(postId) {
  return request({
    url: '/system/post/' + postId,
    method: 'get'
  })
}

// 新增岗位
// postCode / postName 任一重复都会返回 500 并指明是哪个冲突。
export function addPost(data) {
  return request({
    url: '/system/post',
    method: 'post',
    data: data
  })
}

// 修改岗位
// 必带 postId；唯一性校验会排除自身，改回原编码/名称不会误报。
export function updatePost(data) {
  return request({
    url: '/system/post',
    method: 'put',
    data: data
  })
}

// 删除岗位
// 路径变量是 Long[]，多个 postId 用逗号拼接；只要其中任意一个已被用户占用，整批删除都会失败。
export function delPost(postId) {
  return request({
    url: '/system/post/' + postId,
    method: 'delete'
  })
}
