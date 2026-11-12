<template>
  <!-- 登录页：账号密码 + 滑块拼图验证码，成功后按 ?redirect= 回跳；UAT 构建下额外提供免密快捷入口 -->
  <div class="login">
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <!-- UAT 专属：免密快捷进入（生产构建 isUat 恒 false 不渲染） -->
      <div v-if="isUat" class="uat-quick">
        <div class="uat-quick-title">UAT 快捷进入（跳过密码与验证码）</div>
        <div class="uat-quick-buttons">
          <el-button size="large" type="warning" plain style="width: 50%" :loading="uatLoading === 'admin'" @click="uatEnter('admin')">
            管理员
          </el-button>
          <el-button size="large" type="warning" plain style="width: 50%; margin-left: 0" :loading="uatLoading === 'user'" @click="uatEnter('user')">
            普通用户
          </el-button>
        </div>
      </div>
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

      <!-- 记住密码：仅置位表单字段，真正写/读 Cookie 的是 performActualLogin() 与 getCookie() -->
      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <!-- 提交区：表单校验通过才去拉验证码，避免账号没填就白请求一张图 -->
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

    <!-- 博客门户入口：读者/普通用户从门户登录，管理员仍可用本页 -->
    <div class="portal-entry" style="text-align: center; font-size: 13px; padding: 4px 0 14px;">
      <router-link class="link-type" to="/portal/home">进入博客门户 →</router-link>
    </div>
  </div>

  <!-- 滑块拼图验证码弹窗：宽度随原图尺寸动态计算，禁止点遮罩关闭，关闭动作统一走 handleCaptchaCancel 复位状态 -->
  <el-dialog
      v-model="showPuzzleCaptcha"
      :width="captchaImageWidth"
      :before-close="handleCaptchaCancel"
      style="margin: 0 auto !important;top: 50% !important;transform: translateY(-50%) !important;"
      :close-on-click-modal="false"
      :maximizable="false"
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
      <!-- 图片区：底图 + 拼图块叠加渲染，拼图块的 left 就是松手时提交给网关的 x 坐标 -->
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
      <!-- 拖拽区：mousedown 后在 document 上监听 mousemove/mouseup，松手即把位移提交校验 -->
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
// ===== 后台登录页 =====
// 流程：账号密码 => 拉取滑块拼图验证码 => POST 校验滑块 x => userStore.login 换 token => 按 redirect 回跳。
// 依赖接口（见 api/login.js）：GET /captcha 取底图/拼图/token，POST /captcha 校验偏移量，POST /system/auth/login 签发 token。
// 验证码是一次性的：刷新图片、校验失败都要重新 getCode()，旧 token 随新图一起作废。
// 本页只管拿 token 与跳转，token 写入 Cookie、拉取角色权限都在 store/modules/user.js。
// 读者侧的门户登录是另一个页面（/portal/login），本页只在底部提供进入门户的链接。
import {getCaptcha, checkCaptcha} from "@/api/login"
import Cookies from "js-cookie"
import {encrypt, decrypt} from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import {ElMessage, ElNotification} from "element-plus";
import {ref} from "vue";
import { Refresh } from '@element-plus/icons-vue'
import { isUat, enterAs } from '@/utils/uat'

// 卡片标题，由构建期注入的环境变量决定（UAT 与生产各自在 .env 中配置）
const title = import.meta.env.VITE_APP_TITLE
// UAT 免密按钮的 loading：存当前正在请求的角色（'admin' / 'user'），空串表示都没有在请求中
const uatLoading = ref('')

// UAT 快捷进入：跳过密码与验证码，直接写入假登录返回的 token 后整页跳转，
// 目的是让路由守卫用新 token 重跑 getInfo + generateRoutes，按角色重建菜单与权限。
// 失败时若响应拦截器已提示过（e.handled），这里不再叠一条原始报错文案。
function uatEnter(role) {
  uatLoading.value = role
  enterAs(role).catch((e) => {
    uatLoading.value = ''
    // handled=true：request.js 拦截器已给出可操作提示（如“网关 8890 未启动”），
    // 这里不要再补一条原始的 “Request failed with status code 500”。
    if (e && e.handled) return
    ElMessage.error((e && e.message) || 'UAT 快捷进入失败，请确认 UAT 后端已启动')
  })
  // 成功时 enterAs 整页跳转，无需复位
}

// ===== 依赖与页面级状态 =====
// store 负责会话与登录动作，route 用于取 redirect，proxy 用来调用表单组件实例上的 validate()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const {proxy} = getCurrentInstance()
// 旧版「安全验证」弹窗遗留的两个状态：当前登录流程已改走滑块验证码，未再读写，仅保留声明
const showSecurityVerification = ref(false)
const pendingLoginData = ref(null)

// ===== 滑块验证码状态 =====
// 底图与拼图块都是后端下发的 base64 PNG，模板里拼成 data URL 直接渲染；
// jigsawY 为拼图块的纵向落点（随图片一起下发），sliderPosition 为滑块横向位移，松手时原样作为校验参数 x。
const showPuzzleCaptcha = ref(false)
const originalImageBase64 = ref('')
const jigsawImageBase64 = ref('')
const jigsawYPosition = ref(0)
const sliderPosition = ref(0)
const captchaMessage = ref('')
const isImageLoading = ref(false)

const captchaImageWidth = ref(300) // 默认宽度

