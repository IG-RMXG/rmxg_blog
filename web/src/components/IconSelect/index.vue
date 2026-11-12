<template>
  <div class="icon-body">
    <el-input
      v-model="iconName"
      class="icon-search"
      clearable
      placeholder="请输入图标名称"
      @clear="filterIcons"
      @input="filterIcons"
    >
      <template #suffix><i class="el-icon-search el-input__icon" /></template>
    </el-input>
    <!-- 图标网格：三列排布，点击即选中并关闭弹层 -->
    <div class="icon-list">
      <div class="list-container">
        <div v-for="(item, index) in iconList" class="icon-item-wrapper" :key="index" @click="selectedIcon(item)">
          <div :class="['icon-item', { active: activeIcon === item }]">
            <svg-icon :icon-class="item" class-name="icon" style="height: 25px;width: 16px;"/>
            <span>{{ item }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 图标选择面板：一般嵌在 el-popover 里使用，供菜单/按钮配置挑选 svg-icon。
 *
 * 图标清单来自 requireIcons（构建期用 import.meta.glob 扫描 assets/icons/svg），
 * 所以没有任何接口请求，搜索只是本地子串过滤。
 * 对外契约：props.activeIcon 高亮当前已选图标（由父组件传入，本组件不自行修改）；
 * 选中时 emit('selected', name)，如何回填由父组件决定；另通过 defineExpose 暴露 reset 供父组件复位。
 */
import icons from './requireIcons'

const props = defineProps({
  // 当前已选图标名，仅用于高亮
  activeIcon: {
    type: String
  }
})

const iconName = ref('')
const iconList = ref(icons)
const emit = defineEmits(['selected'])

// 每次输入都从全量 icons 重新过滤，而不是在已过滤结果上二次筛选（否则删字符也回不来）。
function filterIcons() {
  iconList.value = icons
  if (iconName.value) {
    iconList.value = icons.filter(item => item.indexOf(iconName.value) !== -1)
  }
}

// 先通知父组件选中项，再模拟一次 body 点击以关闭外层 popover（触发外部点击关闭，项目里的既有做法）。
function selectedIcon(name) {
  emit('selected', name)
  document.body.click()
}

// 清空搜索并还原完整列表，由父组件在弹层关闭时调用。
function reset() {
  iconName.value = ''
  iconList.value = icons
}

// 只暴露 reset：父组件通过 ref 拿到后即可复位，其余状态不对外开放。
defineExpose({
  reset
})
</script>

<style lang='scss' scoped>
   .icon-body {
    width: 100%;
    padding: 10px;
    .icon-search {
      position: relative;
      margin-bottom: 5px;
    }
    /* 固定高度 + 内部滚动：图标数量多时把面板撑在固定尺寸内 */
    .icon-list {
      height: 200px;
      overflow: auto;
      .list-container {
        display: flex;
        flex-wrap: wrap;
        .icon-item-wrapper {
          width: calc(100% / 3);
          height: 25px;
          line-height: 25px;
          cursor: pointer;
          display: flex;
          .icon-item {
            display: flex;
            max-width: 100%;
            height: 100%;
            padding: 0 5px;
            &:hover {
              background: #ececec;
              border-radius: 5px;
            }
            .icon {
              flex-shrink: 0;
            }
            span {
              display: inline-block;
              vertical-align: -0.15em;
              fill: currentColor;
              padding-left: 2px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
          .icon-item.active {
            background: #ececec;
            border-radius: 5px;
          }
        }
      }
    }
  }
</style>