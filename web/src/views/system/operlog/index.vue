<template>
   <div class="app-container">
      <!-- 搜索区：操作地址 / 系统模块 / 操作人员均为 LIKE 模糊匹配，操作类型与操作状态按字典精确匹配 -->
      <!-- 时间范围经 addDateRange 变成 params[beginTime] / params[endTime]，后端按 oper_time 做闭区间比较 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="操作地址" prop="operIp">
            <el-input
               v-model="queryParams.operIp"
               placeholder="请输入操作地址"
               clearable
               style="width: 240px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="系统模块" prop="title">
            <el-input
               v-model="queryParams.title"
               placeholder="请输入系统模块"
               clearable
               style="width: 240px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="操作人员" prop="operName">
            <el-input
               v-model="queryParams.operName"
               placeholder="请输入操作人员"
               clearable
               style="width: 240px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="类型" prop="businessType">
            <el-select
               v-model="queryParams.businessType"
               placeholder="操作类型"
               clearable
               style="width: 240px"
            >
               <el-option
                  v-for="dict in sys_oper_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="操作状态"
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
         <el-form-item label="操作时间" style="width: 308px">
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

      <!-- 工具栏：删除与清空共用 system:operlog:remove 权限；本页没有需要单选的按钮，只用 multiple 控制删除是否可用 -->
      <!-- 日志表增长很快，实际排查问题建议始终带上时间范围，避免只按固定 ORDER BY 全表翻页 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:operlog:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               @click="handleClean"
               v-hasPermi="['system:operlog:remove']"
            >清空</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:operlog:export']"
            >导出</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 表格：类型 / 状态用 dict-tag 渲染，消耗时间按毫秒展示；三列开了服务端排序，默认按操作时间倒序 -->
      <!-- 排序参数会随请求发出，但后端 PageUtils 只消费 pageNum / pageSize，实际顺序固定为 oper_id 倒序 -->
      <el-table ref="operlogRef" v-loading="loading" :data="operlogList" @selection-change="handleSelectionChange" :default-sort="defaultSort" @sort-change="handleSortChange">
         <el-table-column type="selection" width="50" align="center" />
         <el-table-column label="日志编号" align="center" prop="operId" />
         <el-table-column label="系统模块" align="center" prop="title" :show-overflow-tooltip="true" />
         <el-table-column label="操作类型" align="center" prop="businessType">
            <template #default="scope">
               <dict-tag :options="sys_oper_type" :value="scope.row.businessType" />
            </template>
         </el-table-column>
         <el-table-column label="请求方式" align="center" prop="requestMethod" />
         <el-table-column label="操作人员" align="center" prop="operName" width="110" :show-overflow-tooltip="true" sortable="custom" :sort-orders="['descending', 'ascending']" />
         <el-table-column label="操作地址" align="center" prop="operIp" width="130" :show-overflow-tooltip="true" />
         <el-table-column label="操作状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="sys_common_status" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="操作日期" align="center" prop="operTime" width="180" sortable="custom" :sort-orders="['descending', 'ascending']">
            <template #default="scope">
               <span>{{ parseTime(scope.row.operTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="消耗时间" align="center" prop="costTime" width="110" :show-overflow-tooltip="true" sortable="custom" :sort-orders="['descending', 'ascending']">
            <template #default="scope">
               <span>{{ scope.row.costTime }}毫秒</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="View" @click="handleView(scope.row, scope.index)" v-hasPermi="['system:operlog:query']">详细</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：翻页或改每页条数都会重新 getList，查询条件与时间范围自动带上 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 操作日志详细 -->
      <!-- 只读详情：数据直接取表格行对象（同一引用，未做拷贝），弹窗里没有提交入口 -->
      <!-- 详情中的操作状态用 === 0 / === 1 严格比较，后端该字段是 Integer，若改成字符串两个分支都会不命中 -->
      <!-- 请求参数 / 返回参数是后端落库的字符串（通常是 JSON），可能很长，这里只做文本渲染，不做格式化 -->
      <el-dialog title="操作日志详细" v-model="open" width="800px" append-to-body>
         <el-form :model="form" label-width="100px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="操作模块：">{{ form.title }} / {{ typeFormat(form) }}</el-form-item>
                  <el-form-item
                    label="登录信息："
                  >{{ form.operName }} / {{ form.operIp }} </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="请求地址：">{{ form.operUrl }}</el-form-item>
                  <el-form-item label="请求方式：">{{ form.requestMethod }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="操作方法：">{{ form.method }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="请求参数：">{{ form.operParam }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="返回参数：">{{ form.jsonResult }}</el-form-item>
               </el-col>
               <el-col :span="8">
                  <el-form-item label="操作状态：">
                     <div v-if="form.status === 0">正常</div>
                     <div v-else-if="form.status === 1">失败</div>
                  </el-form-item>
               </el-col>
               <el-col :span="8">
                  <el-form-item label="消耗时间：">{{ form.costTime }}毫秒</el-form-item>
               </el-col>
               <el-col :span="8">
                  <el-form-item label="操作时间：">{{ parseTime(form.operTime) }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="异常信息：" v-if="form.status === 1">{{ form.errorMsg }}</el-form-item>
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

<script setup name="Operlog">
import { list, delOperlog, cleanOperlog } from "@/api/system/operlog"

// 操作日志列表页：查询 + 删除 / 清空 / 导出 + 只读详情弹窗（没有新增 / 修改入口）。
// 接口：@/api/system/operlog 的 list / delOperlog / cleanOperlog；详情不单独请求，直接复用列表行。
// 权限点：system:operlog:remove（删除与清空共用）、system:operlog:export、system:operlog:query（详情）；
// 只有前端 v-hasPermi 控制显隐，后端接口本身没有权限注解。
// 字典：sys_oper_type（0其它 1新增 2修改 3删除，其余沿用 RuoYi 约定）、sys_common_status（'0'=正常 '1'=异常）。
// 日志表没有软删列：删除与清空都是物理操作，清空等价 DELETE FROM sys_oper_log，均不可回滚。
// 初始化：script setup 顶层末尾直接调用 getList()，进入页面即拉第一页。

const { proxy } = getCurrentInstance()
const { sys_oper_type, sys_common_status } = proxy.useDict("sys_oper_type", "sys_common_status")

const operlogList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
const defaultSort = ref({ prop: "operTime", order: "descending" })

// 查询条件默认每页 10 条（与后端 PageUtils 的默认值一致）；form 是只读详情的数据载体，不是提交表单
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    operIp: undefined,
    title: undefined,
    operName: undefined,
    businessType: undefined,
    status: undefined
  }
})

const { queryParams, form } = toRefs(data)

/** 查询登录日志 */
function getList() {
  loading.value = true
  // 时间范围由 addDateRange 转成 params[beginTime] / params[endTime]；dateRange 为空时两个值不下发
  list(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    operlogList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 操作日志类型字典翻译 */
function typeFormat(row, column) {
  // selectDictLabel 按字典回显，匹配不到时原样返回 businessType，避免详情里出现空白
  return proxy.selectDictLabel(sys_oper_type.value, row.businessType)
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  // dateRange 没有注册成 el-form 的 prop，resetForm 清不掉，必须单独置空
  dateRange.value = []
  proxy.resetForm("queryRef")
  queryParams.value.pageNum = 1
  // 同时把表头排序恢复默认，避免箭头停在用户点过的列上、与实际数据顺序不一致
  proxy.$refs["operlogRef"].sort(defaultSort.value.prop, defaultSort.value.order)
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.operId)
  // 详情是行内按钮，不依赖单选状态；这里只维护「是否有选中」，用于禁用删除按钮
  multiple.value = !selection.length
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  // order 取 ascending / descending，原样透传；本服务端的 PageUtils 不读取 orderByColumn / isAsc
  queryParams.value.orderByColumn = column.prop
  queryParams.value.isAsc = column.order
  getList()
}

/** 详细按钮操作 */
function handleView(row) {
  open.value = true
  // 直接引用表格行对象（同一引用），弹窗只读，因此不必再请求一次详情接口
  form.value = row
}

/** 删除按钮操作 */
function handleDelete(row) {
  // 按 operId 物理删除，删了不可恢复，必须先二次确认；未传 row 时用多选结果（数组拼进 URL 即逗号分隔）
  const operIds = row.operId || ids.value
  proxy.$modal.confirm('是否确认删除日志编号为"' + operIds + '"的数据项?').then(function () {
    return delOperlog(operIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 清空按钮操作 */
function handleClean() {
  // 清空是整表 DELETE FROM sys_oper_log，不可逆，必须二次确认
  proxy.$modal.confirm("是否确认清空所有操作日志数据项?").then(function () {
    return cleanOperlog()
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("清空成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  // 导出走通用 download（POST 表单参数 + blob 响应）；后端 SysOperlogController 没有 /export 映射，此按钮当前会失败
  // 文件名前缀写成 config_ 而不是 operlog_，与操作日志语义不符
  proxy.download("system/operlog/export",{
    ...queryParams.value,
  }, `config_${new Date().getTime()}.xlsx`)
}

getList()
</script>
