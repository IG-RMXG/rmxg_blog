<template>
  <div class="login">
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <el-form-item prop="username">
        <el-input
            v-model="loginForm.username"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="账号"
        >
          <template #prefix>
            <svg-icon icon-class="user" class="el-input__icon input-icon"/>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            auto-complete="off"
            placeholder="密码"
        >
          <template #prefix>
            <svg-icon icon-class="password" class="el-input__icon input-icon"/>
          </template>
        </el-input>
      </el-form-item>

      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button
            :loading="loading"
            size="large"
            type="primary"
            style="width:100%;"
            @click.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
  </div>

  <el-dialog
      v-model="showPuzzleCaptcha"
      :width="captchaImageWidth"
      :before-close="handleCaptchaCancel"
      style="margin: 0 auto !important;top: 50% !important;transform: translateY(-50%) !important;"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <template #header>
      <div class="dialog-header">
        <el-icon class="refresh-icon" @click="refreshCaptcha">
          <Refresh />
        </el-icon>
      </div>
    </template>
    <div class="puzzle-captcha-container">
      <div class="image-container">
        <div v-if="isImageLoading" class="loading-placeholder">
          <div class="spinner"></div>
          <span>图片加载中...</span>
        </div>
        <div v-else-if="captchaMessage && !originalImageBase64" class="error-placeholder">
          <div class="error-icon">⚠️</div>
          <span>{{ captchaMessage }}</span>
          <el-button size="small" @click="refreshCaptcha" type="primary" link>重新加载</el-button>
        </div>
        <template v-else-if="originalImageBase64">
          <img
              :src="'data:image/png;base64,' + originalImageBase64"
              alt="Original Image"
              class="original-image"
          />
          <img
              :src="'data:image/png;base64,' + jigsawImageBase64"
              alt="Jigsaw Image"
              class="jigsaw-image"
              :style="{
              left: sliderPosition + 'px',
              top: jigsawYPosition+ 1 + 'px'
            }"
          />
        </template>
        <div v-else class="empty-placeholder">
          验证码图片未加载
        </div>
      </div>
      <div class="slider-container">
        <div class="slider-track"
             :class="{ 'slider-track-active': isSliderActive }"
             :style="{ width: (sliderPosition + 24) + 'px' }"></div>
        <div
            class="slider-btn"
            @mousedown="startDrag"
            :style="{ left: sliderPosition + 'px' }"
        >&gt;&gt;</div>
        <div class="slider-text">拖动滑块完成拼图</div>
      </div>
      <div class="captcha-message" v-if="captchaMessage && originalImageBase64">
        {{ captchaMessage }}
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {getCaptcha, checkCaptcha} from "@/api/login"
import Cookies from "js-cookie"
import {encrypt, decrypt} from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import {ElMessage, ElNotification} from "element-plus";
import {ref} from "vue";
import { Refresh } from '@element-plus/icons-vue'

const title = import.meta.env.VITE_APP_TITLE
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const {proxy} = getCurrentInstance()
const showSecurityVerification = ref(false)
const pendingLoginData = ref(null)

const showPuzzleCaptcha = ref(false)
const originalImageBase64 = ref('')
const jigsawImageBase64 = ref('')
const jigsawYPosition = ref(0)
const sliderPosition = ref(0)
const captchaMessage = ref('')
const isImageLoading = ref(false)

const captchaImageWidth = ref(300) // 默认宽度

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{required: true, trigger: "blur", message: "请输入您的账号"}],
  password: [{required: true, trigger: "blur", message: "请输入您的密码"}]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

