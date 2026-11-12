<template>
  <!-- 注册页：账号 + 密码 + 确认密码，提交 /system/auth/register（游客可调用）；成功后跳登录页，无验证码环节 -->
  <div class="register">
    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
      <h3 class="title">{{ title }}</h3>
      <el-form-item prop="username">
        <el-input 
          v-model="registerForm.username" 
          type="text" 
          size="large" 
          auto-complete="off" 
          placeholder="账号"
        >
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large" 
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large" 
          auto-complete="off"
          placeholder="确认密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <!-- 提交区：三项前端校验都通过才发请求，成功后必须点「确定」才离开本页 -->
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="large" 
          type="primary"
          style="width:100%;"
          @click.prevent="handleRegister"
        >
          <span v-if="!loading">注 册</span>
          <span v-else>注 册 中...</span>
        </el-button>
        <div style="float: right;">
          <router-link class="link-type" :to="'/login'">使用已有账户登录</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-register-footer">
      <span>Copyright © 2018-2025 ruoyi.vip All Rights Reserved.</span>
    </div>
  </div>
</template>

<script setup>
// ===== 注册页 =====
// 职责：收集账号/密码 -> POST /system/auth/register（api/login.js，isToken: false 匿名可调）。
// 前端规则只做提前拦截，账号唯一性、密码强度等最终以后端校验为准。
// 注册成功不会自动登录：弹窗确认后跳 /login，由用户手动登录换取 token。
import { ElMessageBox } from "element-plus"
import { register } from "@/api/login"

// 标题与登录页同源，取构建期注入的 VITE_APP_TITLE
const title = import.meta.env.VITE_APP_TITLE
const router = useRouter()
const { proxy } = getCurrentInstance()

// 表单模型：confirmPassword 只用于前端比对，但会随整个对象一起提交给注册接口
const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: ""
})

// 自定义校验器：两次密码需完全一致（区分大小写）；element-plus 的 validator 必须调用 callback，
// 否则该项会一直停在校验中、表单永远拿不到 valid=true。
const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

// 规则与后端注册入参对齐：账号 2~20 位、密码 5~20 位，并禁止 < > " ' \ | 这类会破坏页面或 SQL 的字符；
// 提前在失焦时拦住，减少「提交后才报错」的往返。
const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    { min: 5, max: 20, message: "用户密码长度必须介于 5 和 20 之间", trigger: "blur" },
    { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\\ |", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ]
}

// 提交中标记：防重复点击，同时驱动按钮 loading 与「注 册 中...」文案
const loading = ref(false)

// 注册提交：校验通过 -> 置 loading -> 调接口；成功后用 HTML 字符串拼红色文案（需 dangerouslyUseHTMLString），
// 点「确定」才 push 到 /login；失败由 request 拦截器统一提示，这里只复位 loading 以便重试。
function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login")
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
      })
    }
  })
}
</script>

<style lang='scss' scoped>
/* 布局与登录页一致：整屏背景图 + flex 居中卡片。
   注意：此处引用的是 login-background.jpg，而 src/assets/images 下只有 login-background.png，该背景图取不到。 */
.register {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

/* 卡片固定 400px 宽，输入框高度与登录页保持一致（40px） */
.register-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
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
.register-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.el-register-footer {
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
</style>
