<template>
  <div class="app-container">
    <!-- 搜索区：标题模糊匹配 + 状态 + 分类，分类下拉只含启用分类 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="文章标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="文章状态" clearable style="width: 160px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width: 160px">
          <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏：新增 / 批量删除；搜索栏的显隐由 right-toolbar 控制 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['blog:article:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['blog:article:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表：标题可点开详情；tagNames 由后端聚合为逗号串，置顶行排在最前 -->
    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="标题" align="left" prop="title" :show-overflow-tooltip="true" min-width="220">
        <template #default="scope">
          <el-link type="primary" @click="handleView(scope.row)">{{ scope.row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" prop="categoryName" width="110" />
      <el-table-column label="标签" align="center" prop="tagNames" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="作者" align="center" prop="authorNickName" width="100" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="statusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="置顶" align="center" width="70">
        <template #default="scope">
          <el-tag v-if="scope.row.isTop === '1'" type="danger" size="small">置顶</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="浏览" align="center" prop="viewCount" width="70" />
      <el-table-column label="评论" align="center" prop="commentCount" width="70" />
      <el-table-column label="发布时间" align="center" prop="publishTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.publishTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <!-- 操作列：审核/发布/下架仅管理员可见，按钮由行状态决定；后端还会再校验一次角色 -->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['blog:article:edit']">编辑</el-button>
          <el-button v-if="isAdmin && scope.row.status === '2'" link type="success" icon="CircleCheck" @click="handleChangeStatus(scope.row, '1')" v-hasPermi="['blog:article:audit']">通过</el-button>
          <el-button v-if="isAdmin && scope.row.status === '2'" link type="danger" icon="CircleClose" @click="handleChangeStatus(scope.row, '3')" v-hasPermi="['blog:article:audit']">驳回</el-button>
          <el-button v-if="isAdmin && scope.row.status !== '1'" link type="success" icon="Promotion" @click="handleChangeStatus(scope.row, '1')" v-hasPermi="['blog:article:publish']">发布</el-button>
          <el-button v-if="isAdmin && scope.row.status === '1'" link type="warning" icon="Download" @click="handleChangeStatus(scope.row, '0')" v-hasPermi="['blog:article:publish']">下架</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['blog:article:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页：页码/页长变化都会带着当前查询条件重新请求 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="BlogArticle">
/**
 * 博客文章管理列表：按标题/状态/分类检索 + 分页 + 批量删除。
 * 接口：listArticle / delArticle / changeArticleStatus（@/api/blog/article），
 *       分类筛选项来自 listEnabledCategory（只含 status='0' 的分类）。
 * 权限：进入列表只需 blog:article:list；按钮再按 blog:article:add/edit/remove 细分；
 *       普通用户列表只返回本人文章，审核/发布/下架仅 admin 角色可见；
 *       普通用户保存时只能落在草稿或待审核，不能直接发布。
 * 状态：0=草稿 1=已发布 2=待审核 3=已驳回；通过/发布都置 1，驳回置 3，下架回到 0。
 * 列表返回 TableDataInfo（res.rows/res.total），后端按 is_top DESC, article_id DESC 排序。
 */
import { useRouter } from 'vue-router'
import { listArticle, delArticle, changeArticleStatus } from '@/api/blog/article'
import { listEnabledCategory } from '@/api/blog/category'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const { proxy } = getCurrentInstance()
const userStore = useUserStore()

// 文章状态字典：elTagType 决定 dict-tag 颜色；status 是字符串，模板比较时不能写成数字
const statusOptions = [
  { value: '0', label: '草稿', elTagType: 'info' },
  { value: '1', label: '已发布', elTagType: 'success' },
  { value: '2', label: '待审核', elTagType: 'warning' },
  { value: '3', label: '已驳回', elTagType: 'danger' }
]

// 只用于审核/发布类按钮的显示：普通用户即使伪造请求，后端也会把状态收敛为待审核
const isAdmin = computed(() => userStore.roles.includes('admin'))

const articleList = ref([])
const categoryOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    status: undefined,
    categoryId: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listArticle(queryParams.value).then(response => {
    articleList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 分类下拉单独取"启用"列表（返回数组），与列表接口的 res.rows/res.total 结构不同
function getCategories() {
  listEnabledCategory().then(res => {
    categoryOptions.value = res.data || []
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
  ids.value = selection.map(item => item.articleId)
  multiple.value = !selection.length
}

// 新增/编辑/详情都走独立路由页：/blog/article-edit[/:articleId]、/blog/article-detail/:articleId
function handleAdd() {
  router.push('/blog/article-edit')
}

function handleEdit(row) {
  router.push('/blog/article-edit/' + row.articleId)
}

function handleView(row) {
  router.push('/blog/article-detail/' + row.articleId)
}

// 状态流转：1=发布/通过，3=驳回，0=下架回草稿；确认框带上文章标题，避免误操作
function handleChangeStatus(row, status) {
  const tip = { '1': '发布', '0': '下架', '3': '驳回' }[status] || '操作'
  proxy.$modal.confirm('确认' + tip + '文章《' + row.title + '》？').then(() => {
    return changeArticleStatus({ articleId: row.articleId, status: status })
  }).then(() => {
    proxy.$modal.msgSuccess(tip + '成功')
    getList()
  }).catch(() => {})
}

// 行内删除传 row；工具栏删除拿不到 row（事件对象没有 articleId），此时回退到勾选的 ids；
// 文章与标签关联都是物理删除、不可恢复，所以先二次确认；普通用户批量删到他人文章时后端整批拒绝
function handleDelete(row) {
  const articleIds = row.articleId || ids.value
  proxy.$modal.confirm('确认删除选中的文章？').then(() => {
    return delArticle(articleIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getCategories()
getList()
</script>