// 表单模型：captchaToken 由 /captcha 下发，滑块校验与登录接口必须使用同一枚 token，否则后端判为无效验证码
const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

// 前端只校验非空；账号是否存在、密码是否正确由后端判定并返回具体提示
const loginRules = {
  username: [{required: true, trigger: "blur", message: "请输入您的账号"}],
  password: [{required: true, trigger: "blur", message: "请输入您的密码"}]
}

// 若依模板遗留开关：codeUrl 是旧图形验证码地址（滑块方案下未使用）；captchaEnabled 决定登录失败后是否重拉验证码；
// register 控制「立即注册」入口，本页从未将其置为 true，故该链接默认不展示。
const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
// 登录成功后的落地路由，来自 ?redirect=，由下方 watch 同步
const redirect = ref(undefined)

const isSliderActive = ref(false)

// 监听整个 route 对象而非仅首次：被守卫反复踢回登录页时 redirect 参数可能变化，immediate 保证首屏即取值
watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, {immediate: true})

// 点击登录：表单校验通过后先拉一张新验证码再弹滑块，此时账号密码只留在本地、不提交
function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      getCode()
      showPuzzleCaptcha.value = true
    }
  })
  // 下方整段为旧版「图形验证码 -> 直接登录」实现，已注释保留备查，当前生效的是滑块流程
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


// 取一张新验证码：图片、拼图落点与 token 一次性成套下发，token 存入表单供校验和登录复用。
// 失败时把两张 base64 清空，让模板落到 error-placeholder 分支，而不是显示半张图或空白区域。
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

// 弹窗宽度由原图尺寸推导：原图宽（本身封顶 500px）+ 40px 内边距，再夹在 300~540px 之间，
// 避免小图时弹窗过窄、大图时撑出可视区；等 img.onload 后才 resolve，保证 width 设置时图片尺寸已知。
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



// 滑块拖拽：move/up 监听挂在 document 上，鼠标移出滑块范围也不会丢事件；
// 起点位移 + 横向增量后夹在 [0, 容器宽 - 拼图块宽]，保证拼图块不会越出底图左右边界。
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

// 把滑块横坐标交给网关比对（token 必须是 getCode 下发的那枚）。
// 通过后留 500ms 让成功提示可读，再关弹窗并真正登录；不通过时走 catch 自动换一张新图重试。
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


// 弹窗关闭（右上角按钮/ESC 等）时统一复位滑块状态，避免下次打开还带着上一次的位移与 loading
// Cancel CAPTCHA
function handleCaptchaCancel() {
  showPuzzleCaptcha.value = false
  sliderPosition.value = 0
  captchaMessage.value = ''
  isSliderActive.value = false
  isImageLoading.value = false
}

// 旧版「验证码通过后直接登录」的实现，已被 proceedWithLogin() 取代，当前没有调用方；
// 它仍是「记住密码」唯一的写入点：账号明文、密码 RSA 加密后各存 30 天，取消勾选则清掉三项。
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

// 首屏回填：从 Cookie 恢复「记住密码」的账号与密码（密码是 RSA 密文，需 decrypt 还原），
// Cookie 不存在时保留表单现值，避免把用户刚输入的内容冲掉。
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
// 与 performActualLogin 的两处差异：落地页缺省为 /index（后台首页），失败时刷新验证码而不是保留弹窗。
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

// 初始化：setup 阶段同步回填一次表单，页面渲染时账号/密码已是记住的值，不会闪空
getCookie()
</script>

<style lang='scss' scoped>
/* 页面根容器：背景图铺满 + flex 居中，高度依赖父级链路的 100% */
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

/* 登录卡片：固定 400px 宽、白底圆角，内部输入框统一 40px 高 */
.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  z-index: 1;

  .uat-quick {
    margin: 0 0 20px 0;
    padding: 12px;
    background: #fdf6ec;
    border: 1px solid #f3d19e;
    border-radius: 6px;

    .uat-quick-title {
      color: #b88230;
      font-size: 13px;
      margin-bottom: 10px;
      text-align: center;
    }

    .uat-quick-buttons {
      display: flex;
      gap: 8px;

      :deep(.el-button + .el-button) {
        margin-left: 8px;
      }
    }
  }

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

/* 验证码弹窗内容区：图片区与滑轨自上而下排列，宽度收缩到弹窗内 */
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

/* 拼图块浮在底图之上（z-index 10）；关闭指针事件，拖拽只由下方滑轨接收 */
.jigsaw-image {
  position: absolute;
  cursor: move;
  object-fit: contain;
  object-position: center;
  z-index: 10;
  pointer-events: none;
}

/* 滑轨层级：轨道 2 < 滑块按钮 4；宽度 = 滑块位移 + 24px（半个滑块宽），让进度条右端跟住滑块中心 */
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

/* 48px 宽度需与 startDrag() 中的 jigsawWidth 常量保持一致，否则越界判断会错位 */
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

/* 选择器是单下划线，而 Element Plus 弹窗头部类名为 el-dialog__header；且弹窗内容在传送层内，
   scoped 样式需 :deep() 才能命中，因此这条规则实际不生效。 */
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

/* 提示文字不接收鼠标事件，否则会挡住下方滑块的拖拽 */
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

/* 空规则块：当前无任何声明，弹窗定位由模板上的内联 style 完成，保留作为覆盖入口 */
.el-dialog {
}

</style>
