import { titleCase } from '@/utils/index'
import { trigger } from './config'

/**
 * 脚本生成模块：把设计器 schema（conf）编译成 .vue 文件里 <script setup> 的内容。
 * 与 html.js 严格配套，靠变量名约定衔接：html.js 模板里用到的 xxxOptions / xxxProps /
 * xxxAction / xxxfileList / xxxBeforeUpload，都在这里生成同名常量或方法，只改一侧会生成出引用未定义的代码。
 * type 取 'dialog' 时生成弹窗版脚本（defineModel + confirm 事件 + 打开/关闭/取消/确认回调），
 * 否则生成页面版脚本（submitForm / resetForm）。
 */

// 值是要原样写进生成代码的表达式字符串（不是数字），所以 MB/GB 写成连除形式，便于阅读。
// 文件大小设置
const units = {
  KB: '1024',
  MB: '1024 / 1024',
  GB: '1024 / 1024 / 1024',
}
/**
 * @name: 生成js需要的数据
 * @description: 生成js需要的数据
 * @param {*} conf
 * @param {*} type 弹窗或表单
 * @return {*}
 */
export function makeUpJs(conf, type) {
  // 深拷贝：下面 buildOptions 会清空 dynamic 的示例 options、buildProps 会往 props.props 上写键，
  // 直接在画布数据（activeData）上改会污染设计器
  conf = JSON.parse(JSON.stringify(conf))
  // 按用途分桶累积，最后统一拼装：表单数据 / 校验规则 / 选项常量 / 级联 props / 方法 / 上传变量
  const dataList = []
  const ruleList = []
  const optionsList = []
  const propsList = []
  const methodList = []
  const uploadVarList = []

  conf.fields.forEach((el) => {
    buildAttributes(
      el,
      dataList,
      ruleList,
      optionsList,
      methodList,
      propsList,
      uploadVarList
    )
  })

  const script = buildexport(
    conf,
    type,
    dataList.join('\n'),
    ruleList.join('\n'),
    optionsList.join('\n'),
    uploadVarList.join('\n'),
    propsList.join('\n'),
    methodList.join('\n')
  )
  
  return script
}
/**
 * @name: 生成参数
 * @description: 生成参数，包括表单数据表单验证数据，多选选项数据，上传数据等
 * @return {*}
 */
function buildAttributes(
  el,
  dataList,
  ruleList,
  optionsList,
  methodList,
  propsList,
  uploadVarList
){
  buildData(el, dataList)
  buildRules(el, ruleList)

  // 选项类元素：先生成选项常量；dataType 为 dynamic 时再额外生成 get<Xxx>Options() 取数方法
  if (el.options && el.options.length) {
    buildOptions(el, optionsList)
    if (el.dataType === 'dynamic') {
      const model = `${el.vModel}Options`
      const options = titleCase(model)
      buildOptionMethod(`get${options}`, model, methodList)
    }
  }

  if (el.props && el.props.props) {
    buildProps(el, propsList)
  }

  // 上传元素：生成提交地址与文件列表两个 ref，并配套生成 before-upload；
  // 关闭自动上传时再补一个手动提交方法，否则使用者没有触发上传的入口
  if (el.action && el.tag === 'el-upload') {
    uploadVarList.push(
      `
      // 上传请求路径
      const ${el.vModel}Action = ref('${el.action}')
      // 上传文件列表
      const ${el.vModel}fileList =  ref([])`
    )
    methodList.push(buildBeforeUpload(el))
    if (!el['auto-upload']) {
      methodList.push(buildSubmitUpload(el))
    }
  }

  if (el.children) {
    el.children.forEach((el2) => {
      buildAttributes(
        el2,
        dataList,
        ruleList,
        optionsList,
        methodList,
        propsList,
        uploadVarList
      )
    })
  }
}
/**
 * @name: 生成表单数据formData
 * @description: 生成表单数据formData
 * @param {*} conf
 * @param {*} dataList 数据列表
 * @return {*}
 */
