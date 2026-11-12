/* eslint-disable max-len */
import { trigger } from './config'

/**
 * 模板生成模块：把设计器 schema（conf）编译成 .vue 文件里 <template> 部分的字符串。
 * 调用链见 views/tool/build/index.vue：makeUpHtml(conf, type) -> vueTemplate()，与 js.js 的脚本、
 * css.js 的样式一起交给 beautifier。type 只有两种取值：'file'（生成独立页面表单）与 'dialog'（生成弹窗表单）。
 * 与 js.js 通过变量名约定耦合：模板里出现的 xxxOptions / xxxProps / xxxAction / xxxfileList /
 * xxxBeforeUpload，都要靠 js.js 生成同名常量或方法，改一侧必须同步另一侧。
 */

// 生成期共享上下文（非响应式）：makeUpHtml 入口赋值、返回前置空，这样下面的生成函数不必层层透传 conf。
// 因为是模块级变量，只能在同步的生成流程里使用。
let confGlobal
let someSpanIsNot24

/**
 * 给表单套一层 el-dialog，用于 type === 'dialog' 的弹窗形态。
 * 标题是固定占位文案，使用者导出后自行修改；模板里的 dialogVisible 与 onOpen / onClose / close /
 * handelConfirm 都由 js.js 的弹窗分支一并生成，两边的方法名必须保持一致。
 */
export function dialogWrapper(str) {
  return `<el-dialog v-model="dialogVisible"  @open="onOpen" @close="onClose" title="Dialog Titile">
    ${str}
    <template #footer>
      <el-button @click="close">取消</el-button>
	  <el-button type="primary" @click="handelConfirm">确定</el-button>
    </template>
  </el-dialog>`
}

/**
 * 套上 <template> 外壳，并沿用 RuoYi 的页面容器类名 app-container（全局样式里是 20px 内边距）。
 */
export function vueTemplate(str) {
  return `<template>
    <div class="app-container">
      ${str}
    </div>
  </template>`
}

/**
 * 套上 <script setup> 外壳；js.js 生成的是组合式 API 写法，需要 Vue 3.2+ 才支持。
 */
export function vueScript(str) {
  return `<script setup>
    ${str}
  </script>`
}

/**
 * 套上 <style> 外壳。注意 cssStr 为空时仍会输出一个空的 style 块，属于既有的输出格式。
 */
export function cssStyle(cssStr) {
  return `<style>
    ${cssStr}
  </style>`
}

/**
 * 拼出 <el-form> 外壳 + 底部按钮。
 * label-position 与默认值 right 相同时省略，disabled 用 :disabled 绑定布尔值而不是写死的字符串；
 * 只要存在非 24 栅格的字段（someSpanIsNot24），整张表单外面再包一层 el-row，用 gutter 控制列间距。
 */
function buildFormTemplate(conf, child, type) {
  let labelPosition = ''
  if (conf.labelPosition !== 'right') {
    labelPosition = `label-position="${conf.labelPosition}"`
  }
  const disabled = conf.disabled ? `:disabled="${conf.disabled}"` : ''
  let str = `<el-form ref="${conf.formRef}" :model="${conf.formModel}" :rules="${conf.formRules}" size="${conf.size}" ${disabled} label-width="${conf.labelWidth}px" ${labelPosition}>
      ${child}
      ${buildFromBtns(conf, type)}
    </el-form>`
  if (someSpanIsNot24) {
    str = `<el-row :gutter="${conf.gutter}">
        ${str}
      </el-row>`
  }
  return str
}

/**
 * 生成底部「提交 / 重置」按钮：只在表单开启了 formBtns 且是页面形态（type === 'file'）时输出。
 * 弹窗形态的确定/取消按钮在 dialogWrapper 里，这里不重复生成；走栅格时按钮同样包一层 el-col。
 */
function buildFromBtns(conf, type) {
  let str = ''
  if (conf.formBtns && type === 'file') {
    str = `<el-form-item>
          <el-button type="primary" @click="submitForm">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>`
    if (someSpanIsNot24) {
      str = `<el-col :span="24">
          ${str}
        </el-col>`
    }
  }
  return str
}

// 注意这是「全有或全无」策略：只要表单里出现一个非 24 栅格的字段，其余字段也会一起包 el-col，
// 否则 el-row 下 el-col 与裸 el-form-item 混排会破坏栅格对齐。判断依据见入口处的 someSpanIsNot24。

