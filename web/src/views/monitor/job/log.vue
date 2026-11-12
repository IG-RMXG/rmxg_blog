<template>
   <div class="app-container">
      <!-- 搜索区：任务名 / 任务组 / 执行状态（sys_common_status）/ 执行时间区间 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="任务名称" prop="jobName">
            <el-input
               v-model="queryParams.jobName"
               placeholder="请输入任务名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="任务组名" prop="jobGroup">
            <el-select
               v-model="queryParams.jobGroup"
               placeholder="请选择任务组名"
               clearable
               style="width: 240px"
            >
               <el-option
                  v-for="dict in sys_job_group"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="执行状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="请选择执行状态"
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
         <el-form-item label="执行时间" style="width: 308px">
            <el-date-picker
               v-model="dateRange"
               value-format="YYYY-MM-DD"
               type="daterange"
               range-separator="-"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
            ></el-date-picker>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <!-- 工具栏：删除选中 / 清空全部 / 导出 / 关闭返回任务列表 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['monitor:job:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               @click="handleClean"
               v-hasPermi="['monitor:job:remove']"
            >清空</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['monitor:job:export']"
            >导出</el-button>
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

      <!-- 日志列表：按 jobLogId 倒序；执行状态用 sys_common_status 字典渲染 -->
      <el-table v-loading="loading" :data="jobLogList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="日志编号" width="80" align="center" prop="jobLogId" />
         <el-table-column label="任务名称" align="center" prop="jobName" :show-overflow-tooltip="true" />
         <el-table-column label="任务组名" align="center" prop="jobGroup" :show-overflow-tooltip="true">
            <template #default="scope">
               <dict-tag :options="sys_job_group" :value="scope.row.jobGroup" />
            </template>
         </el-table-column>
         <el-table-column label="调用目标字符串" align="center" prop="invokeTarget" :show-overflow-tooltip="true" />
         <el-table-column label="日志信息" align="center" prop="jobMessage" :show-overflow-tooltip="true" />
         <el-table-column label="执行状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="sys_common_status" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="执行时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['monitor:job:query']">详细</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：与查询条件共用 queryParams；时间区间由 addDateRange 另拼进 params -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 调度日志详细 -->
      <el-dialog title="调度日志详细" v-model="open" width="700px" append-to-body>
         <el-form :model="form" label-width="100px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="日志序号：">{{ form.jobLogId }}</el-form-item>
                  <el-form-item label="任务名称：">{{ form.jobName }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="任务分组：">{{ form.jobGroup }}</el-form-item>
                  <el-form-item label="执行时间：">{{ form.createTime }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="调用方法：">{{ form.invokeTarget }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="日志信息：">{{ form.jobMessage }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="执行状态：">
                     <div v-if="form.status == 0">正常</div>
                     <div v-else-if="form.status == 1">失败</div>
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <!-- 只有失败（status=1）的日志才有异常堆栈，成功时该字段为空 -->
                  <el-form-item label="异常信息：" v-if="form.status == 1">{{ form.exceptionInfo }}</el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button @click="open = false">关 闭</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="JobLog">
/*
 * 定时任务调度日志（入口：任务列表「日志」按钮 → /monitor/job-log/index/:jobId）。
 *
 * 带 jobId 进入时先查任务详情回填任务名/组名再查日志；从菜单直接进入（jobId=0 或为空）则查全量。
 * 接口：@/api/monitor/jobLog（列表/删除/清空），任务详情复用 @/api/monitor/job。
 * 字典：sys_common_status（'0'=成功 '1'=失败）、sys_job_group。
 * 权限点：monitor:job:remove（删除/清空）、monitor:job:export、monitor:job:query。
 */
import { getJob } from "@/api/monitor/job"
import { listJobLog, delJobLog, cleanJobLog } from "@/api/monitor/jobLog"

const { proxy } = getCurrentInstance()
// 字典：sys_common_status 是执行结果（0 成功 / 1 失败），sys_job_group 用于展示与过滤任务组
const { sys_common_status, sys_job_group } = proxy.useDict("sys_common_status", "sys_job_group")

// ===== 列表与弹窗状态 =====
const jobLogList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
// multiple 控制「删除」按钮：没有勾选任何日志时为 true（按钮禁用）
const multiple = ref(true)
const total = ref(0)
// 执行时间区间，查询前由 proxy.addDateRange 展开成 beginTime / endTime
const dateRange = ref([])
// 路由参数里带来源任务 id（从任务列表跳转时写入）
const route = useRoute()

// 表单与查询条件；queryParams 里的 dictName/dictType 是复制模板遗留字段，接口不消费
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  }
})

// data 里并没有 rules（本页无表单校验），解构出的 rules 为 undefined，模板未使用故无影响
const { queryParams, form, rules } = toRefs(data)

/** 查询调度日志列表 */
// 查询前把 dateRange 摊平成 beginTime/endTime；pageNum/pageSize 随 queryParams 一起交给后端 PageHelper
function getList() {
  loading.value = true
  listJobLog(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    jobLogList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 返回按钮
// 用 $tab.closeOpenPage 关掉当前 tags-view 标签并跳回任务列表，而不是 router.back()：
// 从菜单直接进入时没有历史记录，这样两种入口都能正确返回
function handleClose() {
  const obj = { path: "/monitor/job" }
  proxy.$tab.closeOpenPage(obj)
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
// 时间区间不在表单里，resetForm 清不到，必须单独清空后再查询
function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.jobLogId)
  multiple.value = !selection.length
}

/** 详细按钮操作 */
// 直接复用列表行数据，不再请求接口：行里已含详情所需的全部字段
function handleView(row) {
  open.value = true
  form.value = row
}

/** 删除按钮操作 */
// 按钮在工具栏上（不传 row），删除的是勾选的 ids
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除调度日志编号为"' + ids.value + '"的数据项?').then(function () {
    return delJobLog(ids.value)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 清空按钮操作 */
// 整表物理删除且不可恢复，所以必须二次确认
function handleClean() {
  proxy.$modal.confirm("是否确认清空所有调度日志数据项?").then(function () {
    return cleanJobLog()
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("清空成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
// 导出走 proxy.download：GET schedule/job/log/export 直接下载 xlsx；文件名带时间戳避免覆盖
function handleExport() {
  proxy.download("schedule/job/log/export", {
    ...queryParams.value,
  }, `job_log_${new Date().getTime()}.xlsx`)
}

// 页面初始化：带 jobId 时先取任务信息回填过滤条件再查日志；jobId 为 0/空表示从菜单直接进入
(() => {
  const jobId = route.params && route.params.jobId
  if (jobId !== undefined && jobId != 0) {
    getJob(jobId).then(response => {
      queryParams.value.jobName = response.data.jobName
      queryParams.value.jobGroup = response.data.jobGroup
      getList()
    })
  } else {
    getList()
  }
})()
</script>
