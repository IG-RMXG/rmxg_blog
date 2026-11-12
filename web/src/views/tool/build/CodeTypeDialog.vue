<template>
  <el-dialog v-model="open" width="500px" title="选择生成类型" @open="onOpen" @close="onClose">
    <el-form ref="codeTypeForm" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="生成类型" prop="type">
        <el-radio-group v-model="formData.type">
          <el-radio-button v-for="(item, index) in typeOptions" :key="index" :label="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <!-- 仅导出文件时需要文件名；这是独立的 v-if，复制流程下整项不渲染 -->
      <el-form-item v-if="showFileName" label="文件名" prop="fileName">
        <el-input v-model="formData.fileName" placeholder="请输入文件名" clearable />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="onClose">取消</el-button>
      <el-button type="primary" @click="handelConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 「选择生成类型」弹窗。
 *
 * 由表单设计器（index.vue）的两个入口复用：
 * - 复制代码：showFileName=false，只选生成类型，确认后父组件直接把代码写入剪贴板；
 * - 导出文件：showFileName=true，额外收集文件名，确认后父组件另存为 .vue。
 * 本组件只负责收集 type / fileName，真正的代码拼装在父组件的 generateCode() 中完成。
 */
// 与父组件 v-model 双向绑定弹窗显隐，关闭时只改这个值即可
const open = defineModel()
// showFileName 为 true 时才收集文件名；复制代码流程传 false，字段既不显示也不参与校验
const props = defineProps({
  showFileName: Boolean
})
const emit = defineEmits(['confirm'])
// type 默认 file（生成整页 SFC）；fileName 只在导出文件时使用，默认值由 onOpen 填充
const formData = ref({
  fileName: undefined,
  type: 'file'
})
const codeTypeForm = ref()
// 校验规则：type 兜底必选；fileName 规则始终注册，但只有对应表单项被渲染时才会触发，
// 因此复制流程（showFileName=false）不会被文件名规则拦下
const rules = {
  fileName: [{
    required: true,
    message: '请输入文件名',
    trigger: 'blur'
  }],
  type: [{
    required: true,
    message: '生成类型不能为空',
    trigger: 'change'
  }]
}
// 生成形态：file=整页 .vue；dialog=用 el-dialog 包裹的弹窗版，两者在 utils/generator 里走不同模板分支
const typeOptions = ref([
  {
    label: '页面',
    value: 'file'
  },
  {
    label: '弹窗',
    value: 'dialog'
  }
])
// 导出流程每次打开都重置成「时间戳.vue」，避免连续导出时浏览器反复提示同名覆盖
function onOpen() {
  if (props.showFileName) {
    formData.value.fileName = `${+new Date()}.vue`
  }
}
// 取消与确定最终都走这里关闭，等价于父组件的 v-model 置 false
function onClose() {
  open.value = false
}
// 校验通过后回传表单副本：展开成新对象，父组件后续改动不会反写到本弹窗状态
function handelConfirm() {
  codeTypeForm.value.validate(valid => {
    if (!valid) return
    emit('confirm', { ...formData.value })
    onClose()
  })
}
</script>