// span不为24的用el-col包裹
function colWrapper(element, str) {
  if (someSpanIsNot24 || element.span !== 24) {
    return `<el-col :span="${element.span}">
      ${str}
    </el-col>`
  }
  return str
}

// 布局渲染器：按元素的 layout 字段分派。
// colFormItem 表示一个表单字段（要输出 prop，与 rules 的 key 对应），rowFormItem 表示行容器（支持嵌套）。
const layouts = {
  colFormItem(element) {
    let labelWidth = ''
    // 只有元素级 label-width 与表单级不同值时才输出，避免每个字段重复一遍相同的属性
    if (element.labelWidth && element.labelWidth !== confGlobal.labelWidth) {
      labelWidth = `label-width="${element.labelWidth}px"`
    }
    // 没有 trigger（即不会生成 rules）的组件才用原生 required 属性兜底，其余交给 rules，避免重复校验
    const required = !trigger[element.tag] && element.required ? 'required' : ''
    const tagDom = tags[element.tag] ? tags[element.tag](element) : null
    let str = `<el-form-item ${labelWidth} label="${element.label}" prop="${element.vModel}" ${required}>
        ${tagDom}
      </el-form-item>`
    str = colWrapper(element, str)
    return str
  },
  rowFormItem(element) {
    // type / justify / align 只在布局类型不是 default 时才有意义，取默认值时不输出以减少噪音
    const type = element.type === 'default' ? '' : `type="${element.type}"`
    const justify = element.type === 'default' ? '' : `justify="${element.justify}"`
    const align = element.type === 'default' ? '' : `align="${element.align}"`
    const gutter = element.gutter ? `gutter="${element.gutter}"` : ''
    // 逐层递归生成子元素，行容器允许多级嵌套
    const children = element.children.map(el => layouts[el.layout](el))
    let str = `<el-row ${type} ${justify} ${align} ${gutter}>
      ${children.join('\n')}
    </el-row>`
    str = colWrapper(element, str)
    return str
  }
}

