import request from '@/utils/request'

/*
 * 字典类型（sys_dict_type）接口，前缀 /system/dict/type。
 *
 * 字段约定：dictType（如 sys_user_sex）全局唯一；status '0'=正常 '1'=停用，新增默认 '0'。
 *
 * 两个联动规则（都在后端事务里做）：
 *   - 修改 dictType 的值时，会顺带把 sys_dict_data 里同名字段的冗余 dict_type 一起改掉，
 *     否则历史字典数据会失联；
 *   - 删除前会检查该类型下是否还有字典数据，有就报「已分配，不能删除」。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询字典类型列表
export function listType(query) {
  return request({
    url: '/system/dict/type/list',
    method: 'get',
    params: query
  })
}

// 查询字典类型详细
export function getType(dictId) {
  return request({
    url: '/system/dict/type/' + dictId,
    method: 'get'
  })
}

// 新增字典类型
// dictType 重复会返回 500「字典类型已存在」；status 不传由后端补 '0'。
export function addType(data) {
  return request({
    url: '/system/dict/type',
    method: 'post',
    data: data
  })
}

// 修改字典类型
// 改 dictType 会同步刷新字典数据里的冗余列，属于跨表写；返回成功不代表前端缓存已失效。
export function updateType(data) {
  return request({
    url: '/system/dict/type',
    method: 'put',
    data: data
  })
}

// 删除字典类型
// 路径变量是 Long[]，多个 dictId 用逗号拼接；任意一个类型下还有字典数据，整批都会失败。
export function delType(dictId) {
  return request({
    url: '/system/dict/type/' + dictId,
    method: 'delete'
  })
}

// 刷新字典缓存
// 后端没有字典缓存，这个接口是空实现直接返回成功；真正生效的一步是调用方随后执行的
// useDictStore().cleanDict()（清 Pinia 内存缓存），只调接口不刷新页面不会看到变化。
export function refreshCache() {
  return request({
    url: '/system/dict/type/refreshCache',
    method: 'delete'
  })
}

// 获取字典选择框列表
// 只返回 status='0' 的类型（停用的类型不会出现在下拉里），不分页、无查询条件。
export function optionselect() {
  return request({
    url: '/system/dict/type/optionselect',
    method: 'get'
  })
}
