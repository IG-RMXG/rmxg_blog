// 布局设置 store：默认值来自 @/settings，用户在「布局设置」面板里的改动会整体存进
// localStorage 的 'layout-setting'（键名与布局组件共享），刷新时用来覆盖默认值。
// 布尔开关都写成 `storageSetting.xxx === undefined ? 默认值 : 用户值`，
// 是为了让 false 这类有效值不被 `||` 误判成「未设置」。
import defaultSettings from '@/settings'
import { useDark, useToggle } from '@vueuse/core'
import { useDynamicTitle } from '@/utils/dynamicTitle'

// vueuse 的 useDark：读写 localStorage 的 'vueuse-color-scheme'，并切换 <html class="dark">。
// 在模块级只初始化一次，因此全局只有这一个深色模式来源，store 里的 isDark 只是它的快照。
const isDark = useDark()
const toggleDark = useToggle(isDark)

const { sideTheme, showSettings, topNav, tagsView, tagsIcon, fixedHeader, sidebarLogo, dynamicTitle, footerVisible, footerContent } = defaultSettings

// 用户上次保存的布局覆盖项。JSON.parse 没有 try/catch，
// 一旦该键被写成非法 JSON，会在模块加载阶段直接抛错导致整站白屏（需清 localStorage 恢复）。
const storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || ''

const useSettingsStore = defineStore(
  'settings',
  {
    state: () => ({
      title: '',
      theme: storageSetting.theme || '#409EFF',
      sideTheme: storageSetting.sideTheme || sideTheme,
      showSettings: showSettings,
      topNav: storageSetting.topNav === undefined ? topNav : storageSetting.topNav,
      tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
      tagsIcon: storageSetting.tagsIcon === undefined ? tagsIcon : storageSetting.tagsIcon,
      fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
      sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
      dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
      footerVisible: storageSetting.footerVisible === undefined ? footerVisible : storageSetting.footerVisible,
      footerContent: footerContent,
      isDark: isDark.value
    }),
    actions: {
      // 修改布局设置
      // 只覆盖 state 里已经存在的键，防止设置面板传入未知字段污染 store；
      // 注意这里不会同步写 localStorage，落盘由布局设置面板自己负责。
      changeSetting(data) {
        const { key, value } = data
        if (this.hasOwnProperty(key)) {
          this[key] = value
        }
      },
      // 设置网页标题
      // 记录标题后交给 useDynamicTitle 重算 document.title（受 dynamicTitle 开关控制），
      // 路由守卫会在每次切换页面时带上 meta.title 调用这里。
      setTitle(title) {
        this.title = title
        useDynamicTitle()
      },
      // 切换暗黑模式
      // 两处状态要一起改：store 里的 isDark 供组件判断，useDark 的 ref 负责落盘并切 <html> 的 class。
      toggleTheme() {
        this.isDark = !this.isDark
        toggleDark()
      }
    }
  })

export default useSettingsStore