// 组件生成器表：tag -> 生成函数，返回该组件的标签字符串。
// 用法是「属性值有内容才输出对应片段」，空串片段照样参与拼接，所以生成的模板里会残留多余空格，
// 但 Vue 编译时会被忽略，不影响运行结果。
const tags = {
  'el-button': el => {
    const {
      tag, disabled
    } = attrBuilder(el)
    const type = el.type ? `type="${el.type}"` : ''
    const icon = el.icon ? `icon="${el.icon}"` : ''
    const size = el.size ? `size="${el.size}"` : ''
    let child = buildElButtonChild(el)

    if (child) child = `\n${child}\n` // 换行
    return `<${el.tag} ${type} ${icon} ${size} ${disabled}>${child}</${el.tag}>`
  },
  // 单行文本与多行文本共用该 tag，靠 type 区分；autosize 只在配置了 minRows 时输出。
  // 配了 prepend / append 会生成 Vue 2 风格的 slot="prepend" 写法，放到 Vue 3 项目里要改成 <template #prepend>。
  'el-input': el => {
    const {
      disabled, vModel, clearable, placeholder, width
    } = attrBuilder(el)
    const maxlength = el.maxlength ? `:maxlength="${el.maxlength}"` : ''
    const showWordLimit = el['show-word-limit'] ? 'show-word-limit' : ''
    const readonly = el.readonly ? 'readonly' : ''
    const prefixIcon = el['prefix-icon'] ? `prefix-icon='${el['prefix-icon']}'` : ''
    const suffixIcon = el['suffix-icon'] ? `suffix-icon='${el['suffix-icon']}'` : ''
    const showPassword = el['show-password'] ? 'show-password' : ''
    const type = el.type ? `type="${el.type}"` : ''
    const autosize = el.autosize && el.autosize.minRows
      ? `:autosize="{minRows: ${el.autosize.minRows}, maxRows: ${el.autosize.maxRows}}"`
      : ''
    let child = buildElInputChild(el)

    if (child) child = `\n${child}\n` // 换行
    return `<${el.tag} ${vModel} ${type} ${placeholder} ${maxlength} ${showWordLimit} ${readonly} ${disabled} ${clearable} ${prefixIcon} ${suffixIcon} ${showPassword} ${autosize} ${width}>${child}</${el.tag}>`
  },
  'el-input-number': el => {
    const { disabled, vModel, placeholder } = attrBuilder(el)
    const controlsPosition = el['controls-position'] ? `controls-position=${el['controls-position']}` : ''
    const min = el.min ? `:min='${el.min}'` : ''
    const max = el.max ? `:max='${el.max}'` : ''
    const step = el.step ? `:step='${el.step}'` : ''
    const stepStrictly = el['step-strictly'] ? 'step-strictly' : ''
    const precision = el.precision ? `:precision='${el.precision}'` : ''

    return `<${el.tag} ${vModel} ${placeholder} ${step} ${stepStrictly} ${precision} ${controlsPosition} ${min} ${max} ${disabled}></${el.tag}>`
  },
  // 选项子节点由 buildElSelectChild 生成，v-for 的数据源是 js.js 生成的 xxxOptions
  'el-select': el => {
    const {
      disabled, vModel, clearable, placeholder, width
    } = attrBuilder(el)
    const filterable = el.filterable ? 'filterable' : ''
    const multiple = el.multiple ? 'multiple' : ''
    let child = buildElSelectChild(el)

    if (child) child = `\n${child}\n` // 换行
    return `<${el.tag} ${vModel} ${placeholder} ${disabled} ${multiple} ${filterable} ${clearable} ${width}>${child}</${el.tag}>`
  },
  'el-radio-group': el => {
    const { disabled, vModel } = attrBuilder(el)
    const size = `size="${el.size}"`
    let child = buildElRadioGroupChild(el)

    if (child) child = `\n${child}\n` // 换行
    return `<${el.tag} ${vModel} ${size} ${disabled}>${child}</${el.tag}>`
  },
  // 注意：这里与 buildElRadioGroupChild 的绑定顺序相反（:label 取 item.value、:value 取 item.label），
  // 朴素的复制粘贴很容易生成出「值是文字、显示是数字」的选项，迁移到别的 Element Plus 版本时要按语义核对
  'el-checkbox-group': el => {
    const { disabled, vModel } = attrBuilder(el)
    const size = `size="${el.size}"`
    const min = el.min ? `:min="${el.min}"` : ''
    const max = el.max ? `:max="${el.max}"` : ''
    let child = buildElCheckboxGroupChild(el)

    if (child) child = `\n${child}\n` // 换行
    return `<${el.tag} ${vModel} ${min} ${max} ${size} ${disabled}>${child}</${el.tag}>`
  },
  'el-switch': el => {
    const { disabled, vModel } = attrBuilder(el)
    const activeText = el['active-text'] ? `active-text="${el['active-text']}"` : ''
    const inactiveText = el['inactive-text'] ? `inactive-text="${el['inactive-text']}"` : ''
    const activeColor = el['active-color'] ? `active-color="${el['active-color']}"` : ''
    const inactiveColor = el['inactive-color'] ? `inactive-color="${el['inactive-color']}"` : ''
    const activeValue = el['active-value'] !== true ? `:active-value='${JSON.stringify(el['active-value'])}'` : ''
    const inactiveValue = el['inactive-value'] !== false ? `:inactive-value='${JSON.stringify(el['inactive-value'])}'` : ''

    return `<${el.tag} ${vModel} ${activeText} ${inactiveText} ${activeColor} ${inactiveColor} ${activeValue} ${inactiveValue} ${disabled}></${el.tag}>`
  },
  // options / props 分别引用 xxxOptions、xxxProps，两个常量由 js.js 的 buildOptions / buildProps 生成；
  // show-all-levels 默认 true、separator 默认 '/'，与默认一致时不输出对应属性
  'el-cascader': el => {
    const {
      disabled, vModel, clearable, placeholder, width
    } = attrBuilder(el)
    const options = el.options ? `:options="${el.vModel}Options"` : ''
    const props = el.props ? `:props="${el.vModel}Props"` : ''
    const showAllLevels = el['show-all-levels'] ? '' : ':show-all-levels="false"'
    const filterable = el.filterable ? 'filterable' : ''
    const separator = el.separator === '/' ? '' : `separator="${el.separator}"`

    return `<${el.tag} ${vModel} ${options} ${props} ${width} ${showAllLevels} ${placeholder} ${separator} ${filterable} ${clearable} ${disabled}></${el.tag}>`
  },
  'el-slider': el => {
    const { disabled, vModel } = attrBuilder(el)
    const min = el.min ? `:min='${el.min}'` : ''
    const max = el.max ? `:max='${el.max}'` : ''
    const step = el.step ? `:step='${el.step}'` : ''
    const range = el.range ? 'range' : ''
    const showStops = el['show-stops'] ? `:show-stops="${el['show-stops']}"` : ''

    return `<${el.tag} ${min} ${max} ${step} ${vModel} ${range} ${showStops} ${disabled}></${el.tag}>`
  },
  // 时间选择与时间范围共用该 tag，靠 is-range 区分
  'el-time-picker': el => {
    const {
      disabled, vModel, clearable, placeholder, width
    } = attrBuilder(el)
    const startPlaceholder = el['start-placeholder'] ? `start-placeholder="${el['start-placeholder']}"` : ''
    const endPlaceholder = el['end-placeholder'] ? `end-placeholder="${el['end-placeholder']}"` : ''
    const rangeSeparator = el['range-separator'] ? `range-separator="${el['range-separator']}"` : ''
    const isRange = el['is-range'] ? 'is-range' : ''
    const format = el.format ? `format="${el.format}"` : ''
    const valueFormat = el['value-format'] ? `value-format="${el['value-format']}"` : ''
    const pickerOptions = el['picker-options'] ? `:picker-options='${JSON.stringify(el['picker-options'])}'` : ''

    return `<${el.tag} ${vModel} ${isRange} ${format} ${valueFormat} ${pickerOptions} ${width} ${placeholder} ${startPlaceholder} ${endPlaceholder} ${rangeSeparator} ${clearable} ${disabled}></${el.tag}>`
  },
  // 日期选择与日期范围共用该 tag，靠 type 区分；type 为 date（组件默认值）时省略该属性
  'el-date-picker': el => {
    const {
      disabled, vModel, clearable, placeholder, width
    } = attrBuilder(el)
    const startPlaceholder = el['start-placeholder'] ? `start-placeholder="${el['start-placeholder']}"` : ''
    const endPlaceholder = el['end-placeholder'] ? `end-placeholder="${el['end-placeholder']}"` : ''
    const rangeSeparator = el['range-separator'] ? `range-separator="${el['range-separator']}"` : ''
    const format = el.format ? `format="${el.format}"` : ''
    const valueFormat = el['value-format'] ? `value-format="${el['value-format']}"` : ''
    const type = el.type === 'date' ? '' : `type="${el.type}"`
    const readonly = el.readonly ? 'readonly' : ''

    return `<${el.tag} ${type} ${vModel} ${format} ${valueFormat} ${width} ${placeholder} ${startPlaceholder} ${endPlaceholder} ${rangeSeparator} ${clearable} ${readonly} ${disabled}></${el.tag}>`
  },
  'el-rate': el => {
    const { disabled, vModel } = attrBuilder(el)
    const max = el.max ? `:max='${el.max}'` : ''
    const allowHalf = el['allow-half'] ? 'allow-half' : ''
    const showText = el['show-text'] ? 'show-text' : ''
    const showScore = el['show-score'] ? 'show-score' : ''

    return `<${el.tag} ${vModel} ${allowHalf} ${showText} ${showScore} ${disabled}></${el.tag}>`
  },
  'el-color-picker': el => {
    const { disabled, vModel } = attrBuilder(el)
    const size = `size="${el.size}"`
    const showAlpha = el['show-alpha'] ? 'show-alpha' : ''
    const colorFormat = el['color-format'] ? `color-format="${el['color-format']}"` : ''

    return `<${el.tag} ${vModel} ${size} ${showAlpha} ${colorFormat} ${disabled}></${el.tag}>`
  },
  // 上传组件依赖 js.js 生成的四个变量：提交地址、file-list、before-upload 与组件 ref。
  // 其中 before-upload 是无条件绑定的，而 js.js 只在配置了 fileSize 或 accept 时才生成该方法，
  // 两者都不配时生成的代码会引用一个不存在的函数，需要使用者手工补空实现。
  'el-upload': el => {
    const disabled = el.disabled ? ':disabled=\'true\'' : ''
    const action = el.action ? `:action="${el.vModel}Action"` : ''
    const multiple = el.multiple ? 'multiple' : ''
    const listType = el['list-type'] !== 'text' ? `list-type="${el['list-type']}"` : ''
    const accept = el.accept ? `accept="${el.accept}"` : ''
    const name = el.name !== 'file' ? `name="${el.name}"` : ''
    const autoUpload = el['auto-upload'] === false ? ':auto-upload="false"' : ''
    const beforeUpload = `:before-upload="${el.vModel}BeforeUpload"`
    const fileList = `:file-list="${el.vModel}fileList"`
    const ref = `ref="${el.vModel}"`
    let child = buildElUploadChild(el)

    if (child) child = `\n${child}\n` // 换行
    return `<${el.tag} ${ref} ${fileList} ${action} ${autoUpload} ${multiple} ${beforeUpload} ${listType} ${accept} ${name} ${disabled}>${child}</${el.tag}>`
  }
}

