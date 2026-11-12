/*
 * 后台布局的「出厂默认值」。store/modules/settings.js 初始化时读取本文件，
 * 再把 localStorage 里 'layout-setting'（用户在「布局设置」面板保存的结果）逐项覆盖上来。
 *
 * 因此修改下面的值只影响「从未自定义过布局」的浏览器；用户已经保存过的开关以 storage 为准
 * （store 用 === undefined 判断，避免 false 被当成未设置）。
 * 例外：showSettings 与 footerContent 不参与 storage 覆盖，改这里即全局生效。
 *
 * 另：title 是构建期从 VITE_APP_TITLE 读到的静态值，实际 document.title 由
 * settings store 的 setTitle（配合 utils/dynamicTitle）在路由切换时按 meta.title 重写。
 */
export default {
  /**
   * 网页标题
   */
  title: import.meta.env.VITE_APP_TITLE,

  /**
   * 侧边栏主题 深色主题theme-dark，浅色主题theme-light
   */
  sideTheme: 'theme-dark',

  /**
   * 是否系统布局配置
   */
  showSettings: true,

  /**
   * 是否显示顶部导航
   */
  topNav: false,

  /**
   * 是否显示 tagsView
   */
  tagsView: true,
  
  /**
   * 显示页签图标
   */
  tagsIcon: false,

  /**
   * 是否固定头部
   */
  fixedHeader: false,

  /**
   * 是否显示logo
   */
  sidebarLogo: true,

  /**
   * 是否显示动态标题
   */
  dynamicTitle: false,

  /**
   * 是否显示底部版权
   */
  footerVisible: false,

  /**
   * 底部版权文本内容
   */
  footerContent: 'Copyright © 2018-2025 RuoYi. All Rights Reserved.'
}

