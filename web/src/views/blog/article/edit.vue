<template>
  <div class="app-container blog-editor-page">
    <!-- 文章元信息（紧凑一行，把空间让给正文） -->
    <el-form ref="articleRef" :model="form" :rules="rules" class="meta-form" label-width="52px">
      <el-form-item label="标题" prop="title" class="f-title">
        <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="200" show-word-limit size="large" />
      </el-form-item>

      <div class="meta-row">
        <el-form-item label="分类" prop="categoryId" class="f-sm">
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable>
            <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签" class="f-md">
          <el-select v-model="form.tagIds" multiple placeholder="选择标签" clearable>
            <el-option v-for="item in tagOptions" :key="item.tagId" :label="item.tagName" :value="item.tagId" />
          </el-select>
        </el-form-item>

        <el-form-item label="摘要" class="f-lg">
          <el-input v-model="form.summary" placeholder="选填，用于列表展示" maxlength="500" />
        </el-form-item>

        <el-form-item label="封面" class="f-cover">
          <div class="cover-field">
            <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-thumb" alt="封面" />
            <el-button v-if="form.coverUrl" link type="danger" icon="Delete" @click="form.coverUrl = ''" />
            <el-button v-else icon="Upload" @click="triggerCoverUpload">上传封面</el-button>
            <el-input v-if="!form.coverUrl" v-model="form.coverUrl" placeholder="或粘贴封面 URL" />
          </div>
        </el-form-item>

        <el-form-item v-if="isAdmin" label="属性" class="f-attr">
          <el-switch v-model="form.isTop" active-value="1" inactive-value="0" active-text="置顶" />
          <el-switch v-model="form.isRecommend" active-value="1" inactive-value="0" active-text="推荐" />
        </el-form-item>
      </div>
    </el-form>

    <!-- 正文编辑器：占满剩余高度 -->
    <div class="editor-wrap">
      <markdown-editor ref="editorRef" v-model="form.content" />
    </div>

    <!-- 操作栏 -->
    <div class="editor-footer">
      <el-button type="primary" icon="Check" @click="submitForm(isAdmin ? '1' : '2')">
        {{ isAdmin ? '发布' : '提交审核' }}
      </el-button>
      <el-button icon="Document" @click="submitForm('0')">保存草稿</el-button>
      <el-button :type="previewOnly ? 'primary' : 'default'" icon="View" @click="handlePreview">
        {{ previewOnly ? '退出预览' : '预览' }}
      </el-button>
      <el-button icon="Back" @click="cancel">返回</el-button>
      <span class="status-tip" v-if="!isAdmin">普通用户提交后将进入「待审核」，由管理员发布</span>
    </div>

    <input ref="coverInput" type="file" accept="image/*" style="display: none" @change="handleCoverSelect" />
  </div>
</template>

<script setup name="BlogArticleEdit">
import { useRouter, useRoute } from 'vue-router'
import { getArticle, addArticle, updateArticle, uploadBlogImage } from '@/api/blog/article'
import { listEnabledCategory } from '@/api/blog/category'
import { listAllTag } from '@/api/blog/tag'
import useUserStore from '@/store/modules/user'
import MarkdownEditor from '@/components/MarkdownEditor/index.vue'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.roles.includes('admin'))

const articleRef = ref()
const editorRef = ref()
const coverInput = ref()
const categoryOptions = ref([])
const tagOptions = ref([])
const previewOnly = ref(false)

const articleId = computed(() => route.params.articleId ? Number(route.params.articleId) : undefined)

const data = reactive({
  form: {
    articleId: undefined,
    title: undefined,
    summary: undefined,
    content: '',
    coverUrl: undefined,
    categoryId: undefined,
    tagIds: [],
    isTop: '0',
    isRecommend: '0'
  },
  rules: {
    title: [{ required: true, message: '文章标题不能为空', trigger: 'blur' }]
  }
})
const { form, rules } = toRefs(data)

function getCategories() {
  listEnabledCategory().then(res => { categoryOptions.value = res.data || [] })
}

function getTags() {
  listAllTag().then(res => { tagOptions.value = res.data || [] })
}

function loadArticle() {
  if (!articleId.value) return
  getArticle(articleId.value).then(res => {
    const a = res.data
    form.value.articleId = a.articleId
    form.value.title = a.title
    form.value.summary = a.summary
    form.value.content = a.content || ''
    form.value.coverUrl = a.coverUrl
    form.value.categoryId = a.categoryId
    form.value.isTop = a.isTop || '0'
    form.value.isRecommend = a.isRecommend || '0'
    form.value.tagIds = (a.tags || []).map(t => t.tagId)
  })
}

function triggerCoverUpload() {
  coverInput.value && coverInput.value.click()
}

async function handleCoverSelect(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  proxy.$modal.loading('正在上传封面…')
  try {
    const res = await uploadBlogImage(formData)
    proxy.$modal.closeLoading()
    form.value.coverUrl = res.data.url
    proxy.$modal.msgSuccess('封面上传成功')
  } catch (err) {
    proxy.$modal.closeLoading()
  }
}

/** 预览：单独按钮，切换沉浸式预览（编辑区隐藏、实时渲染正文） */
function handlePreview() {
  previewOnly.value = !previewOnly.value
  editorRef.value && editorRef.value.togglePreviewOnly(previewOnly.value)
}

function submitForm(status) {
  proxy.$refs.articleRef.validate(valid => {
    if (!valid) return
    const payload = { ...form.value, status: status }
    const req = articleId.value ? updateArticle(payload) : addArticle(payload)
    req.then(() => {
      proxy.$modal.msgSuccess(status === '1' ? '发布成功' : (status === '0' ? '草稿已保存' : '已提交审核'))
      router.push('/blog/article')
    })
  })
}

function cancel() {
  router.push('/blog/article')
}

getCategories()
getTags()
loadArticle()
</script>

<style scoped>
.blog-editor-page {
  display: flex;
  flex-direction: column;
  /* 84 = navbar(50) + tags-view(34)，再加上 app-container 的上下 padding 与底部版权留白 */
  height: calc(100vh - 165px);
  min-height: 540px;
}

.meta-form {
  flex: none;
}

.meta-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.f-title {
  margin-bottom: 6px !important;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 8px;
}

.meta-row :deep(.el-form-item) {
  margin-right: 8px;
}

.meta-row :deep(.el-form-item__content) {
  min-width: 0;
}

.f-sm { width: 220px; }
.f-md { width: 300px; }
.f-lg { width: 340px; }
.f-cover { width: 320px; }
.f-attr :deep(.el-form-item__content) { display: flex; align-items: center; gap: 16px; }

.f-sm :deep(.el-select),
.f-md :deep(.el-select),
.f-lg :deep(.el-input),
.f-cover :deep(.el-input) {
  width: 100%;
}

.cover-field {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}
.cover-thumb {
  width: 46px;
  height: 32px;
  object-fit: cover;
  border-radius: 3px;
  flex: none;
}

.editor-wrap {
  flex: 1;
  min-height: 320px;
  overflow: hidden;
}

.editor-footer {
  flex: none;
  display: flex;
  align-items: center;
  padding-top: 12px;
}

.status-tip {
  margin-left: 16px;
  color: #909399;
  font-size: 13px;
}
</style>
