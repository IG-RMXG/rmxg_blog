<template>
  <div class="app-container">
    <!-- 搜索区：标签名称模糊匹配，回车即查询 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="标签名称" prop="tagName">
        <el-input v-model="queryParams.tagName" placeholder="请输入标签名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏：新增 / 批量删除（未勾选时禁用） -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['blog:tag:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['blog:tag:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表：按 sort 升序；articleCount 为引用该标签的文章数（不区分文章状态），删除标签不会删文章 -->
    <el-table v-loading="loading" :data="tagList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="标签名称" align="left" prop="tagName" />
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="文章数" align="center" prop="articleCount" width="90" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="140">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['blog:tag:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['blog:tag:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页：页码/页长变化都会带着当前查询条件重新请求 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗：tagId 有值即编辑，表单与校验规则共用 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="tagRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" placeholder="请输入标签名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
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

<script setup name="BlogTag">
/**
 * 博客标签管理（管理员）：条件查询 + 分页列表 + 新增/编辑/删除弹窗。
 * 接口：@/api/blog/tag 的 listTag/getTag/addTag/updateTag/delTag。
 * 权限：菜单与接口均限管理员（blog:tag:*）；列表接口对非管理员返回空表而不是报错。
 * 约定：tagName 全局唯一（改名时排除自身）；sort 越小越靠前；列表返回 res.rows/res.total。
 *       删除标签会在同一事务里先清文章-标签关联，文章本身不受影响。
 */
import { listTag, getTag, addTag, updateTag, delTag } from '@/api/blog/tag'

const { proxy } = getCurrentInstance()

const tagList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')

// form 同时服务新增与编辑（tagId 为空走新增）；pageNum/pageSize 与分页组件双向绑定；
// 校验规则：标签名称必填（blur 触发），sort 由 el-input-number 限制为不小于 0
const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, tagName: undefined },
  rules: { tagName: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listTag(queryParams.value).then(response => {
    tagList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = { tagId: undefined, tagName: undefined, sort: 0, remark: undefined }
  proxy.resetForm('tagRef')
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
  ids.value = selection.map(item => item.tagId)
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加标签'
}

function handleUpdate(row) {
  reset()
  getTag(row.tagId).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改标签'
  })
}

// 以 tagId 是否存在区分新增/编辑；校验通过后保存并刷新当前页
function submitForm() {
  proxy.$refs['tagRef'].validate(valid => {
    if (!valid) return
    if (form.value.tagId != undefined) {
      updateTag(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addTag(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

// 行内删除传 row；工具栏删除拿不到 row（事件对象没有 tagId），此时回退到勾选的 ids；
// 标签为物理删除、不可恢复，所以先二次确认；用户取消会在 catch 里被静默忽略
function handleDelete(row) {
  const tagIds = row.tagId || ids.value
  proxy.$modal.confirm('确认删除选中的标签？').then(() => {
    return delTag(tagIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
