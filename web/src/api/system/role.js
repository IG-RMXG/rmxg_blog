import request from '@/utils/request'

/*
 * 角色管理（sys_role）接口，前缀 /system/role。
 *
 * 数据权限 dataScope：1=全部 2=自定义（配合 deptIds 选部门）3=本部门 4=本部门及以下 5=仅本人。
 * 切回 '1' 时后端会清空 sys_role_dept，所以不需要再传 deptIds；只有 '2' 才用得上部门树。
 *
 * 字段约定：roleName 与 roleKey（权限字符，如 admin）都要求全局唯一；status '0'=正常 '1'=停用。
 * 约束：roleId=1 是超级管理员角色，禁止删除；已分配给用户的角色也不能删；删除是软删（del_flag='2'）。
 *
 * 参数位置别混（混了就是 400/参数为空）：
 *   - authUserCancel 走 JSON body {userId, roleId}；
 *   - authUserCancelAll / authUserSelectAll 走 query，userIds 是逗号拼接的字符串。
 * deptTreeSelect 的响应顶层直接是 depts / data / checkedKeys，不是数组包在 data 里。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询角色列表
// 分页，支持 roleName / roleKey（模糊）与 status 过滤。
export function listRole(query) {
  return request({
    url: '/system/role/list',
    method: 'get',
    params: query
  })
}

// 查询角色详细
export function getRole(roleId) {
  return request({
    url: '/system/role/' + roleId,
    method: 'get'
  })
}

// 新增角色
// roleName 或 roleKey 重复都会返回 500；menuIds 会在后端一并写入 sys_role_menu。
export function addRole(data) {
  return request({
    url: '/system/role',
    method: 'post',
    data: data
  })
}

// 修改角色
// 必带 roleId；menuIds 是整体覆盖（先清后插），不传会把该角色的菜单授权清空。
export function updateRole(data) {
  return request({
    url: '/system/role',
    method: 'put',
    data: data
  })
}

// 角色数据权限
// data {roleId, dataScope, deptIds}；dataScope='1' 时 deptIds 会被忽略并清空原有部门授权。
export function dataScope(data) {
  return request({
    url: '/system/role/dataScope',
    method: 'put',
    data: data
  })
}

// 角色状态修改
// status：'0'=正常 '1'=停用。停用不会让已登录会话失效，只有下次 getInfo 时角色标识才不再返回；
// 权限标识查询没有按角色状态过滤，停用后可能仍保留旧权限（见 SysUserMapper.selectPermsByUserId）。
export function changeRoleStatus(roleId, status) {
  const data = {
    roleId,
    status
  }
  return request({
    url: '/system/role/changeStatus',
    method: 'put',
    data: data
  })
}

// 删除角色
// 路径变量是 Long[]，多个 roleId 用逗号拼接；roleId=1（超级管理员）直接拒绝，
// 任意一个角色已分配用户也会整批失败。删除为软删，同时清掉角色-菜单/部门/用户关联。
export function delRole(roleId) {
  return request({
    url: '/system/role/' + roleId,
    method: 'delete'
  })
}

// 查询角色已授权用户列表
// 分页；query 里必须带 roleId，另外可带 userName / phonenumber 过滤。
export function allocatedUserList(query) {
  return request({
    url: '/system/role/authUser/allocatedList',
    method: 'get',
    params: query
  })
}

// 查询角色未授权用户列表
// 分页；用于「添加用户」弹窗，query 同样必须带 roleId。
export function unallocatedUserList(query) {
  return request({
    url: '/system/role/authUser/unallocatedList',
    method: 'get',
    params: query
  })
}

// 取消用户授权角色
// 单个取消，走 JSON body {userId, roleId}（不是 params，改了后端会读不到参数）。
export function authUserCancel(data) {
  return request({
    url: '/system/role/authUser/cancel',
    method: 'put',
    data: data
  })
}

// 批量取消用户授权角色
// 走 query（params），data 形如 {roleId, userIds}，userIds 必须是逗号拼接的字符串（如 "2,3,4"），
// 传数组会被序列化成 userIds[]=2&userIds[]=3，后端按字符串解析会失败。
export function authUserCancelAll(data) {
  return request({
    url: '/system/role/authUser/cancelAll',
    method: 'put',
    params: data
  })
}

// 授权用户选择
// 走 query（params），data 形如 {roleId, userIds}，userIds 同样是逗号拼接字符串；
// 已授权的用户重复提交不会重复插入（后端按关联表主键去重）。
export function authUserSelectAll(data) {
  return request({
    url: '/system/role/authUser/selectAll',
    method: 'put',
    params: data
  })
}

// 根据角色ID查询部门树结构
// 响应顶层是 { depts, data, checkedKeys }：depts 与 data 是同一棵部门树，checkedKeys 是该角色
// 已勾选的 deptId（只有 dataScope='2' 自定义权限时才非空）。按 response.data 取树、按
// response.checkedKeys 回显勾选都能工作，两种写法在仓库里都存在。
export function deptTreeSelect(roleId) {
  return request({
    url: '/system/role/deptTree/' + roleId,
    method: 'get'
  })
}
