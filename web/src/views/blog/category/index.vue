<template>
  <div class="app-container">
    <!-- 搜索区：分类名称（模糊匹配）+ 状态，输入框回车即查询 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="分类名称" prop="categoryName">
        <el-input v-model="queryParams.categoryName" placeholder="请输入分类名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="分类状态" clearable style="width: 160px">
          <el-option v-for="dict in statusOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏：新增 / 批量删除（未勾选时禁用） -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['blog:category:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['blog:category:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表：按 sort 升序展示；articleCount 为后端统计，非 0 时该分类不可删除 -->
    <el-table v-loading="loading" :data="categoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分类名称" align="left" prop="categoryName" />
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="文章数" align="center" prop="articleCount" width="90" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="statusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="140">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['blog:category:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['blog:category:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页：pageNum/pageSize 与查询参数双向绑定，翻页或改页长都会重新查询 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗：两种模式复用同一表单，categoryId 有值即编辑 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in statusOptions" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
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

<script setup name="BlogCategory">
/**
 * 博客分类管理（管理员）：条件查询 + 分页列表 + 新增/编辑/删除弹窗。
 * 接口：@/api/blog/category 的 listCategory/getCategory/addCategory/updateCategory/delCategory。
 * 权限：页面菜单与后端接口都只对管理员开放（blog:category:*），普通用户看不到入口。
 * 约定：status '0'=正常 '1'=停用；sort 越小越靠前；列表返回 res.rows/res.total。
 *       分类下还有文章时后端会拒绝删除（不会级联删文章），重名校验也由后端负责。
 */
import { listCategory, getCategory, addCategory, updateCategory, delCategory } from '@/api/blog/category'

const { proxy } = getCurrentInstance()

// 分类状态字典：0=正常、1=停用；dict-tag 依赖 elTagType 着色
const statusOptions = [
  { value: '0', label: '正常', elTagType: 'success' },
  { value: '1', label: '停用', elTagType: 'danger' }
]

const categoryList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')

// form 同时服务新增与编辑（categoryId 为空走新增）；pageNum/pageSize 与分页组件双向绑定；
// 校验规则：分类名称必填（blur 触发），sort 由 el-input-number 限制为不小于 0
const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, categoryName: undefined, status: undefined },
  rules: { categoryName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listCategory(queryParams.value).then(response => {
    categoryList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = { categoryId: undefined, categoryName: undefined, sort: 0, status: '0', remark: undefined }
  proxy.resetForm('categoryRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

// 勾选变化：ids 供工具栏批量删除使用；multiple 取反后控制删除按钮禁用态
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.categoryId)
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加分类'
}

function handleUpdate(row) {
  reset()
  getCategory(row.categoryId).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改分类'
  })
}

// 以 categoryId 是否存在区分新增/编辑；校验通过后保存并刷新当前页
function submitForm() {
  proxy.$refs['categoryRef'].validate(valid => {
    if (!valid) return
    if (form.value.categoryId != undefined) {
      updateCategory(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addCategory(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

// 行内删除传 row；工具栏删除拿不到 row（事件对象没有 categoryId），此时回退到勾选的 ids；
// 分类为物理删除、不可恢复，所以先二次确认；用户取消会在 catch 里被静默忽略
function handleDelete(row) {
  const categoryIds = row.categoryId || ids.value
  proxy.$modal.confirm('确认删除选中的分类？').then(() => {
    return delCategory(categoryIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
