import * as components from '@element-plus/icons-vue'

/*
 * Element Plus 图标的全局注册插件（main.js 中 app.use(elementIcons)）。
 *
 * 目的：后台大量地方直接用字符串图标名，如 <el-button icon="Search">、<el-icon><Plus /></el-icon>，
 * 逐个 import 太啰嗦，这里一次性把 @element-plus/icons-vue 的全部导出注册成全局组件。
 *
 * 代价：会注册数十个全局组件（体积随图标库增长）；若要瘦身可改为按需 import。
 */
export default {
  install: (app) => {
    // 模块命名空间对象的每个字段就是一个图标组件（Search / Plus / Delete ...）
    for (const key in components) {
      const componentConfig = components[key]
      // 用组件自身的 name 注册，保证 <Search /> 这类短名可直接使用
      app.component(componentConfig.name, componentConfig)
    }
  }
}
