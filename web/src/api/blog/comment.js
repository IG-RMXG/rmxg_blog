import request from '@/utils/request'

// 评论管理列表
export function listComment(query) {
  return request({
    url: '/blog/comment/list',
    method: 'get',
    params: query
  })
}

// 某篇文章的评论（详情页）
export function listArticleComments(articleId) {
  return request({
    url: '/blog/comment/article/' + articleId,
    method: 'get'
  })
}

// 发表评论
export function addComment(data) {
  return request({
    url: '/blog/comment',
    method: 'post',
    data: data
  })
}

// 审核评论
export function changeCommentStatus(data) {
  return request({
    url: '/blog/comment/status',
    method: 'put',
    data: data
  })
}

// 删除评论
export function delComment(commentId) {
  return request({
    url: '/blog/comment/' + commentId,
    method: 'delete'
  })
}
