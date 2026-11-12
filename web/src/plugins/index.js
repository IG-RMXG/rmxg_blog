// 全局插件装配入口。main.js 里通过 app.use(plugins) 触发 installPlugins()，
// 把工具方法挂到 app.config.globalProperties 上，模板里可直接写 $modal.confirm(...)；
// <script setup> 中没有 this，需用 getCurrentInstance().proxy.$modal 访问。
// 这些对象本身不带响应式状态，只负责调用 Element Plus、浏览器存储与路由。
import tab from './tab'
import auth from './auth'
import cache from './cache'
import modal from './modal'
import download from './download'

/**
 * 注册全部全局工具对象。
 * 本函数就是 app.use(plugins) 的 install 入口；与常见插件的区别是不做依赖注入或响应式扩展，
 * 只把 5 个单例挂到 globalProperties，省去每个页面重复 import。
 * @param {import('vue').App} app Vue 应用实例
 */
export default function installPlugins(app){
  // 页签操作
  app.config.globalProperties.$tab = tab
  // 认证对象
  app.config.globalProperties.$auth = auth
  // 缓存对象
  app.config.globalProperties.$cache = cache
  // 模态框对象
  app.config.globalProperties.$modal = modal
  // 下载文件
  app.config.globalProperties.$download = download
}
