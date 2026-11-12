import request from '@/utils/request'

/*
 * 博客评论接口（对外前缀 /blog/comment，网关 /blog/** -> blog-service:8010）。
 *
 * 状态约定 status：'0'=正常（展示）'1'=隐藏（仅管理员可见，前台不展示）。
 * 新评论默认 0；审核只在 0/1 之间切换，删除是物理删除。
 *
 * 权限：发表评论所有登录用户可用；管理列表、审核、删除仅管理员。
 * 计数联动：新增评论会让文章 comment_count +1，删除会让它 -1（均在事务内）。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 评论管理列表
// 管理员专用；可按文章、状态、关键字筛选，分页参数 pageNum/pageSize 由后端 PageHelper 读取。
export function listComment(query) {
  return request({
    url: '/blog/comment/list',
    method: 'get',
    params: query
  })
}

// 某篇文章的评论（详情页）
// 登录用户均可调用；后端强制只查 status='0'，所以隐藏评论不会漏到前台。
export function listArticleComments(articleId) {
  return request({
    url: '/blog/comment/article/' + articleId,
    method: 'get'
  })
}

// 发表评论
// data 需含 articleId 与 content；parentId 不传则按 0（顶级楼层）处理，
// userId 由后端从 token 取，前端传了也会被覆盖，不要试图伪造作者。
export function addComment(data) {
  return request({
    url: '/blog/comment',
    method: 'post',
    data: data
  })
}

// 审核评论
// 仅管理员；入参 { commentId, status }，status 只应是 '0'（放行）或 '1'（隐藏）。
export function changeCommentStatus(data) {
  return request({
    url: '/blog/comment/status',
    method: 'put',
    data: data
  })
}

// 删除评论
// 仅管理员；物理删除并逐条把对应文章 comment_count -1，耗时随条数线性增长，
// 路径变量是 Long[]，多个 id 用逗号拼接。
export function delComment(commentId) {
  return request({
    url: '/blog/comment/' + commentId,
    method: 'delete'
  })
}
