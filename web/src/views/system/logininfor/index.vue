<template>
   <div class="app-container">
      <!-- 搜索区：登录地址 / 用户名称都是 LIKE 模糊匹配，登录状态按字典精确匹配 -->
      <!-- 时间范围单独用 dateRange 存，请求前由 addDateRange 转成后端认识的 params[beginTime] / params[endTime] -->
      <!-- default-time 把起止补成 00:00:00 与 23:59:59，否则只选日期会漏掉当天零点以后的日志 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="登录地址" prop="ipaddr">
            <el-input
               v-model="queryParams.ipaddr"
               placeholder="请输入登录地址"
               clearable
               style="width: 240px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="用户名称" prop="userName">
            <el-input
               v-model="queryParams.userName"
               placeholder="请输入用户名称"
               clearable
               style="width: 240px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="登录状态"
               clearable
               style="width: 240px"
            >
               <el-option
                  v-for="dict in sys_common_status"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="登录时间" style="width: 308px">
            <el-date-picker
               v-model="dateRange"
               value-format="YYYY-MM-DD HH:mm:ss"
               type="daterange"
               range-separator="-"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
               :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            ></el-date-picker>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <!-- 工具栏：删除与清空共用 system:logininfor:remove 权限；解锁要求恰好选中 1 条（single 是禁用条件） -->
      <!-- 解锁按用户名而不是按日志主键生效，多选时用户名会被拼成 a,b，所以按钮限制单选 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:logininfor:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               @click="handleClean"
               v-hasPermi="['system:logininfor:remove']"
            >清空</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Unlock"
               :disabled="single"
               @click="handleUnlock"
               v-hasPermi="['system:logininfor:unlock']"
            >解锁</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:logininfor:export']"
            >导出</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 表格：登录状态用 dict-tag 渲染；用户名、访问时间开了服务端排序，默认按访问时间倒序 -->
      <!-- 排序参数会随请求发出，但后端 PageUtils 只消费 pageNum / pageSize，实际顺序固定为 info_id 倒序 -->
      <el-table ref="logininforRef" v-loading="loading" :data="logininforList" @selection-change="handleSelectionChange" :default-sort="defaultSort" @sort-change="handleSortChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="访问编号" align="center" prop="infoId" />
         <el-table-column label="用户名称" align="center" prop="userName" :show-overflow-tooltip="true" sortable="custom" :sort-orders="['descending', 'ascending']" />
         <el-table-column label="登录状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="sys_common_status" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="描述" align="center" prop="msg" :show-overflow-tooltip="true" />
         <el-table-column label="访问时间" align="center" prop="accessTime" sortable="custom" :sort-orders="['descending', 'ascending']" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.accessTime) }}</span>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：改页码或每页条数都会重新 getList，查询条件与时间范围自动带上 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />
   </div>
</template>

<script setup name="Logininfor">
import { list, delLogininfor, cleanLogininfor, unlockLogininfor } from "@/api/system/logininfor"

// 登录日志列表页：查询 + 删除 / 清空 / 解锁 / 导出；没有新增入口，日志由后端登录流程写入。
// 接口：@/api/system/logininfor 的 list / delLogininfor / cleanLogininfor / unlockLogininfor。
// 权限点：system:logininfor:remove（删除与清空共用）、system:logininfor:unlock、system:logininfor:export；
// 与其它系统页一样只有前端 v-hasPermi 控制显隐，后端接口本身没有权限注解。
// 字典：sys_common_status，'0'=成功 '1'=失败；本表 status 是字符串，与操作日志表的 Integer 不同。
// 日志表没有软删列：删除按 infoId 物理删除，清空是 DELETE FROM sys_logininfor，两者都不可回滚。
// 初始化：script setup 顶层末尾直接调用 getList()，进入页面即拉第一页。

const { proxy } = getCurrentInstance()
const { sys_common_status } = proxy.useDict("sys_common_status")

const logininforList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const selectName = ref("")
const total = ref(0)
const dateRange = ref([])
const defaultSort = ref({ prop: "accessTime", order: "descending" })

// 查询参数
// 默认每页 10 条，与后端 PageUtils 的 DEFAULT_PAGE_SIZE 对齐；orderByColumn / isAsc 由 handleSortChange 写入
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  ipaddr: undefined,
  userName: undefined,
  status: undefined,
  orderByColumn: undefined,
  isAsc: undefined
})

/** 查询登录日志列表 */
function getList() {
  loading.value = true
  // 日期范围必须经 addDateRange 才变成 params[beginTime] / params[endTime]；dateRange 为空时两个值都是 undefined，序列化时被跳过
  list(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    logininforList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  // dateRange 不在 el-form 里（没有 prop），resetForm 清不掉，必须单独置空
  dateRange.value = []
  proxy.resetForm("queryRef")
  queryParams.value.pageNum = 1
  // 重置时把表头排序也恢复默认，否则箭头还停在用户点过的列上，与数据实际顺序不一致
  proxy.$refs["logininforRef"].sort(defaultSort.value.prop, defaultSort.value.order)
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.infoId)
  multiple.value = !selection.length
  single.value = selection.length != 1
  // 解锁按用户名生效，所以除了主键还要单独收集 userName；后端 unlock 接口只接受单个用户名
  selectName.value = selection.map(item => item.userName)
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  // el-table 给出的 order 是 ascending / descending，这里原样透传；本服务端的 PageUtils 并不读取这两个字段
  queryParams.value.orderByColumn = column.prop
  queryParams.value.isAsc = column.order
  getList()
}

/** 删除按钮操作 */
function handleDelete(row) {
  // 按 infoId 物理删除，删了不可恢复，必须先二次确认；未传 row 时用多选结果
  const infoIds = row.infoId || ids.value
  proxy.$modal.confirm('是否确认删除访问编号为"' + infoIds + '"的数据项?').then(function () {
    return delLogininfor(infoIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 清空按钮操作 */
function handleClean() {
  // 清空是整表清空（DELETE FROM sys_logininfor），比按条删除危险得多，所以文案里明确写「所有」并二次确认
  proxy.$modal.confirm("是否确认清空所有登录日志数据项?").then(function () {
    return cleanLogininfor()
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("清空成功")
  }).catch(() => {})
}

/** 解锁按钮操作 */
function handleUnlock() {
  // 后端没有账户锁定机制，unlock 只是预留能力，接口无条件返回成功
  // 另外后端声明的是 @PutMapping，本页走 GET，真实请求会 405，用户看到的是拦截器弹出的「系统接口405异常」
  const username = selectName.value
  proxy.$modal.confirm('是否确认解锁用户"' + username + '"数据项?').then(function () {
    return unlockLogininfor(username)
  }).then(() => {
    proxy.$modal.msgSuccess("用户" + username + "解锁成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  // 导出走通用 download（POST 表单参数、blob 响应），文件名带时间戳避免重复覆盖
  // 注意：后端 SysLogininforController 没有 /export 映射，此按钮当前实际会失败
  proxy.download("system/logininfor/export", {
    ...queryParams.value,
  }, `logininfor_${new Date().getTime()}.xlsx`)
}

getList()
</script>
