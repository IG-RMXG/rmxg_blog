import request from '@/utils/request'

/*
 * 博客文章接口（对外前缀 /blog/article，网关按 /blog/** 转发到 blog-service:8010）。
 *
 * 鉴权与分权（后端 BlogArticleService 里实现，前端只负责展示差异）：
 *   - 所有接口都要登录：列表未登录时返回空表，其余接口返回业务码 401；
 *     token 与后台共用（Redis 里的 login_tokens:<uuid>）；
 *   - 管理员：可见/可改全部文章，可发布、审核、驳回、置顶、推荐；
 *   - 普通用户：只能操作自己的文章，只能存草稿或提交审核，不能直接发布，
 *     也不能改 isTop / isRecommend（后端会保留原值）。
 *
 * 状态机 status：0=草稿 1=已发布 2=待审核 3=已驳回。
 * 转成已发布时后端写 publishTime，其它状态会清空它；普通用户提交 1/3 会被降级为 2。
 *
 * 返回结构：单条/写操作是 ResponseVO（数据在 res.data）；列表是 TableDataInfo（res.rows / res.total）。
 */

// 查询文章列表
// 未登录时后端返回空表而不是 401（前端按登录态隐藏入口即可），
// 查询条件直接透传 BlogArticle 字段 + RuoYi 的分页参数 pageNum/pageSize。
export function listArticle(query) {
  return request({
    url: '/blog/article/list',
    method: 'get',
    params: query
  })
}

// 查询文章详情（编辑页）
// 纯读取，不增加浏览量；普通用户读他人未发布文章时后端返回业务码 403「文章不存在或无权查看」。
export function getArticle(articleId) {
  return request({
    url: '/blog/article/' + articleId,
    method: 'get'
  })
}

// 浏览文章（详情页，浏览量+1）
// 与 getArticle 的唯一区别是会累加 view_count，且只对已发布文章计数（草稿/待审核不加）。
export function viewArticle(articleId) {
  return request({
    url: '/blog/article/view/' + articleId,
    method: 'get'
  })
}

// 新增文章
// data 除文章字段外还要带 status（0/2）与 tagIds（数组，后端在事务里重建关联表）；
// 想在保存时就发布只有管理员能生效，普通用户传 1 会被后端改成 2。
export function addArticle(data) {
  return request({
    url: '/blog/article',
    method: 'post',
    data: data
  })
}

// 修改文章
// 必须带 articleId（缺失时后端直接报 500「文章ID不能为空」），tagIds 会整体覆盖旧关联。
export function updateArticle(data) {
  return request({
    url: '/blog/article',
    method: 'put',
    data: data
  })
}

// 删除文章
// 后端签名是 @DeleteMapping("/{articleIds}") + Long[]，路径里用逗号拼多个 id 即可批量删除；
// 普通用户删到别人的文章会整批失败（先校验再删）。
export function delArticle(articleId) {
  return request({
    url: '/blog/article/' + articleId,
    method: 'delete'
  })
}

// 文章状态流转（发布/审核/驳回/转草稿）
// 入参 { articleId, status }：0=草稿 1=已发布 2=待审核 3=已驳回。
// 仅管理员可任意切换；普通用户只能提交 2，传其它值会被后端拒绝（错误码与文案都由后端给）。
export function changeArticleStatus(data) {
  return request({
    url: '/blog/article/status',
    method: 'put',
    data: data
  })
}

// 上传博客图片（返回 { url }）
// data 必须是 FormData 且文件字段名固定为 file；后端限制 5MB、仅 jpg/jpeg/png/gif/webp/bmp/svg，
// 成功后 url 形如 /profile/blog/<uuid>.<ext>。上传不再走 1 秒防重复校验（repeatSubmit: false）。
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
