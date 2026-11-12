<template>
   <div class="app-container">
      <!--
        用户授权角色页：路由 /system/user-auth/role/:userId，入口在用户列表的「分配角色」。
        上半部分只读展示用户，下半部分勾选角色；停用角色（status !== '0'）不可勾选。
      -->
      <h4 class="form-header h4">基本信息</h4>
      <el-form :model="form" label-width="80px">
         <el-row>
            <el-col :span="8" :offset="2">
               <el-form-item label="用户昵称" prop="nickName">
                  <el-input v-model="form.nickName" disabled />
               </el-form-item>
            </el-col>
            <el-col :span="8" :offset="2">
               <el-form-item label="登录账号" prop="userName">
                  <el-input v-model="form.userName" disabled />
               </el-form-item>
            </el-col>
         </el-row>
      </el-form>

      <h4 class="form-header h4">角色信息</h4>
      <!-- 角色全量返回、前端分页（slice）；row-key + reserve-selection 保证跨页勾选不丢 -->
      <el-table v-loading="loading" :row-key="getRowKey" @row-click="clickRow" ref="roleRef" @selection-change="handleSelectionChange" :data="roles.slice((pageNum - 1) * pageSize, pageNum * pageSize)">
         <el-table-column label="序号" width="55" type="index" align="center">
            <template #default="scope">
               <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
            </template>
         </el-table-column>
         <el-table-column type="selection" :reserve-selection="true" :selectable="checkSelectable" width="55"></el-table-column>
         <el-table-column label="角色编号" align="center" prop="roleId" />
         <el-table-column label="角色名称" align="center" prop="roleName" />
         <el-table-column label="权限字符" align="center" prop="roleKey" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />

      <el-form label-width="100px">
         <div style="text-align: center;margin-left:-120px;margin-top:30px;">
            <el-button type="primary" @click="submitForm()">提交</el-button>
            <el-button @click="close()">返回</el-button>
         </div>
      </el-form>
   </div>
</template>

<script setup name="AuthRole">
/**
 * 用户授权角色页。
 *
 * 接口：GET /system/user/authRole/{userId} 返回顶层 { user, roles }，roles 每项带 flag
 *      表示该用户当前是否已拥有；PUT /system/user/authRole 的 { userId, roleIds } 走 query，
 *      roleIds 必须是逗号拼接字符串（数组会被序列化成 roleIds[]=...，后端解析不到）。
 */
import { getAuthRole, updateAuthRole } from "@/api/system/user"

const route = useRoute()
const { proxy } = getCurrentInstance()

const loading = ref(true)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const roleIds = ref([])
const roles = ref([])
const form = ref({
  nickName: undefined,
  userName: undefined,
  userId: undefined
})

/** 单击选中行数据 */
function clickRow(row) {
  // 整行点击等价于勾选/取消；停用角色直接忽略，避免误把用户挂到已停用的角色上
  if (checkSelectable(row)) {
    proxy.$refs["roleRef"].toggleRowSelection(row)
  }
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  roleIds.value = selection.map(item => item.roleId)
}

/** 保存选中的数据编号 */
function getRowKey(row) {
  return row.roleId
}

// 检查角色状态
// status 语义与角色管理页一致：'0' 正常、'1' 停用
function checkSelectable(row) {
  return row.status === "0" ? true : false
}

/** 关闭按钮 */
function close() {
  const obj = { path: "/system/user" }
  proxy.$tab.closeOpenPage(obj)
}

/** 提交按钮 */
function submitForm() {
  // 后端只接受逗号分隔的 roleIds 字符串；全不选时提交空串，表示清空该用户的全部角色
  const userId = form.value.userId
  const rIds = roleIds.value.join(",")
  updateAuthRole({ userId: userId, roleIds: rIds }).then(response => {
    proxy.$modal.msgSuccess("授权成功")
    close()
  })
}

// 自执行初始化：仅当路由带 userId 时拉数据，并据 flag 回显已拥有的角色
(() => {
  const userId = route.params && route.params.userId
  if (userId) {
    loading.value = true
    getAuthRole(userId).then(response => {
      form.value = response.user
      roles.value = response.roles
      total.value = roles.value.length
      // 必须等 el-table 渲染完成再 toggleRowSelection，否则拿不到行实例、勾选会失效
      nextTick(() => {
        roles.value.forEach(row => {
          if (row.flag) {
            proxy.$refs["roleRef"].toggleRowSelection(row)
          }
        })
      })
      loading.value = false
    })
  }
})()
</script>
