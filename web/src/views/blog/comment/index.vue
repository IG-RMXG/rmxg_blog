<template>
  <div class="app-container">
    <!-- 搜索区：评论内容关键字 + 状态，回车即查询 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="评论内容" prop="content">
        <el-input v-model="queryParams.content" placeholder="请输入评论关键词" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="评论状态" clearable style="width: 160px">
          <el-option v-for="dict in statusOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏：只有批量删除；评论的发表入口在前台文章详情页，后台不提供新增 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['blog:comment:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表：按 comment_id 倒序；文章标题/评论内容过长时省略并用 tooltip 展示全文 -->
    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文章标题" align="left" prop="articleTitle" :show-overflow-tooltip="true" min-width="180" />
      <el-table-column label="评论内容" align="left" prop="content" :show-overflow-tooltip="true" min-width="220" />
      <el-table-column label="评论人" align="center" width="120">
        <template #default="scope">
          <span>{{ scope.row.nickName || scope.row.userName || '匿名' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="statusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="评论时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <!-- 审核按钮按当前状态互斥：正常可隐藏、隐藏可通过；删除是物理删除并回减文章评论数 -->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button v-if="scope.row.status === '1'" link type="success" icon="CircleCheck" @click="handleStatus(scope.row, '0')" v-hasPermi="['blog:comment:audit']">通过</el-button>
          <el-button v-if="scope.row.status === '0'" link type="warning" icon="Hide" @click="handleStatus(scope.row, '1')" v-hasPermi="['blog:comment:audit']">隐藏</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['blog:comment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页：页码/页长变化都会带着当前查询条件重新请求 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="BlogComment">
/**
 * 博客评论管理（管理员）：按内容关键字/状态检索，支持审核（正常/隐藏）与删除。
 * 接口：@/api/blog/comment 的 listComment/changeCommentStatus/delComment。
 * 权限：查看列表需 blog:comment:list；审核需 blog:comment:audit；删除需 blog:comment:remove。
 * 状态：'0'=正常（前台展示） '1'=隐藏（前台接口强制过滤）；审核只在 0/1 间切换，删除是物理删除。
 * 计数：新增/删除评论会联动文章 comment_count，审核不改变计数。
 */
import { listComment, changeCommentStatus, delComment } from '@/api/blog/comment'

const { proxy } = getCurrentInstance()

// 评论状态字典：0=正常、1=隐藏；与文章状态（草稿/发布等）无关，不要混用
const statusOptions = [
  { value: '0', label: '正常', elTagType: 'success' },
  { value: '1', label: '隐藏', elTagType: 'info' }
]

const commentList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)

// 查询条件直接透传后端：content 模糊匹配，status 为空表示查全部状态
const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, content: undefined, status: undefined }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listComment(queryParams.value).then(response => {
    commentList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.commentId)
  multiple.value = !selection.length
}

// 传目标状态而不是"切换"指令：'0' 放行、'1' 隐藏；成功后刷新列表，按钮文案由模板按当前状态决定
function handleStatus(row, status) {
  changeCommentStatus({ commentId: row.commentId, status: status }).then(() => {
    proxy.$modal.msgSuccess(status === '0' ? '已通过' : '已隐藏')
    getList()
  })
}

// 行内删除传 row；工具栏删除拿不到 row（事件对象没有 commentId），此时回退到勾选的 ids；
// 删除为物理删除，二次确认后调接口，用户取消会在 catch 里被静默忽略
function handleDelete(row) {
  const commentIds = row.commentId || ids.value
  proxy.$modal.confirm('确认删除选中的评论？').then(() => {
    return delComment(commentIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
