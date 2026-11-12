<template>
   <el-form ref="pwdRef" :model="user" :rules="rules" label-width="80px">
      <!-- 自助改密必须验旧密码；管理员给他人重置走用户列表的「重置密码」，免旧密码 -->
      <el-form-item label="旧密码" prop="oldPassword">
         <el-input v-model="user.oldPassword" placeholder="请输入旧密码" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
         <el-input v-model="user.newPassword" placeholder="请输入新密码" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
         <el-input v-model="user.confirmPassword" placeholder="请确认新密码" type="password" show-password/>
      </el-form-item>
      <el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
      <el-button type="danger" @click="close">关闭</el-button>
      </el-form-item>
   </el-form>
</template>

<script setup>
/**
 * 个人中心 - 修改密码（本人操作）。
 *
 * 走 PUT /system/user/profile/updatePwd，后端校验 oldPassword，错误时以 500 + 文案返回；
 * 与用户管理页的「重置密码」（管理员操作、不校验旧密码）不是同一条链路。
 */
import { updateUserPwd } from "@/api/system/user"

const { proxy } = getCurrentInstance()

const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined
})

// 自定义校验器：把「确认密码」与当前 newPassword 比较；点保存时 validate 会重新执行，改过新密码也能拦住
const equalToPassword = (rule, value, callback) => {
  if (user.newPassword !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

// 新密码 6~20 位（比新增用户的 5~20 更严），同样禁止 < > " ' \ | 等字符
const rules = ref({
  oldPassword: [{ required: true, message: "旧密码不能为空", trigger: "blur" }],
  newPassword: [{ required: true, message: "新密码不能为空", trigger: "blur" }, { min: 6, max: 20, message: "长度在 6 到 20 个字符", trigger: "blur" }, { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\\ |", trigger: "blur" }],
  confirmPassword: [{ required: true, message: "确认密码不能为空", trigger: "blur" }, { required: true, validator: equalToPassword, trigger: "blur" }]
})

/** 提交按钮 */
function submit() {
  // 成功后不关页，便于用户看到提示；旧密码错误由请求拦截器统一弹错
  proxy.$refs.pwdRef.validate(valid => {
    if (valid) {
      updateUserPwd(user.oldPassword, user.newPassword).then(response => {
        proxy.$modal.msgSuccess("修改成功")
      })
    }
  })
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage()
}
</script>
