<template>
  <div class="portal-container">
    <div class="p-page-head">
      <h1>{{ isEdit ? '编辑文章' : '写文章' }}</h1>
      <p class="p-muted">
        支持 Markdown / 代码块 / 表格 / 公式 / 流程图；普通用户提交后需管理员审核才会公开。
      </p>
    </div>

    <div class="p-card p-editor">
      <div class="p-form-row">
        <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="文章标题" />
        <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="max-width: 220px">
          <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
        </el-select>
      </div>

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

      <div class="p-form-row" style="margin-top: 14px; align-items: center">
        <el-select v-model="form.tagIds" multiple collapse-tags collapse-tags-tooltip placeholder="选择标签" style="max-width: 340px">
          <el-option v-for="t in tags" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
        </el-select>
        <el-input v-model="form.coverUrl" placeholder="封面图地址（可选）" clearable />
        <el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*">
          <el-button :loading="uploading">上传封面</el-button>
        </el-upload>
      </div>

      <div class="p-md-wrap">
        <markdown-editor v-model="form.content" />
      </div>

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

    <el-dialog v-model="previewVisible" title="预览" width="900px" append-to-body>
      <markdown-viewer :content="form.content" />
    </el-dialog>
  </div>
</template>

<script setup name="PortalWrite">
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

const statusMap = {
  0: '草稿',
  1: '已发布',
  2: '待审核',
  3: '已驳回'
}

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
const saving = ref('')
const uploading = ref(false)
const previewVisible = ref(false)
const loadedStatus = ref('0')

const isAdmin = computed(() => (userStore.roles || []).includes('admin'))
const isEdit = computed(() => !!route.params.articleId)
const statusText = computed(() => statusMap[loadedStatus.value] || statusMap[0])

function loadOptions() {
  listEnabledCategory().then(res => { categories.value = res.data || [] }).catch(() => {})
  listAllTag().then(res => { tags.value = res.data || [] }).catch(() => {})
}

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
      form.tagIds = (data.tagIds || (data.tags || []).map(t => t.tagId)).filter(Boolean)
      loadedStatus.value = data.status || '0'
      form.status = loadedStatus.value
    })
    .catch(() => {
      ElMessage.error('文章加载失败，可能已被删除')
      router.replace('/portal/me')
    })
}

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

function openPreview() {
  if (!form.content.trim()) {
    ElMessage.warning('正文为空，没有可预览的内容')
    return
  }
  previewVisible.value = true
}

onMounted(() => {
  loadOptions()
  loadArticle()
})
</script>
