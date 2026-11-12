import request from '@/utils/request'

/*
 * 系统参数配置（sys_config）接口，前缀 /system/config。
 *
 * 字段约定：configKey 全局唯一；configType 'Y'=系统内置、'N'=普通参数（新增不传时后端默认 'N'）。
 * 内置参数禁止删除：后端判定条件是 configType='Y' 或 configId<100，满足其一就会拒绝。
 *
 * 刷新缓存接口目前是空实现：后端每次直接查库、没有参数缓存，调用它不会报错但也不会
 * 改变任何东西；不要据此判断「改了值没生效」。
 *
 * 返回结构：列表是 TableDataInfo（res.rows / res.total），其余是 ResponseVO。
 */

// 查询参数列表
export function listConfig(query) {
  return request({
    url: '/system/config/list',
    method: 'get',
    params: query
  })
}

// 查询参数详细
export function getConfig(configId) {
  return request({
    url: '/system/config/' + configId,
    method: 'get'
  })
}

// 根据参数键名查询参数值
// 返回的 value 同时放在 data 与 msg 里（兼容老代码直接读 response.msg）；
// 键不存在时 data 为 null、msg 为空串，不会抛 404。
export function getConfigKey(configKey) {
  return request({
    url: '/system/config/configKey/' + configKey,
    method: 'get'
  })
}

// 新增参数配置
// configKey 重复会返回 500「参数键值已存在」；configType 不传时后端补 'N'。
export function addConfig(data) {
  return request({
    url: '/system/config',
    method: 'post',
    data: data
  })
}

// 修改参数配置
export function updateConfig(data) {
  return request({
    url: '/system/config',
    method: 'put',
    data: data
  })
}

// 删除参数配置
// 路径变量是 Long[]，多个 configId 用逗号拼接；只要命中任意一个内置参数
// （configType='Y' 或 configId<100），整批删除都会被拒绝并返回该参数名。
export function delConfig(configId) {
  return request({
    url: '/system/config/' + configId,
    method: 'delete'
  })
}

// 刷新参数缓存
// 用 DELETE 语义表示“丢弃旧缓存”，但后端当前没有参数缓存，直接返回成功（空实现）。
export function refreshCache() {
  return request({
    url: '/system/config/refreshCache',
    method: 'delete'
  })
}
