import request from '@/utils/request'

/**
 * 门户（/portal）只读接口。
 *
 * 对应后端 BlogPortalController（/blog/public/**）：
 * 全部是 GET、无需登录，游客直接浏览已发布内容。
 * 评论、写作等写操作仍然调用 @/api/blog/* 里需要登录的接口。
 *
 * 返回结构分两种，取数时别混：
 *   - 详情/分类/标签/归档/热门/评论 -> ResponseVO，业务数据在 res.data；
 *   - 文章列表 -> TableDataInfo（{ total, rows }），分页表格直接用 res.rows。
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
// month 必须是 yyyy-MM（后端用 DATE_FORMAT 精确匹配发布时间）；orderBy 只认
// latest|hot|comments，其他值都落到默认分支（置顶优先 + 发布时间倒序）。
export function listPortalArticles(query) {
  return request({
    url: BASE + '/articles',
    method: 'get',
    params: query
  })
}

// 文章详情（已发布，浏览量 +1）
// 未发布或不存在的文章统一返回业务码 404，前端按 res.code 处理即可，不要依赖 msg 文案。
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
// 返回 [{ month: 'yyyy-MM', count }]，按月份倒序，前端直接用于时间轴分组。
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
