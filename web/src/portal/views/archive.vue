<template>
  <div class="portal-container">
    <div class="p-page-head">
      <h1>归档</h1>
      <p class="p-muted">按月份浏览已发布文章，共 {{ totalArticles }} 篇</p>
    </div>

    <div v-if="archive.length" class="p-archive">
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listPortalArchive, listPortalArticles } from '@/api/portal'
import { parseTime } from '@/utils/ruoyi'

const route = useRoute()
const router = useRouter()

const archive = ref([])
const articles = ref([])
const loading = ref(false)

const activeMonth = computed(() => (route.query.month ? String(route.query.month) : ''))
const totalArticles = computed(() => archive.value.reduce((sum, item) => sum + (item.count || 0), 0))

function formatMonth(month) {
  const [y, m] = String(month || '').split('-')
  return y && m ? `${y} 年 ${Number(m)} 月` : month
}

function dayText(article) {
  const t = article.publishTime || article.createTime
  return t ? parseTime(t, '{m}-{d}') : ''
}

function selectMonth(month) {
  router.push({ path: '/portal/archive', query: { month } })
}

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

watch(activeMonth, loadArticles)

onMounted(() => {
  listPortalArchive()
    .then(res => {
      archive.value = res.data || []
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
