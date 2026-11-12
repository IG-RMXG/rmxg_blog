<template>
  <div class="component-upload-image">
    <!-- 图片卡片列表：达到 limit 后由 hide 类隐藏"+"号（见 style），disabled 时整块不可操作 -->
    <el-upload
      multiple
      :disabled="disabled"
      :action="uploadImgUrl"
      list-type="picture-card"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :data="data"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      ref="imageUpload"
      :before-remove="handleDelete"
      :show-file-list="true"
      :headers="headers"
      :file-list="fileList"
      :on-preview="handlePictureCardPreview"
      :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon"><plus /></el-icon>
    </el-upload>
    <!-- 上传提示 -->
    <div class="el-upload__tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize">
        大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b>
      </template>
      的文件
    </div>

    <!-- 大图预览：append-to-body 以免被表单/弹窗的 overflow 裁切 -->
    <el-dialog
      v-model="dialogVisible"
      title="预览"
      width="800px"
      append-to-body
    >
      <img
        :src="dialogImageUrl"
        style="display: block; max-width: 100%; margin: 0 auto"
      />
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 多图上传组件（受控，picture-card 卡片样式）
 * - v-model（modelValue）：以英文逗号分隔的图片地址；回写时会去掉 VITE_APP_BASE_API 前缀，只存相对路径
 * - 实际请求地址 = VITE_APP_BASE_API + action（默认 /file/upload），请求头携带 Bearer token
 * - 后端契约：上传成功返回 { code, msg, data: { url } }，code === 200 才算成功
 * - 多选时按"批次"回写：number 与 uploadList 长度相等才统一 emit，避免重复触发父组件
 */
import { getToken } from "@/utils/auth"
import Sortable from 'sortablejs'

// props 契约：action 用相对路径，便于通过环境变量统一切换网关；fileSize 单位为 MB；fileType 为小写扩展名数组
const props = defineProps({
  modelValue: [String, Object, Array],
  // 上传接口地址
  action: {
    type: String,
    default: "/file/upload"
  },
  // 上传携带的参数
  data: {
    type: Object
  },
  // 图片数量限制
  limit: {
    type: Number,
    default: 5
  },
  // 大小限制(MB)
  fileSize: {
    type: Number,
    default: 5
  },
  // 文件类型, 例如['png', 'jpg', 'jpeg']
  fileType: {
    type: Array,
    default: () => ["png", "jpg", "jpeg"]
  },
  // 是否显示提示
  isShowTip: {
    type: Boolean,
    default: true
  },
  // 禁用组件（仅查看图片）
  disabled: {
    type: Boolean,
    default: false
  },
  // 拖动排序
  drag: {
    type: Boolean,
    default: true
  }
})

const { proxy } = getCurrentInstance()
const emit = defineEmits()
// 上传批次状态：number 计本批次已进入上传流程的图片数，uploadList 暂存本批次成功的项；
// 两者相等时才合并写回 modelValue（见 uploadedSuccessfully）
const number = ref(0)
const uploadList = ref([])
// 预览弹窗状态：点击卡片上的放大镜触发 handlePictureCardPreview
const dialogImageUrl = ref("")
const dialogVisible = ref(false)
// baseUrl 单独提出：上传地址拼前缀，回写时又要把它从 url 里剥掉
const baseUrl = import.meta.env.VITE_APP_BASE_API
const uploadImgUrl = ref(baseUrl + props.action) // 上传的图片服务器地址
const headers = ref({ Authorization: "Bearer " + getToken() })
const fileList = ref([])
const showTip = computed(
  () => props.isShowTip && (props.fileType || props.fileSize)
)

// 外部 v-model 变化 → 归一化内部 fileList：逗号分隔字符串或数组统一转成 el-upload 需要的 { name, url } 对象
watch(() => props.modelValue, val => {
  if (val) {
    // 首先将值转为数组
    const list = Array.isArray(val) ? val : props.modelValue.split(",")
    // 然后将数组转为对象数组
    fileList.value = list.map(item => {
      if (typeof item === "string") {
        item = { name: item, url: item }
      }
      return item
    })
  } else {
    fileList.value = []
    return []
  }
},{ deep: true, immediate: true })

