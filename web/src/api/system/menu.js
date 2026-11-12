import request from '@/utils/request'

/*
 * 菜单管理（sys_menu）接口，前缀 /system/menu。
 *
 * 易踩坑：list 返回的是 ResponseVO<List>（扁平数组，前端 handleTree 自己建树），
 * 不是 TableDataInfo —— 按 res.rows 取会得到 undefined。
 *
 * 字段约定：menuType 'M'=目录 'C'=菜单 'F'=按钮；visible '0'=显示 '1'=隐藏；
 * status '0'=正常 '1'=停用；isFrame '0'=是外链 '1'=否；isCache '0'=缓存 '1'=不缓存。
 * 新增/修改时后端会补齐 parentId=0、orderNum='0'、isFrame='1'、isCache='0'、visible='0'、status='0'。
 *
 * 约束：同一父级下菜单名不能重复；有子菜单或已分配给角色时不允许删除。
 *
 * 特例：treeselect / roleMenuTreeselect 路径在 /menu 下，但由 SysRoleController 实现，
 * 响应顶层直接是 menus / checkedKeys（另有 data 镜像），不是标准的 { code, msg, data }。
 */

// 查询菜单列表
// 扁平数组，支持 menuName 模糊、visible、status 过滤。
export function listMenu(query) {
  return request({
    url: '/system/menu/list',
    method: 'get',
    params: query
  })
}

// 查询菜单详细
export function getMenu(menuId) {
  return request({
    url: '/system/menu/' + menuId,
    method: 'get'
  })
}

// 查询菜单下拉树结构
// 返回完整菜单树（含 F 类型按钮节点），常用于给菜单选择父级；不涉及角色，所以不需要 roleId。
export function treeselect() {
  return request({
    url: '/system/menu/treeselect',
    method: 'get'
  })
}

// 根据角色ID查询菜单下拉树结构
// 角色编辑页用：response.menus 是完整菜单树，response.checkedKeys 是该角色已勾选的 menuId 数组。
export function roleMenuTreeselect(roleId) {
  return request({
    url: '/system/menu/roleMenuTreeselect/' + roleId,
    method: 'get'
  })
}

// 新增菜单
// 同一父级（parentId 相同）下 menuName 不能重复；perms 是权限标识（如 system:user:list），
// 目前只用于前端 v-hasPermi 控制按钮显隐 —— 后端接口没有按 perms 做注解鉴权，
// 真正的越权拦截靠各接口自己判断 isAdmin / 数据归属，所以写错 perms 不会报错，只是按钮不显示。
export function addMenu(data) {
  return request({
    url: '/system/menu',
    method: 'post',
    data: data
  })
}

// 修改菜单
// 必带 menuId；后端会拦下「上级菜单选自己」，但不会拦「挂到自己的子孙下」，
// 父级下拉请用 treeselect 的结果并自行排除当前节点的子树。
export function updateMenu(data) {
  return request({
    url: '/system/menu',
    method: 'put',
    data: data
  })
}

// 删除菜单
// 单选删除：有子菜单报「存在子菜单,不允许删除」，已被角色引用报「菜单已分配,不允许删除」；
// 通过校验后会先清 sys_role_menu 关联再删菜单（同一事务）。
export function delMenu(menuId) {
  return request({
    url: '/system/menu/' + menuId,
    method: 'delete'
  })
}