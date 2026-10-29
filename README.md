# rmxg_blog

个人博客系统 —— 全栈 monorepo。

后端为 Spring Cloud Alibaba 微服务架构，前端基于 RuoYi-Vue3 改造。

## 目录结构

```
rmxg_blog/
├── backend/          后端（Spring Cloud Alibaba 微服务）
│   ├── common/       公共模块
│   │   ├── core/     核心响应体（ResponseVO / ResponseStatus）
│   │   └── redis/    Redis 封装
│   ├── gateway-service/   网关服务（8889，按路径转发到各业务服务）
│   ├── system-service/    系统服务（8000，用户 / 角色 / 菜单 / 部门等）
│   ├── blog-service/      博客服务（8010，文章 / 分类 / 标签 / 评论，可独立迁移）
│   └── docker-compose.yml 本地基础设施编排
└── web/              前端（Vue 3 + Vite）
    ├── src/
    └── vite/
```

## 服务与端口

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| gateway-service | 8889 | 唯一入口，按路径前缀转发 |
| system-service | 8000 | 系统业务，context-path `/system` |
| blog-service | 8010 | 博客业务，无 context-path，见 [blog-service/README](backend/blog-service/README.md) |
| 前端 vite | 80 | 开发服务器 |
| MySQL / Redis / Nacos | 3306 / 6379 / 8848（控制台 8081） | docker-compose 提供 |

网关路由（`gateway-service` → 各服务）：

| 网关路径 | 目标服务 |
| --- | --- |
| `/system/**`、`/auth/**`、`/schedule/**`、`/code/**`、`/profile/avatar/**` | system-service |
| `/blog/**`、`/profile/blog/**` | blog-service |

## 技术栈

### 后端 `backend/`

| 项目 | 版本 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.5.6 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2023.0.1.0 |
| groupId / artifactId | `cn.CDPersonal` / `blog-platform` |

Maven 多模块：`system-service`、`blog-service`、`uat-service`、`gateway-service`、`common`（含 `core`、`redis`）。

## UAT 验收环境（与 DEV 完全隔离）

测试功能不进 DEV 的隔离方式：代码同仓库同主干，DEV **不启动** `uat-service` 且网关不路由 `/uat/**`（默认 404），无分支无冲突。

| 维度 | DEV | UAT |
| --- | --- | --- |
| system-service / blog-service | 8000 / 8010（IDEA） | 8001 / 8011（jar） |
| uat-service（假登录等测试功能） | **不存在** | 8020 |
| gateway | 8889 | 8890 |
| 前端 | 80（`yarn dev`） | 81（`yarn dev:uat`） |
| MySQL | `ry-cloud` | `ry-cloud-uat` |
| Redis | db 0 | db 1（会话互不相通） |

UAT 定制功能：登录页「快捷进入」（免密免验证码）、顶栏横幅一键切换管理员/普通用户（验收分权）。均只在 `VITE_APP_ENV=uat` 时渲染，生产构建恒不出现。

```powershell
# 1. 初始化 UAT 库（从 DEV 导种子，幂等，随时重置）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\init-uat-db.ps1
# 2. 启动 UAT 后端（构建 + 8001/8011/8020/8890）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-up.ps1
# 3. 启动 UAT 前端（端口 81）
cd web; yarn dev:uat
# 停止 UAT 后端
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-down.ps1
```

### 前端 `web/`

| 项目 | 版本 |
| --- | --- |
| Vue | 3.5.16 |
| Vite | 6.3.5 |
| Element Plus | 2.10.7 |
| Pinia | 3.0.2 |
| Vue Router | 4.5.1 |
| Axios | 1.9.0 |

基座为 RuoYi-Vue3 3.6.6。

### 博客门户 `web/src/portal/`（`/portal`）

面向读者的独立站点，不是若依后台的一个菜单：

- **入口**：`http://localhost:80/portal/home`（UAT 端口 81 同理），游客可直接浏览
- **独立外观**：自带 `PortalLayout`（顶栏 / 内容 / 页脚），不套若依的侧边栏、标签页、面包屑
- **独立登录页**：`/portal/login`，登录后回跳门户；管理员登录后仍可从顶栏「管理后台」进若依
- **游客可读、写操作需登录**：列表 / 详情 / 评论 / 分类 / 标签 / 归档 / 搜索全部公开；
  评论、写文章、我的文章需要登录，未登录会被引导到 `/portal/login?redirect=…`
- **内容全部来自已发布文章**：草稿、待审核不会出现在门户（后端 `/blog/public/**` 写死 `status='1'`）

| 路由 | 页面 |
| --- | --- |
| `/portal/home` | 首页：文章流、分类筛选、最新/热门/评论排序、搜索、分页 + 侧栏（博主卡 / 热门 / 分类 / 标签 / 最新评论） |
| `/portal/article/:articleId` | 文章详情：Markdown 渲染 + 评论区（登录后可发评论） |
| `/portal/archive` | 归档：按月统计，点月份看当月文章 |
| `/portal/write`、`/portal/write/:articleId` | 写文章 / 编辑：Markdown 编辑器、封面、分类、标签、存草稿 / 提交审核（管理员可直接发布） |
| `/portal/me` | 我的文章：按状态筛选、编辑、提交审核、删除、查看已发布文章 |
| `/portal/login` | 独立登录窗口（UAT 模式下额外提供免密快捷进入） |

站点标题与副标题来自后端配置：`BLOG_PORTAL_TITLE` / `BLOG_PORTAL_SUBTITLE`（见 `backend/blog-service/README.md`）。

### 基础设施

`backend/docker-compose.yml` 提供本地依赖：

| 服务 | 镜像 | 端口 |
| --- | --- | --- |
| Nacos | `nacos/nacos-server:v2.2.3` | 8848 |
| MySQL | `mysql:8.0` | 3306 |
| Redis | `redis:7.0-alpine` | 6379 |

> 编排中的密码（`123456`）仅供本地开发使用。

## 快速开始

### 1. 启动基础设施

```bash
cd backend
docker compose up -d
```

### 2. 启动后端

```bash
cd backend
mvn clean install -DskipTests
# 依次启动 system-service(8000)、blog-service(8010)、gateway-service(8889)
```

> 博客的表结构与菜单数据在 `backend/blog-service/src/main/resources/sql/blog.sql`，
> 首次使用需要先执行一次（目标库 `ry-cloud`）。

### 3. 启动前端

```bash
cd web
yarn install
yarn dev
```

## 分支说明

| 分支 | 说明 |
| --- | --- |
| `main` | 当前主线，全栈 monorepo |
| `master` | 历史存档：2022 年旧版单体博客（Spring Boot 2.7 + Freemarker + MyBatis） |

## License

MIT
