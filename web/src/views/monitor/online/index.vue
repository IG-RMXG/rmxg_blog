<template>
   <div class="app-container">
      <!-- 搜索区：登录地址 / 用户名称，后端按包含匹配（模糊），留空则不过滤 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true">
         <el-form-item label="登录地址" prop="ipaddr">
            <el-input
               v-model="queryParams.ipaddr"
               placeholder="请输入登录地址"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="用户名称" prop="userName">
            <el-input
               v-model="queryParams.userName"
               placeholder="请输入用户名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>
      <!-- 表格数据在模板里按 pageNum/pageSize 再切一次；翻页不请求接口，只有第一页能取到数据 -->
      <el-table
         v-loading="loading"
         :data="onlineList.slice((pageNum - 1) * pageSize, pageNum * pageSize)"
         style="width: 100%;"
      >
         <!-- 序号必须手工算：数据已在模板里切片，type="index" 只会从 1 开始 -->
         <el-table-column label="序号" width="50" type="index" align="center">
            <template #default="scope">
               <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
            </template>
         </el-table-column>
         <el-table-column label="会话编号" align="center" prop="tokenId" :show-overflow-tooltip="true" />
         <el-table-column label="登录名称" align="center" prop="userName" :show-overflow-tooltip="true" />
         <el-table-column label="主机" align="center" prop="ipaddr" :show-overflow-tooltip="true" />
         <el-table-column label="登录时间" align="center" prop="loginTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.loginTime) }}</span>
            </template>
         </el-table-column>
         <!-- 强退按 tokenId 让指定会话失效，同一用户的其它登录会话不受影响 -->
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Delete" @click="handleForceLogout(scope.row)" v-hasPermi="['monitor:online:forceLogout']">强退</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页只改本地 pageNum/pageSize，由上面的 slice 生效，不触发请求 -->
      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />
   </div>
</template>

<script setup name="Online">
/*
 * 在线用户监控（菜单：系统监控 → 在线用户）。
 *
 * 数据源是 Redis 里的 login_tokens:*，不落库：一个会话一条记录，会话过期即从列表消失，
 * 因此每次查询拿到的都是「此刻的快照」，不存在翻到后面几页更实时这回事。
 *
 * 分页：后端 /online/list 支持 pageNum/pageSize（默认 pageSize=10，在内存切片后返回），
 * 本页却只在前端对返回的 rows 再做一次切片，没有把页码传给接口，<pagination> 也不监听 @pagination。
 * 由此带来的限制：接口单次最多返回 10 条，在线会话超过 10 个时翻到第 2 页会看到空列表。
 *
 * 权限点：monitor:online:forceLogout。
 */
import { forceLogout, list as initData } from "@/api/monitor/online"

const { proxy } = getCurrentInstance()

// ===== 列表状态 =====
// onlineList 只保存接口本次返回的那一页 rows（后端默认 pageSize=10）；total 是过滤后的会话总数
const onlineList = ref([])
const loading = ref(true)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 查询条件：字段为空时后端不按其过滤；过滤在服务端完成，但只返回一页数据
const queryParams = ref({
  ipaddr: undefined,
  userName: undefined
})

/** 查询登录日志列表 */
// 名称沿用 RuoYi 原文案；实际查的是在线会话，过滤在服务端完成，前端只负责切片展示
function getList() {
  loading.value = true
  initData(queryParams.value).then(response => {
    onlineList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  pageNum.value = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 强退按钮操作 */
// 传 tokenId 而非 userId：同一用户多处登录会有多个会话，这里只踢掉选中的那一份
function handleForceLogout(row) {
    proxy.$modal.confirm('是否确认强退名称为"' + row.userName + '"的用户?').then(function () {
  return forceLogout(row.tokenId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>