const isSliderActive = ref(false)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, {immediate: true})

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      getCode()
      showPuzzleCaptcha.value = true
    }
  })
  // proxy.$refs.loginRef.validate(valid => {
  //   if (valid) {
  //     loading.value = true
  //     // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
  //     if (loginForm.value.rememberMe) {
  //       Cookies.set("username", loginForm.value.username, { expires: 30 })
  //       Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
  //       Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
  //     } else {
  //       // 否则移除
  //       Cookies.remove("username")
  //       Cookies.remove("password")
  //       Cookies.remove("rememberMe")
  //     }
  //     // 调用action的登录方法
  //     userStore.login(loginForm.value).then(() => {
  //       const query = route.query
  //       const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
  //         if (cur !== "redirect") {
  //           acc[cur] = query[cur]
  //         }
  //         return acc
  //       }, {})
  //       router.push({ path: redirect.value || "/", query: otherQueryParams })
  //     }).catch(() => {
  //       loading.value = false
  //       // 重新获取验证码
  //       if (captchaEnabled.value) {
  //         getCode()
  //       }
  //     })
  //   }
  // })
}


async function getCode() {
  try {
    isImageLoading.value = true
    captchaMessage.value = '' // 清除之前的错误信息
    const res = await getCaptcha()
    // 设置图片
    originalImageBase64.value = res.data.originalImageBase64
    jigsawImageBase64.value = res.data.jigsawImageBase64
    jigsawYPosition.value = res.data.jigsawY

    loginForm.value.captchaToken = res.data.token

    await getImageDimensions(originalImageBase64.value)
  } catch (e) {
    ElMessage.error({message: "验证码获取异常", type: 'warning'})
    captchaMessage.value = '验证码加载失败，请重试'
    // 清空图片数据，确保显示错误状态
    originalImageBase64.value = ''
    jigsawImageBase64.value = ''
  } finally {
    isImageLoading.value = false
  }
}

// 获取图片实际尺寸的函数
function getImageDimensions(base64Data) {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      // 使用相对保守的计算方式
      const maxWidth = Math.min(img.width, 500) // 限制最大宽度
      const minWidth = 300 // 最小宽度
      captchaImageWidth.value = Math.max(Math.min(maxWidth + 40, 600), minWidth)
      resolve()
    }
    img.src = 'data:image/png;base64,' + base64Data
  })
}

// 刷新验证码
function refreshCaptcha() {
  getCode()
  sliderPosition.value = 0
  captchaMessage.value = ''
}



function startDrag(e) {
  isSliderActive.value = true
  const startX = e.clientX
  const startSliderPos = sliderPosition.value

  // 获取容器宽度和拼图块宽度
  const container = e.target.parentElement
  const containerWidth = container.offsetWidth
  const jigsawWidth = 48 // 拼图块宽度，与CSS中保持一致

  const handleMouseMove = (moveEvent) => {
    const deltaX = moveEvent.clientX - startX
    // 限制在容器范围内移动，确保拼图块不会超出左右边界
    const maxPosition = containerWidth - jigsawWidth
    sliderPosition.value = Math.max(0, Math.min(maxPosition, startSliderPos + deltaX))
  }

  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    // 鼠标松开时调用验证接口
    const puzzleXPosition = sliderPosition.value
    confirmCaptcha(puzzleXPosition)
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

// Confirm CAPTCHA completion
function confirmCaptcha(puzzleXPosition) {
  checkCaptcha({
    token: loginForm.value.captchaToken,
    x: puzzleXPosition
  }).then(res => {
    if (res.code == 200) {
      // 验证成功，执行登录
      ElNotification.success({ title: res.msg })
      setTimeout(() => {
        showPuzzleCaptcha.value = false
        // 重置状态
        sliderPosition.value = 0
        isSliderActive.value = false
        proceedWithLogin()
      }, 500)
    }
  }).catch(error => {
    setTimeout(() => {
      refreshCaptcha()
    }, 500)
  })
}


// Cancel CAPTCHA
function handleCaptchaCancel() {
  showPuzzleCaptcha.value = false
  sliderPosition.value = 0
  captchaMessage.value = ''
  isSliderActive.value = false
  isImageLoading.value = false
}

function performActualLogin(loginData) {
  loading.value = true
  // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
  if (loginData.rememberMe) {
    Cookies.set("username", loginData.username, {expires: 30})
    Cookies.set("password", encrypt(loginData.password), {expires: 30})
    Cookies.set("rememberMe", loginData.rememberMe, {expires: 30})
  } else {
    // 否则移除
    Cookies.remove("username")
    Cookies.remove("password")
    Cookies.remove("rememberMe")
  }
  // 调用action的登录方法
  userStore.login(loginData).then(() => {
    const query = route.query
    const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
      if (cur !== "redirect") {
        acc[cur] = query[cur]
      }
      return acc
    }, {})
    router.push({path: redirect.value || "/", query: otherQueryParams})
  }).catch(() => {
    loading.value = false
    // 重新获取验证码
    if (captchaEnabled.value) {
      getCode()
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  // 注意：必须保留 captchaToken，否则下面 proceedWithLogin 传给后端的
  // 第四个参数会是 undefined，滑块验证码就白做了。
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
    captchaToken: loginForm.value.captchaToken
  }
}

// 滑块验证通过后真正调用后端登录
function proceedWithLogin() {
  loading.value = true
  userStore.login({
    username: loginForm.value.username,
    password: loginForm.value.password,
    rememberMe: loginForm.value.rememberMe,
    captchaToken: loginForm.value.captchaToken
  }).then(() => {
    const query = route.query
    const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
      if (cur !== "redirect") {
        acc[cur] = query[cur]
      }
      return acc
    }, {})
    router.push({ path: redirect.value || "/index", query: otherQueryParams })
  }).catch(() => {
    loading.value = false
    refreshCaptcha()
  })
}

