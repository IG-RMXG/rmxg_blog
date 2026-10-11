<template>
  <div class="app-container">
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['blog:comment:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button v-if="scope.row.status === '1'" link type="success" icon="CircleCheck" @click="handleStatus(scope.row, '0')" v-hasPermi="['blog:comment:audit']">通过</el-button>
          <el-button v-if="scope.row.status === '0'" link type="warning" icon="Hide" @click="handleStatus(scope.row, '1')" v-hasPermi="['blog:comment:audit']">隐藏</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['blog:comment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="BlogComment">
import { listComment, changeCommentStatus, delComment } from '@/api/blog/comment'

const { proxy } = getCurrentInstance()

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

function handleStatus(row, status) {
  changeCommentStatus({ commentId: row.commentId, status: status }).then(() => {
    proxy.$modal.msgSuccess(status === '0' ? '已通过' : '已隐藏')
    getList()
  })
}

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
