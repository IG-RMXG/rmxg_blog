<template>
  <div class="upload-file">
    <!-- 上传入口：disabled 时不渲染上传控件，只保留下方文件列表供查看 -->
    <el-upload
      multiple
      :action="uploadFileUrl"
      :before-upload="handleBeforeUpload"
      :file-list="fileList"
      :data="data"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      :on-success="handleUploadSuccess"
      :show-file-list="false"
      :headers="headers"
      class="upload-file-uploader"
      ref="fileUpload"
      v-if="!disabled"
    >
      <!-- 上传按钮 -->
      <el-button type="primary">选取文件</el-button>
    </el-upload>
    <!-- 上传提示 -->
    <div class="el-upload__tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize"> 大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b> </template>
      <template v-if="fileType"> 格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b> </template>
      的文件
    </div>
    <!-- 文件列表 -->
    <transition-group ref="uploadFileList" class="upload-file-list el-upload-list el-upload-list--text" name="el-fade-in-linear" tag="ul">
      <li :key="file.uid" class="el-upload-list__item ele-upload-list__item-content" v-for="(file, index) in fileList">
        <el-link :href="file.url" :underline="false" target="_blank">
          <span class="el-icon-document"> {{ getFileName(file.name) }} </span>
        </el-link>
        <div class="ele-upload-list__item-content-action">
          <el-link :underline="false" @click="handleDelete(index)" type="danger" v-if="!disabled">&nbsp;删除</el-link>
        </div>
      </li>
    </transition-group>
  </div>
</template>

