<template>
  <div class="app-container">
    <!-- 文章主体：标题/作者/分类/时间等元信息、标签，以及 Markdown 正文 -->
    <el-card v-loading="loading" shadow="never" class="article-card">
      <div class="article-header">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
          <span>作者：{{ article.authorNickName || '-' }}</span>
          <span>分类：{{ article.categoryName || '未分类' }}</span>
          <span>发布于：{{ parseTime(article.publishTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          <span>浏览 {{ article.viewCount || 0 }}</span>
          <span>评论 {{ article.commentCount || 0 }}</span>
        </div>
        <div v-if="tagNames.length" class="article-tags">
          <el-tag v-for="t in tagNames" :key="t" size="small" class="tag-item">{{ t }}</el-tag>
        </div>
      </div>
      <el-divider />
      <markdown-viewer :content="article.content" />
    </el-card>

    <!-- 评论区：发表框 + 评论列表；无评论且加载完成时显示空状态 -->
    <el-card shadow="never" class="comment-card">
      <template #header>
        <span>评论（{{ comments.length }}）</span>
      </template>
      <div class="comment-form">
        <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="友善评论，理性发言" maxlength="1000" show-word-limit />
        <div class="comment-form-footer">
          <el-button type="primary" :loading="submitting" @click="submitComment">发表评论</el-button>
        </div>
      </div>
      <el-divider v-if="comments.length" />
      <div v-for="c in comments" :key="c.commentId" class="comment-item">
        <div class="comment-head">
          <span class="comment-user">{{ c.nickName || c.userName || '匿名' }}</span>
          <span class="comment-time">{{ parseTime(c.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </div>
        <div class="comment-content">{{ c.content }}</div>
      </div>
      <el-empty v-if="!loading && !comments.length" description="暂无评论" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup name="BlogArticleDetail">
/**
 * 文章详情/阅读页（后台 Layout 内，路由 /blog/article-detail/:articleId）。
 * 数据：viewArticle 读取正文并给已发布文章浏览量 +1（每次进入/刷新都会计数，后端无防刷）；
 *       listArticleComments 只返回正常评论（后端固定 status='0'），按 comment_id 倒序。
 * 评论：登录用户可发表，输入上限 1000 字；提交成功后清空输入框并重新拉取评论与文章信息。
 * 可读范围：普通用户只能看自己的文章或已发布文章，越权时后端返回 403。
 * 正文由 MarkdownViewer 渲染；面向读者的门户详情是另一个独立页面，本页不处理登录跳转。
 */
import { useRoute, useRouter } from 'vue-router'
import { viewArticle } from '@/api/blog/article'
import { listArticleComments, addComment } from '@/api/blog/comment'
import MarkdownViewer from '@/components/MarkdownViewer/index.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

// 路由参数是字符串，先转数字再请求；参数缺失或非法时 Number 得到 NaN，loadArticle 会直接跳过
const articleId = computed(() => Number(route.params.articleId))
const article = ref({})
const comments = ref([])
const commentContent = ref('')
const submitting = ref(false)
const loading = ref(true)

// 接口返回的 tags 是对象数组，这里只取名称用于展示；filter(Boolean) 过滤后端可能返回的空值
const tagNames = computed(() => {
  const tags = article.value.tags || []
  return tags.map(t => t.tagName).filter(Boolean)
})

// 注意：viewArticle 会累加 view_count，进入页面与"发表评论后刷新文章信息"都会触发一次
function loadArticle() {
  if (!articleId.value) return
  viewArticle(articleId.value).then(res => {
    article.value = res.data || {}
    loading.value = false
  })
}

// 该接口由后端强制限定 status='0'，隐藏评论不会出现在详情页
function loadComments() {
  listArticleComments(articleId.value).then(res => {
    comments.value = res.data || []
  })
}

// 前端只做非空校验（字数由 textarea 的 maxlength 限制为 1000）；作者信息由后端从 token 解析。
// submitting 既用于按钮 loading，也用于挡住连续点击造成的重复提交
function submitComment() {
  if (!commentContent.value.trim()) {
    proxy.$modal.msgWarning('评论内容不能为空')
    return
  }
  submitting.value = true
  addComment({ articleId: articleId.value, content: commentContent.value.trim() }).then(() => {
    commentContent.value = ''
    submitting.value = false
    proxy.$modal.msgSuccess('评论成功')
    loadComments()
    // 刷新文章评论数
    loadArticle()
  }).catch(() => {
    submitting.value = false
  })
}

loadArticle()
loadComments()
</script>

<style scoped>
.article-card { margin-bottom: 16px; }
.article-title { margin: 0 0 12px; font-size: 24px; line-height: 1.4; }
.article-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #909399; font-size: 13px; }
.article-tags { margin-top: 10px; }
.tag-item { margin-right: 6px; }

.comment-form-footer { margin-top: 10px; text-align: right; }
.comment-item { padding: 12px 0; border-bottom: 1px solid #f0f2f5; }
.comment-head { margin-bottom: 6px; }
.comment-user { font-weight: 600; color: #303133; margin-right: 12px; }
.comment-time { color: #909399; font-size: 12px; }
.comment-content { color: #606266; line-height: 1.7; white-space: pre-wrap; }
</style>
