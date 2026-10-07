# 本地开发依赖（Docker）

三个中间件统一由 `backend/docker-compose.yml` 编排，同时服务两个项目：

- **RuoYi-Cloud**（当前主力）→ 库 `ry-cloud` / `ry-config` / `ry-seata`
- **rmxg_blog 自带后端骨架** → 库 `nacos_dev` / `system`

| 服务 | 镜像 | 容器名 | 端口 | 用途 |
|------|------|--------|------|------|
| MySQL | `mysql:8.0` | `mysql-server` | 3306 | 两个项目的全部业务库 |
| Nacos | `nacos/nacos-server:v3.0.2` | `nacos-server` | 8848 / 9848 / 9849 / **8081** | 注册中心 / 配置中心（standalone + MySQL 持久化） |
| Redis | `redis:7.0-alpine` | `redis-server` | 6379 | gateway-service 验证码等缓存 |

连接凭据统一为 `root` / `123456`。

## 快速开始

项目名固定为 `backend`，所以数据卷叫 `backend_mysql_data` / `backend_redis_data`。

```powershell
# 在仓库根目录执行
docker compose -f backend/docker-compose.yml --project-directory backend up -d
docker compose -f backend/docker-compose.yml --project-directory backend ps
```

或者 `cd backend` 之后直接用 `docker compose up -d`（效果一样）。

## ⚠️ Nacos 必须是 3.x

RuoYi-Cloud 的版本栈是：

```
spring-boot            4.1.0
spring-cloud           2025.1.2
spring-cloud-alibaba   2025.1.0.0     → Nacos 客户端 3.x
```

**连 2.x 服务端会起不来**（跨大版本），所以这里固定 `v3.0.2`。

Nacos 3.x 有两个容易踩的坑：

1. **强制要求 `NACOS_AUTH_TOKEN`**，即使 `NACOS_AUTH_ENABLE=false` 也一样。
   不提供合法的 Base64 值（解码后 ≥ 32 字节）会直接 `exit 255` 无限重启。
   compose 里已配好 token + identity key/value。
2. **控制台换端口了**。2.x 是 `http://localhost:8848/nacos/`，
   3.x 的 OpenAPI 仍在 8848，但**控制台在容器内的 8080**。
   宿主机 8080 要留给 RuoYi-Cloud 网关，所以映射成了 **8081**：
   <http://localhost:8081/>（`NACOS_AUTH_ENABLE=false`，无需登录）

另外健康检查端点也变了：`/nacos/v1/console/health/readiness` 返回 **410 Gone**，
compose 里用的是 `/nacos/v1/ns/operator/metrics`。

## RuoYi-Cloud 的库（手动导入，不在 init 脚本里）

