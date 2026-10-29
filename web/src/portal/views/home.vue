<template>
  <div class="portal-container">
    <!-- 顶部横幅 -->
    <section class="p-hero">
      <div>
        <h1>{{ site.title || '我的博客' }}</h1>
        <p class="p-hero__desc">{{ site.subtitle || '记录、思考与分享' }}</p>
        <div class="p-hero__stats">
          <div>
            <strong>{{ site.articleCount || 0 }}</strong>
            <span>篇文章</span>
          </div>
          <div>
            <strong>{{ site.categoryCount || 0 }}</strong>
            <span>个分类</span>
          </div>
          <div>
            <strong>{{ site.tagCount || 0 }}</strong>
            <span>个标签</span>
          </div>
          <div>
            <strong>{{ site.viewCount || 0 }}</strong>
            <span>次阅读</span>
          </div>
        </div>
      </div>
      <div class="p-hero__search">
        <el-input
          v-model="searchInput"
          size="large"
          placeholder="搜索标题或摘要…"
          clearable
          @keyup.enter="submitSearch"
        >
          <template #append>
            <el-button @click="submitSearch">搜索</el-button>
          </template>
        </el-input>
        <div class="p-hero__hint">共 {{ total }} 篇已发布文章</div>
      </div>
    </section>

    <div class="p-layout">
      <main class="p-layout__main">
        <!-- 分类筛选 + 排序 -->
        <div class="p-filter">
          <div class="p-filter__group">
            <button class="p-chip" :class="{ active: !activeCategoryId }" @click="selectCategory(null)">全部</button>
            <button
              v-for="c in categories"
              :key="c.categoryId"
              class="p-chip"
              :class="{ active: String(activeCategoryId) === String(c.categoryId) }"
              @click="selectCategory(c.categoryId)"
            >
              {{ c.categoryName }}
              <em>{{ c.articleCount || 0 }}</em>
            </button>
          </div>
          <div class="p-sort">
            <span
              v-for="s in sortOptions"
              :key="s.value"
              :class="{ active: activeOrderBy === s.value }"
              @click="selectOrder(s.value)"
            >{{ s.label }}</span>
          </div>
        </div>

        <!-- 当前筛选条件 -->
        <div v-if="activeTag || activeKeyword" class="p-active-filter">
          <span v-if="activeKeyword">搜索「{{ activeKeyword }}」</span>
          <span v-if="activeTag">标签「{{ activeTag.tagName }}」</span>
          <el-button link type="primary" @click="clearFilters">清除筛选</el-button>
        </div>

        <!-- 文章列表 -->
        <div v-loading="loading" class="p-articles">
          <article-card v-for="a in articles" :key="a.articleId" :article="a" />
          <el-empty
            v-if="!loading && !articles.length"
            :description="activeKeyword || activeTag ? '没有匹配的文章，换个条件试试' : '这里还没有发布的文章'"
          />
        </div>

        <div v-if="total > pageSize" class="p-pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="activePageNum"
            @current-change="goPage"
          />
        </div>
      </main>

      <aside class="p-layout__side">
        <site-sidebar
          :site="site"
          :categories="categories"
          :tags="tags"
          :hot="hot"
          :comments="latestComments"
        />
      </aside>
    </div>
  </div>
</template>

<script setup name="PortalHome">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '@/portal/components/ArticleCard.vue'
import SiteSidebar from '@/portal/components/SiteSidebar.vue'
import { usePortalSite } from '@/portal/composables/usePortalSite'
import {
  listPortalArticles,
  listPortalCategories,
  listPortalTags,
  listPortalHot,
  listPortalLatestComments
} from '@/api/portal'

const route = useRoute()
const router = useRouter()
const { site } = usePortalSite()

const PAGE_SIZE = 8

const sortOptions = [
  { label: '最新', value: 'latest' },
  { label: '热门', value: 'hot' },
  { label: '评论', value: 'comments' }
]

const articles = ref([])
const total = ref(0)
const loading = ref(false)

const categories = ref([])
const tags = ref([])
const hot = ref([])
const latestComments = ref([])

const searchInput = ref('')

// URL 是唯一数据源：刷新/前进后退/分享链接都能复现同一个列表
const activeCategoryId = computed(() => route.query.categoryId || '')
const activeKeyword = computed(() => (route.query.keyword ? String(route.query.keyword) : ''))
const activeOrderBy = computed(() => (route.query.orderBy ? String(route.query.orderBy) : 'latest'))
const activePageNum = computed(() => Number(route.query.pageNum || 1))
const pageSize = PAGE_SIZE

const activeTag = computed(() => {
  const id = route.query.tagId
  return id ? tags.value.find(t => String(t.tagId) === String(id)) || { tagName: '标签' } : null
})

watch(
  () => route.query,
  () => {
    searchInput.value = activeKeyword.value
    loadArticles()
  },
  { immediate: true }
)

function loadArticles() {
  loading.value = true
  const params = {
    pageNum: activePageNum.value,
    pageSize,
    orderBy: activeOrderBy.value
  }
  if (activeCategoryId.value) params.categoryId = activeCategoryId.value
  if (route.query.tagId) params.tagId = route.query.tagId
  if (activeKeyword.value) params.keyword = activeKeyword.value
  if (route.query.month) params.month = route.query.month

  listPortalArticles(params)
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

function pushQuery(patch) {
  const next = { ...route.query, ...patch }
  // 去掉空值，保持地址栏干净
  Object.keys(next).forEach(k => {
    if (next[k] === '' || next[k] === null || next[k] === undefined) delete next[k]
  })
  router.push({ path: '/portal/home', query: next })
}

function selectCategory(categoryId) {
  pushQuery({ categoryId: categoryId || '', pageNum: '' })
}

function selectOrder(orderBy) {
  pushQuery({ orderBy: orderBy === 'latest' ? '' : orderBy, pageNum: '' })
}

function submitSearch() {
  pushQuery({ keyword: searchInput.value.trim(), pageNum: '' })
}

function clearFilters() {
  const next = { ...route.query }
  delete next.keyword
  delete next.tagId
  delete next.categoryId
  delete next.pageNum
  router.push({ path: '/portal/home', query: next })
}

function goPage(pageNum) {
  pushQuery({ pageNum })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  searchInput.value = activeKeyword.value
  // 侧栏数据与文章列表分开加载：它们不随筛选变化，整个会话只取一次
  listPortalCategories().then(res => { categories.value = res.data || [] }).catch(() => {})
  listPortalTags().then(res => { tags.value = res.data || [] }).catch(() => {})
  listPortalHot(5).then(res => { hot.value = res.data || [] }).catch(() => {})
  listPortalLatestComments(5).then(res => { latestComments.value = res.data || [] }).catch(() => {})
})
</script>
