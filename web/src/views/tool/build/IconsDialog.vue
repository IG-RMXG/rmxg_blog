<template>
  <div class="icon-dialog">
    <el-dialog v-model="value" width="980px" :close-on-click-modal="false" :modal-append-to-body="false" @open="onOpen"
      @close="onClose">
      <template #header="{ close, titleId, titleClass }">
        选择图标
        <el-input v-model="key" size="small" :style="{ width: '260px' }" placeholder="请输入图标名称" prefix-icon="Search"
          clearable />
      </template>
      <!-- 图标网格：单击即选中并关闭；列表可能很长，滚动交给弹窗正文区 -->
      <ul class="icon-ul">
        <li v-for="icon in iconList" :key="icon" :class="active === icon ? 'active-item' : ''" @click="onSelect(icon)">
          <div>
            <el-icon :size="30">
              <component :is="icon" />
            </el-icon>
            <div>{{ icon }}</div>
          </div>
        </li>
      </ul>
    </el-dialog>
  </div>
</template>
<script setup>
/**
 * 图标选择弹窗。
 *
 * 把 @element-plus/icons-vue 的导出表一次性展开成图标名列表，支持按名称过滤后网格展示，
 * 点击即回传并关闭。父组件 RightPanel 用它给组件的 prefix-icon / suffix-icon 选值。
 */
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { watch } from 'vue'

// iconList 是当前展示的图标名；originList 是全量快照（普通数组无需响应式），
// 清空关键字时用它还原，省得反复重新枚举模块
const iconList = ref([])
const originList = []
// 搜索关键字；下面 for 循环的变量同名会局部遮蔽它，两者含义不同，别混用
const key = ref('')
// 最近一次选中的图标名，只用于网格高亮；弹窗关闭时不会重置
const active = ref('')
// 选中结果直接回传图标名字符串，由父组件写回对应属性字段
const emit = defineEmits(['select'])
// 与父组件 v-model 双向绑定弹窗显隐
const value = defineModel()
// 模块导出即「图标名 -> 图标组件」，这里只需要名字，所以只取 Object.entries 的键
for (const [key] of Object.entries(ElementPlusIconsVue)) {
  iconList.value.push(key)
  originList.push(key)
}

// 打开/关闭都不做重置：搜索关键字和高亮会保留到下次打开，符合「接着上次找」的使用习惯
function onOpen() { }
function onClose() { }
// 点选即生效：先回传图标名再关闭，因此没有单独的「确定」按钮
function onSelect(icon) {
  active.value = icon
  emit('select', icon)
  value.value = false
}

// 监听搜索框：按名称包含关系过滤（区分大小写）；关键字清空时直接用全量快照还原
watch(key, (val) => {
  if (val) {
    iconList.value = originList.filter(name => name.indexOf(val) > -1)
  } else {
    iconList.value = originList
  }
})
</script>
<style lang="scss" scoped>
.icon-ul {
  margin: 0;
  padding: 0;
  font-size: 0;

  li {
    list-style-type: none;
    text-align: center;
    font-size: 14px;
    display: inline-flex;
    width: 16.66%;
    box-sizing: border-box;
    height: 108px;
    padding: 6px 6px 6px 6px;
    cursor: pointer;
    overflow: hidden;
    align-items: center;
    justify-content: center;

    &:hover {
      background: #f2f2f2;
    }

    &.active-item {
      background: #e1f3fb;
      color: #7a6df0
    }

    i {
      font-size: 30px;
      line-height: 50px;
      margin-bottom: 10px;
    }
  }
}

.icon-dialog {
  :deep() {
    .el-dialog {
      border-radius: 8px;
      margin-bottom: 0;
      margin-top: 4vh !important;
      display: flex;
      flex-direction: column;
      max-height: 92vh;
      overflow: hidden;
      box-sizing: border-box;

      .el-dialog__header {
        padding-top: 14px;
      }

      .el-dialog__body {
        margin: 0 20px 20px 20px;
        padding: 0;
        overflow: auto;
      }
    }
  }
}
</style>