`docker/mysql/init/` 只管 `nacos_dev` / `system`。RuoYi-Cloud 的四个库需要单独导入，
SQL 在 `D:\Program\Project\GitProjects\RuoYi-Cloud\sql\`：

| 库 | 表数 | 来源 SQL | 说明 |
|----|------|---------|------|
| `ry-cloud` | 20 | `ry_20260417.sql` | 系统表，含 `sys_menu`(84条菜单) / `sys_user` |
| `ry-cloud` | +11 | `quartz.sql` | 定时任务表，与上一行**同一个库** |
| `ry-config` | 16 | `ry_config_20260918.sql` | Nacos 配置，含 9 条配置 |
| `ry-seata` | 4 | `ry_seata_20210128.sql` | Seata 表 |

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$env:MYSQL_PWD = '123456'
$sql = "D:\Program\Project\GitProjects\RuoYi-Cloud\sql"

# ry_20260417.sql 和 quartz.sql 里没有 CREATE DATABASE，要先建库
& $mysql -h 127.0.0.1 -P 3306 -uroot -e "CREATE DATABASE IF NOT EXISTS ``ry-cloud`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 注意：必须用 .NET 按 UTF-8 读，PowerShell 管道会把中文转成 GBK 导致乱码
function Import-Sql($file, $db) {
  $text = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
  $args = @('-h','127.0.0.1','-P','3306','-uroot','--default-character-set=utf8mb4')
  if ($db) { $args += $db }
  $text | & $mysql @args
}
Import-Sql "$sql\ry_20260417.sql" 'ry-cloud'
Import-Sql "$sql\quartz.sql"      'ry-cloud'
Import-Sql "$sql\ry_config_20260918.sql"   # 自带 CREATE DATABASE
```

**导入后还要改一处**：`ry-config` 里 seed 的数据源密码是占位符 `password`，
需要改成 `123456`，否则 RuoYi-Cloud 连不上库：

```sql
UPDATE `ry-config`.config_info
SET content = REPLACE(content, 'password: password', 'password: 123456')
WHERE content LIKE '%password: password%';
```

涉及 `ruoyi-system-dev.yml`、`ruoyi-gen-dev.yml`、`ruoyi-job-dev.yml` 三条。
改完需要重启 Nacos 容器才会重新读取。

## 端口冲突

宿主机若已装本地 MySQL / Redis 占用相同端口，先停掉：

```powershell
Get-NetTCPConnection -LocalPort 3306,6379,8848,8081 -State Listen
Stop-Service MySQL80    # 需管理员权限
```

> 端口被占用时 MySQL 会启动失败，而失败后数据卷里可能残留半初始化的数据，
> 因此**务必先释放端口再启动**。

| 端口 | 归属 |
|------|------|
| 3306 / 6379 / 8848 / 9848 / 9849 | 本 compose |
| **8081** | Nacos 3 控制台（宿主） |
| 8080 | 留给 RuoYi-Cloud 网关，**不要**给 Nacos 占用 |

## 拉不动镜像时（Docker Hub 被墙）

本机直连 Docker Hub 会失败，原因是 **`auth.docker.io`（匿名 token 端点）连不上**：

```
failed to authorize: failed to fetch anonymous token:
Get "https://auth.docker.io/token?...": EOF
```

小镜像偶尔能成功，大镜像几乎必然失败，所以“有时能拉”不代表网络没问题。
用国内镜像源拉取后打成标准名字即可：

```powershell
docker pull docker.m.daocloud.io/library/mysql:8.0
docker pull docker.m.daocloud.io/nacos/nacos-server:v3.0.2
docker pull docker.m.daocloud.io/library/redis:7.0-alpine

docker tag docker.m.daocloud.io/library/mysql:8.0          mysql:8.0
docker tag docker.m.daocloud.io/nacos/nacos-server:v3.0.2  nacos/nacos-server:v3.0.2
docker tag docker.m.daocloud.io/library/redis:7.0-alpine   redis:7.0-alpine
```

compose 里保持官方镜像名不变，换机器 / 换网络都不用改配置。

## Maven 依赖（RuoYi-Cloud 构建必需）

本机 `Maven\conf\settings.xml` 里的 `localRepository` 指向不存在的
`E:\maven\worlk\hngqcg`，且配的内网私服 `193.193.193.188:8081` 连不上。
已通过用户级 `C:\Users\Administrator\.m2\settings.xml` 修正：

- `localRepository` → `C:\Users\Administrator\.m2\repository`
- `central` 镜像 → `https://maven.aliyun.com/repository/public`
  （Spring Boot 4.1.0 在该镜像上可正常获取）

验证是否生效：

```powershell
mvn -X validate 2>&1 | Select-String "Using local repository at"
```

## 重新初始化 MySQL

`docker/mysql/init/` 的脚本只在数据卷为空的首次启动执行。

```powershell
docker compose -f backend/docker-compose.yml --project-directory backend down
docker volume rm backend_mysql_data
docker compose -f backend/docker-compose.yml --project-directory backend up -d
```

> 删卷会**清空所有数据**，包括手动导入的 `ry-cloud` / `ry-config` / `ry-seata`，
> 之后需要按上文重新导入。

注意：`docker compose up -d` 在 compose 文件变更后可能**重建容器**，
但只要卷名不变（项目名固定为 `backend`），数据就会保留。

## 常用命令

```powershell
$dc = "docker compose -f backend/docker-compose.yml --project-directory backend"
& $dc ps
& $dc logs -f nacos
docker exec -it mysql-server mysql -uroot -p123456 -e "SHOW DATABASES;"
docker exec -it redis-server redis-cli ping
& $dc down          # 停止（保留数据卷）
```

## 与仓库内 SQL 的关系

`backend/system-service/src/main/resources/sql/` 下的 `sys_dict_type.sql`、
`sys_dict_data.sql` 是原始导出脚本（`USE ruoyi-vue`，仅表结构）。
`docker/mysql/init/` 下对应文件内容一致，仅补上 `USE system` 与基础数据，
以便容器初始化时可直接执行。改动表结构时请同步这两处。
