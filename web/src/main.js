/*
 * 应用入口：创建 Vue 3 实例、装配全局能力，最后挂载到 index.html 的 #app。
 *
 * 装配顺序存在依赖，调整前先确认：
 *   1. import 阶段就会执行模块顶层副作用 —— './permission' 在被导入时就注册了全局路由守卫，
 *      且依赖 router 单例，所以它必须排在 router 导入之后；
 *   2. Element Plus 必须先 use，再把 ElDialog 覆盖成自定义包装组件，否则同名注册会被官方覆盖；
 *   3. mount 放最后：指令、全局方法、全局组件全部注册完再渲染首屏，避免首屏用的组件还没注册。
 */
import { createApp } from 'vue'

import Cookies from 'js-cookie'

// Element Plus 全量引入 + 中文语言包：组件/指令不逐个按需注册，视图里可直接写 el-* 标签。
// dark/css-vars.css 是暗色主题的 CSS 变量表，必须显式引入，否则 <html class="dark">
// 时组件配色不会跟着切换（深色模式开关由 settings store 的 useDark 控制）。
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import locale from 'element-plus/es/locale/lang/zh-cn'

import '@/assets/styles/index.scss' // global css

import App from './App'
import store from './store'
import router from './router'
import directive from './directive' // directive

// 注册指令
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

// svg图标
import 'virtual:svg-icons-register'
import SvgIcon from '@/components/SvgIcon'
import elementIcons from '@/components/SvgIcon/svgicon'

import './permission' // permission control

import { useDict } from '@/utils/dict'
import { getConfigKey } from "@/api/system/config"
import { parseTime, resetForm, addDateRange, handleTree, selectDictLabel, selectDictLabels } from '@/utils/ruoyi'

// 分页组件
import Pagination from '@/components/Pagination'
// 自定义表格工具组件
import RightToolbar from '@/components/RightToolbar'
// 富文本组件
import Editor from "@/components/Editor"
// 文件上传组件
import FileUpload from "@/components/FileUpload"
// 图片上传组件
import ImageUpload from "@/components/ImageUpload"
// 图片预览组件
import ImagePreview from "@/components/ImagePreview"
// 字典标签组件
import DictTag from '@/components/DictTag'
// 弹窗包装组件（带最大化/还原 + 整页布局适配）
import AppDialog from '@/components/Dialog'

const app = createApp(App)

// 这些工具函数挂在 globalProperties 上，模板表达式里可直接调用（如 {{ parseTime(row.createTime) }}）；
// <script setup> 内部没有 this，仍需按需 import，两处指向同一份实现，不要另行复制一份。
// 全局方法挂载
app.config.globalProperties.useDict = useDict
app.config.globalProperties.download = download
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.handleTree = handleTree
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.getConfigKey = getConfigKey
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels

// 全局组件挂载
app.component('DictTag', DictTag)
app.component('Pagination', Pagination)
app.component('FileUpload', FileUpload)
app.component('ImageUpload', ImageUpload)
app.component('ImagePreview', ImagePreview)
app.component('RightToolbar', RightToolbar)
app.component('Editor', Editor)

app.use(router)
app.use(store)
app.use(plugins)
app.use(elementIcons)
app.component('svg-icon', SvgIcon)

directive(app)

// 使用element-plus 并且设置全局的大小
app.use(ElementPlus, {
  locale: locale,
  // 支持 large、default、small
  size: Cookies.get('size') || 'default'
})

/*
 * 全局覆盖 el-dialog：换成带「最大化 / 还原」按钮的包装组件。
 *
 * 放在 app.use(ElementPlus) 之后，用同名注册覆盖掉 Element Plus 自己的注册，
 * 因此全站 29 处 <el-dialog>（视图里没有本地 import ElDialog）都会自动生效，无需逐个改。
 * 包装组件对 props / 事件 / 插槽完全透传，并支持 :maximizable="false" 单独关掉按钮。
 */
app.component('ElDialog', AppDialog)

// 挂载点 #app 由 index.html 提供。mount 会先清空容器原有子节点，
// 因此首屏那张 #loader-wrapper 遮罩会在挂载瞬间被移出，无需再手动补 .loaded 类。
app.mount('#app')
