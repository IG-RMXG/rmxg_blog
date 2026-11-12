<template>
  <el-drawer v-model="showSettings" :withHeader="false" :lock-scroll="false" direction="rtl" size="300px">
    <!-- 主题风格：深色 / 浅色侧边栏二选一，只影响侧边栏与 Logo 底色，不改内容区 -->
    <div class="setting-drawer-title">
      <h3 class="drawer-title">主题风格设置</h3>
    </div>
    <div class="setting-drawer-block-checbox">
      <div class="setting-drawer-block-checbox-item" @click="handleTheme('theme-dark')">
        <img src="@/assets/images/dark.svg" alt="dark" />
        <div v-if="sideTheme === 'theme-dark'" class="setting-drawer-block-checbox-selectIcon" style="display: block;">
          <i aria-label="图标: check" class="anticon anticon-check">
            <svg viewBox="64 64 896 896" data-icon="check" width="1em" height="1em" :fill="theme" aria-hidden="true" focusable="false" class>
              <path d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.9c4.1-5.1.4-12.8-6.3-12.8z" />
            </svg>
          </i>
        </div>
      </div>
      <div class="setting-drawer-block-checbox-item" @click="handleTheme('theme-light')">
        <img src="@/assets/images/light.svg" alt="light" />
        <div v-if="sideTheme === 'theme-light'" class="setting-drawer-block-checbox-selectIcon" style="display: block;">
          <i aria-label="图标: check" class="anticon anticon-check">
            <svg viewBox="64 64 896 896" data-icon="check" width="1em" height="1em" :fill="theme" aria-hidden="true" focusable="false" class>
              <path d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.9c4.1-5.1.4-12.8-6.3-12.8z" />
            </svg>
          </i>
        </div>
      </div>
    </div>
    <!-- 主题颜色选中即生效：会重算 --el-color-primary 及各级明暗变量 -->
    <div class="drawer-item">
      <span>主题颜色</span>
      <span class="comp-style">
        <el-color-picker v-model="theme" :predefine="predefineColors" @change="themeChange"/>
      </span>
    </div>
    <el-divider />

    <!-- 以下开关直接 v-model 到 settings store，改动即时作用于布局；要下次仍生效需点「保存配置」 -->
    <h3 class="drawer-title">系统布局配置</h3>

    <div class="drawer-item">
      <span>开启 TopNav</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.topNav" @change="topNavChange" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>开启 Tags-Views</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示页签图标</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsIcon" :disabled="!settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>固定 Header</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.fixedHeader" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示 Logo</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.sidebarLogo" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>动态标题</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.dynamicTitle" @change="dynamicTitleChange" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>底部版权</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.footerVisible" class="drawer-switch" />
      </span>
    </div>

    <el-divider />

    <!-- 保存只写 localStorage；重置会清缓存并整页刷新（布局配置仅在 store 初始化时读取一次） -->
    <el-button type="primary" plain icon="DocumentAdd" @click="saveSetting">保存配置</el-button>
    <el-button plain icon="Refresh" @click="resetSetting">重置配置</el-button>
  </el-drawer>

</template>

<script setup>
// 布局设置抽屉：向父级暴露 openSetting()，由 layout/index.vue 通过 ref 调起。
// 面板内的开关直接绑定 settings store（即时生效），只有「保存配置」才写入
// localStorage 的 layout-setting —— 这也是 store 初始化时读取的键，所以重置后必须刷新页面。
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import { handleThemeStyle } from '@/utils/theme'

const { proxy } = getCurrentInstance()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const showSettings = ref(false)
// theme / sideTheme 用本地 ref 承接，是为了让取色器和预览图即时回显；保存时统一从 store 读当前值
const theme = ref(settingsStore.theme)
const sideTheme = ref(settingsStore.sideTheme)
const storeSettings = computed(() => settingsStore)
// 预置主题色，第一个是 Element Plus 的默认主色
const predefineColors = ref(["#409EFF", "#ff4500", "#ff8c00", "#ffd700", "#90ee90", "#00ced1", "#1e90ff", "#c71585"])

