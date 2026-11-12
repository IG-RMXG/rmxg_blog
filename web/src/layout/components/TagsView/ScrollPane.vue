<template>
  <el-scrollbar
    ref="scrollContainer"
    :vertical="false"
    class="scroll-container"
    @wheel.prevent="handleScroll"
  >
    <slot />
  </el-scrollbar>
</template>

<script setup>
// 页签横向滚动容器：隐藏纵向滚动、把滚轮事件转成横向滚动，
// 路由切换时把当前页签滚进视口（moveToTarget 由父组件通过 ref 调用）。
import useTagsViewStore from '@/store/modules/tagsView'

// 相邻页签之间的估算间距，判断是否需要滚动时用来预留一点缓冲
const tagAndTagSpacing = ref(4)
const { proxy } = getCurrentInstance()

// 真正可滚动的节点在 ElScrollbar 内部（对应样式里的 .el-scrollbar__wrap），需穿透 $refs 取
const scrollWrapper = computed(() => proxy.$refs.scrollContainer.$refs.wrapRef)

// 用原生监听（capture 阶段）而非组件事件，用户手动横向滚动时也通知父组件收起右键菜单
onMounted(() => {
  scrollWrapper.value.addEventListener('scroll', emitScroll, true)
})

// 卸载时解绑，否则滚动节点被移除后监听仍挂在旧引用上
onBeforeUnmount(() => {
  scrollWrapper.value.removeEventListener('scroll', emitScroll)
})

// 垂直滚轮转成横向滚动：wheelDelta 兼容旧浏览器，deltaY 取负并放大 40 倍统一量纲，
// 最后除以 4 放缓速度，避免页签刷地一下滚过头
function handleScroll(e) {
  const eventDelta = e.wheelDelta || -e.deltaY * 40
  const $scrollWrapper = scrollWrapper.value
  $scrollWrapper.scrollLeft = $scrollWrapper.scrollLeft + eventDelta / 4
}

// 只向父组件抛 scroll 事件，不带数据：父组件只用它来收起右键菜单
const emits = defineEmits()
const emitScroll = () => {
  emits('scroll')
}

const tagsViewStore = useTagsViewStore()
const visitedViews = computed(() => tagsViewStore.visitedViews)

// 把当前页签滚进可视区：首/尾页签直接对齐两端，中间页签通过比较相邻项的位置决定往哪边滚，
// 已经在可视范围内时不动，避免每次切换都抖动
function moveToTarget(currentTag) {
  const $container = proxy.$refs.scrollContainer.$el
  const $containerWidth = $container.offsetWidth
  const $scrollWrapper = scrollWrapper.value

  let firstTag = null
  let lastTag = null

  // find first tag and last tag
  if (visitedViews.value.length > 0) {
    firstTag = visitedViews.value[0]
    lastTag = visitedViews.value[visitedViews.value.length - 1]
  }

  if (firstTag === currentTag) {
    $scrollWrapper.scrollLeft = 0
  } else if (lastTag === currentTag) {
    $scrollWrapper.scrollLeft = $scrollWrapper.scrollWidth - $containerWidth
  } else {
    const tagListDom = document.getElementsByClassName('tags-view-item')
    const currentIndex = visitedViews.value.findIndex(item => item === currentTag)
    let prevTag = null
    let nextTag = null
    for (const k in tagListDom) {
      if (k !== 'length' && Object.hasOwnProperty.call(tagListDom, k)) {
        if (tagListDom[k].dataset.path === visitedViews.value[currentIndex - 1].path) {
          prevTag = tagListDom[k]
        }
        if (tagListDom[k].dataset.path === visitedViews.value[currentIndex + 1].path) {
          nextTag = tagListDom[k]
        }
      }
    }

    // the tag's offsetLeft after of nextTag
    const afterNextTagOffsetLeft = nextTag.offsetLeft + nextTag.offsetWidth + tagAndTagSpacing.value

    // the tag's offsetLeft before of prevTag
    const beforePrevTagOffsetLeft = prevTag.offsetLeft - tagAndTagSpacing.value
    if (afterNextTagOffsetLeft > $scrollWrapper.scrollLeft + $containerWidth) {
      $scrollWrapper.scrollLeft = afterNextTagOffsetLeft - $containerWidth
    } else if (beforePrevTagOffsetLeft < $scrollWrapper.scrollLeft) {
      $scrollWrapper.scrollLeft = beforePrevTagOffsetLeft
    }
  }
}

// 父组件在路由变化后通过 ref 调用，用来同步滚动位置
defineExpose({
  moveToTarget,
})
</script>

<style lang='scss' scoped>
/* 页签排成一行不换行，超出部分靠横向滚动；纵向高度写死略大于外层 34px，给滚动条留位 */
.scroll-container {
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  width: 100%;
  :deep(.el-scrollbar__bar) {
    bottom: 0px;
  }
  :deep(.el-scrollbar__wrap) {
    height: 39px;
  }
}
</style>