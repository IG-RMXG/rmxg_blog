# 博客门户（/portal）技术说明

> 面向开发者 / 维护者。只想了解「怎么用」，请看 [操作手册](操作手册.md)。
>
> 本次改动的提交：`9cb6bed 新增独立博客门户 /portal（游客可读、登录可写）`

**目录**

1. [这是什么](#1-这是什么)
2. [数据流](#2-数据流)
3. [目录与文件职责](#3-目录与文件职责)
4. [路由](#4-路由)
5. [路由守卫](#5-路由守卫webpermissionjs)
6. [接口契约](#6-接口契约blogpublic)
7. [内容可见性与状态流转](#7-内容可见性与状态流转)
8. [站点配置](#8-站点配置)
9. [本地运行与自检](#9-本地运行与自检)
10. [扩展指南](#10-扩展指南)
11. [已知边界](#11-已知边界)
12. [故障排查](#12-故障排查)
13. [相关文档](#13-相关文档)

## 1. 这是什么

门户是**面向读者的独立站点**，和若依后台（`/index`、`/system/**`、`/blog/**` 那些管理页）**共用同一个前端工程、同一个端口、同一套登录 token**，但在体验上是两个产品：

| | 若依后台 | 博客门户 |
| --- | --- | --- |
| 入口 | `/login` → `/index` | `/portal/login` → `/portal/home` |
| 外观 | Layout + 侧边栏 + 标签页 + 面包屑 | 自带 `PortalLayout`：顶栏 / 内容 / 页脚 |
| 菜单 | 后端 `getRouters` 动态菜单 | 固定 3 个导航项，不请求菜单 |
| 面向 | 管理员（及有后台权限的账号） | 读者、普通用户 |
| 游客 | 全部重定向到登录页 | 可直接浏览已发布内容 |

核心规则：**游客可读、写操作需登录、内容只暴露已发布文章。**

## 2. 数据流

```
浏览器
  └─ vite 代理 /dev-api（UAT 为 /uat-api）
       └─ gateway-service :8889（UAT :8890）
            └─ blog-service :8010（UAT :8011）
                 ├─ /blog/public/**   只读、无需登录（门户用）
                 └─ /blog/**          需要登录（评论、写作、后台管理用）
```

门户的读接口全部落在 `/blog/public/**`，网关**不需要新增任何转发规则**——它本来就把 `/blog/**` 转给 blog-service。

## 3. 目录与文件职责

前端（`web/src/`）：

| 文件 | 职责 |
| --- | --- |
| `api/portal.js` | 门户的 9 个只读接口封装 |
| `portal/layout/index.vue` | 门户外壳：顶栏（品牌/导航/搜索/写文章/用户菜单）、`<router-view/>`、页脚 |
| `portal/views/home.vue` | 首页：文章流 + 分类筛选 + 排序 + 搜索 + 分页 + 侧栏数据加载 |
| `portal/views/article.vue` | 文章详情：Markdown 渲染、标签、评论列表与发表 |
| `portal/views/archive.vue` | 归档：月份列表 + 当月文章 |
| `portal/views/write.vue` | 写作 / 编辑：表单、Markdown 编辑器、封面上传、草稿/审核/发布、预览 |
| `portal/views/me.vue` | 我的文章：状态筛选、编辑、提交审核、删除、查看 |
| `portal/views/login.vue` | 独立登录页（含 UAT 免密快捷进入） |
| `portal/components/ArticleCard.vue` | 列表卡片（封面、置顶/推荐标记、摘要、标签、元信息） |
| `portal/components/SiteSidebar.vue` | 侧栏：博主卡、热门文章、分类、标签云、最新评论（按传入数据渲染，缺项自动隐藏） |
| `portal/composables/usePortalSite.js` | 站点信息共享缓存（单飞请求 + 60 秒软过期） |
| `portal/styles/portal.css` | 门户全部样式与设计令牌，收敛在 `.portal-root` / `.portal-login` 下 |

后端（`backend/blog-service/src/main/java/cn/CDPersonal/blog/portal/`）：

| 文件 | 职责 |
| --- | --- |
| `BlogPortalController.java` | `/blog/public/**` 路由，只有 GET |
| `BlogPortalService.java` | 站点统计组装、浏览量累加、limit 归一化 |
| `BlogPortalMapper.java` | 只读 SQL（全部写死 `status='1'`） |
| `domain/PortalQuery.java` | 列表检索条件（categoryId/tagId/keyword/month/orderBy） |
| `domain/PortalSiteInfo.java` | 站点标题、副标题、博主、统计 |
| `domain/PortalAuthor.java` | 博主信息（只含昵称、头像，不含邮箱手机号） |
| `domain/PortalArchiveItem.java` | 归档项（月份 + 篇数） |

## 4. 路由

全部注册在 `web/src/router/index.js` 的 `constantRoutes` 里（不是动态路由，不依赖后端菜单）：

| 路由 | 组件 | meta | 登录要求 |
| --- | --- | --- | --- |
| `/portal` | `portal/layout/index.vue` | — | 重定向到 `/portal/home` |
| `/portal/home` | `portal/views/home.vue` | `title: '首页'` | 否 |
| `/portal/article/:articleId(\d+)` | `portal/views/article.vue` | `title: '文章详情'` | 否 |
| `/portal/archive` | `portal/views/archive.vue` | `title: '归档'` | 否 |
| `/portal/write/:articleId(\d+)?` | `portal/views/write.vue` | `title: '写文章'`, `requiresAuth: true` | 是 |
| `/portal/me` | `portal/views/me.vue` | `title: '我的文章'`, `requiresAuth: true` | 是 |
| `/portal/login` | `portal/views/login.vue` | `title: '登录'` | 否（已登录会被送回首页） |

所有门户路由都带 `meta.portal: true`，供守卫识别。

## 5. 路由守卫（`web/src/permission.js`）

门户在自己的路由空间里走一套独立分支，**不调用 `generateRoutes()`**（门户不使用若依动态菜单，多拉一次既慢又会污染后台的 `sidebarRouters`）。伪代码：

```
if (是 /portal 路径) {
  if (无 token) {
    清空 roles（游客）
    if (页面 requiresAuth) → /portal/login?redirect=<当前地址>
    else → 放行
  } else {
    if (访问的是 /portal/login) → /portal/home
    if (store 里没有 roles) {
      getInfo() 成功 → 重入当前路由
      getInfo() 失败 → 清本地 token、重置 store，按游客处理
    } else → 放行
  }
}
```

几个刻意为之的点：

- **不调 `logOut()` 处理过期 token**：token 已失效时再请求一次退出接口只会又吃一个 401，直接本地清理即可。
- **登录回跳只接受 `/portal` 开头的地址**：`redirect` 参数来自 URL，避免开放重定向。
- **门户与后台共用 `Admin-Token` cookie**：管理员在门户登录后点「管理后台」是同一会话，不需要二次登录。
- **游客不加载用户信息**：没有 token 时一个用户接口都不请求，首屏只打公开接口。

## 6. 接口契约（`/blog/public/**`）

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/blog/public/site` | — | 标题、副标题、博主、文章/分类/标签/评论/浏览量统计 |
| GET | `/blog/public/articles` | `categoryId` `tagId` `keyword` `month`（`yyyy-MM`）`orderBy`（`latest`\|`hot`\|`comments`）`pageNum` `pageSize` | 已发布文章分页列表 |
| GET | `/blog/public/articles/{articleId}` | — | 已发布文章详情，浏览量 +1；未发布返回 `code=404` |
| GET | `/blog/public/articles/{articleId}/comments` | — | 正常状态评论，按楼层正序 |
| GET | `/blog/public/categories` | — | 有已发布文章的分类（含篇数），按 `sort` |
| GET | `/blog/public/tags` | — | 有已发布文章的标签（含篇数） |
| GET | `/blog/public/archive` | — | 按月统计篇数，月份倒序 |
| GET | `/blog/public/hot` | `limit`（默认 5，上限 20） | 按浏览量倒序 |
| GET | `/blog/public/comments/latest` | `limit`（默认 5，上限 20） | 最新评论，带文章标题 |

约定与安全：

- 没有 POST/PUT/DELETE，也没有任何写操作；评论、写作仍走 `/blog/comment`、`/blog/article` 这些需要登录的接口。
- 所有查询固定 `status = '1'`，草稿（0）、待审核（2）、已驳回（3）不可能从门户泄漏。
- 条件全部 `#{}` 参数化；排序字段用 `<choose>` 白名单，不存在 `${}` 拼接。
- 分类/标签只返回**有已发布文章**的项，避免门户出现「点进去是空的」分类。
- 博主信息只返回 `userId / nickName / avatar`，不暴露邮箱、手机号等隐私字段。
- 统计口径统一为「已发布文章」：文章数、分类数/标签数（只算有已发布文章的）、评论数、浏览量之和。

## 7. 内容可见性与状态流转

文章状态与既有博客模块共用一套（见 `BlogArticleService` 常量）：

| status | 含义 | 门户可见 | 谁能改到该状态 |
| --- | --- | --- | --- |
| `0` | 草稿 | 否 | 作者（草稿/待审核之间） |
| `1` | 已发布 | 是 | 管理员（门户写作页「立即发布」，或后台审核） |
| `2` | 待审核 | 否 | 普通用户提交、管理员驳回/通过 |
| `3` | 已驳回 | 否 | 管理员 |

普通用户保存时如果传 `status=1`，后端 `normalizeStatusForSave` 会强制改成 `2`（待审核），所以**门户不存在普通用户绕过审核直接发布的路径**；前端的「立即发布」按钮也只在 `roles` 含 `admin` 时渲染。

## 8. 站点配置

```yaml
# backend/blog-service/src/main/resources/application.yml
blog:
  portal:
    title: ${BLOG_PORTAL_TITLE:CDPersonal 的博客}
    subtitle: ${BLOG_PORTAL_SUBTITLE:记录、思考与分享 —— 写代码，也写生活}
```

只影响展示，不影响业务。改完重启 blog-service 生效；前端站点信息有 60 秒缓存（`usePortalSite`），刷新页面或等一分钟即可看到新标题。

## 9. 本地运行与自检

前提：`blog-service`（8010）已经是**包含门户接口的新代码**——IDE 里改过后端就要重启一次，否则 `/blog/public/site` 会 404。

```powershell
# 1) 公开接口探针（走前端代理 → 网关 → blog-service）
curl.exe -s http://127.0.0.1:80/dev-api/blog/public/site

# 2) 浏览器打开
#    DEV: http://localhost:80/portal/home
#    UAT: http://localhost:81/portal/home   （yarn dev:uat）
```

三种视角自测清单：

| 视角 | 步骤 | 预期 |
| --- | --- | --- |
| 游客 | 打开 `/portal/home`，点任意文章 | 列表、详情、评论都能看；评论框位置显示「登录后即可参与评论」 |
| 普通用户 | `/portal/login` 用普通账号登录 → 写文章 → 保存草稿 → 提交审核 | 有「保存草稿/提交审核」，**没有**「立即发布」；门户首页看不到这篇 |
| 管理员 | 同一入口登录 → 写作页 | 多一个「立即发布」；发布后首页立刻出现 |

自检也可以直接跑接口：

```bash
# 游客视角：列表（应为已发布文章）
curl -s "http://127.0.0.1:80/dev-api/blog/public/articles?pageNum=1&pageSize=10"
# 搜索 / 分类 / 标签 / 归档
curl -s "http://127.0.0.1:80/dev-api/blog/public/articles?keyword=Markdown"
curl -s "http://127.0.0.1:80/dev-api/blog/public/articles?categoryId=1"
curl -s "http://127.0.0.1:80/dev-api/blog/public/articles?tagId=1&month=2026-10"
```

## 10. 扩展指南

**加一个门户页面**：在 `portal/views/` 新建组件 → 在 `router/index.js` 的门户 `children` 里注册（`meta.portal: true`；需要登录再加 `meta.requiresAuth: true`）→ 在 `portal/layout/index.vue` 的 `.p-nav` 里加入口。

**加一个公开查询接口**：在 `BlogPortalMapper` 加 `@Select`（记得写死 `status='1'` 并用 `#{}`）→ `BlogPortalService` 加方法 → `BlogPortalController` 加 `@GetMapping` → `api/portal.js` 加封装。公开接口不要写成 `/blog/public/**` 之外的路径，否则等于给自己留鉴权口子。

**改主题色**：只改 `portal/styles/portal.css` 里的 `--p-primary` 等令牌即可，Element Plus 组件会跟着变（门户根节点已覆盖 `--el-color-primary` 系列变量）。样式全部收敛在 `.portal-root` / `.portal-login` 下，不会影响若依后台。

**加侧栏模块**：`SiteSidebar.vue` 的 props 都是可选数组，传了就渲染、不传就隐藏；新模块照此模式加 `v-if`，数据在页面里用 `Promise.all` 拉取。

## 11. 已知边界

- **不做注册**：账号由后台「系统管理 → 用户管理」创建；门户登录页只负责登录。
- **不做点赞/收藏/上下篇/RSS/SEO**：这些不在当前需求内；`like_count` 字段存在但门户没有写入入口。
- **不是 SSR**：Vite 单页应用，分享链接能打开但搜索引擎抓取有限。
- **站点信息缓存 60 秒**：`usePortalSite.js` 里的 `CACHE_MS`，需要更实时可调小或调用 `reloadPortalSite()`。
- **门户不参与若依动态菜单**：新增门户页面不需要动 `sys_menu`，也不需要给角色配菜单权限。
- **UAT 与 DEV 数据隔离**：UAT 门户连的是 `ry-cloud-uat` + Redis db1，和 DEV 完全分开，详见 [`backend/uat-service/README.md`](../../backend/uat-service/README.md)。

## 12. 故障排查

| 现象 | 原因与处理 |
| --- | --- |
| `/portal/home` 页面能打开但文章区一直空 | `blog-service` 还是旧代码，`/blog/public/**` 返回 404。重启 blog-service（IDE 里重启，或用新 jar） |
| 控制台 `404 /blog/public/site` | 同上；也可能是网关没起（`dev-status.ps1` 看 8889/8890） |
| 登录后又被踢回登录页 | token 失效或浏览器禁用了 cookie（token 存 `Admin-Token` cookie） |
| 评论按钮点了没反应 | 未登录时只会显示「登录」入口；登录后正常出现「发表评论」 |
| 门户标题还是旧的 | 站点信息有 60 秒缓存；或 `BLOG_PORTAL_TITLE` 没重启生效 |
| UAT 点「以普通用户进入」报 500 | 不是门户问题，是 UAT 网关/服务没起，见 `uat-service/README.md` 第七节 |
| 后台还是旧样式 | 门户样式全部在 `.portal-root` 作用域内，不会串到后台；若真发现串色，检查是否把规则写到了 `portal.css` 之外 |

## 13. 相关文档

- [操作手册](操作手册.md) —— 给读者 / 普通用户 / 管理员的使用说明
- [`README.md`](../../README.md) —— 仓库总览与启动方式
- [`backend/blog-service/README.md`](../../backend/blog-service/README.md) —— 博客服务接口与配置
- [`backend/uat-service/README.md`](../../backend/uat-service/README.md) —— UAT 环境与假登录

## 附录：本次交付的验证记录

用无头 Chrome（CDP）跑的真实链路，不是接口 mock。截图产物落在 `logs/portal-shots/`（该目录被 `.gitignore` 忽略，不入库）。

| 视角 | 验证内容 | 结果 |
| --- | --- | --- |
| 游客 | 首页文章流 / 侧栏 4 个区块 / 文章详情 Markdown 渲染 / 评论可见 / 归档月份 / 访问写作页跳登录 | 通过 |
| 管理员 | 真实账号登录落门户 / `getInfo` 返回 admin + 100 权限 / 写文章存草稿 / 草稿不在首页 / 发布后首页 +1 / 分类与标签筛选 / 发表评论 / 下拉菜单 / 退出 | 通过 |
| 普通用户 | `ry`（角色 common）登录落门户 / 无「管理后台」/ 我的文章只显示自己的 / 写作页无「立即发布」 | 通过 |
| 回归 | 游客访问 `/index` 仍跳登录 / 门户 token 打开后台侧边栏与 5 个顶级菜单 / 后台文章管理页可打开 | 通过 |
| UAT | `:81/portal` 游客浏览、UAT 标识、登录页免密进入普通用户 | 通过 |
