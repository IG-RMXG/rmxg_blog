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

    <!-- 隐藏的文件选择器：由「上传封面」按钮调用 click() 触发；accept 只做前端过滤，真正校验在后端 -->
    <input ref="coverInput" type="file" accept="image/*" style="display: none" @change="handleCoverSelect" />
  </div>
</template>

<script setup name="BlogArticleEdit">
/**
 * 文章新建/编辑页：同一组件复用，route.params.articleId 有值时加载旧文章进入编辑，否则为新增。
 * 接口：getArticle 回填、addArticle/updateArticle 保存、uploadBlogImage 上传封面，
 *       listEnabledCategory / listAllTag 提供下拉选项；正文编辑器为 MarkdownEditor。
 * 权限差异：管理员可发布（status=1）并设置置顶/推荐；普通用户只能存草稿（0）或提交审核（2），
 *           置顶/推荐开关不渲染，后端也会把这两个字段回退为库中原值。
 * 注意：普通用户保存已发布文章时，后端会把状态收敛为草稿/待审核，相当于先下线再重审。
 * 保存成功后统一返回 /blog/article。
 */
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

// 管理员判定同时决定按钮文案（发布/提交审核）和保存时提交的目标状态
const isAdmin = computed(() => userStore.roles.includes('admin'))

const articleRef = ref()
const editorRef = ref()
const coverInput = ref()
const categoryOptions = ref([])
const tagOptions = ref([])
const previewOnly = ref(false)

// 可选路由参数：有 articleId 走 updateArticle，没有则走 addArticle
const articleId = computed(() => route.params.articleId ? Number(route.params.articleId) : undefined)

// form 即提交给后端的 BlogArticle 载荷：tagIds 是标签主键数组（后端按此全量覆盖关联），
// isTop/isRecommend 用字符串 '0'/'1' 与后端字段一致；校验只要求标题必填，正文/分类等允许为空
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

// 两个下拉都一次性拉全量：分类只含启用项，标签不区分状态，接口均直接返回数组
function getCategories() {
  listEnabledCategory().then(res => { categoryOptions.value = res.data || [] })
}

function getTags() {
  listAllTag().then(res => { tagOptions.value = res.data || [] })
}

// 编辑回填：接口返回 tags 为对象数组，表单只保存 tagIds；置顶/推荐缺省补 '0' 以匹配 el-switch
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

// 选完文件立刻清空 input.value，否则再次选择同一个文件不会触发 change；
// 文件大小/格式由后端限制（5MB，jpg/jpeg/png/gif/webp/bmp/svg），失败提示由请求拦截器统一弹出
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

// status 由按钮决定：'1'=发布（仅管理员会直接生效）、'2'=提交审核、'0'=存草稿；
// 以 articleId 区分新增/更新，成功后统一回到列表页
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
  /* 84 = navbar(50) + tags-view(34)，再加上 app-container 的上下 padding 与底部版权留白；
     --uat-banner-h 为 UAT 横幅高度（DEV 下为 0，行为不变） */
  height: calc(100vh - 165px - var(--uat-banner-h, 0px));
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
