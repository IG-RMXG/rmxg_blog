import request from '@/utils/request'

// 查询文章列表
export function listArticle(query) {
  return request({
    url: '/blog/article/list',
    method: 'get',
    params: query
  })
}

// 查询文章详情（编辑页）
export function getArticle(articleId) {
  return request({
    url: '/blog/article/' + articleId,
    method: 'get'
  })
}

// 浏览文章（详情页，浏览量+1）
export function viewArticle(articleId) {
  return request({
    url: '/blog/article/view/' + articleId,
    method: 'get'
  })
}

// 新增文章
export function addArticle(data) {
  return request({
    url: '/blog/article',
    method: 'post',
    data: data
  })
}

// 修改文章
export function updateArticle(data) {
  return request({
    url: '/blog/article',
    method: 'put',
    data: data
  })
}

// 删除文章
export function delArticle(articleId) {
  return request({
    url: '/blog/article/' + articleId,
    method: 'delete'
  })
}

// 文章状态流转（发布/审核/驳回/转草稿）
export function changeArticleStatus(data) {
  return request({
    url: '/blog/article/status',
    method: 'put',
    data: data
  })
}

// 上传博客图片（返回 { url }）
export function uploadBlogImage(data) {
  return request({
    url: '/blog/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data',
      repeatSubmit: false
    }
  })
}
