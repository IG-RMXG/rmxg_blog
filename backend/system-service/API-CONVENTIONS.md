# RuoYi 业务接口移植规范（rmxg_blog / system-service）

本文件描述在 `backend/system-service` 里补齐 RuoYi 业务 controller 时必须遵守的约定。
**新增任何接口前先读本文件**，避免又出现"页面能打开但没数据"的问题。

---

## 1. 路径与转发

```
前端 /dev-api/xxx  → vite 去掉 /dev-api → 网关 8889 → 转发 → system-service 8000
```

- `server.servlet.context-path: /system`，所以 controller 上**只写业务段**：
  `@RequestMapping("/dict/type")` 的对外路径是 `/system/dict/type/**`。
- 网关只转发**这四类前缀**：`/system/**`、`/auth/**`、`/schedule/**`、`/code/**`
  （见 `gateway-service` 的 `SystemServiceForwardController`）。**新增其它前缀必须同步改这里**，否则 404。
- 前端 api 文件里的 url：多数带 `/system`，但**定时任务和代码生成不带**：
  - `monitor/job.js`、`monitor/jobLog.js` → `/schedule/job/**`
  - `tool/gen.js` → `/code/gen/**`
  这两类打的是 `/schedule/**`、`/code/**`；因为 system-service 的 context-path 是 `/system`，
  转发到 8000 后会命中 `/system/schedule/**`、`/system/code/**`，Spring 自动补 context-path。
  **所以 controller 上写 `@RequestMapping("/schedule/job")`，不要再加 `/system`。**
- `/captcha` 例外：由 gateway-service 自己提供，不带前缀。

## 2. 两类返回结构（最容易搞错）

### A. 分页列表 —— 用 `TableDataInfo`，前端读 `response.rows` / `response.total`

适用：`user` `role` `post` `dict/type` `dict/data` `config` `notice` `operlog` `logininfor` `job` `job/log` `gen`

```java
@GetMapping("/list")
public TableDataInfo list(SysXxx query) {
    startPage();                                   // 必须在查库之前调用
    return PageUtils.getDataTable(service.selectXxxList(query));
}
```

返回形如 `{"total":10,"rows":[...],"code":200,"msg":"查询成功"}`。
前端分页参数固定是 `pageNum` / `pageSize`（见 `PageUtils.startPage()`）。

### B. 非分页 / 树形 / 详情 —— 用 `ResponseVO<T>`，前端读 `response.data`

适用：`dept` `menu` 列表、各类详情、treeselect、getRouters

```java
@GetMapping("/list")
public ResponseVO<List<SysDept>> list(SysDept query) {
    return ResponseVO.success(service.selectDeptList(query));
}
```

**树形接口返回扁平数组**，由前端 `proxy.handleTree(response.data, "deptId")` 自己建树 —— 后端**不要**组装 children。

### C. 例外：`/system/user/getInfo` 直接返回 VO，不带 `ResponseVO` 包装

前端 `store/modules/user.js` 读顶层 `res.user` / `res.roles` / `res.permissions`。
包进 `data` 会拿到 undefined，权限全空。详见 `SysUserController#getInfo`。

## 3. 增删改的返回

前端只判断 `code == 200`，`data` 可以为 null：

```java
ResponseVO.success()                                      // 成功
ResponseVO.error(500, "具体原因")                          // 业务失败，前端会 ElMessage 弹 msg
```

- 新增/修改用 `@RequestBody`；查询用普通参数绑定；删除用 `@PathVariable Long[] ids`（前端传逗号分隔，Spring 自动转数组）。
- 操作留痕：`setCreateBy(LoginUserHolder.getUserName())` / `setUpdateBy(...)`，时间列用 SQL 里的 `NOW()`。

## 4. 实体与 Mapper 约定

- 实体放 `cn.CDPersonal.common.domain.entity`，**继承 `BaseEntity`**（提供 createBy/createTime/updateBy/updateTime/remark/params），字段名用驼峰。
  已存在：`SysDictType`。已补：`SysDictData`。
