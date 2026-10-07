# rmxg_blog

个人博客系统 —— 全栈 monorepo。

后端为 Spring Cloud Alibaba 微服务架构，前端基于 RuoYi-Vue3 改造。

## 目录结构

```
rmxg_blog/
├── backend/          后端（Spring Cloud Alibaba 微服务）
│   ├── common/       公共模块
│   │   ├── core/     核心工具、通用实体与基础控制器
│   │   └── redis/    Redis 封装
│   ├── gateway-service/   网关服务
│   ├── system-service/    系统服务（业务主体）
│   └── docker-compose.yml 本地基础设施编排
└── web/              前端（Vue 3 + Vite）
    ├── src/
    └── vite/
```

## 技术栈

### 后端 `backend/`

| 项目 | 版本 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.5.6 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2023.0.1.0 |
| groupId / artifactId | `cn.CDPersonal` / `blog-platform` |

Maven 多模块：`system-service`、`gateway-service`、`common`（含 `core`、`redis`）。

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
# 依次启动 gateway-service、system-service
```

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
