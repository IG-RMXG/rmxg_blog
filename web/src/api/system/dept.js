import request from '@/utils/request'

/*
 * 部门管理（sys_dept）接口，前缀 /system/dept。
 *
 * 易踩坑：列表接口返回的是 ResponseVO<List>（扁平数组，前端用 handleTree 自己建树），
 * 不是 TableDataInfo —— 别按 res.rows 取数据，页面会空白。
 *
 * 字段/规则：status '0'=正常 '1'=停用；orderNum 越小越靠前；
 * 删除是软删（del_flag 置 '2'），且存在下级部门或有用户挂在该部门时会被拒绝。
 * 查询一律只看 del_flag='0' 的数据，所以删掉的部门不会再出现在任何下拉里。
 */

// 查询部门列表
// 非分页，按 parentId 组织；query 支持 deptName / status 等条件过滤。
export function listDept(query) {
  return request({
    url: '/system/dept/list',
    method: 'get',
    params: query
  })
}

// 查询部门列表（排除节点）
// 专供「上级部门」下拉使用：排除 deptId 自身及其所有子孙，避免把部门挂到自己下面成环。
export function listDeptExcludeChild(deptId) {
  return request({
    url: '/system/dept/list/exclude/' + deptId,
    method: 'get'
  })
}

// 查询部门详细
export function getDept(deptId) {
  return request({
    url: '/system/dept/' + deptId,
    method: 'get'
  })
}

// 新增部门
// parentId 为 0 表示顶级部门；指定了不存在的上级会被后端拒绝（500「上级部门不存在」）。
export function addDept(data) {
  return request({
    url: '/system/dept',
    method: 'post',
    data: data
  })
}

// 修改部门
// 必带 deptId；后端会校验上级不能是自己或自己的下级，并据此报 500 拦下。
export function updateDept(data) {
  return request({
    url: '/system/dept',
    method: 'put',
    data: data
  })
}

// 删除部门
// 单选删除（不是批量）：存在下级部门或部门下有用户都会被拒绝并给出具体原因；
// 成功时只做软删（del_flag='2'），用户表里的历史 dept_id 仍指向这条记录。
export function delDept(deptId) {
  return request({
    url: '/system/dept/' + deptId,
    method: 'delete'
  })
}