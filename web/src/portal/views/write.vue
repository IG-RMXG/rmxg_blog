<template>
  <div class="portal-container">
    <!-- 写作页：新建（/portal/write）与编辑（/portal/write/:articleId）复用同一套表单，靠 isEdit 区分 -->
    <div class="p-page-head">
      <h1>{{ isEdit ? '编辑文章' : '写文章' }}</h1>
      <p class="p-muted">
        支持 Markdown / 代码块 / 表格 / 公式 / 流程图；普通用户提交后需管理员审核才会公开。
      </p>
    </div>

    <div class="p-card p-editor">
      <!-- 标题必填；分类可以留空，保存时后台按「未分类」处理 -->
      <div class="p-form-row">
        <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="文章标题" />
        <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="max-width: 220px">
          <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
        </el-select>
      </div>

      <!-- 摘要只在列表卡片里展示；留空时卡片会显示默认文案，不会报错 -->
      <div style="margin-top: 14px">
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="摘要（列表页展示，建议 1-2 句话）"
        />
      </div>

      <!-- 标签多选 + 封面地址 + 封面上传：上传接管了 el-upload 的 http-request，成功后回填地址，也可以手填外链 -->
      <div class="p-form-row" style="margin-top: 14px; align-items: center">
        <el-select v-model="form.tagIds" multiple collapse-tags collapse-tags-tooltip placeholder="选择标签" style="max-width: 340px">
          <el-option v-for="t in tags" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
        </el-select>
        <el-input v-model="form.coverUrl" placeholder="封面图地址（可选）" clearable />
        <el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*">
          <el-button :loading="uploading">上传封面</el-button>
        </el-upload>
      </div>

      <!-- 正文编辑器：v-model 直接绑定 form.content（存的是 Markdown 源码），预览与保存都读同一个值 -->
      <div class="p-md-wrap">
        <markdown-editor v-model="form.content" />
      </div>

      <!-- 操作栏：普通用户只有「草稿 / 提交审核」，管理员多一个「立即发布」；
           saving 存的是正在提交的目标状态，让对应按钮单独转圈并互斥防连点 -->
      <div class="p-editor__toolbar">
        <el-button type="primary" :loading="saving === '0'" @click="save('0')">保存草稿</el-button>
        <el-button :loading="saving === '2'" @click="save('2')">提交审核</el-button>
        <el-button v-if="isAdmin" :loading="saving === '1'" @click="save('1')">立即发布</el-button>
        <el-button @click="openPreview">预览</el-button>
        <el-button text @click="router.push('/portal/me')">我的文章</el-button>
        <span class="p-editor__tip">
          状态：{{ statusText }}<template v-if="form.articleId"> · 文章 ID {{ form.articleId }}</template>
        </span>
      </div>
    </div>

    <!-- 预览弹窗：用只读渲染器展示当前编辑内容，不落库；append-to-body 防止被编辑器容器裁切 -->
    <el-dialog v-model="previewVisible" title="预览" width="900px" append-to-body>
      <markdown-viewer :content="form.content" />
    </el-dialog>
  </div>
</template>

