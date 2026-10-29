<template>
  <div class="portal-container">
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listArticle, delArticle, changeArticleStatus } from '@/api/blog/article'
import { parseTime } from '@/utils/ruoyi'

const router = useRouter()

const statusMap = {
  0: '草稿',
  1: '已发布',
  2: '待审核',
  3: '已驳回'
}

const statusFilters = [
  { label: '全部', value: '' },
  { label: '草稿', value: '0' },
  { label: '待审核', value: '2' },
  { label: '已发布', value: '1' },
  { label: '已驳回', value: '3' }
]

const PAGE_SIZE = 8

const articles = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const activeStatus = ref('')

const pageSize = PAGE_SIZE

const query = computed(() => {
  const params = { pageNum: pageNum.value, pageSize }
  if (activeStatus.value) params.status = activeStatus.value
  return params
})

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

function selectStatus(status) {
  activeStatus.value = status
  pageNum.value = 1
  load()
}

function goPage(page) {
  pageNum.value = page
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function submitReview(article) {
  changeArticleStatus({ articleId: article.articleId, status: '2' })
    .then(() => {
      ElMessage.success('已提交审核')
      load()
    })
    .catch(() => {})
}

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
