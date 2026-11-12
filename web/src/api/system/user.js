import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/*
 * 用户管理（sys_user）接口，前缀 /system/user。
 *
 * 字段约定：status '0'=正常 '1'=停用；userName 全局唯一，手机号、邮箱也各自唯一。
 * 约束：userId=1 是超级管理员，禁止删除；删除是软删（del_flag='2'），同时清角色/岗位关联；
 * 新增不传密码时后端用默认 123456（BCrypt 存储），重置密码传空也会回落成 123456。
 * 修改用户时后端不会更新 user_name 和 password，改密码只能走 resetUserPwd / updateUserPwd。
 *
 * 返回值不是同一种壳，页面取数时要分清：
 *   - getUser() 是平铺的 UserDetailVO（user、roles、posts、roleIds、postIds 直接在顶层）；
 *   - getUserProfile() 顶层是 data（当前用户）+ roleGroup + postGroup（逗号拼接的字符串）；
 *   - uploadAvatar() 顶层是 imgUrl（不是 url），要配合 /profile/avatar 静态映射使用；
 *   - getAuthRole() 顶层是 user + roles（每个角色带 flag 表示该用户是否已拥有）；
 *   - 列表是 TableDataInfo（res.rows / res.total），其余写操作是 ResponseVO。
 */

// 查询用户列表
// 分页，支持 userName / phonenumber 模糊、status 精确、deptId（包含其子部门）。
export function listUser(query) {
  return request({
    url: '/system/user/list',
    method: 'get',
    params: query
  })
}

// 查询用户详细
// 新增页会不传 userId 调用，parseStrEmpty 把 undefined/null 变成空串，请求落到 /system/user/
// （带尾斜杠），后端用同一方法返回「可选的岗位/角色 + 空用户」，所以新增页也能复用该函数。
// 编辑页传 userId 时额外返回已选的 roleIds / postIds 供回显。
export function getUser(userId) {
  return request({
    url: '/system/user/' + parseStrEmpty(userId),
    method: 'get'
  })
}

// 新增用户
// data 里的 roleIds / postIds 会写入两张关联表；userName、手机号、邮箱任一重复都会返回 500。
export function addUser(data) {
  return request({
    url: '/system/user',
    method: 'post',
    data: data
  })
}

// 修改用户
// 必带 userId；roleIds / postIds 只要有值就是「整体覆盖」（先清后插），空数组等于清空，
// 不传则保持原关联不动。
export function updateUser(data) {
  return request({
    url: '/system/user',
    method: 'put',
    data: data
  })
}

// 删除用户
// 路径变量是 Long[]，多个 userId 用逗号拼接；包含 userId=1 时整批失败（超级管理员不可删）。
export function delUser(userId) {
  return request({
    url: '/system/user/' + userId,
    method: 'delete'
  })
}

// 用户密码重置
// 管理员给他人重置：不需要旧密码，password 为空时后端回落成默认 123456，入库前统一 BCrypt。
export function resetUserPwd(userId, password) {
  const data = {
    userId,
    password
  }
  return request({
    url: '/system/user/resetPwd',
    method: 'put',
    data: data
  })
}

// 用户状态修改
// status：'0'=正常 '1'=停用。停用只改库，不会主动踢掉该用户已登录的会话（token 到期才失效）。
export function changeUserStatus(userId, status) {
  const data = {
    userId,
    status
  }
  return request({
    url: '/system/user/changeStatus',
    method: 'put',
    data: data
  })
}

// 查询用户个人信息
// 只能查当前登录人（userId 由后端从 token 取，不接受参数），顶层返回 data / roleGroup / postGroup。
export function getUserProfile() {
  return request({
    url: '/system/user/profile',
    method: 'get'
  })
}

// 修改用户个人信息
// 后端只取 nickName / phonenumber / email / sex 四项，userId 强制为当前登录人，
// 传 userName、deptId、status 都不会生效（防止越权改自己的部门/状态）。
export function updateUserProfile(data) {
  return request({
    url: '/system/user/profile',
    method: 'put',
    data: data
  })
}

// 用户密码重置
// 与上面 resetUserPwd 的区别：这里是用户自己改密码，后端会校验 oldPassword，
// 旧密码错误会以 500 + 文案返回；库中历史明文密码也能通过校验（兼容老数据）。
export function updateUserPwd(oldPassword, newPassword) {
  const data = {
    oldPassword,
    newPassword
  }
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    data: data
  })
}

// 用户头像上传
// data 是 FormData，文件字段名必须叫 avatarfile；成功后读 response.imgUrl 并回写 store.avatar。
// 扩展名不在白名单内会被后端统一按 png 落盘，不会报错。
export function uploadAvatar(data) {
  return request({
    url: '/system/user/profile/avatar',
    method: 'post',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: data
  })
}

// 查询授权角色
// 顶层返回 user + roles：roles 是全量角色，每项的 flag 标记该用户当前是否已拥有，
// 前端据此直接渲染勾选状态，不需要再拉一次角色列表。
export function getAuthRole(userId) {
  return request({
    url: '/system/user/authRole/' + userId,
    method: 'get'
  })
}

// 保存授权角色
// 走 query（params）而不是 body：{userId, roleIds}，roleIds 必须是逗号拼接字符串（如 "2,3"），
// 传空串表示清空该用户的全部角色；数组会被序列化成 roleIds[]=2&roleIds[]=3，后端解析不到。
export function updateAuthRole(data) {
  return request({
    url: '/system/user/authRole',
    method: 'put',
    params: data
  })
}

// 查询部门下拉树结构
// 用户表单里选部门用；返回已嵌套的树（不是扁平数组，不需要 handleTree）。
export function deptTreeSelect() {
  return request({
    url: '/system/user/deptTree',
    method: 'get'
  })
}
