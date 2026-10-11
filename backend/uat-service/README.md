# uat-service —— UAT 验收专用服务

给验收/演示用的**测试功能微服务**（端口 **8020**）。当前提供两个能力：

- **假登录**：指定角色直接签发一个**真实可用**的 token，跳过密码与滑块验证码
- **一键切角色**：配合前端横幅，随时在「管理员 / 普通用户」之间切换身份，用来验收分权

> 这个服务**只在 UAT 环境部署**。DEV 环境不启动它，网关也不路由它的接口（默认 404）——
> 所以 DEV 的运行时里根本不存在这些测试功能，不需要开分支，也就不会有代码冲突。

---

## 一、隔离原理（为什么不会影响 DEV）

三重闸门，任何一道单独失效都不会污染 DEV：

| # | 闸门 | 机制 | DEV 下的实际表现 |
|---|---|---|---|
| 1 | **进程隔离** | 只有 `scripts/uat-up.ps1` 会拉起本服务 | 8020 端口空闲，进程不存在 |
| 2 | **路由隔离** | 网关 `gateway.forward.uat-service` 默认为空 | 请求 `/uat/**` 返回 **404**（并打印一条拒绝日志） |
| 3 | **数据隔离** | 连 `ry-cloud-uat` 库 + Redis **db1** | 与 DEV 的 `ry-cloud` / db0 完全隔离，token 互不相通 |

一次性资源配置对照：

| 资源 | DEV | UAT |
|---|---|---|
| system-service | 8000 | 8001 |
| blog-service | 8010 | 8011 |
| **uat-service** | **不存在** | **8020** |
| gateway | 8889 | 8890 |
| 前端 | 80（`yarn dev`） | 81（`yarn dev:uat`） |
| MySQL | `ry-cloud` | `ry-cloud-uat` |
| Redis | db 0 | **db 1** |

---

## 二、快速开始（3 步）

在**仓库根目录**执行：

```powershell
# 1) 初始化 UAT 库（从 DEV 的 ry-cloud 导一份种子；幂等，可随时重置）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\init-uat-db.ps1

# 2) 启动 UAT 后端四件套（8001 / 8011 / 8020 / 8890，含构建）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-up.ps1
#    已经构建过可加 -SkipBuild 跳过 mvn install

# 3) 启动 UAT 前端（端口 81）
cd web
yarn dev:uat
```

浏览器打开 <http://localhost:81> → 登录页点「**管理员**」或「**普通用户**」即可直接进入系统。
进入后在**顶栏横幅**上可以随时切换身份。

