<template>
  <div class="portal-container">
    <div v-loading="loading" class="p-article-page">
      <!-- 左栏：正文 + 评论；右栏是侧栏。加载中由外层 v-loading 出遮罩 -->
      <div class="p-article-main">
        <!-- 文章正文 -->
        <template v-if="article.articleId">
        <article class="p-card p-article">
          <h1 class="p-article__title">{{ article.title }}</h1>
          <div class="p-article__meta">
            <span>{{ article.authorNickName || '博主' }}</span>
            <span>发布于 {{ publishText }}</span>
            <span v-if="article.categoryName">{{ article.categoryName }}</span>
            <span>阅读 {{ article.viewCount || 0 }}</span>
            <span>评论 {{ article.commentCount || 0 }}</span>
          </div>

          <div v-if="article.coverUrl" class="p-article__cover">
            <img :src="article.coverUrl" :alt="article.title" />
          </div>

          <!-- 正文原样交给 MarkdownViewer 渲染（代码块/表格/公式），summary 只在列表页用 -->
          <div class="p-article__body">
            <markdown-viewer :content="article.content" />
          </div>

          <!-- 标签跳首页并按 tagId 过滤，与侧栏标签的落点保持一致 -->
          <div v-if="tagList.length" class="p-article__tags">
            <router-link
              v-for="t in tagList"
              :key="t.tagId"
              class="p-tag"
              :to="{ path: '/portal/home', query: { tagId: t.tagId } }"
            ># {{ t.tagName }}</router-link>
          </div>
        </article>

        <!-- 评论 -->
        <section id="comments" class="p-card p-comments">
          <h3 class="p-section-title">评论（{{ comments.length }}）</h3>

          <!-- 评论输入框仅登录可见；长度上限 1000 与后端校验保持一致，超长由后端兜底拦截 -->
          <div v-if="logged" class="p-comments__form">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="友善评论，理性发言…"
            />
            <div class="p-comments__actions">
              <el-button type="primary" :loading="submitting" @click="submitComment">发表评论</el-button>
            </div>
          </div>
          <div v-else class="p-comments__login-tip">
            <span>登录后即可参与评论</span>
            <el-button type="primary" round @click="goLogin">登录</el-button>
          </div>

          <!-- 评论列表由后端按楼层正序返回，前端不再排序；无评论时显示空态 -->
          <div class="p-comments__list">
            <div v-for="c in comments" :key="c.commentId" class="p-comment">
              <el-avatar :size="34">{{ (c.nickName || c.userName || '客').slice(0, 1) }}</el-avatar>
              <div class="p-comment__body">
                <div class="p-comment__head">
                  <span class="p-comment__name">{{ c.nickName || c.userName || '访客' }}</span>
                  <span class="p-comment__time">{{ parseTime(c.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
                </div>
                <p class="p-comment__text">{{ c.content }}</p>
              </div>
            </div>
            <el-empty v-if="!comments.length" description="还没有评论，来说两句吧" :image-size="70" />
          </div>
        </section>
      </template>

      <!-- 文章不存在 / 未发布 -->
      <div v-else-if="!loading" class="p-card p-article-empty">
        <el-empty description="文章不存在或尚未发布">
          <el-button type="primary" @click="router.push('/portal/home')">回首页</el-button>
        </el-empty>
      </div>
      </div>

      <!-- 侧栏只传站点信息与热门文章：详情页不再重复展示分类/标签/最新评论 -->
      <aside class="p-article-aside">
        <site-sidebar :site="site" :hot="hot" />
      </aside>
    </div>
  </div>
</template>

<script setup name="PortalArticle">
/**
 * 门户文章详情页（路由 /portal/article/:articleId）。
 *
 * 依赖接口：getPortalArticle（详情，每次请求后端浏览量 +1）、listPortalComments（评论）、
 * addComment（发表评论，需登录）、listPortalHot（侧栏热门）。
 * 文章未发布或不存在时空态提示「文章不存在或尚未发布」，不泄露草稿内容。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownViewer from '@/components/MarkdownViewer/index.vue'
import SiteSidebar from '@/portal/components/SiteSidebar.vue'
import { usePortalSite } from '@/portal/composables/usePortalSite'
import { getPortalArticle, listPortalComments, listPortalHot } from '@/api/portal'
import { addComment } from '@/api/blog/comment'
import useUserStore from '@/store/modules/user'
import { parseTime } from '@/utils/ruoyi'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { site } = usePortalSite()

const article = ref({})
const comments = ref([])
const hot = ref([])
// 初值为 true：首屏请求返回前先压住「文章不存在」空态，否则会先闪一下错误提示
const loading = ref(true)
// 提交中标记：按钮 loading 兼做防连点，避免同一段内容重复发两条评论
const submitting = ref(false)
const commentContent = ref('')

// 评论框与发表按钮都只认 token：未登录时换成一条登录引导，不做「点了再报错」
const logged = computed(() => !!userStore.token)
// 路由已用 (\d+) 约束，这里再转数字；非法值会得到 NaN，被 loadArticle 直接拦掉
const articleId = computed(() => Number(route.params.articleId))
// 详情页精确到分钟；未发布的文章没有 publishTime，此时「发布于」整段不显示
const publishText = computed(() => (article.value.publishTime ? parseTime(article.value.publishTime, '{y}-{m}-{d} {h}:{i}') : ''))
// 详情接口返回的是 tags 对象数组（列表页给的却是 tagNames 字符串），两者结构不同、不能混用
const tagList = computed(() => article.value.tags || [])

// 加载详情。注意详情接口会把浏览量 +1，所以只能在挂载与文章切换时调用，
// 不要把它塞进 computed 或模板表达式里，否则每次渲染都会凭空加一次阅读数。
function loadArticle() {
  if (!articleId.value) {
    loading.value = false
    return
  }
  loading.value = true
  // 先清空旧文章：站内切换文章时，避免上一篇的标题/标签在新数据返回前短暂残留
  article.value = {}
  getPortalArticle(articleId.value)
    .then(res => {
      article.value = res.data || {}
      loadComments()
      // 站内跳转不会自动复位滚动位置，手动回顶，否则会停在上一次的阅读位置
      window.scrollTo({ top: 0 })
    })
    .catch(() => {
      article.value = {}
    })
    .finally(() => {
      loading.value = false
    })
}

// 评论单独请求：正文成功后再拉；失败只清空评论，不影响正文展示
function loadComments() {
  listPortalComments(articleId.value)
    .then(res => {
      comments.value = res.data || []
    })
    .catch(() => {
      comments.value = []
    })
}

// 发表评论：前端只挡空内容（长度由输入框 maxlength 限制），敏感词/频率等规则仍以后端为准；
// 成功后本地先把评论数 +1（详情接口不会重拉），再重新拉列表，保证楼层与计数一致。
function submitComment() {
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  submitting.value = true
  addComment({ articleId: articleId.value, content })
    .then(() => {
      commentContent.value = ''
      article.value.commentCount = (article.value.commentCount || 0) + 1
      loadComments()
      ElMessage.success('评论发表成功')
    })
    .catch(() => {})
    .finally(() => {
      submitting.value = false
    })
}

// 带上 redirect=当前文章完整路径，登录后能回到原评论位置
function goLogin() {
  router.push({ path: '/portal/login', query: { redirect: route.fullPath } })
}

// 站内从一篇跳到另一篇时，路由只改参数、组件会被复用（不会重新 mounted），
// 所以必须监听 articleId 手动重新加载，否则页面停留在上一篇内容。
watch(articleId, loadArticle)

onMounted(() => {
  // 正文与侧栏热门并行请求：热门只是装饰，失败也不影响正文阅读
  loadArticle()
  listPortalHot(5).then(res => { hot.value = res.data || [] }).catch(() => {})
})
</script>

<style scoped>
/* 左栏用 minmax(0, 1fr) 而非 1fr：正文里的长代码块/表格会把 1fr 撑破，minmax 才能正确收缩 */
.p-article-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  align-items: start;
  margin-top: 26px;
  padding-bottom: 10px;
}

.p-article-empty {
  padding: 40px 0;
}

.p-article-main > * + * {
  margin-top: 18px;
}

/* 粘性侧栏的上偏移：需与固定顶栏高度视觉对齐，改顶栏高度时要同步调整这里 */
.p-article-aside {
  position: sticky;
  top: 84px;
}

.p-comments__actions {
  margin-top: 10px;
  text-align: right;
}

.p-comments__list {
  margin-top: 8px;
}

/* 窄屏改成单栏：侧栏落到正文下方，粘性定位必须一起取消，否则会贴在半屏处遮挡阅读 */
@media (max-width: 1024px) {
  .p-article-page {
    grid-template-columns: minmax(0, 1fr);
  }

  .p-article-aside {
    position: static;
  }
}
</style>