// 拼出各组件共用的属性片段：v-model 统一绑到生成代码里的 formData.字段名。
// width 只判断有没有 style.width，实际永远输出 100%，元素上配置的具体宽度值不会被采用
// （表单字段默认撑满所在栅格）。
function attrBuilder(el) {
  return {
    vModel: `v-model="${confGlobal.formModel}.${el.vModel}"`,
    clearable: el.clearable ? 'clearable' : '',
    placeholder: el.placeholder ? `placeholder="${el.placeholder}"` : '',
    width: el.style && el.style.width ? ':style="{width: \'100%\'}"' : '',
    disabled: el.disabled ? ':disabled=\'true\'' : ''
  }
}

// 下面这组 buildEl*Child 按「元素上对应属性是否有值」决定生成哪些子节点，返回值直接拼进标签内部，
// 因此配置项与子节点的对应关系要看各自的 key，不能凭 tag 猜。
// el-buttin 子级
function buildElButtonChild(conf) {
  const children = []
  if (conf.default) {
    children.push(conf.default)
  }
  return children.join('\n')
}

// el-input innerHTML
function buildElInputChild(conf) {
  const children = []
  if (conf.prepend) {
    children.push(`<template slot="prepend">${conf.prepend}</template>`)
  }
  if (conf.append) {
    children.push(`<template slot="append">${conf.append}</template>`)
  }
  return children.join('\n')
}

