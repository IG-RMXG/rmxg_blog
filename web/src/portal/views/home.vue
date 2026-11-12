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

    <!-- 两栏：左侧列表随筛选变化，右侧侧栏是一次性加载的固定数据 -->
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

        <!-- 只有超过一页时才出现分页条，单页时不留无意义的页码占位 -->
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

      <!-- 侧栏：分类、标签、热门、最新评论在 onMounted 里一次加载，切筛选条件时不重新请求 -->
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
/**
 * 门户首页（/portal/home）：站点横幅、搜索、分类/排序筛选、文章列表分页与右侧栏。
 *
 * 核心约定：列表的筛选状态全部存在 URL query 里（categoryId / tagId / keyword / orderBy / pageNum），
 * URL 是唯一数据源——刷新、前进后退、分享链接都能还原同一个列表。
 * 所有筛选动作只改 URL，真正的请求由 watch(route.query, ..., { immediate: true }) 统一发起。
 */
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

// 每页条数写死在这里（没有「滚动加载更多」那种追加逻辑），改这一个常量即可调整整页密度
const PAGE_SIZE = 8

// value 就是后端 orderBy 的取值；latest 是默认排序，所以不会出现在 URL 里
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

// 输入框缓冲：输入过程中不发请求，只有回车/点搜索时才写进 URL；URL 变化时由下面的 watch 回填
const searchInput = ref('')

// URL 是唯一数据源：刷新/前进后退/分享链接都能复现同一个列表
const activeCategoryId = computed(() => route.query.categoryId || '')
const activeKeyword = computed(() => (route.query.keyword ? String(route.query.keyword) : ''))
const activeOrderBy = computed(() => (route.query.orderBy ? String(route.query.orderBy) : 'latest'))
const activePageNum = computed(() => Number(route.query.pageNum || 1))
const pageSize = PAGE_SIZE

// 当前标签名要用标签列表反查；标签数据还没回来、或该标签已被删除时用「标签」占位，避免提示里出现空引号
const activeTag = computed(() => {
  const id = route.query.tagId
  return id ? tags.value.find(t => String(t.tagId) === String(id)) || { tagName: '标签' } : null
})

// 路由 query 一变就重新拉列表：切分类/标签、改排序、翻页、浏览器前进后退都走这一条路径；
// immediate 让首屏也加载一次，这也是 onMounted 里不再重复请求列表的原因。
watch(
  () => route.query,
  () => {
    searchInput.value = activeKeyword.value
    loadArticles()
  },
  { immediate: true }
)

// 按 URL 上的筛选条件取列表；空值不拼进参数，直接交给后端用默认查询条件
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

// 统一改 URL 的入口：用 push（而非 replace）保证每次筛选都能用浏览器后退回退，
// 并顺手清掉空值，让地址栏只保留真正生效的条件。
function pushQuery(patch) {
  const next = { ...route.query, ...patch }
  // 去掉空值，保持地址栏干净
  Object.keys(next).forEach(k => {
    if (next[k] === '' || next[k] === null || next[k] === undefined) delete next[k]
  })
  router.push({ path: '/portal/home', query: next })
}

// 换分类必须回到第 1 页：否则会停在旧分类的页码上，新分类的该页可能根本不存在
function selectCategory(categoryId) {
  pushQuery({ categoryId: categoryId || '', pageNum: '' })
}

// latest 是默认排序，不写进 URL，避免同一份列表出现两个等价地址（不利于分享与缓存）
function selectOrder(orderBy) {
  pushQuery({ orderBy: orderBy === 'latest' ? '' : orderBy, pageNum: '' })
}

// 搜索只在这里提交（回车或点按钮），输入过程中不触发请求
function submitSearch() {
  pushQuery({ keyword: searchInput.value.trim(), pageNum: '' })
}

// 只清「筛选」类参数，保留 orderBy 等其它条件（用户可能就是从排序链接进来的）
function clearFilters() {
  const next = { ...route.query }
  delete next.keyword
  delete next.tagId
  delete next.categoryId
  delete next.pageNum
  router.push({ path: '/portal/home', query: next })
}

// 翻页后平滑回顶：否则用户会停在上一页列表末尾，看不到新一页的开头
function goPage(pageNum) {
  pushQuery({ pageNum })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  searchInput.value = activeKeyword.value
  // 侧栏数据与文章列表分开加载：它们不随筛选变化，整个会话只取一次
  // 四个请求并发发出、各自 catch，任一失败只影响对应区块，不会连累整页
  listPortalCategories().then(res => { categories.value = res.data || [] }).catch(() => {})
  listPortalTags().then(res => { tags.value = res.data || [] }).catch(() => {})
  listPortalHot(5).then(res => { hot.value = res.data || [] }).catch(() => {})
  listPortalLatestComments(5).then(res => { latestComments.value = res.data || [] }).catch(() => {})
})
</script>
