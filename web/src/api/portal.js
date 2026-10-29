import request from '@/utils/request'

/**
 * 门户（/portal）只读接口。
 *
 * 对应后端 BlogPortalController（/blog/public/**）：
 * 全部是 GET、无需登录，游客直接浏览已发布内容。
 * 评论、写作等写操作仍然调用 @/api/blog/* 里需要登录的接口。
 */
const BASE = '/blog/public'

// 站点信息：标题、副标题、博主、统计
export function getPortalSite() {
  return request({
    url: BASE + '/site',
    method: 'get'
  })
}

// 文章列表（分页）：categoryId / tagId / keyword / month / orderBy / pageNum / pageSize
export function listPortalArticles(query) {
  return request({
    url: BASE + '/articles',
    method: 'get',
    params: query
  })
}

// 文章详情（已发布，浏览量 +1）
export function getPortalArticle(articleId) {
  return request({
    url: BASE + '/articles/' + articleId,
    method: 'get'
  })
}

// 文章评论（正常状态，按楼层正序）
export function listPortalComments(articleId) {
  return request({
    url: BASE + '/articles/' + articleId + '/comments',
    method: 'get'
  })
}

// 有已发布文章的分类
export function listPortalCategories() {
  return request({
    url: BASE + '/categories',
    method: 'get'
  })
}

// 有已发布文章的标签
export function listPortalTags() {
  return request({
    url: BASE + '/tags',
    method: 'get'
  })
}

// 归档（按月统计）
export function listPortalArchive() {
  return request({
    url: BASE + '/archive',
    method: 'get'
  })
}

// 热门文章
export function listPortalHot(limit = 5) {
  return request({
    url: BASE + '/hot',
    method: 'get',
    params: { limit }
  })
}

// 最新评论（侧栏）
export function listPortalLatestComments(limit = 5) {
  return request({
    url: BASE + '/comments/latest',
    method: 'get',
    params: { limit }
  })
}