function buildElSelectChild(conf) {
  const children = []
  if (conf.options && conf.options.length) {
    children.push(`<el-option v-for="(item, index) in ${conf.vModel}Options" :key="index" :label="item.label" :value="item.value" :disabled="item.disabled"></el-option>`)
  }
  return children.join('\n')
}

function buildElRadioGroupChild(conf) {
  const children = []
  if (conf.options && conf.options.length) {
    const tag = conf.optionType === 'button' ? 'el-radio-button' : 'el-radio'
    const border = conf.border ? 'border' : ''
    children.push(`<${tag} v-for="(item, index) in ${conf.vModel}Options" :key="index" :value="item.value" :disabled="item.disabled" ${border}>{{item.label}}</${tag}>`)
  }
  return children.join('\n')
}

function buildElCheckboxGroupChild(conf) {
  const children = []
  if (conf.options && conf.options.length) {
    const tag = conf.optionType === 'button' ? 'el-checkbox-button' : 'el-checkbox'
    const border = conf.border ? 'border' : ''
    children.push(`<${tag} v-for="(item, index) in ${conf.vModel}Options" :key="index" :label="item.value" :value="item.label" :disabled="item.disabled" ${border} />`)
  }
  return children.join('\n')
}

function buildElUploadChild(conf) {
  const list = []
  if (conf['list-type'] === 'picture-card') list.push('<i class="el-icon-plus"></i>')
  else list.push(`<el-button size="small" type="primary" icon="el-icon-upload">${conf.buttonText}</el-button>`)
  if (conf.showTip) list.push(`<div slot="tip" class="el-upload__tip">只能上传不超过 ${conf.fileSize}${conf.sizeUnit} 的${conf.accept}文件</div>`)
  return list.join('\n')
}

/**
 * 模板生成入口：conf 由 views/tool/build/index.vue 的 AssembleFormData 拼出，含 fields（元素树）与 formConf。
 * someSpanIsNot24 只统计顶层 fields（它决定整张表单是否包 el-row）；嵌套在行容器里的字段，
 * 只有自身 span 不为 24 时才会被 colWrapper 包 el-col。
 * 返回前把模块级 confGlobal 置空，返回的是纯字符串，调用方拿到后不要再依赖生成期上下文。
 * @param {object} conf 表单级配置 + fields 元素树
 * @param {'file'|'dialog'} type 生成形态：页面表单 / 弹窗表单
 * @returns {string} <el-form>（弹窗形态则再包一层 el-dialog）的模板字符串
 */
export function makeUpHtml(conf, type) {
  const htmlList = []
  confGlobal = conf
  someSpanIsNot24 = conf.fields.some(item => item.span !== 24)
  conf.fields.forEach(el => {
    htmlList.push(layouts[el.layout](el))
  })
  const htmlStr = htmlList.join('\n')

  let temp = buildFormTemplate(conf, htmlStr, type)
  if (type === 'dialog') {
    temp = dialogWrapper(temp)
  }
  confGlobal = null
  return temp
}