- Mapper 放各自模块的 `mapper` 包，用**注解式 SQL**（`@Select`/`@Insert`/`@Update`/`@Delete`），标注 `@Mapper`。
- 列名到属性名的下划线→驼峰转换已全局开启（`mybatis.configuration.map-underscore-to-camel-case: true`），SQL 里直接写列名即可。
- 动态条件用 `@Select("<script> ... </script>")` + `<if test="...">`。**注意 `params` 在 `BaseEntity` 里恒非 null**，可安全写 `params.beginTime`。
- 新增要回填主键：`@Options(useGeneratedKeys = true, keyProperty = "xxxId")`。
- 批量删除用 `@Delete("<script> ... <foreach collection=\"array\" ...>")`。

## 5. 必须实现的响应字段（否则页面空白）

前端表格列直接绑 `scope.row.xxx`，字段名必须和实体属性一致。典型：

| 页面 | 关键字段 |
|---|---|
| 用户 | `userId` `userName` `nickName` `dept.deptName` `phonenumber` `status` `createTime` `deptId` `postIds` `roleIds` |
| 角色 | `roleId` `roleName` `roleKey` `roleSort` `status` `createTime` |
| 部门 | `deptId` `parentId` `deptName` `orderNum` `leader` `phone` `email` `status` `children`(前端生成) |
| 菜单 | `menuId` `menuName` `parentId` `orderNum` `path` `component` `menuType` `visible` `status` `perms` `icon` |
| 岗位 | `postId` `postCode` `postName` `postSort` `status` `createTime` |
| 参数 | `configId` `configKey` `configValue` `configType` `createTime` |
| 通知 | `noticeId` `noticeTitle` `noticeType` `status` `createTime` |

字典翻译（如状态列显示"正常/停用"）由前端 `useDict` 调 `/system/dict/data/type/{dictType}` 完成，后端不用管。

## 6. 参考实现

`dict` 模块是完整样板，新模块照它写：

```
dict/mapper/SysDictTypeMapper.java      @Mapper + 注解 SQL + <script> 动态条件
dict/mapper/SysDictDataMapper.java
dict/service/SysDictService.java        @Service + @Transactional
dict/controller/SysDictTypeController.java   分页列表 + CRUD + optionselect
dict/controller/SysDictDataController.java
```

## 7. 验证要求

每批完成后必须：

1. `mvn -pl system-service compile` 通过；
2. 重启 8000，用真实 HTTP 打一遍关键接口，确认 `total/rows` 或 `data` 结构正确、条数非 0；
3. 有条件时用浏览器打开对应页面确认有数据（不能只看接口）。

## 8. 当前进度

