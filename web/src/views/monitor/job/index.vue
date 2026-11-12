<template>
   <div class="app-container">
      <!-- 搜索区：任务名（模糊匹配）/ 任务组 / 状态 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="任务名称" prop="jobName">
            <el-input
               v-model="queryParams.jobName"
               placeholder="请输入任务名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="任务组名" prop="jobGroup">
            <el-select v-model="queryParams.jobGroup" placeholder="请选择任务组名" clearable style="width: 200px">
               <el-option
                  v-for="dict in sys_job_group"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="任务状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择任务状态" clearable style="width: 200px">
               <el-option
                  v-for="dict in sys_job_status"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <!-- 工具栏：新增 / 修改 / 删除 / 导出 / 日志；按钮受 monitor:job:* 权限点与选中行数控制 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['monitor:job:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['monitor:job:edit']"
            >修改</el-button>
         </el-col>
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
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['monitor:job:export']"
            >导出</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="info"
               plain
               icon="Operation"
               @click="handleJobLog"
               v-hasPermi="['monitor:job:query']"
            >日志</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 任务列表：状态列用开关直接切启停（'0'=正常 '1'=暂停），切换前会二次确认 -->
      <el-table v-loading="loading" :data="jobList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="任务编号" width="100" align="center" prop="jobId" />
         <el-table-column label="任务名称" align="center" prop="jobName" :show-overflow-tooltip="true" />
         <el-table-column label="任务组名" align="center" prop="jobGroup">
            <template #default="scope">
               <dict-tag :options="sys_job_group" :value="scope.row.jobGroup" />
            </template>
         </el-table-column>
         <el-table-column label="调用目标字符串" align="center" prop="invokeTarget" :show-overflow-tooltip="true" />
         <el-table-column label="cron执行表达式" align="center" prop="cronExpression" :show-overflow-tooltip="true" />
         <el-table-column label="状态" align="center">
            <template #default="scope">
               <el-switch
                  v-model="scope.row.status"
                  active-value="0"
                  inactive-value="1"
                  @change="handleStatusChange(scope.row)"
               ></el-switch>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-tooltip content="修改" placement="top">
                  <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['monitor:job:edit']"></el-button>
               </el-tooltip>
               <el-tooltip content="删除" placement="top">
                  <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['monitor:job:remove']"></el-button>
               </el-tooltip>
               <el-tooltip content="执行一次" placement="top">
                  <el-button link type="primary" icon="CaretRight" @click="handleRun(scope.row)" v-hasPermi="['monitor:job:changeStatus']"></el-button>
               </el-tooltip>
               <el-tooltip content="任务详细" placement="top">
                  <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['monitor:job:query']"></el-button>
               </el-tooltip>
               <el-tooltip content="调度日志" placement="top">
                  <el-button link type="primary" icon="Operation" @click="handleJobLog(scope.row)" v-hasPermi="['monitor:job:query']"></el-button>
               </el-tooltip>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：分页参数与查询条件共用同一个 queryParams，翻页后重新查询 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改定时任务对话框 -->
      <el-dialog :title="title" v-model="open" width="820px" append-to-body>
         <el-form ref="jobRef" :model="form" :rules="rules" label-width="120px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="任务名称" prop="jobName">
                     <el-input v-model="form.jobName" placeholder="请输入任务名称" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="任务分组" prop="jobGroup">
                     <el-select v-model="form.jobGroup" placeholder="请选择">
                        <el-option
                           v-for="dict in sys_job_group"
                           :key="dict.value"
                           :label="dict.label"
                           :value="dict.value"
                        ></el-option>
                     </el-select>
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <!-- 调用目标：支持「Bean名.方法(参数)」或「全限定类名.方法(参数)」，参数仅限字符串/布尔/整型/长整型/浮点型 -->
                  <el-form-item prop="invokeTarget">
                     <template #label>
                        <span>
                           调用方法
                           <el-tooltip placement="top">
                              <template #content>
                                 <div>
                                    Bean调用示例：ryTask.ryParams('ry')
                                    <br />Class类调用示例：com.ruoyi.quartz.task.RyTask.ryParams('ry')
                                    <br />参数说明：支持字符串，布尔类型，长整型，浮点型，整型
                                 </div>
                              </template>
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                        </span>
                     </template>
                     <el-input v-model="form.invokeTarget" placeholder="请输入调用目标字符串" />
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <!-- cron 表达式：可手输，也可点「生成表达式」用可视化生成器回填 -->
                  <el-form-item label="cron表达式" prop="cronExpression">
                     <el-input v-model="form.cronExpression" placeholder="请输入cron执行表达式">
                        <template #append>
                           <el-button type="primary" @click="handleShowCron">
                              生成表达式
                              <i class="el-icon-time el-icon--right"></i>
                           </el-button>
                        </template>
                     </el-input>
                  </el-form-item>
               </el-col>
               <!-- 状态只在编辑时出现：新增沿用 reset 里的 '0'（正常），创建后再用列表开关启停 -->
               <el-col :span="24" v-if="form.jobId !== undefined">
                  <el-form-item label="状态">
                     <el-radio-group v-model="form.status">
                        <el-radio
                           v-for="dict in sys_job_status"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="执行策略" prop="misfirePolicy">
                     <el-radio-group v-model="form.misfirePolicy">
                        <el-radio-button value="1">立即执行</el-radio-button>
                        <el-radio-button value="2">执行一次</el-radio-button>
                        <el-radio-button value="3">放弃执行</el-radio-button>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="是否并发" prop="concurrent">
                     <el-radio-group v-model="form.concurrent">
                        <el-radio-button value="0">允许</el-radio-button>
                        <el-radio-button value="1">禁止</el-radio-button>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>

     <!-- Cron 生成器：destroy-on-close 让每次打开都按最新的 expression 重建内部标签页 -->
     <el-dialog title="Cron表达式生成器" v-model="openCron" append-to-body destroy-on-close>
       <!-- @fill 只回填表达式，关闭动作由子组件的 @hide 触发 -->
       <crontab ref="crontabRef" @hide="openCron=false" @fill="crontabFill" :expression="expression"></crontab>
     </el-dialog>

      <!-- 任务日志详细 -->
      <el-dialog title="任务详细" v-model="openView" width="700px" append-to-body>
         <!-- 只读详情：状态/并发/执行策略等枚举值在这里转成中文文案 -->
         <el-form :model="form" label-width="120px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="任务编号：">{{ form.jobId }}</el-form-item>
                  <el-form-item label="任务名称：">{{ form.jobName }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="任务分组：">{{ jobGroupFormat(form) }}</el-form-item>
                  <el-form-item label="创建时间：">{{ form.createTime }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="cron表达式：">{{ form.cronExpression }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="下次执行时间：">{{ parseTime(form.nextValidTime) }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="调用目标方法：">{{ form.invokeTarget }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="任务状态：">
                     <div v-if="form.status == 0">正常</div>
                     <div v-else-if="form.status == 1">暂停</div>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="是否并发：">
                     <div v-if="form.concurrent == 0">允许</div>
                     <div v-else-if="form.concurrent == 1">禁止</div>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="执行策略：">
                     <div v-if="form.misfirePolicy == 0">默认策略</div>
                     <div v-else-if="form.misfirePolicy == 1">立即执行</div>
                     <div v-else-if="form.misfirePolicy == 2">执行一次</div>
                     <div v-else-if="form.misfirePolicy == 3">放弃执行</div>
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button @click="openView = false">关 闭</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Job">
/*
 * 定时任务管理（菜单：系统监控 → 定时任务）。
 *
 * 接口：@/api/monitor/job，路径前缀 /schedule/**（网关转发到 system-service）。
 * 字典：sys_job_group（任务组）、sys_job_status（'0'=正常 '1'=暂停）。
 * 权限点：monitor:job:add / edit / remove / export / query / changeStatus。
 *
 * 现状提醒：项目尚未接入 Quartz，本页的启停开关与「执行一次」只改数据库并写一条调度日志，
 * 不会真正触发 invokeTarget（详见 api/monitor/job.js 顶部说明）。
 */
import Crontab from '@/components/Crontab'
import { listJob, getJob, delJob, addJob, updateJob, runJob, changeJobStatus } from "@/api/monitor/job"

const router = useRouter()
const { proxy } = getCurrentInstance()
// 字典由 useDict 统一拉取并缓存，模板里直接用 sys_job_group / sys_job_status
const { sys_job_group, sys_job_status } = proxy.useDict("sys_job_group", "sys_job_status")

// ===== 列表与弹窗状态 =====
const jobList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
// ids 保存选中行的 jobId；single/multiple 是由选中行数推导出来的两个按钮禁用态
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
// 详情弹窗、Cron 生成器弹窗，以及传给生成器的初始表达式
const openView = ref(false)
const openCron = ref(false)
const expression = ref("")

// 表单、查询条件与校验规则集中放在 data 里再 toRefs 展开（RuoYi 模板惯例）
const data = reactive({
  form: {},
  // 分页参数由 <pagination> 的 v-model 写回；jobName/jobGroup/status 为查询条件
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    jobName: undefined,
    jobGroup: undefined,
    status: undefined
  },
  // 三个必填项与后端校验保持一致，前端先拦一次避免无效请求
  rules: {
    jobName: [{ required: true, message: "任务名称不能为空", trigger: "blur" }],
    invokeTarget: [{ required: true, message: "调用目标字符串不能为空", trigger: "blur" }],
    cronExpression: [{ required: true, message: "cron执行表达式不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询定时任务列表 */
// response 为 TableDataInfo：rows 是当前页数据，total 是总条数
function getList() {
  loading.value = true
  listJob(queryParams.value).then(response => {
    jobList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 任务组名字典翻译 */
// selectDictLabel 找不到对应字典项时原样返回 value，避免表格出现空白
function jobGroupFormat(row, column) {
  return proxy.selectDictLabel(sys_job_group.value, row.jobGroup)
}

/** 取消按钮 */
// 关弹窗并复位表单，防止下次打开时残留上一次的数据和校验红字
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
// 默认值：执行策略=1（立即执行）、并发=1（禁止）、状态 '0'（正常）；status 会随表单一起提交
function reset() {
  form.value = {
    jobId: undefined,
    jobName: undefined,
    jobGroup: undefined,
    invokeTarget: undefined,
    cronExpression: undefined,
    misfirePolicy: 1,
    concurrent: 1,
    status: "0"
  }
  // resetFields 会把各字段恢复到表单首次渲染时的值，并清掉校验提示
  proxy.resetForm("jobRef")
}

/** 搜索按钮操作 */
// 搜索一律回到第 1 页，避免停留在末页时结果为空
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
// 先清空输入与校验，再走 handleQuery（页码统一由它重置）
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
// 恰好 1 行才允许「修改」（single=false），0 行时禁用「删除」（multiple=true）
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.jobId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

// 更多操作触发
// 目前模板未使用（页面用的是各按钮的独立事件），保留自 RuoYi 原版的下拉菜单分发
function handleCommand(command, row) {
  switch (command) {
    case "handleRun":
      handleRun(row)
      break
    case "handleView":
      handleView(row)
      break
    case "handleJobLog":
      handleJobLog(row)
      break
    default:
      break
  }
}

// 任务状态修改
// el-switch 的 v-model 已经先改掉 row.status，所以取消或接口失败时必须把值翻回去，
// 否则界面显示与库里不一致（catch 分支做的就是回滚）
function handleStatusChange(row) {
  // 以新状态决定提示文案：'0' 正常 → 启用，'1' 暂停 → 停用
  let text = row.status === "0" ? "启用" : "停用"
  proxy.$modal.confirm('确认要"' + text + '""' + row.jobName + '"任务吗?').then(function () {
    return changeJobStatus(row.jobId, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功")
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0"
  })
}

/* 立即执行一次 */
// 只触发一次执行，不改变 cron 的下次执行时间；后端当前仅写一条成功日志，不会真正调用 invokeTarget
function handleRun(row) {
  proxy.$modal.confirm('确认要立即执行一次"' + row.jobName + '"任务吗?').then(function () {
    return runJob(row.jobId, row.jobGroup)
  }).then(() => {
    proxy.$modal.msgSuccess("执行成功")})
  .catch(() => {})
}

/** 任务详细信息 */
// 按 id 重新查一次取最新数据，避免详情里显示列表加载时的旧值
function handleView(row) {
  getJob(row.jobId).then(response => {
    form.value = response.data
    openView.value = true
  })
}

/** cron表达式按钮操作 */
// 把当前表达式带入生成器，便于在其基础上微调
function handleShowCron() {
  expression.value = form.value.cronExpression
  openCron.value = true
}

/** 确定后回传值 */
// 生成器点确定只回填表达式，关闭弹窗由子组件的 @hide 负责
function crontabFill(value) {
  form.value.cronExpression = value
}

/** 任务日志列表查询 */
// row 可能为空（工具栏的「日志」按钮）：此时 jobId 传 0，表示查看全部任务的日志
function handleJobLog(row) {
  const jobId = row.jobId || 0
  router.push('/monitor/job-log/index/' + jobId)
}

/** 新增按钮操作 */
// 新增前先 reset，避免复用上一次编辑残留在 form 里的数据
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加任务"
}

/** 修改按钮操作 */
// 工具栏进入时 row 为空，jobId 退化为选中行的 ids（single 保证只有 1 行被选中）
function handleUpdate(row) {
  reset()
  const jobId = row.jobId || ids.value
  getJob(jobId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改任务"
  })
}

/** 提交按钮 */
// 有 jobId 走更新，否则新增；成功后关闭弹窗并刷新列表（停留在当前页）
function submitForm() {
  proxy.$refs["jobRef"].validate(valid => {
    if (valid) {
      if (form.value.jobId != undefined) {
        updateJob(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addJob(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
// 支持单行删除（row.jobId）与批量删除（选中 ids 用逗号拼接后由后端按 Long[] 接收）
function handleDelete(row) {
  const jobIds = row.jobId || ids.value
  proxy.$modal.confirm('是否确认删除定时任务编号为"' + jobIds + '"的数据项?').then(function () {
    return delJob(jobIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
// 导出走 proxy.download：GET schedule/job/export 直接下载 xlsx；文件名带时间戳避免覆盖
function handleExport() {
  proxy.download("schedule/job/export", {
    ...queryParams.value,
  }, `job_${new Date().getTime()}.xlsx`)
}

// 页面初始化即加载列表（script setup 顶层直接调用，无需 onMounted）
getList()
</script>
