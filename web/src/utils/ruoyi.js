/**
 * 通用js方法封装处理
 * Copyright (c) 2019 ruoyi
 */

/**
 * 使用说明：这里的函数被页面与 api 层广泛复用，其中 resetForm 由 main.js
 * 挂到 globalProperties，页面里以 proxy.resetForm('queryRef') 方式调用，
 * 依赖 this 指向组件实例，直接 import 后普通调用取不到 $refs。
 * 各函数的具体约定见下方各处注释。
 */

// 模式占位符：{y} 年 {m} 月 {d} 日 {h} 时 {i} 分 {s} 秒，另有 {a} 星期；缺省输出 y-m-d h:i:s。
// 字符串入参会把 - 换成 /（iOS/Safari 不接受 "2024-01-01 12:00:00" 这种写法）并去掉毫秒后缀；
// 10 位数字按秒级时间戳 ×1000 处理，匹配不到时返回 null。
// 日期格式化
export function parseTime(time, pattern) {
  if (arguments.length === 0 || !time) {
    return null
  }
  const format = pattern || '{y}-{m}-{d} {h}:{i}:{s}'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if ((typeof time === 'string') && (/^[0-9]+$/.test(time))) {
      time = parseInt(time)
    } else if (typeof time === 'string') {
      time = time.replace(new RegExp(/-/gm), '/').replace('T', ' ').replace(new RegExp(/\.[\d]{3}/gm), '')
    }
    if ((typeof time === 'number') && (time.toString().length === 10)) {
      time = time * 1000
    }
    date = new Date(time)
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  }
  const time_str = format.replace(/{(y|m|d|h|i|s|a)+}/g, (result, key) => {
    let value = formatObj[key]
    // Note: getDay() returns 0 on Sunday
    if (key === 'a') { return ['日', '一', '二', '三', '四', '五', '六'][value] }
    if (result.length > 0 && value < 10) {
      value = '0' + value
    }
    return value || 0
  })
  return time_str
}

// 需以 proxy.resetForm('xxxRef') 调用，this 指向组件实例；resetFields 只重置注册过 prop 的字段
// 表单重置
export function resetForm(refName) {
  if (this.$refs[refName]) {
    this.$refs[refName].resetFields()
  }
}

// 把 el-date-picker 的 [开始, 结束] 塞进 params.params，后端分页接口按 beginTime/endTime 接收；
// 传 propName 可改成 beginXxx/endXxx，最终由 tansParams 序列化成 params[beginTime]=... 形式。
// 添加日期范围
export function addDateRange(params, dateRange, propName) {
  let search = params
  search.params = typeof (search.params) === 'object' && search.params !== null && !Array.isArray(search.params) ? search.params : {}
  dateRange = Array.isArray(dateRange) ? dateRange : []
  if (typeof (propName) === 'undefined') {
    search.params['beginTime'] = dateRange[0]
    search.params['endTime'] = dateRange[1]
  } else {
    search.params['begin' + propName] = dateRange[0]
    search.params['end' + propName] = dateRange[1]
  }
  return search
}

// 按字典回显 label：value 与 dictValue 用 == 比较（后端可能回传数字）；
// 只有 undefined 返回空串，值匹配不到时原样返回 value，避免页面出现空白。
// 回显数据字典
export function selectDictLabel(datas, value) {
  if (value === undefined) {
    return ""
  }
  var actions = []
  Object.keys(datas).some((key) => {
    if (datas[key].value == ('' + value)) {
      actions.push(datas[key].label)
      return true
    }
  })
  if (actions.length === 0) {
    actions.push(value)
  }
  return actions.join('')
}

// 多值回显：value 可为数组或分隔符串，逐项匹配后拼回，末尾去掉一个 separator；
// 匹配不到的值原样保留，避免脏数据把整行变成空白。
// 回显数据字典（字符串、数组）
export function selectDictLabels(datas, value, separator) {
  if (value === undefined || value.length ===0) {
    return ""
  }
  if (Array.isArray(value)) {
    value = value.join(",")
  }
  var actions = []
  var currentSeparator = undefined === separator ? "," : separator
  var temp = value.split(currentSeparator)
  Object.keys(value.split(currentSeparator)).some((val) => {
    var match = false
    Object.keys(datas).some((key) => {
      if (datas[key].value == ('' + temp[val])) {
        actions.push(datas[key].label + currentSeparator)
        match = true
      }
    })
    if (!match) {
      actions.push(temp[val] + currentSeparator)
    }
  })
  return actions.join('').substring(0, actions.join('').length - 1)
}

