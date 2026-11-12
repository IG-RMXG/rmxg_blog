<template>
  <!-- 顶栏右侧「布局大小」入口：下拉选择全局组件尺寸，当前档位置灰（disabled） -->
  <div>
    <el-dropdown trigger="click" @command="handleSetSize">
      <div class="size-icon--style">
        <svg-icon class-name="size-icon" icon-class="size" />
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="item of sizeOptions" :key="item.value" :disabled="size === item.value" :command="item.value">
            {{ item.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import useAppStore from "@/store/modules/app"

/*
 * 全局组件尺寸切换（large / default / small）。
 *
 * 尺寸真正生效的位置在 main.js：app.use(ElementPlus, { size: Cookies.get('size') })。
 * Element Plus 只在安装时读取一次该配置，运行时改 store 不会影响已经注册的组件，
 * 所以这里写入 Cookie 后必须整页刷新，新尺寸才会对全站生效。
 */

// 尺寸值持久化在 Cookie 的 size 键里（由 app store 的 setSize 写入）
const appStore = useAppStore()
// 当前尺寸，用于把选中的那一项置灰
const size = computed(() => appStore.size)
// route / router 目前未使用（随模板带入），保留不影响运行
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
// 三档取值必须与 Element Plus 的 size 一致，否则组件会回退成默认尺寸
const sizeOptions = ref([
  { label: "较大", value: "large" },
  { label: "默认", value: "default" },
  { label: "稍小", value: "small" },
])

function handleSetSize(size) {
  // 先弹全屏遮罩给出反馈，再等 1s 刷新；刷新会丢失当前页面状态，是有意的取舍
  proxy.$modal.loading("正在设置布局大小，请稍候...")
  appStore.setSize(size)
  // setTimeout 传字符串（等价 eval）是历史写法；延迟 1s 是为了让 loading 提示能被看到
  setTimeout("window.location.reload()", 1000)
}
</script>

<style lang='scss' scoped>
/* line-height 与顶栏 navbar 的 50px 高度对齐，使图标垂直居中 */
.size-icon--style {
  font-size: 18px;
  line-height: 50px;
  padding-right: 7px;
}
</style>