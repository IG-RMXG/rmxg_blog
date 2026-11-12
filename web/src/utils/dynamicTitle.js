/**
 * 浏览器标签页标题。
 *
 * 标题由 settings store 驱动：布局组件在路由切换后通过 setTitle 写入当前菜单名，
 * 这里决定是否再拼上系统名。dynamicTitle 开关来自默认配置与 localStorage 的
 * layout-setting，关闭时固定显示系统名（见 @/store/modules/settings.js）。
 */
import defaultSettings from '@/settings'
import useSettingsStore from '@/store/modules/settings'

/**
 * 动态修改标题
 */
export function useDynamicTitle() {
  const settingsStore = useSettingsStore()
  // 开关与标题都取自 settings store；title 尚未写入时，标题只剩分隔符与系统名
  if (settingsStore.dynamicTitle) {
    document.title = settingsStore.title + ' - ' + defaultSettings.title
  } else {
    document.title = defaultSettings.title
  }
}