| 模块 | 状态 |
|---|---|
| `auth`（login/logout/refresh） | 完成 |
| `user/getInfo`、`menu/getRouters` | 完成 |
| `dict/type`、`dict/data` | 完成（7+6 接口，含分页） |
| `online`（在线用户 + 强退） | 完成（数据源是 Redis `login_tokens:*`，session 存 POJO 含 ipaddr/loginTime） |
| `operlog`、`logininfor` | 完成（含 clean / 批量删 / unlock 占位） |
| `job`、`job/log` | 完成（`/schedule` 前缀；**未接 Quartz**，runJob 只记日志） |
| 网关转发 | 已扩展到 `/schedule/**`、`/code/**` |
| `gen`（代码生成） | 完成 8/9：导入/列表/详情/修改/删除/同步/**预览（真实现）**；`genCode/{tableName}` 返回 501（缺 vm 模板，不假装成功） |
| `config`、`notice` | 完成（7+6 接口，含 `/notice/listTop`、内置参数禁删） |
| `dept`、`post` | 完成（dept 回扁平数组由前端建树；ancestors 自动计算+级联；删除有子部门/用户/岗位引用校验） |
| `user` 其余、`role`、`menu` 管理 | 由并行任务实现中 |

### gen 模块的能力边界（避免误解）

- **导入 / 列表 / 详情 / 修改 / 删除 / 同步数据库**：完整实现，界面可用。
- **代码预览**：真实现 —— 按表结构实时拼出 Java/XML/SQL/JS 文本，
  **不依赖 RuoYi 的 Velocity 模板**（本仓库没有 `vm/` 模板目录）。预览键名形如
  `vm/java/domain.java.vm`，前端取 `.vm` 前最后一段作为页签名。
- **生成到自定义路径**：未实现，返回 501 + 明确提示。要真正落盘需补模板引擎与模板文件。
- `moduleName` 按表名前缀猜（`sys_notice` → `sys`），`packageName` 固定 `cn.CDPersonal`，
  实际使用应在生成配置页里改这两项。

## 9. 已踩过的坑（务必避开）

1. **前端 api 里的 url 少 `/system` 前缀** → 网关匹配不到转发规则，Tomcat 直接 404。
2. **把 `/index` 放进 `web/src/permission.js` 的白名单** → 守卫不调 `generateRoutes()`，菜单永远为空。
3. **顶级菜单 `path` 没有前导斜杠**（库里存的是 `system`）→ 注册为顶级路由时 vue-router 4 抛错，登录后无法跳转。
4. **`getRouters` 只回数据库原始列** → 前端要 `meta.title`/`meta.icon`/`hidden`，缺了会渲染出"有箭头没文字"的空菜单。
5. **`/system/user/getInfo` 包了 `ResponseVO`** → 前端读顶层 `res.user`，包起来就全是 undefined。
6. **树形接口（dept/menu list）自己组装了 children** → 前端用 `handleTree()` 自己建树，后端要回**扁平数组**。
7. **`.ps1` 存成无 BOM 的 UTF-8** → Windows PowerShell 5.1 按 GBK 解码，中文乱码并引发语法错误。
8. **`mvn -pl 单模块`** 解析不了兄弟模块 → 先在 `backend` 跑 `mvn -DskipTests install`。
9. **内置参数判据用 `config_id <= 100`** → RuoYi 建库脚本对 `sys_config` 设了 `AUTO_INCREMENT = 100`，用户新增的第一条参数 id 正好是 100，会被误判成"内置参数"而永远删不掉。要用 `config_type = 'Y'`，id 兜底判据写成 `< 100`。
10. **接口返回的 `createTime` 比库里少 8 小时** → 连接串 `serverTimezone=Asia/Shanghai` 把 DATETIME 读成 `Date` 瞬间值，Jackson 的 `@JsonFormat` 又按默认 UTC 输出（库里 `2026-10-08 02:01:45` → JSON `2026-10-07 18:01:45`）。**这不是 config 独有**，dict 等所有走 `BaseEntity.createTime` 的接口都一样。
    ✅ **已修**：`application.yml` 加了 `spring.jackson.time-zone: GMT+8`，实测接口返回与库内一致。
11. **Redis 键被 JDK 序列化污染** → `common/redis` 的 `RedisConfiguration` 里那个带 `StringRedisSerializer` 的 `redisTemplate` 没生效（`@ConditionalOnMissingBean` 被 Boot 默认的 `RedisTemplate` 抢先），实际写入的键名是 `"\xac\xed\x00\x05t\x00-login_tokens:xxx"`。后果：**在线用户页永远为空**，因为 `keys("login_tokens:*")` 扫不到（键前缀对不上），而且不报任何错。
    ✅ **已修**：`TokenService` 不再用 `RedisService`，改用 `StringRedisTemplate` + Jackson 存 JSON。**新增使用 Redis 的代码请直接用 `StringRedisTemplate`**，不要用 `RedisService`。
12. **网关转发要补 target 的 context-path** → system-service 的 context-path 是 `/system`。
    `/system/**` 原样转发没问题，但 `/auth/**`、`/schedule/**`、`/code/**` 这三类前端 url 不带 `/system`，
    直接拼 `base + requestURI` 会得到 `8000/schedule/...`，落在 context-path 之外 → Tomcat 404。
    正确做法：目标路径不以 `/system` 开头时先补上 `/system`。
13. **MyBatis 嵌套映射会静默返回 null** → `dept_xxx AS dept_deptId` 这种别名在默认 `autoMappingBehavior=PARTIAL` 下**不报错但返回 null**；改用 `@One` 后 `dept_id`→`deptId` 又会被关联属性"吞掉"（`deptId` 也变 null）。
    症状：接口 200、但前端某列空白（如用户列表的"部门"列）。必须用 `@Results` 显式列出标量字段再挂 `@One`。
14. **菜单实体不继承 BaseEntity** → `auth.domain.SysMenu` 是独立类，没有 `setCreateBy/setUpdateBy`，
    菜单管理写库时需自己补这两个字段（已加）。

