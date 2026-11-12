import request from '@/utils/request'

/*
 * 代码生成（gen_table / gen_table_column 配置）接口。
 *
 * 前缀是 /code/**（不是 /system）：网关会补上 system-service 的 context-path /system 再转发，
 * 所以这里写 /system/code/... 会 404。
 *
 * 能力边界：除「生成到自定义路径」(genCode) 外都已实现；预览是按库表结构实时拼代码文本，
 * 不依赖 vm 模板文件，所以预览能看、genCode 会返回 501「缺少 vm 模板」。
 *
 * 返回结构：list / listDbTable 是 TableDataInfo（res.rows / res.total），
 * 其余是 ResponseVO，其中 previewTable 的 data 是 { "vm/java/domain.java.vm": 代码文本, ... }。
 */

// 查询生成表数据
// 只返回已导入 gen_table 的表（分页），未导入的表用 listDbTable 查。
export function listTable(query) {
  return request({
    url: '/code/gen/list',
    method: 'get',
    params: query
  })
}
// 查询db数据库列表
// 扫当前 schema 的 information_schema 得到「可以导入」的表，支持 tableName / tableComment 过滤。
export function listDbTable(query) {
  return request({
    url: '/code/gen/db/list',
    method: 'get',
    params: query
  })
}

// 查询表详细信息
// 返回 gen_table 配置 + columns（字段级配置），编辑页靠这份数据渲染字段表格；
// tableId 不存在时返回业务码 404「表信息不存在」，不是空对象。
export function getGenTable(tableId) {
  return request({
    url: '/code/gen/' + tableId,
    method: 'get'
  })
}

// 修改代码生成信息
// 提交整张表配置（含 columns 的查询方式/显示类型/字典等），后端不校验 tableId 是否存在，
// 传错也会返回成功 —— 保存后请重新拉一次详情确认字段配置真的落库了。
export function updateGenTable(data) {
  return request({
    url: '/code/gen',
    method: 'put',
    data: data
  })
}

// 导入表
// 参数走 query（params），字段名是 tables，值为逗号分隔的表名（如 "sys_user,sys_role"）；
// 已导入过的表会被跳过，全部被跳过时才返回 500「所选表已导入或不存在」。
// 导入会按库表结构生成 gen_table_column 初始配置。
export function importTable(data) {
  return request({
    url: '/code/gen/importTable',
    method: 'post',
    params: data
  })
}

// 预览生成代码
// data 是「虚拟文件路径 -> 代码文本」的有序 Map（vm/java/domain.java.vm 等），顺序即展示顺序；
// 代码是按当前库表结构实时拼的，改完字段配置重新打开预览即可，不需要先生成文件。
export function previewTable(tableId) {
  return request({
    url: '/code/gen/preview/' + tableId,
    method: 'get'
  })
}

// 删除表数据
// 只删 gen_table / gen_table_column 里的生成配置，不会 DROP 数据库表；
// 路径变量是 Long[]，多个 tableId 用逗号拼接。
export function delTable(tableId) {
  return request({
    url: '/code/gen/' + tableId,
    method: 'delete'
  })
}

// 生成代码（自定义路径）
// 当前后端未实现（缺少 vm 模板），固定返回 501 + 提示文案，页面会弹「系统接口异常」；
// 需要落盘生成时请改后端，或先用 previewTable 预览后手工复制。
export function genCode(tableName) {
  return request({
    url: '/code/gen/genCode/' + tableName,
    method: 'get'
  })
}

// 同步数据库
// 先删除该表在 gen_table_column 里的全部列配置，再按当前库表结构重建 ——
// 字段的查询方式、显示类型、字典类型等手工调整都会丢，同步前请先确认。
// tableName 不是已导入的表时后端静默返回成功，不会有任何变化。
export function synchDb(tableName) {
  return request({
    url: '/code/gen/synchDb/' + tableName,
    method: 'get'
  })
}
