<template>
  <el-image
    :src="`${realSrc}`"
    fit="cover"
    :style="`width:${realWidth};height:${realHeight};`"
    :preview-src-list="realSrcList"
    preview-teleported
  >
    <template #error>
      <div class="image-slot">
        <el-icon><picture-filled /></el-icon>
      </div>
    </template>
  </el-image>
</template>

<script setup>
/**
 * 图片展示/预览组件（el-image 封装）
 * - src 支持英文逗号分隔的多张图片：第一张作为展示图，全部图片进入大图轮播列表
 * - width / height 传数字按 px 处理，传字符串则原样使用（如 "100%"、"8rem"）
 * - preview-teleported 把预览层挂到 body，避免被表格单元格或卡片的 overflow 裁切
 * - 加载失败时显示 #error 插槽中的占位图标
 */
const props = defineProps({
  src: {
    type: String,
    default: ""
  },
  width: {
    type: [Number, String],
    default: ""
  },
  height: {
    type: [Number, String],
    default: ""
  }
})

// 封面取逗号分隔的第一张：列表/卡片场景只展示一张，多图由预览轮播承接
const realSrc = computed(() => {
  if (!props.src) {
    return
  }
  let real_src = props.src.split(",")[0]
  return real_src
})

// 预览列表即全部图片，顺序与后端多图字段的逗号拼接顺序一致，也就是点击后的轮播顺序
const realSrcList = computed(() => {
  if (!props.src) {
    return
  }
  let real_src_list = props.src.split(",")
  let srcList = []
  real_src_list.forEach(item => {
    return srcList.push(item)
  })
  return srcList
})

// 宽高换算（下方 realHeight 同理）：数字补 px；字符串（百分比、rem 等）原样返回，兼容 "100%" 这类自适应写法
const realWidth = computed(() =>
  typeof props.width == "string" ? props.width : `${props.width}px`
)

const realHeight = computed(() =>
  typeof props.height == "string" ? props.height : `${props.height}px`
)
</script>

<style lang="scss" scoped>
.el-image {
  border-radius: 5px;
  background-color: #ebeef5;
  box-shadow: 0 0 5px 1px #ccc;
  :deep(.el-image__inner) {
    /* 悬停轻微放大，提示图片可点击查看大图 */
    transition: all 0.3s;
    cursor: pointer;
    &:hover {
      transform: scale(1.2);
    }
  }
  :deep(.image-slot) {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    color: #909399;
    font-size: 30px;
  }
}
</style>
