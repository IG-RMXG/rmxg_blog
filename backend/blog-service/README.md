# blog-service —— 博客微服务

博客模块的独立可部署单元。从 `system-service` 抽离出来，可单独打包 / 部署 / 迁移。

## 基本信息

| 项 | 值 |
| --- | --- |
| Maven 模块 | `backend/blog-service`（artifactId `blog-service`） |
| 启动类 | `cn.CDPersonal.BlogApplication` |
| 端口 | **8010** |
| context-path | **无**（控制器直接挂在 `/blog/**`） |
| 依赖的中间件 | MySQL、Redis |
| 唯一内部模块依赖 | `cn.CDPersonal:core`（只提供 `ResponseVO` / `ResponseStatus`） |

## 后端路由（由 `gateway-service` 决定）

| 网关路径 | 目标 | 说明 |
| --- | --- | --- |
| `/blog/**` | `blog-service:8010`（路径原样） | 博客业务接口 |
| `/profile/blog/**` | `blog-service:8010`（路径原样） | 博客上传图片（静态资源） |
| 其余（`/system/**`、`/auth/**`、`/profile/avatar/**` 等） | `system-service:8000`（补 `/system` 前缀） | 与博客无关 |

前端因此用 `/dev-api/blog/article/list` 这样的地址；`/dev-api` 由 vite 代理到网关 8889。

> ⚠️ `/profile/blog/**` 必须先于通用的 `/profile/**` 判定，见
> [`ServiceForwardController#resolveTarget`](../gateway-service/src/main/java/cn/CDPersonal/gateway/controller/ServiceForwardController.java)。

## 接口一览

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | `/blog/article/list` | 登录（管理员看全部，普通用户只看自己） |
| GET | `/blog/article/{articleId}` | 登录（自己的或已发布） |
| GET | `/blog/article/view/{articleId}` | 登录（浏览量 +1） |
| POST | `/blog/article` | 登录（普通用户不能直接发布，会转待审核） |
| PUT | `/blog/article` | 登录（只能改自己的） |
| PUT | `/blog/article/status` | 管理员任意；普通用户只能提交审核 |
| DELETE | `/blog/article/{articleIds}` | 登录（只能删自己的） |
| GET | `/blog/category/list`（分页）、`/blog/category/enabled`（下拉） | 管理列表仅管理员；下拉所有登录用户 |
| POST/PUT/DELETE | `/blog/category/**` | 仅管理员 |
| GET | `/blog/tag/list`（分页）、`/blog/tag/all`（下拉） | 管理列表仅管理员；下拉所有登录用户 |
| POST/PUT/DELETE | `/blog/tag/**` | 仅管理员 |
| GET | `/blog/comment/list`（管理）、`/blog/comment/article/{articleId}`（详情页） | 管理列表仅管理员；详情页所有登录用户 |
| POST | `/blog/comment` | 登录用户 |
| PUT | `/blog/comment/status`、DELETE `/blog/comment/{commentIds}` | 仅管理员 |
| POST | `/blog/upload` | 登录用户（图片上传，jpg/jpeg/png/gif/webp/bmp/svg，≤5MB） |

## 数据表

建表 + 菜单数据脚本：[`src/main/resources/sql/blog.sql`](src/main/resources/sql/blog.sql)

| 表 | 说明 |
| --- | --- |
| `blog_category` | 分类 |
| `blog_tag` | 标签 |
| `blog_article` | 文章（`content` 存 Markdown 源码） |
| `blog_article_tag` | 文章-标签关联 |
| `blog_comment` | 评论 |

脚本同时会往 `sys_menu` / `sys_role_menu` 写入「博客管理」菜单（menu_id 2000 起）与按钮权限。
**这些表在 `ry-cloud` 库**（与 `system-service` 同库），因为菜单与角色数据由系统服务维护。

## 鉴权（跨服务契约）

本服务不签发 token，只读取校验：

- 登录由 `system-service` 完成，会话写入 Redis：键 `login_tokens:<uuid>`，值 `{"userId":1,...}`
- 请求头：`Authorization: Bearer <uuid>`
- 本服务按需续期（默认 30 分钟，配置项 `blog.auth.token-expire-minutes`，**必须与 system-service 的 `TokenService.EXPIRE_MINUTES` 一致**）
- 角色判定：查 `sys_user` / `sys_role` / `sys_user_role`，`role_key` 含 `admin` 即管理员

> 改 token 存储格式或有效期时，`system-service` 与 `blog-service` 必须同步修改。

## 配置项（全部支持环境变量覆盖）

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BLOG_PORT` | `8010` | 服务端口 |
| `BLOG_DB_URL` | `jdbc:mysql://localhost:3306/ry-cloud?...` | 数据库连接 |
| `BLOG_DB_USERNAME` | `root` | 数据库账号 |
| `BLOG_DB_PASSWORD` | `123456` | 数据库密码 |
| `BLOG_REDIS_HOST` | `localhost` | Redis 主机 |
| `BLOG_REDIS_PORT` | `6379` | Redis 端口 |
| `BLOG_REDIS_PASSWORD` | 空 | Redis 密码 |
| `BLOG_REDIS_DATABASE` | `0` | Redis 库序号 |
| `BLOG_TOKEN_EXPIRE_MINUTES` | `30` | token 续期时长 |
| `APP_UPLOAD_PATH` | 进程工作目录 `/uploads` | 上传根目录（也支持 `-Dapp.upload.path=`） |

网关侧对应 `GATEWAY_BLOG_SERVICE`（默认 `http://localhost:8010`）。

## 如何运行

**IDEA**：运行 `cn.CDPersonal.BlogApplication`，Project SDK 选 JDK 17。
启动顺序：MySQL/Redis → `system-service`(8000) → `blog-service`(8010) → `gateway-service`(8889)。

**命令行**：

```powershell
cd backend
mvn -DskipTests install
java -jar blog-service/target/blog-service-1.0.jar
```

> `blog-service` 依赖 `cn.CDPersonal:core`，因此首次构建要在 `backend` 目录整体 `install`
> （单独 `-pl blog-service` 会解析不到 `core`）。

## 迁移指南

要把它搬到别的项目/环境，只需要：

1. **拷模块**：整个 `backend/blog-service` 目录。它不引用 `system-service` 的任何业务代码。
2. **带上 `core`**：`ResponseVO` / `ResponseStatus` 来自 `cn.CDPersonal:core`；
   若目标工程没有该模块，把这两个类一起带上即可（或改成目标工程的统一响应体）。
3. **建表**：执行 `src/main/resources/sql/blog.sql`。
   若目标环境没有 `sys_menu` / `sys_role_menu` / `sys_user` / `sys_role` / `sys_user_role`，
   删掉脚本里的菜单段，并把 `BlogTokenService` / `BlogUserMapper` 换成目标环境的鉴权实现。
4. **配环境变量**：`BLOG_DB_*` / `BLOG_REDIS_*` 指到目标的库和 Redis。
5. **配网关**：把 `/blog/**` 与 `/profile/blog/**` 指向新地址。
6. **改前端**：`web/src/api/blog/**` 里的 `url` 前缀。

涉及「用户/角色」的部分是唯一的跨服务耦合点，集中在
[`cn.CDPersonal.blog.security`](src/main/java/cn/CDPersonal/blog/security) 包内，替换成本可控。