<script setup>
/**
 * 文件上传组件（受控）
 * - v-model（modelValue）：以英文逗号分隔的文件地址字符串，也支持直接传数组，见 watch 的归一化逻辑
 * - 实际请求地址 = VITE_APP_BASE_API + action（默认 /file/upload），请求头携带 Bearer token
 * - 后端契约：上传成功返回 { code, msg, data: { url } }，code === 200 才算成功
 * - 多选上传按"批次"回写：number 与 uploadList 长度相等时才统一 emit，避免父组件被多次触发
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
  // 数量限制
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
    default: () => ["doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "pdf"]
  },
  // 是否显示提示
  isShowTip: {
    type: Boolean,
    default: true
  },
  // 禁用组件（仅查看文件）
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
// 上传批次状态：number 计本批次已进入上传流程的文件数，uploadList 暂存本批次成功的项；
// 两者相等时才合并写回 modelValue（见 uploadedSuccessfully），保证多选上传只回写一次
const number = ref(0)
const uploadList = ref([])
// 上传地址与鉴权：token 取自 Cookie 中的当前登录态（@/utils/auth），在组件初始化时读取一次
const uploadFileUrl = ref(import.meta.env.VITE_APP_BASE_API + props.action) // 上传文件服务器地址
const headers = ref({ Authorization: "Bearer " + getToken() })
const fileList = ref([])
const showTip = computed(
  () => props.isShowTip && (props.fileType || props.fileSize)
)

// 外部 v-model 变化 → 归一化内部 fileList：字符串/数组统一转成 { name, url, uid } 对象数组；
// 纯字符串项没有 uid，这里补一个基于时间戳的 uid 作为列表 key（transition-group 依赖 key 做动画）
watch(() => props.modelValue, val => {
  if (val) {
    let temp = 1
    // 首先将值转为数组
    const list = Array.isArray(val) ? val : props.modelValue.split(',')
    // 然后将数组转为对象数组
    fileList.value = list.map(item => {
      if (typeof item === "string") {
        item = { name: item, url: item }
      }
      item.uid = item.uid || new Date().getTime() + temp++
      return item
    })
  } else {
    fileList.value = []
    return []
  }
},{ deep: true, immediate: true })

// 上传前校检格式和大小
function handleBeforeUpload(file) {
  // 校检文件类型
  if (props.fileType.length) {
    const fileName = file.name.split('.')
    const fileExt = fileName[fileName.length - 1]
    const isTypeOk = props.fileType.indexOf(fileExt) >= 0
    if (!isTypeOk) {
      proxy.$modal.msgError(`文件格式不正确，请上传${props.fileType.join("/")}格式文件!`)
      return false
    }
  }
  // 逗号校验的原因：modelValue 用英文逗号拼接多个地址，文件名含逗号会在回写/解析时被拆坏
  // 校检文件名是否包含特殊字符
  if (file.name.includes(',')) {
    proxy.$modal.msgError('文件名不正确，不能包含英文逗号!')
    return false
  }
  // 校检文件大小
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  // 每个通过校验的文件先计数再放行；loading 与计数配套，最终由 uploadedSuccessfully 统一关闭
  proxy.$modal.loading("正在上传文件，请稍候...")
  number.value++
  return true
}

// 文件个数超出
function handleExceed() {
  proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
}

// 上传失败
function handleUploadError(err) {
  proxy.$modal.msgError("上传文件失败")
  proxy.$modal.closeLoading()
}

// 上传成功回调
// 后端返回 { code, msg, data: { url } }；name 复用 url，展示时由 getFileName 从路径截取文件名
function handleUploadSuccess(res, file) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.data.url, url: res.data.url })
    uploadedSuccessfully()
  } else {
    number.value--
    proxy.$modal.closeLoading()
    proxy.$modal.msgError(res.msg)
    proxy.$refs.fileUpload.handleRemove(file)
    uploadedSuccessfully()
  }
}

// 删除文件
function handleDelete(index) {
  fileList.value.splice(index, 1)
  emit("update:modelValue", listToString(fileList.value))
}

// 上传结束处理
// 本批次收尾：过滤掉占位项（无 url 的本地文件），合并本批次成功项后统一回写 v-model 并关闭 loading
function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value.filter(f => f.url !== undefined).concat(uploadList.value)
    uploadList.value = []
    number.value = 0
    emit("update:modelValue", listToString(fileList.value))
    proxy.$modal.closeLoading()
  }
}

// 获取文件名称
function getFileName(name) {
  // 如果是url那么取最后的名字 如果不是直接返回
  if (name.lastIndexOf("/") > -1) {
    return name.slice(name.lastIndexOf("/") + 1)
  } else {
    return name
  }
}

// 对象转成指定字符串分隔
// 回写顺序：只取 url 字段、以英文逗号拼接，末尾去掉多余分隔符（与 watch 中的 split(',') 互为逆操作）
function listToString(list, separator) {
  let strs = ""
  separator = separator || ","
  for (let i in list) {
    if (list[i].url) {
      strs += list[i].url + separator
    }
  }
  return strs != '' ? strs.substr(0, strs.length - 1) : ''
}

// 初始化拖拽排序
// Sortable 必须等列表真正渲染后再挂载；transition-group 实例本身不是 DOM 节点，
// 优先取 $el，取不到时退回实例引用
onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const element = proxy.$refs.uploadFileList?.$el || proxy.$refs.uploadFileList
      Sortable.create(element, {
        ghostClass: 'file-upload-darg',
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
/* 拖拽中的占位样式：对应 onMounted 里 Sortable 的 ghostClass */
.file-upload-darg {
  opacity: 0.5;
  background: #c8ebfb;
}
.upload-file-uploader {
  margin-bottom: 5px;
}
.upload-file-list .el-upload-list__item {
  border: 1px solid #e4e7ed;
  line-height: 2;
  margin-bottom: 10px;
  position: relative;
  /* 关闭 item 过渡动画：避免与 Sortable 拖拽时的位移动画叠加 */
  transition: none !important;
}
.upload-file-list .ele-upload-list__item-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: inherit;
}
.ele-upload-list__item-content-action .el-link {
  margin-right: 10px;
}
</style>
