<template>
   <div class="app-container">
      <!-- 搜索区：编码/名称模糊匹配、状态精确匹配，输入框回车与点「搜索」等价 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="岗位编码" prop="postCode">
            <el-input
               v-model="queryParams.postCode"
               placeholder="请输入岗位编码"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="岗位名称" prop="postName">
            <el-input
               v-model="queryParams.postName"
               placeholder="请输入岗位名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="岗位状态" clearable style="width: 200px">
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

      <!-- 工具栏：修改/删除同时承担批量操作，可用性由勾选状态 single/multiple 决定 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:post:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['system:post:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:post:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:post:export']"
            >导出</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 表格：多选列供工具栏批量操作；行内操作直接作用于当前行，与工具栏入口互不影响 -->
      <el-table v-loading="loading" :data="postList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="岗位编号" align="center" prop="postId" />
         <el-table-column label="岗位编码" align="center" prop="postCode" />
         <el-table-column label="岗位名称" align="center" prop="postName" />
         <el-table-column label="岗位排序" align="center" prop="postSort" />
         <el-table-column label="状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" width="180" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:post:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:post:remove']">删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：pageNum/pageSize 双向绑定查询参数，翻页或改每页条数都会重新请求；
           无数据时整块隐藏，避免出现 0 条时的空白分页条 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改岗位对话框 -->
      <el-dialog :title="title" v-model="open" width="500px" append-to-body>
         <el-form ref="postRef" :model="form" :rules="rules" label-width="80px">
            <!-- 编码与名称的全局唯一由后端校验并指出冲突项，前端不重复查重 -->
            <el-form-item label="岗位名称" prop="postName">
               <el-input v-model="form.postName" placeholder="请输入岗位名称" />
            </el-form-item>
            <el-form-item label="岗位编码" prop="postCode">
               <el-input v-model="form.postCode" placeholder="请输入编码名称" />
            </el-form-item>
            <el-form-item label="岗位顺序" prop="postSort">
               <el-input-number v-model="form.postSort" controls-position="right" :min="0" />
            </el-form-item>
            <el-form-item label="岗位状态" prop="status">
               <el-radio-group v-model="form.status">
                  <el-radio
                     v-for="dict in sys_normal_disable"
                     :key="dict.value"
                     :value="dict.value"
                  >{{ dict.label }}</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
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

<script setup name="Post">
/**
 * 岗位管理页（标准分页 CRUD）。
 * 职责：维护 sys_post 岗位字典；岗位与用户是多对多（sys_user_post），已被用户引用的岗位不允许删除。
 * 接口：@/api/system/post（/system/post 前缀）。列表是 TableDataInfo（res.rows / res.total），
 *       其余接口是 ResponseVO（res.data）。
 * 权限点：system:post:add / system:post:edit / system:post:remove / system:post:export。
 */
import { listPost, addPost, delPost, getPost, updatePost } from "@/api/system/post"

// 取 app 级全局能力：useDict、resetForm、$modal、download 都挂在 proxy 上
const { proxy } = getCurrentInstance()
// 字典 sys_normal_disable：0 正常 / 1 停用
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

// ===== 页面状态 =====
const postList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
// 勾选结果：ids 供工具栏的批量修改/删除使用，行内操作不使用
const ids = ref([])
// 两个禁用开关都是「取反」语义：single=true 表示不满足单选(未选或多选)，multiple=true 表示一条都没选
const single = ref(true)
const multiple = ref(true)
// 分页总数，仅大于 0 时才显示分页条
const total = ref(0)
const title = ref("")

// ===== 查询条件 / 表单 / 校验 =====
// 分页与筛选条件同在一个对象里，直接作为 listPost 的 params 提交；pageNum 从 1 开始
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    postCode: undefined,
    postName: undefined,
    status: undefined
  },
  // 只做必填校验，编码/名称的唯一性由后端判断，页面不预先查重
  rules: {
    postName: [{ required: true, message: "岗位名称不能为空", trigger: "blur" }],
    postCode: [{ required: true, message: "岗位编码不能为空", trigger: "blur" }],
    postSort: [{ required: true, message: "岗位顺序不能为空", trigger: "blur" }],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询岗位列表 */
// 分页接口：列表在 response.rows、总数在 response.total；查询时由 handleQuery 重置到第一页
function getList() {
  loading.value = true
  listPost(queryParams.value).then(response => {
    postList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    postId: undefined,
    postCode: undefined,
    postName: undefined,
    postSort: 0,
    status: "0",
    remark: undefined
  }
  proxy.resetForm("postRef")
}

/** 搜索按钮操作 */
// 条件变化后回到第一页，避免停留在旧的深页码上出现空列表
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
// 勾选变化即重算 ids 与两个禁用开关，工具栏不额外保存选中行对象
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.postId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加岗位"
}

/** 修改按钮操作 */
// 行内传入 row；工具栏入口没有 postId，回退到勾选的 ids（受 single 限制，此时只有 1 条）
function handleUpdate(row) {
  reset()
  const postId = row.postId || ids.value
  getPost(postId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改岗位"
  })
}

/** 提交按钮 */
// postId 有无区分新增/修改；成功后关闭弹窗并刷新当前页（页码保持不变）
function submitForm() {
  proxy.$refs["postRef"].validate(valid => {
    if (valid) {
      if (form.value.postId != undefined) {
        updatePost(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPost(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
// 行内按 row 单删；工具栏没有 postId，用 ids 批量删；任一岗位已被用户占用则整批失败
function handleDelete(row) {
  const postIds = row.postId || ids.value
  proxy.$modal.confirm('是否确认删除岗位编号为"' + postIds + '"的数据项？').then(function() {
    return delPost(postIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
// 直接走后端导出(/system/post/export)触发浏览器下载，参数复用当前查询条件；
// 文件名带时间戳，避免同一目录下重复下载互相覆盖
function handleExport() {
  proxy.download("system/post/export", {
    ...queryParams.value
  }, `post_${new Date().getTime()}.xlsx`)
}

// 页面挂载即加载第一页数据
getList()
</script>
