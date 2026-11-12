<template>
  <!-- 代码生成「基本信息」子表单：只负责表级元信息录入，不持有独立状态、也不调用接口 -->
  <!-- 编辑的数据对象由父组件 editTable.vue 以 info 传入，此处 v-model 直接写回该引用，最终随 updateGenTable 一并提交 -->
  <el-form ref="basicInfoForm" :model="info" :rules="rules" label-width="150px">
    <el-row>
      <!-- 表名称需与数据库中的真实表名一致，生成代码时按它定位表结构 -->
      <el-col :span="12">
        <el-form-item label="表名称" prop="tableName">
          <el-input placeholder="请输入仓库名称" v-model="info.tableName" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="表描述" prop="tableComment">
          <el-input placeholder="请输入" v-model="info.tableComment" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="实体类名称" prop="className">
          <el-input placeholder="请输入" v-model="info.className" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="作者" prop="functionAuthor">
          <el-input placeholder="请输入" v-model="info.functionAuthor" />
        </el-form-item>
      </el-col>
      <!-- 备注是唯一非必填项，校验规则里未声明，留空不会阻塞提交 -->
      <el-col :span="24">
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" :rows="3" v-model="info.remark"></el-input>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
// 父组件传入的生成配置对象（GenTable）：本组件不 emit 任何事件，靠共享引用把修改同步回父级
// 默认值为 null，实际使用由父组件保证传入非空对象，否则 el-form 的 model 为空会取不到字段
defineProps({
  info: {
    type: Object,
    default: null
  }
})

// 表单校验
// 仅 4 项必填：表名称、表描述、实体类名称、作者；这四项会写进生成代码的类注释与表映射
// trigger 统一用 blur，因为全是文本输入，没有选择类控件
const rules = ref({
  tableName: [{ required: true, message: "请输入表名称", trigger: "blur" }],
  tableComment: [{ required: true, message: "请输入表描述", trigger: "blur" }],
  className: [{ required: true, message: "请输入实体类名称", trigger: "blur" }],
  functionAuthor: [{ required: true, message: "请输入作者", trigger: "blur" }]
})
</script>
