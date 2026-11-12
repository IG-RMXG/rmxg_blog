<template>
  <div class="top-right-btn" :style="style">
    <!-- 三个圆形按钮：搜索区显隐（受 search 控制）、刷新、列显隐（columns 为空时不显示） -->
    <el-row>
      <el-tooltip class="item" effect="dark" :content="showSearch ? '隐藏搜索' : '显示搜索'" placement="top" v-if="search">
        <el-button circle icon="Search" @click="toggleSearch()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button circle icon="Refresh" @click="refresh()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="显隐列" placement="top" v-if="Object.keys(columns).length > 0">
        <el-button circle icon="Menu" @click="showColumn()" v-if="showColumnsType == 'transfer'"/>
        <el-dropdown trigger="click" :hide-on-click="false" style="padding-left: 12px" v-if="showColumnsType == 'checkbox'">
          <el-button circle icon="Menu" />
          <template #dropdown>
            <el-dropdown-menu>
              <!-- 全选/反选 按钮 -->
              <el-dropdown-item>
                <el-checkbox :indeterminate="isIndeterminate" v-model="isChecked" @change="toggleCheckAll"> 列展示 </el-checkbox>
              </el-dropdown-item>
              <div class="check-line"></div>
              <template v-for="(item, key) in columns" :key="item.key">
                <el-dropdown-item>
                  <el-checkbox v-model="item.visible" @change="checkboxChange($event, key)" :label="item.label" />
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </el-row>
    <!-- 仅 transfer 模式用：穿梭框左右两栏分别是「显示 / 隐藏」，checkbox 模式走上面的下拉，不会打开此弹窗 -->
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-transfer
        :titles="['显示', '隐藏']"
        v-model="value"
        :data="transferData"
        @change="dataChange"
      ></el-transfer>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 表格页右上角工具栏：搜索区显隐、刷新、列显隐（下拉复选框 或 穿梭框两种形态）。
 *
 * 对外契约：
 *   - showSearch / update:showSearch → 父页面用 v-model:showSearch 控制查询表单显隐；
 *   - queryTable 事件 → 点刷新时触发，父页面在回调里重新拉列表（是否回到第一页由父页面决定）；
 *   - columns → 列配置，同时支持「数组」与「对象」两种形态，下面每个方法都得分支处理；
 *     组件是就地修改 columns 里各项的 visible（不复制、不 emit），所以父组件必须传响应式对象，
 *     否则列显隐不会生效。
 */
const props = defineProps({
  /* 是否显示检索条件 */
  showSearch: {
    type: Boolean,
    default: true
  },
  /* 显隐列信息（数组格式、对象格式） */
  columns: {
    type: [Array, Object],
    default: () => ({})
  },
  /* 是否显示检索图标 */
  search: {
    type: Boolean,
    default: true
  },
  /* 显隐列类型（transfer穿梭框、checkbox复选框） */
  showColumnsType: {
    type: String,
    default: "checkbox"
  },
  /* 右外边距 */
  gutter: {
    type: Number,
    default: 10
  },
})

// 事件名不加 on 前缀；update:showSearch 配合父级 v-model:showSearch 使用。
const emits = defineEmits(['update:showSearch', 'queryTable'])

// 显隐数据
const value = ref([])
// 弹出层标题
const title = ref("显示/隐藏")
// 是否显示弹出层
const open = ref(false)

// gutter 取一半作为右外边距，让工具栏与右侧按钮组在视觉上居中对齐。
const style = computed(() => {
  const ret = {}
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`
  }
  return ret
})

// 是否全选/半选 状态
// 三个派生状态都兼容两种 columns 形态：数组用 every/some，对象用 Object.values。
// isChecked 的 set 留空——勾选状态只由各列 visible 推导，写入动作交给 toggleCheckAll。
const isChecked = computed({
  get: () => Array.isArray(props.columns) ? props.columns.every(col => col.visible) : Object.values(props.columns).every((col) => col.visible),
  set: () => {}
})
const isIndeterminate = computed(() => Array.isArray(props.columns) ? props.columns.some((col) => col.visible) && !isChecked.value : Object.values(props.columns).some((col) => col.visible) && !isChecked.value)
// 穿梭框数据：用下标当 key（数组取 index、对象取 Object.keys 顺序），必须与 dataChange 里的索引规则一致。
const transferData = computed(() => Array.isArray(props.columns) ? props.columns.map((item, index) => ({ key: index, label: item.label })) : Object.keys(props.columns).map((key, index) => ({ key: index, label: props.columns[key].label })))

// 搜索
function toggleSearch() {
  emits("update:showSearch", !props.showSearch)
}

// 刷新
function refresh() {
  emits("queryTable")
}

// 右侧列表元素变化
// 穿梭框右栏是「隐藏」列，所以这里按「不在 data 中即显示」取反写入 visible。
function dataChange(data) {
  if (Array.isArray(props.columns)) {
    for (let item in props.columns) {
      const key = props.columns[item].key
      props.columns[item].visible = !data.includes(key)
    }
  } else {
    Object.keys(props.columns).forEach((key, index) => {
      props.columns[key].visible = !data.includes(index)
    })
  }
}

// 打开显隐列dialog
function showColumn() {
  open.value = true
}

if (props.showColumnsType == "transfer") {
  // transfer穿梭显隐列初始默认隐藏列
  // 仅在初始化时跑一次，后续由 dataChange 同步；checkbox 模式不需要初始值，直接跳过。
  if (Array.isArray(props.columns)) {
    for (let item in props.columns) {
      if (props.columns[item].visible === false) {
        value.value.push(parseInt(item))
      }
    }
  } else {
    Object.keys(props.columns).forEach((key, index) => {
      if (props.columns[key].visible === false) {
        value.value.push(index)
      }
    })
  }
}

// 单勾选
// 数组形态按下标 key 找回对应列对象，对象形态直接用下标取值。
function checkboxChange(event, key) {
  if (Array.isArray(props.columns)) {
    props.columns.filter(item => item.key == key)[0].visible = event
  } else {
    props.columns[key].visible = event
  }
}

// 切换全选/反选
// 兼容数组/对象两种形态；勾选状态由各列 visible 汇总，写回后 isChecked 会自动重算，无需单独维护。
function toggleCheckAll() {
  const newValue = !isChecked.value
  if (Array.isArray(props.columns)) {
    props.columns.forEach((col) => (col.visible = newValue))
  } else {
    Object.values(props.columns).forEach((col) => (col.visible = newValue))
  }
}
</script>

<style lang='scss' scoped>
:deep(.el-transfer__button) {
  border-radius: 50%;
  display: block;
  margin-left: 0px;
}
:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}
:deep(.el-dropdown-menu__item) {
  line-height: 30px;
  padding: 0 17px;
}
.check-line {
  width: 90%;
  height: 1px;
  background-color: #ccc;
  margin: 3px auto;
}
</style>