getCookie()
</script>

<style lang='scss' scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.png");
  background-size: cover;
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  z-index: 1;

  .el-input {
    height: 40px;

    input {
      height: 40px;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.login-code {
  width: 33%;
  height: 40px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-code-img {
  height: 40px;
  padding-left: 12px;
}

.puzzle-captcha-container {
  text-align: center;
  max-width: 100%;
}

.image-container {
  position: relative;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  min-height: 150px;
}

.loading-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 150px;
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #909399;
  font-size: 14px;
  gap: 10px;
}

.error-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 150px;
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 14px;
  gap: 10px;
  padding: 20px;
  text-align: center;
}

.error-icon {
  font-size: 24px;
}

.empty-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 150px;
  background-color: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
  font-size: 14px;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.original-image {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  object-fit: contain;
  max-width: 100%;
  height: auto;
  display: block;
}

.slider-container {
  position: relative;
  width: 100%;
  height: 30px;
  margin: 10px auto;
  background: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
}

.jigsaw-image {
  position: absolute;
  cursor: move;
  object-fit: contain;
  object-position: center;
  z-index: 10;
  pointer-events: none;
}

.slider-track {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 30px;
  background: #e4e7ed;
  transform: translateY(-50%);
  border-radius: 4px;
  transition: background 0.3s ease;
  z-index: 2;
}

.slider-track-active {
  background: #67c23a !important;
}

.slider-btn {
  position: absolute;
  top: 50%;
  width: 48px;
  height: 30px;
  background: #ffffff;
  border-radius: 4px;
  transform: translateY(-50%);
  cursor: move;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
  border: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: #808080;
  z-index: 4;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.captcha-message {
  margin-top: 10px;
  color: #f56c6c;
  font-size: 12px;
}

.dialog-header {
  text-align: right;
  width: 100%;
}

.el-dialog_header {
  padding-bottom: initial;
}

.refresh-icon {
  cursor: pointer;
  font-size: 18px;
  color: #808080;
  flex-shrink: 0;
}

.refresh-icon:hover {
  color: #66b1ff;
}

.slider-text {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  transform: translateY(-50%);
  text-align: center;
  color: #909399;
  font-size: 14px;
  pointer-events: none;
  z-index: 1;
}

.el-dialog {
}

</style>
