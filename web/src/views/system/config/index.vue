<template>
   <div class="app-container">
      <!-- 检索区：参数名称/键名模糊匹配，系统内置取自 sys_yes_no（Y=内置），创建时间区间由 addDateRange 折成 beginTime/endTime -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="参数名称" prop="configName">
            <el-input
               v-model="queryParams.configName"
               placeholder="请输入参数名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="参数键名" prop="configKey">
            <el-input
               v-model="queryParams.configKey"
               placeholder="请输入参数键名"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="系统内置" prop="configType">
            <el-select v-model="queryParams.configType" placeholder="系统内置" clearable style="width: 240px">
               <el-option
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="创建时间" style="width: 308px;">
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

      <!-- 工具栏：新增/改删/导出按 system:config:* 权限点控制，「刷新缓存」复用 system:config:remove -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:config:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['system:config:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:config:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:config:export']"
            >导出</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Refresh"
               @click="handleRefreshCache"
               v-hasPermi="['system:config:remove']"
            >刷新缓存</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 列表：后端返回 { rows, total }，参数键名/键值等长文本列用 show-overflow-tooltip 悬浮查看 -->
      <el-table v-loading="loading" :data="configList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="参数主键" align="center" prop="configId" />
         <el-table-column label="参数名称" align="center" prop="configName" :show-overflow-tooltip="true" />
         <el-table-column label="参数键名" align="center" prop="configKey" :show-overflow-tooltip="true" />
         <el-table-column label="参数键值" align="center" prop="configValue" :show-overflow-tooltip="true" />
         <el-table-column label="系统内置" align="center" prop="configType">
            <template #default="scope">
               <dict-tag :options="sys_yes_no" :value="scope.row.configType" />
            </template>
         </el-table-column>
         <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:config:edit']" >修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:config:remove']">删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：pageNum/pageSize 双向绑定查询条件，total 为 0 时整块隐藏 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改参数配置对话框 -->
      <el-dialog :title="title" v-model="open" width="500px" append-to-body>
         <el-form ref="configRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="参数名称" prop="configName">
               <el-input v-model="form.configName" placeholder="请输入参数名称" />
            </el-form-item>
            <el-form-item label="参数键名" prop="configKey">
               <el-input v-model="form.configKey" placeholder="请输入参数键名" />
            </el-form-item>
            <el-form-item label="参数键值" prop="configValue">
               <el-input v-model="form.configValue" type="textarea" placeholder="请输入参数键值" />
            </el-form-item>
            <el-form-item label="系统内置" prop="configType">
               <el-radio-group v-model="form.configType">
                  <el-radio
                     v-for="dict in sys_yes_no"
                     :key="dict.value"
                     :value="dict.value"
                  >{{ dict.label }}</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Config">
/*
 * 系统参数配置页（列表 + 新增/编辑弹窗），读写 sys_config。
 * 接口前缀 /system/config：listConfig 分页查询、getConfig 详情、addConfig/updateConfig 写入、
 * delConfig 批量删除、refreshCache 刷新缓存。
 * 权限点：system:config:add / edit / remove / export。
 * 关键规则：configKey 全局唯一（重名由后端报错）；内置参数（configType='Y' 或 configId<100）
 * 后端一律拒删，前端不做额外拦截，失败提示由请求拦截器统一弹出。
 */
import { listConfig, getConfig, delConfig, addConfig, updateConfig, refreshCache } from "@/api/system/config"

const { proxy } = getCurrentInstance()
const { sys_yes_no } = proxy.useDict("sys_yes_no")

// ===== 列表与弹窗状态 =====
// single / multiple 是「反义」禁用标志：single=true 表示未恰好选中 1 行（禁用工具栏修改），
// multiple=true 表示一行都没选（禁用删除）。
const configList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])

// 查询条件 / 表单 / 校验规则合放在一个 reactive 里；分页按后端约定传 pageNum/pageSize，列表返回 { rows, total }。
// rules 只做非空校验，configKey 唯一性由后端校验并以「参数键值已存在」报错。
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    configName: undefined,
    configKey: undefined,
    configType: undefined
  },
  rules: {
    configName: [{ required: true, message: "参数名称不能为空", trigger: "blur" }],
    configKey: [{ required: true, message: "参数键名不能为空", trigger: "blur" }],
    configValue: [{ required: true, message: "参数键值不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 列表、增删改、刷新缓存后都走这里重新取数；查询条件与创建时间区间经 addDateRange 合并。
/** 查询参数列表 */
function getList() {
  loading.value = true
  listConfig(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    configList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

// 清空表单并复位校验状态；configId 置空用于判定「新增」，configType 预置 'Y' 表示弹窗默认按内置参数创建。
/** 表单重置 */
function reset() {
  form.value = {
    configId: undefined,
    configName: undefined,
    configKey: undefined,
    configValue: undefined,
    configType: "Y",
    remark: undefined
  }
  proxy.resetForm("configRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.configId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加参数"
}

// 行内触发用 row.configId；工具栏触发时 row 是点击事件，回退到勾选的 ids（数组，序列化后是单个编号）。
/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const configId = row.configId || ids.value
  getConfig(configId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改参数"
  })
}

// 以 configId 是否存在区分新增/修改；后端没有参数缓存，保存后立即生效，不必再点「刷新缓存」。
/** 提交按钮 */
function submitForm() {
  proxy.$refs["configRef"].validate(valid => {
    if (valid) {
      if (form.value.configId != undefined) {
        updateConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

// 二次确认后批量删除（编号逗号拼接）；命中任一内置参数时后端整批拒绝并回传该参数名。
/** 删除按钮操作 */
function handleDelete(row) {
  const configIds = row.configId || ids.value
  proxy.$modal.confirm('是否确认删除参数编号为"' + configIds + '"的数据项？').then(function () {
    return delConfig(configIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

// 沿用当前查询条件（含分页参数）导出，文件名带时间戳避免覆盖。
/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/config/export", {
    ...queryParams.value
  }, `config_${new Date().getTime()}.xlsx`)
}

// 后端为兼容保留的空实现（参数每次直接查库），成功提示只代表请求到达，不代表真的清过缓存。
/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess("刷新缓存成功")
  })
}

getList()
</script>
