<template>
  <div class="portal-container">
    <div v-loading="loading" class="p-article-page">
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

          <div class="p-article__body">
            <markdown-viewer :content="article.content" />
          </div>

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

      <aside class="p-article-aside">
        <site-sidebar :site="site" :hot="hot" />
      </aside>
    </div>
  </div>
</template>

<script setup name="PortalArticle">
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
const loading = ref(true)
const submitting = ref(false)
const commentContent = ref('')

const logged = computed(() => !!userStore.token)
const articleId = computed(() => Number(route.params.articleId))
const publishText = computed(() => (article.value.publishTime ? parseTime(article.value.publishTime, '{y}-{m}-{d} {h}:{i}') : ''))
const tagList = computed(() => article.value.tags || [])

function loadArticle() {
  if (!articleId.value) {
    loading.value = false
    return
  }
  loading.value = true
  article.value = {}
  getPortalArticle(articleId.value)
    .then(res => {
      article.value = res.data || {}
      loadComments()
      window.scrollTo({ top: 0 })
    })
    .catch(() => {
      article.value = {}
    })
    .finally(() => {
      loading.value = false
    })
}

function loadComments() {
  listPortalComments(articleId.value)
    .then(res => {
      comments.value = res.data || []
    })
    .catch(() => {
      comments.value = []
    })
}

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

function goLogin() {
  router.push({ path: '/portal/login', query: { redirect: route.fullPath } })
}

watch(articleId, loadArticle)

onMounted(() => {
  loadArticle()
  listPortalHot(5).then(res => { hot.value = res.data || [] }).catch(() => {})
})
</script>

<style scoped>
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

@media (max-width: 1024px) {
  .p-article-page {
    grid-template-columns: minmax(0, 1fr);
  }

  .p-article-aside {
    position: static;
  }
}
</style>