// 上传前loading加载
// 类型同时看 MIME（file.type）与扩展名，兼容部分浏览器 MIME 缺失的情况；
// fileType 为空数组时退化为"只要是 image 就放行"
function handleBeforeUpload(file) {
  let isImg = false
  if (props.fileType.length) {
    let fileExtension = ""
    if (file.name.lastIndexOf(".") > -1) {
      fileExtension = file.name.slice(file.name.lastIndexOf(".") + 1)
    }
    isImg = props.fileType.some(type => {
      if (file.type.indexOf(type) > -1) return true
      if (fileExtension && fileExtension.indexOf(type) > -1) return true
      return false
    })
  } else {
    isImg = file.type.indexOf("image") > -1
  }
  if (!isImg) {
    proxy.$modal.msgError(`文件格式不正确，请上传${props.fileType.join("/")}图片格式文件!`)
    return false
  }
  if (file.name.includes(',')) {
    proxy.$modal.msgError('文件名不正确，不能包含英文逗号!')
    return false
  }
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传头像图片大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  proxy.$modal.loading("正在上传图片，请稍候...")
  number.value++
}

// 文件个数超出
function handleExceed() {
  proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
}

// 上传成功回调
function handleUploadSuccess(res, file) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.data.url, url: res.data.url })
    uploadedSuccessfully()
  } else {
    number.value--
    proxy.$modal.closeLoading()
    proxy.$modal.msgError(res.msg)
    proxy.$refs.imageUpload.handleRemove(file)
    uploadedSuccessfully()
  }
}

// 删除图片
// 该函数同时被当作 el-upload 的 before-remove 钩子：先从自身 fileList 删除并回写 v-model，
// 再返回 false 阻止 el-upload 重复移除；未命中同名项或仍有文件在上传（uploadList 与 number 不等）时不走这段逻辑
function handleDelete(file) {
  const findex = fileList.value.map(f => f.name).indexOf(file.name)
  if (findex > -1 && uploadList.value.length === number.value) {
    fileList.value.splice(findex, 1)
    emit("update:modelValue", listToString(fileList.value))
    return false
  }
}

// 上传结束处理
// 本批次收尾：过滤掉占位项（无 url 的本地图片），合并本批次成功项后统一回写 v-model 并关闭 loading
function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value.filter(f => f.url !== undefined).concat(uploadList.value)
    uploadList.value = []
    number.value = 0
    emit("update:modelValue", listToString(fileList.value))
    proxy.$modal.closeLoading()
  }
}

// 上传失败
function handleUploadError() {
  proxy.$modal.msgError("上传图片失败")
  proxy.$modal.closeLoading()
}

// 预览
function handlePictureCardPreview(file) {
  dialogImageUrl.value = file.url
  dialogVisible.value = true
}

// 对象转成指定字符串分隔
// 与 FileUpload 的同名函数相比有两点差异：
// 1) 回写前剥掉 baseUrl 前缀，库里只存相对路径，后续换域名/走网关不用清洗历史数据；
// 2) 跳过 blob: 开头的地址——那是 el-upload 的本地预览 URL，只在浏览器内存里有效，不能入库
function listToString(list, separator) {
  let strs = ""
  separator = separator || ","
  for (let i in list) {
    if (undefined !== list[i].url && list[i].url.indexOf("blob:") !== 0) {
      strs += list[i].url.replace(baseUrl, "") + separator
    }
  }
  return strs != "" ? strs.substr(0, strs.length - 1) : ""
}

// 初始化拖拽排序
// 要等卡片渲染完再挂载，且须定位到内部的 .el-upload-list：
// el-upload 根节点还包含上传按钮等结构，直接对它挂 Sortable 会拖错元素
onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const element = proxy.$refs.imageUpload?.$el?.querySelector('.el-upload-list')
      Sortable.create(element, {
        onEnd: (evt) => {
          const movedItem = fileList.value.splice(evt.oldIndex, 1)[0]
          fileList.value.splice(evt.newIndex, 0, movedItem)
          emit('update:modelValue', listToString(fileList.value))
        }
      })
    })
  }
})
</script>

<style scoped lang="scss">
/* 达到数量上限时隐藏"+"号上传入口（对应模板上的 :class="{ hide: ... }"） */
// .el-upload--picture-card 控制加号部分
:deep(.hide .el-upload--picture-card) {
    display: none;
}

:deep(.el-upload.el-upload--picture-card.is-disabled) {
  display: none !important;
} 
</style>