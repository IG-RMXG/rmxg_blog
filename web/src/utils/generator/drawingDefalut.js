/**
 * 表单设计器（views/tool/build）的初始画布数据，本数组即 drawingList 的初值。
 * 只放一个「手机号」示例元素：用户打开设计器就能看到并直接选中一个真实组件，
 * index.vue 也用它初始化 activeData / activeId，避免首屏右侧属性面板为空。
 * 元素字段与 config.js 里的物料保持一致：
 * - layout 决定用哪个布局渲染器（layouts.colFormItem）；
 * - vModel 是生成代码中 formData 的字段名，同时作为 el-form-item 的 prop 与 rules 的 key；
 * - formId 是画布内节点的唯一标识，新增元素时要保证不重复；
 * - regList 是正则校验，pattern 直接写字符串形式的正则字面量，生成时原样内联进 new RegExp(...)。
 */
export default [
  {
    layout: 'colFormItem',
    tagIcon: 'input',
    label: '手机号',
    vModel: 'mobile',
    formId: 6,
    tag: 'el-input',
    placeholder: '请输入手机号',
    defaultValue: '',
    span: 24,
    style: { width: '100%' },
    clearable: true,
    prepend: '',
    append: '',
    'prefix-icon': 'Cellphone',
    'suffix-icon': '',
    maxlength: 11,
    'show-word-limit': true,
    readonly: false,
    disabled: false,
    required: true,
    changeTag: true,
    // 大陆手机号校验；pattern 的字符串形式会被 js.js 原样拼进生成代码
    regList: [{
      pattern: '/^1(3|4|5|7|8|9)\\d{9}$/',
      message: '手机号格式错误'
    }]
  }
]
