// 应用外壳的 UI 状态：侧边栏展开/隐藏、设备类型、全局组件尺寸。
// 侧边栏开关与 size 持久化在 Cookie（sidebarStatus 存 '1'/'0'，size 存 'default' 等），
// 刷新后读回；device 只存内存，由响应式布局在窗口宽度变化时改写。
import Cookies from 'js-cookie'

const useAppStore = defineStore(
  'app',
  {
    // opened 的初值：Cookie 里的 '1'/'0' 经 !!+ 转成布尔，取不到（首次访问）默认展开。
    // hide 为 true 表示侧边栏被强制隐藏（全屏看板、移动端等），此时折叠按钮不再生效。
    state: () => ({
      sidebar: {
        opened: Cookies.get('sidebarStatus') ? !!+Cookies.get('sidebarStatus') : true,
        withoutAnimation: false,
        hide: false
      },
      device: 'desktop',
      size: Cookies.get('size') || 'default'
    }),
    actions: {
      // 折叠/展开侧边栏并同步 Cookie；sideBar 已被强制隐藏（hide=true）时忽略这次点击。
      // withoutAnimation 原样传给布局组件，用来跳过过渡动画。
      toggleSideBar(withoutAnimation) {
        if (this.sidebar.hide) {
          return false
        }
        this.sidebar.opened = !this.sidebar.opened
        this.sidebar.withoutAnimation = withoutAnimation
        if (this.sidebar.opened) {
          Cookies.set('sidebarStatus', 1)
        } else {
          Cookies.set('sidebarStatus', 0)
        }
      },
      // 强制收起侧边栏（移动端切换路由后自动调用），同时把 Cookie 置为关闭。
      // 与 toggleSideBar 的区别是不做取反，重复调用结果一致。
      closeSideBar({ withoutAnimation }) {
        Cookies.set('sidebarStatus', 0)
        this.sidebar.opened = false
        this.sidebar.withoutAnimation = withoutAnimation
      },
      toggleDevice(device) {
        this.device = device
      },
      // 设置全局组件尺寸（Element Plus 的 size：large / default / small）并写入 Cookie 持久化，
      // 布局设置面板调用，影响全站表单与按钮大小。
      setSize(size) {
        this.size = size
        Cookies.set('size', size)
      },
      toggleSideBarHide(status) {
        this.sidebar.hide = status
      }
    }
  })

export default useAppStore
