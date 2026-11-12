<template>
  <!-- 全局分页组件（main.js 注册为 <pagination>）。父组件用 v-model:page / v-model:limit 双向绑定，
       真正的查询由 pagination 事件触发，组件本身不保存页码状态。 -->
  <div :class="{ 'hidden': hidden }" class="pagination-container">
    <el-pagination
      :background="background"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :layout="layout"
      :page-sizes="pageSizes"
      :pager-count="pagerCount"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
/*
 * 列表页通用分页条。
 *
 * 双向绑定约定：接收 page / limit，变更时抛 update:page / update:limit。
 * 父组件必须写成 v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"，
 * 否则组件里看到的页码变了、父组件的查询参数仍是旧值。
 *
 * 事件 pagination({ page, limit })：父组件在此发请求（通常直接 @pagination="getList"）。
 * autoScroll 决定翻页后是否回到页面顶部，scrollTo 的 800 是动画时长（ms）。
 */
import { scrollTo } from '@/utils/scroll-to'

// 总条数（必填）：为 0 时父组件一般配合 v-show="total > 0" 隐藏整条分页
const props = defineProps({
  total: {
    required: true,
    type: Number
  },
  // 当前页码（v-model:page），默认第 1 页
  page: {
    type: Number,
    default: 1
  },
  // 每页条数（v-model:limit），默认 20 条
  limit: {
    type: Number,
    default: 20
  },
  // 可选的每页条数规格，切换后由 handleSizeChange 重新查询
  pageSizes: {
    type: Array,
    default() {
      return [10, 20, 30, 50]
    }
  },
  // 移动端页码按钮的数量端默认值5
  // 该默认值在组件定义时按当时的视口宽度求值一次，之后缩放窗口不会重算（刷新后才按新宽度）
  pagerCount: {
    type: Number,
    default: document.body.clientWidth < 992 ? 5 : 7
  },
  // el-pagination 的布局：总数 / 条数切换 / 上下页 / 页码 / 跳页
  layout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper'
  },
  // 页码按钮是否带背景色
  background: {
    type: Boolean,
    default: true
  },
  // 翻页后是否自动滚回顶部，避免停留在表格底部看不到新数据
  autoScroll: {
    type: Boolean,
    default: true
  },
  // 隐藏整条分页，供不方便用 v-show 的场景使用
  hidden: {
    type: Boolean,
    default: false
  }
})

// 未声明事件名，属宽松约定：依赖调用方接 update:page / update:limit / pagination
const emit = defineEmits()
// 页码的受控包装：get 读 props、set 只回抛事件，组件内不存副本，避免与父组件状态不同步
const currentPage = computed({
  get() {
    return props.page
  },
  set(val) {
    emit('update:page', val)
  }
})
// 每页条数的受控包装，与 currentPage 同理
const pageSize = computed({
  get() {
    return props.limit
  },
  set(val){
    emit('update:limit', val)
  }
})

function handleSizeChange(val) {
  // 调大每页条数后当前页码可能越过末页（会翻到空白页），此时回到第 1 页
  if (currentPage.value * val > props.total) {
    currentPage.value = 1
  }
  // 先回抛页码变更，再抛 pagination 让父组件按新条数重新查询
  emit('pagination', { page: currentPage.value, limit: val })
  if (props.autoScroll) {
    scrollTo(0, 800)
  }
}

// 切换页码：只抛事件，是否重新请求由父组件决定
function handleCurrentChange(val) {
  emit('pagination', { page: val, limit: pageSize.value })
  if (props.autoScroll) {
    scrollTo(0, 800)
  }
}
</script>

<style scoped>
/* 白底：分页条紧贴表格下方，避免透出页面/弹窗背景色 */
.pagination-container {
  background: #fff;
}
/* hidden 为 true 时整条隐藏且不占位 */
.pagination-container.hidden {
  display: none;
}
</style>