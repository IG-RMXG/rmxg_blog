<template>
  <div class="user-info-head" @click="editCropper()">
    <!-- 头像区域：点击整块（含悬停「+」遮罩）打开裁剪弹窗 -->
    <img :src="options.img" title="点击上传头像" class="img-circle img-lg" />
    <el-dialog :title="title" v-model="open" width="800px" append-to-body @opened="modalOpened" @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{ height: '350px' }">
          <!-- v-if=visible：等弹窗展开、容器有宽高后才挂载裁剪器，避免按 0 尺寸初始化导致裁剪框错位 -->
          <vue-cropper
            ref="cropper"
            :img="options.img"
            :info="true"
            :autoCrop="options.autoCrop"
            :autoCropWidth="options.autoCropWidth"
            :autoCropHeight="options.autoCropHeight"
            :fixedBox="options.fixedBox"
            :outputType="options.outputType"
            @realTime="realTime"
            v-if="visible"
          />
        </el-col>
        <el-col :xs="24" :md="12" :style="{ height: '350px' }">
          <!-- 右侧实时预览：尺寸与位置来自裁剪器的 realTime 回调 -->
          <div class="avatar-upload-preview">
            <img :src="options.previews.url" :style="options.previews.img" />
          </div>
        </el-col>
      </el-row>
      <br />
      <el-row>
        <el-col :lg="2" :md="2">
          <!-- 选图不直接上传：http-request 被空实现接管，仅把本地图读进裁剪器 -->
          <el-upload
            action="#"
            :http-request="requestUpload"
            :show-file-list="false"
            :before-upload="beforeUpload"
          >
            <el-button>
              选择
              <el-icon class="el-icon--right"><Upload /></el-icon>
            </el-button>
          </el-upload>
        </el-col>
        <el-col :lg="{ span: 1, offset: 2 }" :md="2">
          <el-button icon="Plus" @click="changeScale(1)"></el-button>
        </el-col>
        <el-col :lg="{ span: 1, offset: 1 }" :md="2">
          <el-button icon="Minus" @click="changeScale(-1)"></el-button>
        </el-col>
        <el-col :lg="{ span: 1, offset: 1 }" :md="2">
          <el-button icon="RefreshLeft" @click="rotateLeft()"></el-button>
        </el-col>
        <el-col :lg="{ span: 1, offset: 1 }" :md="2">
          <el-button icon="RefreshRight" @click="rotateRight()"></el-button>
        </el-col>
        <el-col :lg="{ span: 2, offset: 6 }" :md="2">
          <el-button type="primary" @click="uploadImg()">提 交</el-button>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 头像上传 / 裁剪组件（个人中心左侧头像）。
 *
 * 流程：选本地图 → FileReader 转 dataURL 交给裁剪器 → 裁剪结果以 avatarfile 字段
 *      POST /system/user/profile/avatar → 用返回的 imgUrl 更新 options.img 与 user store。
 * 裁剪参数固定 200x200、fixedBox（不可拖拽改框），输出 PNG。
 */
import "vue-cropper/dist/index.css"
import { VueCropper } from "vue-cropper"
import { uploadAvatar } from "@/api/system/user"
import useUserStore from "@/store/modules/user"

const userStore = useUserStore()
const { proxy } = getCurrentInstance()

const open = ref(false)
const visible = ref(false)
const title = ref("修改头像")

//图片裁剪数据
const options = reactive({
  img: userStore.avatar,     // 裁剪图片的地址
  autoCrop: true,            // 是否默认生成截图框
  autoCropWidth: 200,        // 默认生成截图框宽度
  autoCropHeight: 200,       // 默认生成截图框高度
  fixedBox: true,            // 固定截图框大小 不允许改变
  outputType: "png",         // 默认生成截图为PNG格式
  filename: 'avatar',        // 文件名称
  previews: {}               //预览数据
})

/** 编辑头像 */
function editCropper() {
  open.value = true
}

/** 打开弹出层结束时的回调 */
function modalOpened() {
  // 等弹窗过渡结束再渲染裁剪器：此时容器已有宽高，过早渲染会按 0 尺寸初始化导致裁剪框错位
  visible.value = true
}

/** 覆盖默认上传行为 */
// 空实现：阻止 el-upload 选中文件后立即 POST，改为先本地裁剪、点「提交」再上传
function requestUpload() {}

/** 向左旋转 */
function rotateLeft() {
  proxy.$refs.cropper.rotateLeft()
}

/** 向右旋转 */
function rotateRight() {
  proxy.$refs.cropper.rotateRight()
}

/** 图片缩放 */
function changeScale(num) {
  num = num || 1
  proxy.$refs.cropper.changeScale(num)
}

/** 上传预处理 */
function beforeUpload(file) {
  // 只按 MIME 前缀拦非图片；真正的扩展名白名单在后端（识别不到时统一按 png 落盘）
  if (file.type.indexOf("image/") == -1) {
    proxy.$modal.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。")
  } else {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => {
      options.img = reader.result
      options.filename = file.name
    }
  }
}

/** 上传图片 */
function uploadImg() {
  // getCropBlob 是回调式 API，FormData 必须在回调里组装，否则拿到的是空 Blob
  proxy.$refs.cropper.getCropBlob(data => {
    let formData = new FormData()
    formData.append("avatarfile", data, options.filename)
    uploadAvatar(formData).then(response => {
      open.value = false
      options.img = response.imgUrl
      userStore.avatar = options.img
      proxy.$modal.msgSuccess("修改成功")
      visible.value = false
    })
  })
}

/** 实时预览 */
// data 含 { url, img }：url 给预览图，img 是裁剪框的尺寸/位移样式，直接绑到 style 上
function realTime(data) {
  options.previews = data
}

/** 关闭窗口 */
// 丢弃未提交的裁剪结果，恢复成 store 里已保存的头像，下次打开从干净状态开始
function closeDialog() {
  options.img = userStore.avatar
  options.visible = false
}
</script>

<style lang='scss' scoped>
.user-info-head {
  position: relative;
  display: inline-block;
  height: 120px;
}

/* 悬停遮罩用 :after 铺满头像块；line-height 110px 与 120px 容器高度对齐，让「+」垂直居中 */
.user-info-head:hover:after {
  content: "+";
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  color: #eee;
  background: rgba(0, 0, 0, 0.5);
  font-size: 24px;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  cursor: pointer;
  line-height: 110px;
  border-radius: 50%;
}
</style>