<script setup name="PortalWrite">
/**
 * 门户写作页（/portal/write 新建、/portal/write/:articleId 编辑，路由带 requiresAuth）。
 *
 * 同一套表单服务两种模式：没有 articleId 走 addArticle，有则走 updateArticle。
 * 目标状态由按钮决定：存草稿传 '0'、提交审核传 '2'、立即发布传 '1'（仅管理员；
 * 普通用户即使传 1 也会被后端降级为 2）。所以 form.status 是「本次提交的目标状态」，
 * 服务端当前状态单独记在 loadedStatus 里，只用于界面展示。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownEditor from '@/components/MarkdownEditor/index.vue'
import MarkdownViewer from '@/components/MarkdownViewer/index.vue'
import { listEnabledCategory } from '@/api/blog/category'
import { listAllTag } from '@/api/blog/tag'
import {
  getArticle,
  addArticle,
  updateArticle,
  uploadBlogImage
} from '@/api/blog/article'
import useUserStore from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 与后端 blog_article.status 的取值一一对应，只用于展示当前状态文字
const statusMap = {
  0: '草稿',
  1: '已发布',
  2: '待审核',
  3: '已驳回'
}

// articleId 为 null 表示新建；status 是本次要提交的目标状态，不是文章当前状态
const form = reactive({
  articleId: null,
  title: '',
  summary: '',
  content: '',
  coverUrl: '',
  categoryId: null,
  tagIds: [],
  status: '0'
})

const categories = ref([])
const tags = ref([])
// saving 存正在提交的目标状态（'' 表示空闲），让「草稿/审核/发布」三个按钮各自转圈且互斥
const saving = ref('')
const uploading = ref(false)
const previewVisible = ref(false)
// 服务端返回的当前状态，保存成功后同步更新，底部状态文案用它而不是 form.status
const loadedStatus = ref('0')

// 只有 admin 能直接发布；普通用户提交后进入待审核，由管理员在后台通过
const isAdmin = computed(() => (userStore.roles || []).includes('admin'))
// 路由带 :articleId 即编辑模式，否则是新建
const isEdit = computed(() => !!route.params.articleId)
// 未知状态按草稿显示，避免编辑器底部那行状态文案出现 undefined
const statusText = computed(() => statusMap[loadedStatus.value] || statusMap[0])

// 下拉数据：分类只取启用中的，标签取全量；失败只影响下拉选项，不阻塞写正文
function loadOptions() {
  listEnabledCategory().then(res => { categories.value = res.data || [] }).catch(() => {})
  listAllTag().then(res => { tags.value = res.data || [] }).catch(() => {})
}

// 编辑模式才回填；新建模式直接返回，表单保持初始空值。
// 注意：这里读详情用的是 getArticle（不累加浏览量），不要换成门户详情接口。
function loadArticle() {
  if (!isEdit.value) return
  getArticle(route.params.articleId)
    .then(res => {
      const data = res.data || {}
      form.articleId = data.articleId
      form.title = data.title || ''
      form.summary = data.summary || ''
      form.content = data.content || ''
      form.coverUrl = data.coverUrl || ''
      form.categoryId = data.categoryId || null
      // tagIds 优先用后端给的扁平 id 数组；老数据只有 tags 对象数组时再从中取 id，
      // 两级都取不到就得到空数组（filter(Boolean) 顺便去掉 null）
      form.tagIds = (data.tagIds || (data.tags || []).map(t => t.tagId)).filter(Boolean)
      loadedStatus.value = data.status || '0'
      form.status = loadedStatus.value
    })
    .catch(() => {
      // 加载失败通常意味着文章已被删除或无权访问，退回列表，避免在空表单上误操作
      ElMessage.error('文章加载失败，可能已被删除')
      router.replace('/portal/me')
    })
}

// 保存/提交：校验规则按目标状态区分——草稿允许正文为空（先写标题占位），
// 但提交审核或发布必须有正文，否则审核通过后门户上会出现一篇空文章。
function save(status) {
  if (!form.title.trim()) {
    ElMessage.warning('请先填写标题')
    return
  }
  if (status !== '0' && !form.content.trim()) {
    ElMessage.warning('正文不能为空')
    return
  }
  saving.value = status
  const payload = {
    articleId: form.articleId,
    title: form.title.trim(),
    summary: form.summary.trim(),
    content: form.content,
    coverUrl: form.coverUrl.trim(),
    categoryId: form.categoryId,
    tagIds: form.tagIds,
    status
  }
  const request = form.articleId ? updateArticle(payload) : addArticle(payload)
  request
    .then(res => {
      // 新增时把后端返回的 id 写回 form：否则用户再点一次保存会又新增一篇，而不是更新刚建的这篇。
      if (!form.articleId) {
        // addArticle 返回新文章 id（ResponseVO<Long>）
        form.articleId = res.data
      }
      loadedStatus.value = status
      if (status === '2') {
        ElMessage.success('已提交审核，管理员通过后即可在门户看到')
      } else if (status === '1') {
        ElMessage.success('已发布')
      } else {
        ElMessage.success('草稿已保存')
      }
      router.push('/portal/me')
    })
    .catch(() => {})
    .finally(() => {
      saving.value = ''
    })
}

// el-upload 的自定义上传（http-request 接管上传逻辑）：FormData 的字段名必须是 file，
// 后端还限制 5MB 与 jpg/png/gif/webp 等图片类型，超限由后端与拦截器统一报错。
// 成功后后端返回 { url }，这里只回填输入框，用户仍可手工改成外链地址。
async function uploadCover(options) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const res = await uploadBlogImage(fd)
    if (res.data && res.data.url) {
      form.coverUrl = res.data.url
      ElMessage.success('封面已上传')
    } else {
      ElMessage.error('上传失败：未返回图片地址')
    }
  } catch (e) {
    ElMessage.error('封面上传失败')
  } finally {
    uploading.value = false
  }
}

// 预览前先挡空正文：否则弹窗里只有一片空白，看起来像预览坏了
function openPreview() {
  if (!form.content.trim()) {
    ElMessage.warning('正文为空，没有可预览的内容')
    return
  }
  previewVisible.value = true
}

onMounted(() => {
  // 两个请求并行：下拉选项失败不影响编辑回填，编辑回填失败会退回列表
  loadOptions()
  loadArticle()
})
</script>