停止：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-down.ps1   # 停 8001/8011/8020/8890/81
```

> `uat-up.ps1` 启动完会自动打一次 `/uat/status` 自检，把 db/redis/两个测试账号的就绪情况打出来。

---

## 三、接口说明

本服务**无 context-path**，接口直接挂在 `/uat/**`。经网关访问是 `http://localhost:8890/uat/...`；
经前端代理访问是 `http://localhost:81/uat-api/uat/...`（`/uat-api` 由 vite 重写掉）。

### 1. `GET /uat/status` —— 环境自检

排查问题**先看这个**。

```powershell
Invoke-RestMethod -Uri 'http://127.0.0.1:8890/uat/status' | ConvertTo-Json -Depth 6
```

```json
{
  "code": 200,
  "data": {
    "service": "uat-service",
    "db": true,
    "redis": true,
    "accounts": {
      "admin": { "userName": "uat_admin", "ready": true, "roles": ["admin"] },
      "user":  { "userName": "uat_user",  "ready": true, "roles": ["common"] }
    }
  }
}
```

`db` / `redis` 为 `false` 或 `ready` 为 `false` → 见第六节故障排查。

### 2. `POST /uat/fake-login` —— 假登录

请求体 `{"role": "admin"}` 或 `{"role": "user"}`（大小写不敏感）。**无需任何鉴权头**。

```powershell
$r = Invoke-RestMethod -Uri 'http://127.0.0.1:8890/uat/fake-login' -Method POST `
       -ContentType 'application/json' -Body '{"role":"admin"}'
$token = $r.data.token

# 拿到的 token 与真实登录完全等价，可直接调其它服务
Invoke-RestMethod -Uri 'http://127.0.0.1:8890/system/user/getInfo' `
  -Headers @{ Authorization = "Bearer $token" }
```

```json
{
  "code": 200,
  "data": {
    "token": "8f3a...（32 位 uuid）",
    "expiresIn": 1800,
    "userId": 105,
    "userName": "uat_admin",
    "nickName": "UAT管理员",
    "roles": ["admin"]
  }
}
```

出错时的返回：

| code | 含义 |
|---|---|
| 400 | `role` 不是 `admin`/`user` → `role 仅支持 admin / user` |
| 500 | 测试账号不存在或被停用 → `测试账号未就绪：uat_admin` |

### 3. 「一键切角色」是怎么实现的

**没有单独的切换接口**，前端就是再调一次 `fake-login`：

```
点「切换为普通用户」
  → removeToken()（清掉当前会话）
  → POST /uat/fake-login {"role":"user"}
  → setToken(新 token)
  → window.location.href = '/index'（整页重载）
```

整页重载是刻意的：让前端的权限守卫用新 token 重新走 `getInfo` + `generateRoutes`，
菜单与按钮权限随角色**整体重建**，比局部刷状态可靠。

---

## 四、测试账号

由本服务**启动时自动创建**（幂等，重复启动不会重复插），落在 `ry-cloud-uat` 库：

| 账号 | 昵称 | 角色 | role_id | 说明 |
|---|---|---|---|---|
| `uat_admin` | UAT管理员 | `admin` | 1 | 全部菜单与权限 |
| `uat_user` | UAT普通用户 | `common` | 2 | 只有博客「文章管理」等授权菜单 |

- 密码字段填的是固定占位串（`uat-only-no-login`），**不能用密码登录**，只能走假登录
- 这两个账号的权限行为与真实账号一致，所以可以拿来验收分权：`uat_user` 看不到分类/标签/评论管理，也删不了别人的文章

---

## 五、配置项

全部支持环境变量覆盖（`src/main/resources/application.yml`）：

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `UAT_PORT` | `8020` | 服务端口 |
| `UAT_DB_URL` | `jdbc:mysql://localhost:3306/ry-cloud-uat?...` | 必须指向 UAT 库 |
| `UAT_DB_USERNAME` | `root` | 数据库账号 |
| `UAT_DB_PASSWORD` | `123456` | 数据库密码 |
| `UAT_REDIS_HOST` | `localhost` | Redis 主机 |
| `UAT_REDIS_PORT` | `6379` | Redis 端口 |
| `UAT_REDIS_DATABASE` | **`1`** | **必须与 UAT 的 system-service 一致**，否则 token 不被认可 |
| `UAT_TOKEN_EXPIRE_MINUTES` | `30` | **必须与 system-service 的 `TokenService.EXPIRE_MINUTES` 一致** |

网关侧对应 `GATEWAY_UAT_SERVICE`（`scripts/uat-up.ps1` 会设成 `http://localhost:8020`）。

单独起本服务调试：

```powershell
cd backend
mvn -DskipTests install                     # 首次需整体 install（依赖 core 模块）
java -jar uat-service/target/uat-service-1.0.jar
```

---

## 六、⚠️ 注意事项（重点）

### 安全边界

1. **绝对不要部署到生产或 DEV 环境。** 本服务的 `fake-login` **刻意不做鉴权**——它就是给未登录的验收人员用的，谁能访问到 8020 谁就能拿到任意角色的 token。
2. **不要在 DEV 网关配置 `GATEWAY_UAT_SERVICE`。** 一旦配上，DEV 的 8889 就会把 `/uat/**` 转发过来，测试功能立刻暴露在开发环境。DEV 下 `gateway.forward.uat-service` 必须保持为空。
3. **生产库千万别接。** 假登录会往目标 Redis 写真实会话，并读取目标库的用户表。`UAT_DB_URL` / `UAT_REDIS_DATABASE` 指向生产库等于开后门。
4. 暴露给同事/外部验收时，建议只在**内网**开放 8020（或干脆只通过网关 8890 访问，8020 不对外）。

### 使用依赖（容易踩）

5. **必须先跑 `scripts\init-uat-db.ps1`。** 库不存在时本服务**不会启动失败**——只在控制台打一行 `[uat] 测试账号 ensure 失败`，然后用 `/uat/fake-login` 会报 `测试账号未就绪`。所以起完服务**先看 `/uat/status`**。
6. **重新执行 `init-uat-db.ps1` 之后，必须重启 uat-service。** 该脚本是「删库 + 从 DEV 导种子」重建，而 DEV 库里并没有 `uat_admin`/`uat_user`（已实测），所以重建后这两个账号会消失；只有重启 uat-service 才会重新 ensure 出来。
7. **Redis db 序号必须和 UAT 的 system-service / blog-service 一致（db1）。** 若不一致，假登录会成功返回 token，但任何接口都认不出它（表现为 401/500）——因为 token 读的是另一个 db。
8. **token 有效期的两处配置必须一致。** 本服务只负责签发与续期，校验方是 system-service 的 `TokenService`；两边对不上会导致会话提前失效或续期异常。

### 跨服务契约（改代码时）

9. 假登录的 token 之所以"真实可用"，是因为它写的 Redis 键与值与 system-service **完全同构**：

   | 项 | 值 |
   |---|---|
   | 键 | `login_tokens:<32位uuid>` |
   | 值 | `{"userId":105,"loginTime":...,"ipaddr":"uat-fake-login"}` |
   | 有效期 | 30 分钟 |

   **改动 system-service 的 `TokenService`（键名 / 会话 JSON 结构 / TTL）时，必须同步改本服务的 `UatFakeLoginService`**，否则假登录会静默失效。这一点在代码注释里也标了 `★ 跨服务契约`。

### 维护提示

10. `scripts/*.ps1` 必须存为 **UTF-8 带 BOM**。Windows PowerShell 5.1 会把无 BOM 的 UTF-8 当 GBK 解码，中文注释变乱码后**直接导致语法错误**（本项目已踩过两次）。
11. 本服务不起 `ApplicationRunner` 之外的定时任务、不注册任何全局过滤器——保持"只做测试功能"的最小面。

---

## 七、故障排查

| 现象 | 原因与处理 |
|---|---|
| `/uat/**` 返回 404 | ① 网关没配 `GATEWAY_UAT_SERVICE`（DEV 的正常表现）；② uat-service 没启动 |
| `/uat/fake-login` 返回 500 `测试账号未就绪` | UAT 库没初始化，或重新 init 后没重启 uat-service → 看 `/uat/status` 的 `accounts` |
| 假登录成功但接口仍 401/500 | Redis db 不一致：`UAT_REDIS_DATABASE` 与 UAT 的 `SYSTEM_REDIS_DATABASE` 必须相同（都应是 1） |
| `/uat/status` 里 `db=false` | MySQL 没起，或 `UAT_DB_URL` 指向的库不存在（`ry-cloud-uat`） |
| `/uat/status` 里 `redis=false` | Redis 容器没起（`docker ps` 看 `redis-server`） |
| 前端登录页看不到「快捷进入」 | 前端不是以 uat 模式启动的：要用 `yarn dev:uat`（`VITE_APP_ENV=uat` 才渲染） |
| 前端调 `/uat-api/...` 404 | UAT 网关 8890 没起；或 `vite.config.js` 的 `/uat-api` 代理被改动 |
| 顶栏横幅遮住内容顶部 | 横幅高度是靠 `--uat-banner-h` 传递的，若改过 `AppMain.vue` 的高度计算或 `UatBanner` 的 `ResizeObserver`，会失准（见这两个文件的注释） |
| 启动脚本报 `NO_PROXY`/`no_proxy` 键冲突 | 本机 `Start-Process` 的环境变量大小写怪癖；`uat-up.ps1` 已改用 .NET Process 启动，不用管 |

---

## 八、目录与依赖

```
backend/uat-service/
├── pom.xml                                      # 最小依赖：web + mybatis + mysql + core + redis
├── README.md                                    # 本文件
└── src/main/
    ├── java/cn/CDPersonal/
    │   ├── UatApplication.java                  # 启动类（8020）
    │   └── uat/
    │       ├── controller/UatController.java    # /uat/status、/uat/fake-login
    │       ├── service/UatFakeLoginService.java # 假登录 + 测试账号 ensure（跨服务契约在此）
    │       └── mapper/UatUserMapper.java        # 测试账号的建号与查询（只读业务表 + 插号）
    └── resources/application.yml
```

- 唯一内部依赖：`cn.CDPersonal:core`（统一响应体 `ResponseVO`）
- 首次构建要在 `backend` 目录整体 `mvn install`（单独 `-pl uat-service` 解析不到 `core`）
- 不依赖 `system-service` 的任何业务代码，可整体拷走（换用户体系时只需替换 `uat` 包内的 mapper 与 ensure 逻辑）