function buildData(conf, dataList) {
  // 布局/按钮类元素没有 vModel，不产生 formData 字段
  if (conf.vModel === undefined) return
  let defaultValue
  // 字符串默认值用单引号包裹以保持生成代码可读，数组/布尔/null 等交给 JSON.stringify 序列化
  if (typeof conf.defaultValue === 'string' && !conf.multiple) {
    defaultValue = `'${conf.defaultValue}'`
  } else {
    defaultValue = `${JSON.stringify(conf.defaultValue)}`
  }
  dataList.push(`${conf.vModel}: ${defaultValue},`)
}
/**
 * @name: 生成表单验证数据rule
 * @description: 生成表单验证数据rule
 * @param {*} conf
 * @param {*} ruleList 验证数据列表
 * @return {*}
 */
function buildRules(conf, ruleList) {
  if (conf.vModel === undefined) return
  const rules = []
  if (trigger[conf.tag]) {
    if (conf.required) {
      // 多选（默认值为数组）需额外声明 type: 'array'：数组的必填语义与空字符串不同，
      // 提示语也随之换成「至少选择一个」
      const type = Array.isArray(conf.defaultValue) ? "type: 'array'," : ''
      let message = Array.isArray(conf.defaultValue)
        ? `请至少选择一个${conf.vModel}`
        : conf.placeholder
      if (message === undefined) message = `${conf.label}不能为空`
      rules.push(
        `{ required: true, ${type} message: '${message}', trigger: '${
          trigger[conf.tag]
        }' }`
      )
    }
    // regList 里的 pattern 是字符串形式的正则字面量，会被原样内联进生成代码的 new RegExp(...)
    if (conf.regList && Array.isArray(conf.regList)) {
      conf.regList.forEach((item) => {
        if (item.pattern) {
          rules.push(
            `{ pattern: new RegExp(${item.pattern}), message: '${
              item.message
            }', trigger: '${trigger[conf.tag]}' }`
          )
        }
      })
    }
    ruleList.push(`${conf.vModel}: [${rules.join(',')}],`)
  }
}
/**
 * @name: 生成选项数据
 * @description: 生成选项数据，单选多选下拉等
 * @param {*} conf
 * @param {*} optionsList 选项数据列表
 * @return {*}
 */
function buildOptions(conf, optionsList) {
  if (conf.vModel === undefined) return
  // dynamic 场景清空示例数据，避免把设计器里的假选项写进生成代码，取数由 get<Xxx>Options 负责
  if (conf.dataType === 'dynamic') {
    conf.options = []
  }
  const str = `const ${conf.vModel}Options = ref(${JSON.stringify(conf.options)})`
  optionsList.push(str)
}
/**
 * @name: 生成方法
 * @description: 生成方法
 * @param {*} methodName 方法名
 * @param {*} model
 * @param {*} methodList 方法列表
 * @return {*}
 */
function buildOptionMethod(methodName, model, methodList) {
  const str = `function ${methodName}() {
    // TODO 发起请求获取数据
    ${model}.value
  }`
  methodList.push(str)
}
/**
 * @name: 生成表单组件需要的props设置
 * @description: 生成表单组件需要的props设置，如；级联组件
 * @param {*} conf
 * @param {*} propsList
 * @return {*}
 */
function buildProps(conf, propsList) {
  // dynamic 时把自定义的 value / label / children 键名映射进 props.props，
  // 让 cascader 这类组件能识别非默认字段名的数据源；与默认键名相同则不写入，保留组件默认行为
  if (conf.dataType === 'dynamic') {
    conf.valueKey !== 'value' && (conf.props.props.value = conf.valueKey)
    conf.labelKey !== 'label' && (conf.props.props.label = conf.labelKey)
    conf.childrenKey !== 'children' &&
      (conf.props.props.children = conf.childrenKey)
  }
  const str = `
  // props设置
  const ${conf.vModel}Props = ref(${JSON.stringify(conf.props.props)})`
  propsList.push(str)
}
// 只有配置了 fileSize 或 accept 才生成对应判断；两者都为空时返回空串，即不产出该方法

/**
 * @name: 生成上传组件的相关内容
 * @description: 生成上传组件的相关内容
 * @param {*} conf
 * @return {*}
 */
