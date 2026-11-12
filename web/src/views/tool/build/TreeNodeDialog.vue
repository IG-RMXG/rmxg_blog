<template>
  <div>
    <el-dialog title="添加选项" v-model="open" width="800px" :close-on-click-modal="false" :modal-append-to-body="false"
      @open="onOpen" @close="onClose">
      <el-form ref="treeNodeForm" :model="formData" :rules="rules" label-width="100px">
        <!-- 选项名/选项值两行；值的类型由右侧下拉决定，提交时再转换 -->
        <el-col :span="24">
          <el-form-item label="选项名" prop="label">
            <el-input v-model="formData.label" placeholder="请输入选项名" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="选项值" prop="value">
            <el-input v-model="formData.value" placeholder="请输入选项值" clearable>
              <template #append>
                <el-select v-model="dataType" :style="{ width: '100px' }">
                  <el-option v-for="(item, index) in dataTypeOptions" :key="index" :label="item.label" :value="item.value"
                    :disabled="item.disabled" />
                </el-select>
              </template>

            </el-input>
          </el-form-item>
        </el-col>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handelConfirm">确 定</el-button>
          <el-button @click="onClose">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
/**
 * 树形选项「添加选项」弹窗（用于级联选择器的静态数据）。
 *
 * 只负责收集单个节点的 label / value 并抛给父组件；节点插到哪个父级、树如何重组
 * 由 RightPanel 通过 currentNode 指向的数组决定，本组件不感知树结构。
 * 选项值支持字符串与数字两种类型，选「数字」时在提交前统一转成 number。
 */
// 与父组件 v-model 双向绑定弹窗显隐
const open = defineModel()
// 只抛出提交事件，节点真正写入画布数据由父组件的 addNode 完成
const emit = defineEmits(['confirm'])
// 单次提交的节点数据，onOpen 时整体替换，避免上一次输入残留到新节点
const formData = ref({
  label: undefined,
  value: undefined
})
// 两个字段都必填、失焦触发；这里只做设计器输入校验，与生成代码里的表单校验规则无关
const rules = {
  label: [
    {
      required: true,
      message: '请输入选项名',
      trigger: 'blur'
    }
  ],
  value: [
    {
      required: true,
      message: '请输入选项值',
      trigger: 'blur'
    }
  ]
}
// 选项值类型：渲染在选项值输入框的 append 插槽里，决定提交前是否把值转成数字
const dataType = ref('string')
// 只支持字符串/数字，因为设计器产出的选项值会被直接内联进生成代码当常量用
const dataTypeOptions = ref([
  {
    label: '字符串',
    value: 'string'
  },
  {
    label: '数字',
    value: 'number'
  }
])
// 节点 id 自增序列，作为 el-tree 的 node-key，删除节点时靠它定位；
// 与画布组件的 formId 编号体系互不相干，从 100 起只是习惯性起点
const id = ref(100)
// el-form 实例，提交时用它的 validate 拦截空值
const treeNodeForm = ref()

// 每次打开都重置表单，保证上一次填的选项名/值不会带进新节点
function onOpen() {
  formData.value = {
    label: undefined,
    value: undefined
  }
}

// 关闭即回写 v-model，父组件的 dialogVisible 随之变 false
function onClose() {
  open.value = false
}

// 先校验再落值：选「数字」时用 parseFloat 转换（空串会得到 NaN，靠上面必填规则先挡住），
// 随后补上自增 id，提交同一个 formData 对象，下一次 onOpen 会整体换新不影响已入树的节点
function handelConfirm() {
  treeNodeForm.value.validate(valid => {
    if (!valid) return
    if (dataType.value === 'number') {
      formData.value.value = parseFloat(formData.value.value)
    }
    formData.value.id = id.value++
    emit('commit', formData.value)
    onClose()
  })
}
</script>
