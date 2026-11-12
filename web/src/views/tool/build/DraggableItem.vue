<template>
  <el-col :span="element.span" :class="className" @click.stop="activeItem(element)">
    <el-form-item :label="element.label" :label-width="element.labelWidth ? element.labelWidth + 'px' : null"
      :required="element.required" v-if="element.layout === 'colFormItem'">
      <render :key="element.tag" :conf="element" v-model="element.defaultValue" />
    </el-form-item>
    <el-row :gutter="element.gutter" :class="element.class" @click.stop="activeItem(element)" v-else>
      <span class="component-name"> {{ element.componentName }} </span>
      <draggable group="componentsGroup" :animation="340" :list="element.children" class="drag-wrapper" item-key="label"
        ref="draggableItemRef" :component-data="getComponentData()">
        <template #item="scoped">
          <draggable-item :key="scoped.element.renderKey" :drawing-list="element.children" :element="scoped.element"
            :index="index" :active-id="activeId" :form-conf="formConf" @activeItem="activeItem(scoped.element)"
            @copyItem="copyItem(scoped.element, element.children)"
            @deleteItem="deleteItem(scoped.index, element.children)" />
        </template>
      </draggable>
    </el-row>
    <!-- 复制/删除按钮绝对定位在节点右上角，默认 display:none，hover 或选中时由样式放出；
         @click.stop 避免冒泡触发整行选中 -->
    <span class="drawing-item-copy" title="复制" @click.stop="copyItem(element)">
      <el-icon><CopyDocument /></el-icon>
    </span>
    <span class="drawing-item-delete" title="删除" @click.stop="deleteItem(index)">
      <el-icon><Delete /></el-icon>
    </span>
  </el-col>
</template>
<script setup name="DraggableItem">
/**
 * 画布中的单个可拖拽节点（递归组件，模板里直接用自己的标签名实现嵌套渲染）。
 *
 * layout=colFormItem 时渲染成一个 el-form-item，内部由 render 按 tag 渲染真实控件；
 * layout=rowFormItem 时渲染成 el-row 容器，并把 children 交回自身递归渲染。
 * 本组件不直接改数据：选中、复制、删除都以事件形式抛给父级（index.vue）处理。
 */
import draggable from "vuedraggable/dist/vuedraggable.common"
import render from '@/utils/generator/render'

// element：组件描述对象，即画布数据的引用，改动会直接反映到画布与导出代码
// index：在父级列表中的下标，删除时按它定位
// drawingList：父级列表，顶层复制/删除没有显式父级时用它兜底
// activeId：当前选中节点的 formId，用于高亮与操作按钮显隐
// formConf：表单全局配置，这里只用到 unFocusedComponentBorder
const props = defineProps({
  element: Object,
  index: Number,
  drawingList: Array,
  activeId: {
    type: [String, Number]
  },
  formConf: Object
})
// 根节点 class：由 layout 决定是普通项还是容器项，再叠加选中态，样式据此显示操作按钮
const className = ref('')
// 内层 draggable 的实例引用，用于需要直接拿到 sortable 实例的场景
const draggableItemRef = ref(null)
// 三个事件全部向上冒泡，父级决定真正的选中/复制/删除行为
const emits = defineEmits(['activeItem', 'copyItem', 'deleteItem'])

// 递归渲染时子级也绑定了点击，统一收敛成一个 activeItem 事件抛给父级
function activeItem(item) {
  emits('activeItem', item)
}
// 递归层会传入各自的 children 作为 parent；顶层按钮不传，回退到 props.drawingList
function copyItem(item, parent) {
  emits('copyItem', item, parent ?? props.drawingList)
}
// 删除同理：parent 可能是子级 children，也可能是顶层 drawingList
function deleteItem(item, parent) {
  emits('deleteItem', item, parent ?? props.drawingList)
}

// 把容器的排列配置透传给内层 draggable，保证拖入的子项排布与容器设置一致
function getComponentData() {
  return {
    gutter: props.element.gutter,
    justify: props.element.justify,
    align: props.element.align
  }
}

// 监听选中项变化重算根节点 class；immediate 保证首屏就带上正确的项类型与描边样式。
// 依赖只有 activeId，因此切换「显示未选中组件边框」开关后，已渲染的节点要重新选中一次才会刷新
watch(() => props.activeId, (val) => {
  className.value = (props.element.layout === 'rowFormItem' ? 'drawing-row-item' : 'drawing-item') + (val === props.element.formId ? ' active-from-item' : '')
  if (props.formConf.unFocusedComponentBorder) {
    className.value += ' unfocus-bordered'
  }
}, { immediate: true })
</script>