function buildBeforeUpload(conf) {
  const unitNum = units[conf.sizeUnit]
  let rightSizeCode = ''
  let acceptCode = ''
  const returnList = []
  if (conf.fileSize) {
    rightSizeCode = `let isRightSize = file.size / ${unitNum} < ${conf.fileSize}
    if(!isRightSize){
      proxy.$modal.msgError('文件大小超过 ${conf.fileSize}${conf.sizeUnit}')
    }`
    returnList.push('isRightSize')
  }
  if (conf.accept) {
    acceptCode = `let isAccept = new RegExp('${conf.accept}').test(file.type)
    if(!isAccept){
      proxy.$modal.msgError('应该选择${conf.accept}类型的文件')
    }`
    returnList.push('isAccept')
  }
  const str = `
  /**
   * @name: 上传之前的文件判断
   * @description: 上传之前的文件判断，判断文件大小文件类型等
   * @param {*} file
   * @return {*}
   */  
  function ${conf.vModel}BeforeUpload(file) {
    ${rightSizeCode}
    ${acceptCode}
    return ${returnList.join('&&')}
  }`
  return returnList.length ? str : ''
}
// 手动上传（auto-upload 为 false）时的提交方法：用 this.$refs 取上传组件实例再 submit()。
// 这是 Options API 写法，与其余 setup 风格的生成代码不一致，放进 <script setup> 需改写

/**
 * @name: 生成提交表单方法
 * @description: 生成提交表单方法
 * @param {Object} conf vModel 表单ref
 * @return {*}
 */
function buildSubmitUpload(conf) {
  const str = `function submitUpload() {
    this.$refs['${conf.vModel}'].submit()
  }`
  return str
}
/**
 * @name: 组装js代码
 * @description: 组装js代码方法
 * @return {*}
 */
function buildexport(
  conf,
  type,
  data,
  rules,
  selectOptions,
  uploadVar,
  props,
  methods
) {
  let str = `
    const { proxy } = getCurrentInstance()
    const ${conf.formRef} = ref()
    const data = reactive({
      ${conf.formModel}: {
        ${data}
      },
      ${conf.formRules}: {
        ${rules}
      }
    })

    const {${conf.formModel}, ${conf.formRules}} = toRefs(data)

    ${selectOptions}

    ${uploadVar}

    ${props}

    ${methods}
  `
  
  // 弹窗形态：额外生成 visible 的双向绑定、confirm 事件与打开/关闭/取消/确认四个回调；
  // onClose 里的 resetFields 依赖上面按 conf.formRef 生成的表单 ref 名，改 formRef 要同步这里
  if(type === 'dialog') {
    str += `
      // 弹窗设置
      const dialogVisible = defineModel()
      // 弹窗确认回调
      const emit = defineEmits(['confirm'])
      /**
       * @name: 弹窗打开后执行
       * @description: 弹窗打开后执行方法
       * @return {*}
       */
      function onOpen(){

      }
      /**
       * @name: 弹窗关闭时执行
       * @description: 弹窗关闭方法，重置表单
       * @return {*}
       */
      function onClose(){
        ${conf.formRef}.value.resetFields()
      }
      /**
       * @name: 弹窗取消
       * @description: 弹窗取消方法
       * @return {*}
       */
      function close(){
        dialogVisible.value = false
      }
      /**
       * @name: 弹窗表单提交
       * @description: 弹窗表单提交方法
       * @return {*}
       */
      function handelConfirm(){
        ${conf.formRef}.value.validate((valid) => {
          if (!valid) return
          // TODO 提交表单

          close()
          // 回调父级组件
          emit('confirm')
        })
      }
    `
  // 页面形态：只生成 submitForm / resetForm；submitForm 内只做校验，提交请求留给使用者补
  } else {
    str += `
    /**
     * @name: 表单提交
     * @description: 表单提交方法
     * @return {*}
     */
    function submitForm() {
      ${conf.formRef}.value.validate((valid) => {
        if (!valid) return
        // TODO 提交表单
      })
    }
    /**
     * @name: 表单重置
     * @description: 表单重置方法
     * @return {*}
     */
    function resetForm() {
      ${conf.formRef}.value.resetFields()
    }
    `
  }
  return str
}