// 关闭 TopNav 时必须同时恢复侧边栏菜单并解除隐藏，
// 否则会出现「顶部菜单已关、侧边栏也隐藏」的空布局
/** 是否需要topnav */
function topNavChange(val) {
  if (!val) {
    appStore.toggleSideBarHide(false)
    permissionStore.setSidebarRouters(permissionStore.defaultRoutes)
  }
}

// 立刻重设一次标题，让开关即时套用/取消「页面名 - 系统名」格式（实现见 utils/dynamicTitle）
/** 是否需要dynamicTitle */
function dynamicTitleChange() {
  useSettingsStore().setTitle(useSettingsStore().title)
}

// 取色器变更时同步 store，并重算 --el-color-primary 与各级 light/dark 变量
function themeChange(val) {
  settingsStore.theme = val
  handleThemeStyle(val)
}

// 侧边栏主题只是切换配色类名与变量，不重建路由，也不影响当前打开的面板
function handleTheme(val) {
  settingsStore.sideTheme = val
  sideTheme.value = val
}

// 只持久化布局相关字段（token、用户信息等不在此处），键名 layout-setting 需与 store 初始化时读取的一致
function saveSetting() {
  proxy.$modal.loading("正在保存到本地，请稍候...")
  let layoutSetting = {
    "topNav": storeSettings.value.topNav,
    "tagsView": storeSettings.value.tagsView,
    "tagsIcon": storeSettings.value.tagsIcon,
    "fixedHeader": storeSettings.value.fixedHeader,
    "sidebarLogo": storeSettings.value.sidebarLogo,
    "dynamicTitle": storeSettings.value.dynamicTitle,
    "footerVisible": storeSettings.value.footerVisible,
    "sideTheme": storeSettings.value.sideTheme,
    "theme": storeSettings.value.theme
  }
  localStorage.setItem("layout-setting", JSON.stringify(layoutSetting))
  // closeLoading() 在此处是被当作参数立即执行的，并非延迟 1 秒才关闭遮罩（保持原行为）
  setTimeout(proxy.$modal.closeLoading(), 1000)
}

// 清掉持久化缓存后必须整页刷新，布局配置才会重新读默认值；
// 这里的 setTimeout 传的是字符串，会经由全局 eval 执行，效果等同于延迟刷新
function resetSetting() {
  proxy.$modal.loading("正在清除设置缓存并刷新，请稍候...")
  localStorage.removeItem("layout-setting")
  setTimeout("window.location.reload()", 1000)
}

// 供父组件通过 ref 调用；只负责打开抽屉，不重置面板里的临时状态
function openSetting() {
  showSettings.value = true
}

defineExpose({
  openSetting
})
</script>

<style lang='scss' scoped>
.setting-drawer-title {
  margin-bottom: 12px;
  color: var(--el-text-color-primary, rgba(0, 0, 0, 0.85));
  line-height: 22px;
  font-weight: bold;

  .drawer-title {
    font-size: 14px;
  }
}

.setting-drawer-block-checbox {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 10px;
  margin-bottom: 20px;

  .setting-drawer-block-checbox-item {
    position: relative;
    margin-right: 16px;
    border-radius: 2px;
    cursor: pointer;

    img {
      width: 48px;
      height: 48px;
    }

    .setting-drawer-block-checbox-selectIcon {
      position: absolute;
      top: 0;
      right: 0;
      width: 100%;
      height: 100%;
      padding-top: 15px;
      padding-left: 24px;
      color: #1890ff;
      font-weight: 700;
      font-size: 14px;
    }
  }
}

.drawer-item {
  color: var(--el-text-color-regular, rgba(0, 0, 0, 0.65));
  padding: 12px 0;
  font-size: 14px;

  .comp-style {
    float: right;
    margin: -3px 8px 0px 0px;
  }
}
</style>