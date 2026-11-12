<template>
  <div class="portal-container">
    <!-- 归档页：左侧是按月统计的月份列表，右侧是选中月份下的已发布文章 -->
    <div class="p-page-head">
      <h1>归档</h1>
      <p class="p-muted">按月份浏览已发布文章，共 {{ totalArticles }} 篇</p>
    </div>

    <!-- 有归档数据才渲染左右两栏；没有已发布文章时走下方 v-else 的空态 -->
    <div v-if="archive.length" class="p-archive">
      <!-- 月份按钮：高亮与否只看 URL 里的 month，点击只改 URL，请求统一由 watch 触发 -->
      <aside class="p-card p-archive__months">
        <button
          v-for="item in archive"
          :key="item.month"
          class="p-archive__month"
          :class="{ active: item.month === activeMonth }"
          @click="selectMonth(item.month)"
        >
          <span>{{ formatMonth(item.month) }}</span>
          <em>{{ item.count }}</em>
        </button>
      </aside>

      <!-- 选中月份的文章列表：只在选月后请求，一次取 50 条（归档通常按月看，页内不再分页） -->
      <section v-loading="loading" class="p-card p-archive__list">
        <div v-for="a in articles" :key="a.articleId" class="p-archive__item">
          <span class="p-archive__date">{{ dayText(a) }}</span>
          <div class="p-archive__main">
            <router-link :to="'/portal/article/' + a.articleId">{{ a.title }}</router-link>
            <div class="p-muted p-archive__meta">
              <span v-if="a.categoryName">{{ a.categoryName }}</span>
              <span>阅读 {{ a.viewCount || 0 }}</span>
              <span>评论 {{ a.commentCount || 0 }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && !articles.length" description="该月份没有文章" :image-size="70" />
      </section>
    </div>

    <div v-else class="p-card p-archive-empty">
      <el-empty description="还没有已发布的文章" />
    </div>
  </div>
</template>

<script setup name="PortalArchive">
/**
 * 门户归档页。
 *
 * 数据来源：listPortalArchive() 返回已发布文章按月统计的 [{ month: '2025-08', count: n }]，
 * listPortalArticles({ month }) 取某月的文章（按月浏览，一次取 50 条，不做页内分页）。
 * 当前月份只以 URL 的 ?month= 为准（可分享、可前进后退），切月只改 URL，
 * 真正的请求由 watch(activeMonth) 统一触发。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listPortalArchive, listPortalArticles } from '@/api/portal'
import { parseTime } from '@/utils/ruoyi'

const route = useRoute()
const router = useRouter()

const archive = ref([])
const articles = ref([])
const loading = ref(false)

// 当前月份的唯一定义来源是 URL；空串表示还没选月（首屏会补成最近的一个月）
const activeMonth = computed(() => (route.query.month ? String(route.query.month) : ''))
// 头部总篇数由各月计数累加得出，省掉一次「全部文章总数」的请求；没有归档时自然是 0
const totalArticles = computed(() => archive.value.reduce((sum, item) => sum + (item.count || 0), 0))

// '2025-08' → '2025 年 8 月'；格式不符合预期时原样返回，避免页面出现 "undefined 月"
function formatMonth(month) {
  const [y, m] = String(month || '').split('-')
  return y && m ? `${y} 年 ${Number(m)} 月` : month
}

// 条目上只显示「月-日」，年份已由左侧选中的月份表达，避免每条都重复写年份
function dayText(article) {
  const t = article.publishTime || article.createTime
  return t ? parseTime(t, '{m}-{d}') : ''
}

// 只改 URL（push 以保留浏览历史），列表请求交给 watch(activeMonth)、不在这里重复发一次
function selectMonth(month) {
  router.push({ path: '/portal/archive', query: { month } })
}

// 没有选中月份就不发请求（首屏由 onMounted 先把 month 补进 URL）；
// 请求失败时清空列表，避免上一个月的内容残留在页面上、看起来像加载成功了。
function loadArticles() {
  if (!activeMonth.value) {
    articles.value = []
    return
  }
  loading.value = true
  listPortalArticles({ month: activeMonth.value, pageNum: 1, pageSize: 50, orderBy: 'latest' })
    .then(res => {
      articles.value = res.rows || []
    })
    .catch(() => {
      articles.value = []
    })
    .finally(() => {
      loading.value = false
    })
}

// 月份变化（点月份、浏览器前进后退、首屏 replace）都从这里重新拉数据，是唯一的加载入口
watch(activeMonth, loadArticles)

onMounted(() => {
  // 首屏只取按月统计，文章列表等月份确定后再取，避免多打一次请求
  listPortalArchive()
    .then(res => {
      archive.value = res.data || []
      // 这里分三种走法：URL 没带 month → 由下面的 replace 补上月份、交给 watch 去加载；
      // 完全没有归档 → 清空列表；URL 已带 month（刷新或分享链接进来）→ 直接加载，
      // 因为 watch 只在值发生变化时才回调，此时月份没变、不会自动触发。
      // 首次进入默认展开最近的月份，并把月份写进 URL（可分享、可前进后退）
      if (!activeMonth.value && archive.value.length) {
        router.replace({ path: '/portal/archive', query: { month: archive.value[0].month } })
      } else if (!archive.value.length) {
        articles.value = []
      } else {
        loadArticles()
      }
    })
    .catch(() => {
      archive.value = []
    })
})
</script>

<style scoped>
/* flex 子项默认 min-width:auto，长标题会把这一列撑破，置 0 后才能正常收缩换行 */
.p-archive__main {
  min-width: 0;
  flex: 1;
}

.p-archive__meta {
  display: flex;
  gap: 14px;
  margin-top: 4px;
}

.p-archive-empty {
  padding: 30px 0;
}
</style>
