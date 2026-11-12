<template>
   <!-- 授权用户 -->
   <el-dialog title="选择用户" v-model="visible" width="800px" top="5vh" append-to-body>
      <!-- 只查询“尚未分配该角色”的用户（unallocatedUserList 接口），勾选后一次性批量授权 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true">
         <el-form-item label="用户名称" prop="userName">
            <el-input
               v-model="queryParams.userName"
               placeholder="请输入用户名称"
               clearable
               style="width: 180px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="手机号码" prop="phonenumber">
            <el-input
               v-model="queryParams.phonenumber"
               placeholder="请输入手机号码"
               clearable
               style="width: 180px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>
      <!-- 候选用户表：点击整行也会切换勾选（@row-click → toggleRowSelection），固定高度 260px 便于分页常驻 -->
      <el-row>
         <el-table @row-click="clickRow" ref="refTable" :data="userList" @selection-change="handleSelectionChange" height="260px">
            <el-table-column type="selection" width="55"></el-table-column>
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
         </el-table>
         <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
         />
      </el-row>
      <template #footer>
         <div class="dialog-footer">
            <el-button type="primary" @click="handleSelectUser">确 定</el-button>
            <el-button @click="visible = false">取 消</el-button>
         </div>
      </template>
   </el-dialog>
</template>

<script setup name="SelectUser">
/**
 * 角色授权选人弹窗（由 authUser.vue 以 ref 方式挂载，不单独占用路由）
 * 职责：分页列出“尚未分配该角色”的用户，勾选后批量授权。
 * 接口：unallocatedUserList（未分配用户分页）、authUserSelectAll（批量授权）。
 * 对外契约：props.roleId 由父组件传入（Number 或 String）；暴露 show() 供父组件打开弹窗；授权成功 emit("ok")。
 */
import { authUserSelectAll, unallocatedUserList } from "@/api/system/role"

// roleId 由父组件以 :roleId 传入；父页面的 roleId 来自路由参数，是字符串，故类型同时接受 Number 与 String
const props = defineProps({
  roleId: {
    type: [Number, String]
  }
})

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

// 弹窗状态：visible 控制显隐；userIds 为勾选用户 ID 集合，提交时按逗号拼接为字符串
const userList = ref([])
const visible = ref(false)
const total = ref(0)
const userIds = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleId: undefined,
  userName: undefined,
  phonenumber: undefined
})

// 显示弹框
// 每次打开都把父组件的 roleId 同步到本地查询条件并重新拉取列表；pageNum 未重置，重复打开会沿用上次页码
function show() {
  queryParams.roleId = props.roleId
  getList()
  visible.value = true
}

/**选择行 */
// 整行点击即切换该行勾选状态；toggleRowSelection 不带第二个参数时为“取反”，勾选集合由 el-table 维护
function clickRow(row) {
  proxy.$refs["refTable"].toggleRowSelection(row)
}

// 多选框选中数据
function handleSelectionChange(selection) {
  userIds.value = selection.map(item => item.userId)
}

// 查询表数据
// 只查“尚未分配给该角色”的用户，因此弹窗中不会出现已授权用户，避免重复授权
function getList() {
  unallocatedUserList(queryParams).then(res => {
    userList.value = res.rows
    total.value = res.total
  })
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

const emit = defineEmits(["ok"])
/** 选择授权用户操作 */
// 未勾选时直接提示并中止；成功后用后端返回的 msg 提示，关闭弹窗并通过 emit("ok") 让父页刷新已授权列表
function handleSelectUser() {
  const roleId = queryParams.roleId
  const uIds = userIds.value.join(",")
  if (uIds == "") {
    proxy.$modal.msgError("请选择要分配的用户")
    return
  }
  authUserSelectAll({ roleId: roleId, userIds: uIds }).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    visible.value = false
    emit("ok")
  })
}

// 只对外暴露 show()：父组件只能打开弹窗，内部状态与提交逻辑不外泄
defineExpose({
  show,
})
</script>
