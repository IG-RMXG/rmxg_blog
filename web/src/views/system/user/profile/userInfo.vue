<template>
   <el-form ref="userRef" :model="form" :rules="rules" label-width="80px">
      <!-- 个人中心只开放昵称/手机/邮箱/性别四项，其余字段后端直接忽略 -->
      <el-form-item label="用户昵称" prop="nickName">
         <el-input v-model="form.nickName" maxlength="30" />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
         <el-input v-model="form.phonenumber" maxlength="11" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
         <el-input v-model="form.email" maxlength="50" />
      </el-form-item>
      <!-- 性别写死 0=男 1=女，不走 sys_user_sex 字典（与用户管理页的取值保持一致） -->
      <el-form-item label="性别">
         <el-radio-group v-model="form.sex">
            <el-radio value="0">男</el-radio>
            <el-radio value="1">女</el-radio>
         </el-radio-group>
      </el-form-item>
      <el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
      <el-button type="danger" @click="close">关闭</el-button>
      </el-form-item>
   </el-form>
</template>

<script setup>
/**
 * 个人中心 - 基本资料表单。
 *
 * 数据由父级 profile 页经 props.user 传入，watch 回填本地 form；
 * 保存走 PUT /system/user/profile，后端只取 nickName/phonenumber/email/sex，userId 强制为登录人。
 */
import { updateUserProfile } from "@/api/system/user"

const props = defineProps({
  user: {
    type: Object
  }
})

const { proxy } = getCurrentInstance()

const form = ref({})
// 与用户管理页不同：这里手机号、邮箱都是必填，管理页允许留空
const rules = ref({
  nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
  email: [{ required: true, message: "邮箱地址不能为空", trigger: "blur" }, { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
  phonenumber: [{ required: true, message: "手机号码不能为空", trigger: "blur" }, { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }],
})

/** 提交按钮 */
function submit() {
  // 成功后把手机/邮箱写回 props.user，左侧资料卡立即更新，不用重新拉一次 profile
  proxy.$refs.userRef.validate(valid => {
    if (valid) {
      updateUserProfile(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功")
        props.user.phonenumber = form.value.phonenumber
        props.user.email = form.value.email
      })
    }
  })
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage()
}

// 回显当前登录用户信息
// 父级是异步取数，immediate 保证首次也能回填；父级整体替换 user 对象时会再次触发
watch(() => props.user, user => {
  if (user) {
    form.value = { nickName: user.nickName, phonenumber: user.phonenumber, email: user.email, sex: user.sex }
  }
},{ immediate: true })
</script>
