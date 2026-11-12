
<template>
   <div class="app-container">
      <!-- 已授权用户：按用户名称/手机号检索当前角色下的用户，roleId 取自路由参数，进页面即固定 -->
      <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true">
         <el-form-item label="用户名称" prop="userName">
            <el-input
               v-model="queryParams.userName"
               placeholder="请输入用户名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="手机号码" prop="phonenumber">
            <el-input
               v-model="queryParams.phonenumber"
               placeholder="请输入手机号码"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <!-- 工具栏：“添加用户”复用 system:role:add 权限点，“批量取消授权”复用 system:role:remove；
           添加用户仅打开 selectUser 子组件弹窗，提交动作在子组件内完成 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="openSelectUser"
               v-hasPermi="['system:role:add']"
            >添加用户</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="CircleClose"
               :disabled="multiple"
               @click="cancelAuthUserAll"
               v-hasPermi="['system:role:remove']"
            >批量取消授权</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button 
               type="warning" 
               plain 
               icon="Close"
               @click="handleClose"
            >关闭</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 已授权用户表格：状态列由 dict-tag 渲染 sys_normal_disable；行内“取消授权”走 system:role:remove -->
      <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="用户名称" prop="userName" :show-overflow-tooltip="true" />
         <el-table-column label="用户昵称" prop="nickName" :show-overflow-tooltip="true" />
         <el-table-column label="邮箱" prop="email" :show-overflow-tooltip="true" />
         <el-table-column label="手机" prop="phonenumber" :show-overflow-tooltip="true" />
         <el-table-column label="状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="CircleClose" @click="cancelAuthUser(scope.row)" v-hasPermi="['system:role:remove']">取消授权</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />
      <!-- 选人弹窗子组件：由 openSelectUser 通过 ref.show() 打开，授权成功后 emit("ok") 触发列表刷新 -->
      <select-user ref="selectRef" :roleId="queryParams.roleId" @ok="handleQuery" />
   </div>
</template>

<script setup name="AuthUser">
/**
 * 角色已授权用户页（路由 /system/role-auth/user/:roleId，菜单中隐藏，由角色列表“分配用户”进入）
 * 职责：查看某角色下已分配的用户，支持单个/批量取消授权，并通过 selectUser.vue 弹窗批量添加用户。
 * 接口：allocatedUserList（已授权分页）、authUserCancel（单个取消）、authUserCancelAll（批量取消）。
 * 权限点：system:role:add（添加用户）、system:role:remove（取消授权/批量取消）。
 * 约定：列表返回 { rows, total }；取消授权按 roleId + userId(s) 定位关系记录。
 */
import selectUser from "./selectUser"
import { allocatedUserList, authUserCancel, authUserCancelAll } from "@/api/system/role"

const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

// ===== 列表状态 =====
const userList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const multiple = ref(true)
const total = ref(0)
const userIds = ref([])

// ===== 查询条件 =====
// roleId 取自路由参数（字符串类型），进入页面后固定不变；pageNum/pageSize 由 pagination 组件双向绑定；
// 后端按 pageNum/pageSize 分页，返回 rows=当前页数据、total=总条数。
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleId: route.params.roleId,
  userName: undefined,
  phonenumber: undefined,
})

/** 查询授权用户列表 */
// loading 只在成功回调里复位，接口失败时依赖全局请求拦截器提示，列表会停留在加载态
function getList() {
  loading.value = true
  allocatedUserList(queryParams).then(response => {
    userList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 返回按钮 */
// 用 $tab.closeOpenPage 关闭当前标签页并激活角色列表，避免直接 router.push 导致同一路由多标签并存
function handleClose() {
  const obj = { path: "/system/role" }
  proxy.$tab.closeOpenPage(obj)
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
// 这里只关心“有没有选中”，multiple=true 即禁用批量取消授权按钮（反向标记，不是“已多选”）
function handleSelectionChange(selection) {
  userIds.value = selection.map(item => item.userId)
  multiple.value = !selection.length
}

/** 打开授权用户表弹窗 */
// 通过 ref 调用子组件暴露的 show()；子组件重新同步 roleId 后自行拉取“未分配用户”
function openSelectUser() {
  proxy.$refs["selectRef"].show()
}

/** 取消授权按钮操作 */
// 授权关系由 roleId + userId 共同定位，取消只解除关系，不影响用户本身；用户在确认框点“取消”时走 catch 静默返回
function cancelAuthUser(row) {
  proxy.$modal.confirm('确认要取消该用户"' + row.userName + '"角色吗？').then(function () {
    return authUserCancel({ userId: row.userId, roleId: queryParams.roleId })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("取消授权成功")
  }).catch(() => {})
}

/** 批量取消授权按钮操作 */
// 勾选的用户 ID 先以逗号拼接成字符串提交（接口约定），一次请求完成批量取消；形参 row 未被使用，目标来自表格勾选
function cancelAuthUserAll(row) {
  const roleId = queryParams.roleId
  const uIds = userIds.value.join(",")
  proxy.$modal.confirm("是否取消选中用户授权数据项?").then(function () {
    return authUserCancelAll({ roleId: roleId, userIds: uIds })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("取消授权成功")
  }).catch(() => {})
}

// 页面初始化即拉取当前角色的已授权用户第一页
getList()
</script>
