import request from '@/utils/request'

/*
 * 字典数据（sys_dict_data）接口，前缀 /system/dict/data。
 *
 * 日常用得最多的是 getDicts(dictType)：utils/dict.js 的 useDict() 拿它把字典转成
 * [{ label, value, elTagType, elTagClass }] 并缓存进 Pinia 的 useDictStore（纯内存，
 * 刷新页面即重建）。所以后台改了字典后，不刷新页面是看不到新值的。
 *
 * 字段约定：status '0'=正常 '1'=停用（新增默认 '0'）；isDefault 'Y'=默认选中（默认 'N'）；
 * listClass 对应 el-tag 的类型（primary/success/info/warning/danger），cssClass 是自定义类名。
 * 字典数据用 dictType 字符串关联字典类型，不是外键，因此别随意改已有类型的 dictType 值。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询字典数据列表
// 管理页用，需要分页；query 通常带 dictType，支持按字典标签/状态过滤。
export function listData(query) {
  return request({
    url: '/system/dict/data/list',
    method: 'get',
    params: query
  })
}

// 查询字典数据详细
export function getData(dictCode) {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'get'
  })
}

// 根据字典类型查询字典数据信息
// 不分页、只返回 status='0' 的记录，按 dictSort 升序；相同 dictSort 的相对顺序不稳定，
// 因此同一类型里不要把两个排序值配成一样。前端 useDict() 的结果会写进 Pinia 缓存。
export function getDicts(dictType) {
  return request({
    url: '/system/dict/data/type/' + dictType,
    method: 'get'
  })
}

// 新增字典数据
// 必须带 dictType，且该类型要在字典类型表里已存在（后端不做外键校验，写错只会查不到）；
// status 缺省 '0'、isDefault 缺省 'N'。
export function addData(data) {
  return request({
    url: '/system/dict/data',
    method: 'post',
    data: data
  })
}

// 修改字典数据
export function updateData(data) {
  return request({
    url: '/system/dict/data',
    method: 'put',
    data: data
  })
}

// 删除字典数据
// 路径变量是 Long[]，多个 dictCode 用逗号拼接；物理删除，删除后使用该字典的下拉会少选项。
export function delData(dictCode) {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'delete'
  })
}
