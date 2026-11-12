<template>
   <div class="app-container">
      <!-- 搜索区：部门接口非分页，过滤后仍返回整棵树，由 handleTree 在前端组装 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="部门名称" prop="deptName">
            <el-input
               v-model="queryParams.deptName"
               placeholder="请输入部门名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="部门状态" clearable style="width: 200px">
               <el-option
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <!-- 工具栏：新增受 system:dept:add 控制；展开/折叠只影响表格渲染，不触发查询 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:dept:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="info"
               plain
               icon="Sort"
               @click="toggleExpandAll"
            >展开/折叠</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 树形表格：row-key=deptId + tree-props 展开子部门；顶级部门(parentId=0)不提供删除入口 -->
      <el-table
         v-if="refreshTable"
         v-loading="loading"
         :data="deptList"
         row-key="deptId"
         :default-expand-all="isExpandAll"
         :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
         <el-table-column prop="deptName" label="部门名称" width="260"></el-table-column>
         <el-table-column prop="orderNum" label="排序" width="200"></el-table-column>
         <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
               <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="200">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dept:edit']">修改</el-button>
               <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['system:dept:add']">新增</el-button>
               <el-button v-if="scope.row.parentId != 0" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dept:remove']">删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 添加或修改部门对话框 -->
      <el-dialog :title="title" v-model="open" width="600px" append-to-body>
         <el-form ref="deptRef" :model="form" :rules="rules" label-width="80px">
            <!-- 上级部门仅在 parentId !== 0 时显示：根部门不能改挂上级，隐藏字段时其必填规则也会随之失效 -->
            <el-row>
               <el-col :span="24" v-if="form.parentId !== 0">
                  <el-form-item label="上级部门" prop="parentId">
                     <el-tree-select
                        v-model="form.parentId"
                        :data="deptOptions"
                        :props="{ value: 'deptId', label: 'deptName', children: 'children' }"
                        value-key="deptId"
                        placeholder="选择上级部门"
                        check-strictly
                     />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="部门名称" prop="deptName">
                     <el-input v-model="form.deptName" placeholder="请输入部门名称" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="显示排序" prop="orderNum">
                     <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="负责人" prop="leader">
                     <el-input v-model="form.leader" placeholder="请输入负责人" maxlength="20" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="联系电话" prop="phone">
                     <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="邮箱" prop="email">
                     <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="部门状态">
                     <el-radio-group v-model="form.status">
                        <el-radio
                           v-for="dict in sys_normal_disable"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Dept">
/**
 * 部门管理页（树形、无分页）。
 * 职责：维护 sys_dept 组织架构树；用户归属与数据权限的分配在其他页面完成。
 * 接口：@/api/system/dept（/system/dept 前缀）。list 返回扁平数组，前端 handleTree 建树，不能按 res.rows 取。
 * 权限点：system:dept:add / system:dept:edit / system:dept:remove。
 * 约定：删除是软删（del_flag='2'）；存在下级部门或仍有用户挂靠时后端拒绝并说明原因。
 */
import { listDept, getDept, delDept, addDept, updateDept, listDeptExcludeChild } from "@/api/system/dept"

// 取 app 级全局能力：useDict、resetForm、handleTree、$modal、$refs 都挂在 proxy 上
const { proxy } = getCurrentInstance()
// 字典 sys_normal_disable：0 正常 / 1 停用
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

// ===== 页面状态 =====
const deptList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const title = ref("")
const deptOptions = ref([])
// 部门树默认全展开；isExpandAll 是目标状态，refreshTable 负责重建表格使展开状态生效
const isExpandAll = ref(true)
const refreshTable = ref(true)

// ===== 查询条件 / 表单 / 校验 =====
// 部门接口非分页，queryParams 里没有 pageNum/pageSize，只放名称与状态
const data = reactive({
  form: {},
  queryParams: {
    deptName: undefined,
    status: undefined
  },
  // parentId 的必填规则只在上级部门表单项渲染时生效（根部门会隐藏该字段）
  rules: {
    parentId: [{ required: true, message: "上级部门不能为空", trigger: "blur" }],
    deptName: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
    orderNum: [{ required: true, message: "显示排序不能为空", trigger: "blur" }],
    email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
    // 字符类里的 | 只是字面量、不表示「或」，实际等价于匹配 13x~19x 的 11 位号码；沿用原规则以免与服务端不一致
    phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }]
  },
})

const { queryParams, form, rules } = toRefs(data)

/** 查询部门列表 */
// 后端给的是扁平列表，handleTree 按 deptId/parentId 就地挂 children 后返回根节点数组
function getList() {
  loading.value = true
  listDept(queryParams.value).then(response => {
    deptList.value = proxy.handleTree(response.data, "deptId")
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
// parentId 故意留空而不是 0：新增时上级部门表单项可见且必填，强制明确归属
function reset() {
  form.value = {
    deptId: undefined,
    parentId: undefined,
    deptName: undefined,
    orderNum: 0,
    leader: undefined,
    phone: undefined,
    email: undefined,
    status: "0"
  }
  proxy.resetForm("deptRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 新增按钮操作 */
// 行内「新增」以当前部门为上级；工具栏新增时 parentId 为空，需在弹窗里选择上级部门
// 下拉每次都重拉全量部门，保证刚建好的兄弟部门能立刻出现在选项里
function handleAdd(row) {
  reset()
  listDept().then(response => {
    deptOptions.value = proxy.handleTree(response.data, "deptId")
  })
  if (row != undefined) {
    form.value.parentId = row.deptId
  }
  open.value = true
  title.value = "添加部门"
}

/** 展开/折叠操作 */
// default-expand-all 只在表格创建时读取，改 prop 不会重排已渲染节点；
// 因此先卸载(v-if=false)、nextTick 后重建，用新的 isExpandAll 重新初始化
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
  })
}

/** 修改按钮操作 */
// 上级部门下拉改用 listDeptExcludeChild，排除自身及全部子孙，避免把部门挂到自己的下级下成环
function handleUpdate(row) {
  reset()
  listDeptExcludeChild(row.deptId).then(response => {
    deptOptions.value = proxy.handleTree(response.data, "deptId")
  })
  getDept(row.deptId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改部门"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deptRef"].validate(valid => {
    if (valid) {
      if (form.value.deptId != undefined) {
        updateDept(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addDept(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
// 软删除且不可撤销，故先二次确认；有下级部门或仍有用户挂靠时后端拒绝并返回具体原因
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.deptName + '"的数据项?').then(function() {
    return delDept(row.deptId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

// 页面挂载即加载整棵部门树
getList()
</script>
