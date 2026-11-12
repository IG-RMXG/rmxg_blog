<template>
  <div class="portal-container">
    <!-- 「我的文章」：顶部状态筛选 + 新建入口，列表每一行的操作按钮按文章状态裁剪 -->
    <div class="p-page-head">
      <h1>我的文章</h1>
      <p class="p-muted">这里只显示你自己的文章；发布需管理员审核通过后才会出现在门户首页。</p>
    </div>

    <div class="p-me__toolbar">
      <div class="p-filter__group">
        <button
          v-for="s in statusFilters"
          :key="s.value"
          class="p-chip"
          :class="{ active: activeStatus === s.value }"
          @click="selectStatus(s.value)"
        >{{ s.label }}</button>
      </div>
      <div class="p-me__spacer"></div>
      <el-button type="primary" round @click="router.push('/portal/write')">写新文章</el-button>
    </div>

    <!-- 状态标签的配色类由 p-status--{status} 动态拼出（定义在 portal.css），改状态值时要同步改样式 -->
    <div v-loading="loading" class="p-card p-me__list">
      <div v-for="a in articles" :key="a.articleId" class="p-me__item">
        <span class="p-status" :class="'p-status--' + a.status">{{ statusMap[a.status] || '未知' }}</span>
        <div class="p-me__main">
          <router-link class="p-me__title" :to="a.status === '1' ? '/portal/article/' + a.articleId : '/portal/write/' + a.articleId">
            {{ a.title }}
          </router-link>
          <div class="p-me__meta">
            <span>{{ a.categoryName || '未分类' }}</span>
            <span v-if="a.tagNames">{{ a.tagNames }}</span>
            <span>{{ a.publishTime ? '发布于 ' + parseTime(a.publishTime, '{y}-{m}-{d}') : '更新于 ' + parseTime(a.updateTime || a.createTime, '{y}-{m}-{d}') }}</span>
            <span>阅读 {{ a.viewCount || 0 }}</span>
            <span>评论 {{ a.commentCount || 0 }}</span>
          </div>
        </div>
        <div class="p-me__actions">
          <el-button link type="primary" @click="router.push('/portal/write/' + a.articleId)">编辑</el-button>
          <el-button v-if="a.status === '1'" link type="primary" @click="router.push('/portal/article/' + a.articleId)">查看</el-button>
          <el-button v-if="a.status === '0' || a.status === '3'" link type="primary" @click="submitReview(a)">提交审核</el-button>
          <el-button link type="danger" @click="remove(a)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && !articles.length" description="还没有文章，去写第一篇吧">
        <el-button type="primary" @click="router.push('/portal/write')">写文章</el-button>
      </el-empty>
    </div>

    <!-- 只有超过一页才显示分页条，单页时不留空白占位 -->
    <div v-if="total > pageSize" class="p-pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="goPage"
      />
    </div>
  </div>
</template>

<script setup name="PortalMe">
/**
 * 门户「我的文章」列表页（/portal/me，路由带 requiresAuth）。
 *
 * 数据来自 listArticle：普通用户只能看到自己的文章，管理员能看到全部；筛选与分页都由后端完成。
 * 行内操作按文章状态裁剪——只有草稿/已驳回能提交审核（置为 2 待审核），只有已发布才有「查看」。
 * 本页不把状态写进 URL（与首页的约定不同），刷新后会回到「全部 + 第 1 页」。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listArticle, delArticle, changeArticleStatus } from '@/api/blog/article'
import { parseTime } from '@/utils/ruoyi'

const router = useRouter()

// 状态取值必须与后端 blog_article.status 保持一致，不能随意改动；未知值在模板里回退成「未知」
const statusMap = {
  0: '草稿',
  1: '已发布',
  2: '待审核',
  3: '已驳回'
}

// value 为空串表示「全部」，此时不会把 status 拼进查询条件；顺序按写作流程排，便于从草稿一路走到发布
const statusFilters = [
  { label: '全部', value: '' },
  { label: '草稿', value: '0' },
  { label: '待审核', value: '2' },
  { label: '已发布', value: '1' },
  { label: '已驳回', value: '3' }
]

// 与首页同量级的分页大小；这里的列表行更高，8 条差不多一屏
const PAGE_SIZE = 8

const articles = ref([])
const total = ref(0)
const loading = ref(false)
// 页码与状态筛选只存在组件内（不写 URL），所以切筛选时需要手动把页码重置为 1
const pageNum = ref(1)
const activeStatus = ref('')

const pageSize = PAGE_SIZE

// 只有选了具体状态才带 status 参数；否则后端返回当前用户可见的全部文章
const query = computed(() => {
  const params = { pageNum: pageNum.value, pageSize }
  if (activeStatus.value) params.status = activeStatus.value
  return params
})

// 列表接口对未登录返回空表而不是 401，所以这里不用再判断登录态；失败时清空表格和总数
function load() {
  loading.value = true
  listArticle(query.value)
    .then(res => {
      articles.value = res.rows || []
      total.value = res.total || 0
    })
    .catch(() => {
      articles.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

// 切筛选必须把页码拉回第 1 页：否则会带着旧页码去查新状态，容易查到空的一页
function selectStatus(status) {
  activeStatus.value = status
  pageNum.value = 1
  load()
}

// 翻页后平滑回顶，否则翻完页还停在上一页的列表末尾
function goPage(page) {
  pageNum.value = page
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 提交审核：普通用户只能把文章置为 2（待审核），1（已发布）只有管理员能直接设，所以这里固定传 '2'
function submitReview(article) {
  changeArticleStatus({ articleId: article.articleId, status: '2' })
    .then(() => {
      ElMessage.success('已提交审核')
      load()
    })
    .catch(() => {})
}

// 删除：先二次确认（取消也会走 catch，所以末尾统一吞掉 rejection）；
// 如果删掉的是当前页最后一条且不在第 1 页，先回退一页再刷新，避免停在一页空白上。
function remove(article) {
  ElMessageBox.confirm(`确定删除《${article.title}》吗？删除后不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(() => {
    return delArticle(article.articleId)
  }).then(() => {
    ElMessage.success('已删除')
    if (articles.value.length === 1 && pageNum.value > 1) {
      pageNum.value -= 1
    }
    load()
  }).catch(() => {})
}

onMounted(load)
</script>
