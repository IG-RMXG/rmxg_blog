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

Maven 多模块：`system-service`、`blog-service`、`gateway-service`、`common`（含 `core`、`redis`）。

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