// 依次替换 %s；占位符比实参多时整体返回空串，避免出现半截文案
// 字符串格式化(%s )
export function sprintf(str) {
  var args = arguments, flag = true, i = 1
  str = str.replace(/%s/g, function () {
    var arg = args[i++]
    if (typeof arg === 'undefined') {
      flag = false
      return ''
    }
    return arg
  })
  return flag ? str : ''
}

// 转换字符串，undefined,null等转化为""
export function parseStrEmpty(str) {
  if (!str || str == "undefined" || str == "null") {
    return ""
  }
  return str
}

// 递归合并：target 的值覆盖 source；target[p] 为 null 或原始值时读 .constructor 会抛错，
// 由 catch 兜底后直接覆盖。
// 数据合并
export function mergeRecursive(source, target) {
  for (var p in target) {
    try {
      if (target[p].constructor == Object) {
        source[p] = mergeRecursive(source[p], target[p])
      } else {
        source[p] = target[p]
      }
    } catch (e) {
      source[p] = target[p]
    }
  }
  return source
}

/**
 * 构造树型结构数据
 * @param {*} data 数据源
 * @param {*} id id字段 默认 'id'
 * @param {*} parentId 父节点字段 默认 'parentId'
 * @param {*} children 孩子节点字段 默认 'children'
 */
// 两遍遍历：先按 id 建索引并补空 children，再按 parentId 挂到父节点，找不到父节点的视为根；
// 会就地修改 data 中每个节点（写入 children），传响应式数组时等于直接产生副作用。
export function handleTree(data, id, parentId, children) {
  let config = {
    id: id || 'id',
    parentId: parentId || 'parentId',
    childrenList: children || 'children'
  }

  var childrenListMap = {}
  var tree = []
  for (let d of data) {
    let id = d[config.id]
    childrenListMap[id] = d
    if (!d[config.childrenList]) {
      d[config.childrenList] = []
    }
  }

  for (let d of data) {
    let parentId = d[config.parentId]
    let parentObj = childrenListMap[parentId]
    if (!parentObj) {
      tree.push(d)
    } else {
      parentObj[config.childrenList].push(d)
    }
  }
  return tree
}

/**
* 参数处理
* @param {*} params  参数
*/
// 拼成 a=1&b=2& 形式：末尾会多一个 &，由调用方截断（见 request.js 的 url.slice(0,-1)）；
// 嵌套对象展开成 prop[key]=value；null、空串、undefined 跳过，但 0 和 false 会保留。
export function tansParams(params) {
  let result = ''
  for (const propName of Object.keys(params)) {
    const value = params[propName]
    var part = encodeURIComponent(propName) + "="
    if (value !== null && value !== "" && typeof (value) !== "undefined") {
      if (typeof value === 'object') {
        for (const key of Object.keys(value)) {
          if (value[key] !== null && value[key] !== "" && typeof (value[key]) !== 'undefined') {
            let params = propName + '[' + key + ']'
            var subPart = encodeURIComponent(params) + "="
            result += subPart + encodeURIComponent(value[key]) + "&"
          }
        }
      } else {
        result += part + encodeURIComponent(value) + "&"
      }
    }
  }
  return result
}

// 处理菜单 path：折叠重复斜杠并去掉末尾斜杠；空值或字符串 'undefined' 原样返回
// 返回项目路径
export function getNormalPath(p) {
  if (p.length === 0 || !p || p == 'undefined') {
    return p
  }
  let res = p.replace('//', '/')
  if (res[res.length - 1] === '/') {
    return res.slice(0, res.length - 1)
  }
  return res
}

// 后端出错时返回的是 application/json，此时不是文件流，下载逻辑要改走错误提示
// 验证是否为blob格式
export function blobValidate(data) {
  return data.type !== 'application/json'
}
