<template>
   <div class="app-container">
      <!-- 检索区：名称/类型为模糊匹配，状态取自 sys_normal_disable，创建时间区间由 addDateRange 折成 beginTime/endTime -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="字典名称" prop="dictName">
            <el-input
               v-model="queryParams.dictName"
               placeholder="请输入字典名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="字典类型" prop="dictType">
            <el-input
               v-model="queryParams.dictType"
               placeholder="请输入字典类型"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="字典状态"
               clearable
               style="width: 240px"
            >
               <el-option
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="创建时间" style="width: 308px">
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

      <!-- 工具栏：按钮可见性由 v-hasPermi 控制；「刷新缓存」复用 system:dict:remove 权限点 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:dict:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['system:dict:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:dict:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:dict:export']"
            >导出</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Refresh"
               @click="handleRefreshCache"
               v-hasPermi="['system:dict:remove']"
            >刷新缓存</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 列表：「字典类型」列渲染成路由链接，跳到该类型下的字典数据页（/system/dict-data/index/:dictId） -->
      <el-table v-loading="loading" :data="typeList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="字典编号" align="center" prop="dictId" />
         <el-table-column label="字典名称" align="center" prop="dictName" :show-overflow-tooltip="true"/>
         <el-table-column label="字典类型" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
               <router-link :to="'/system/dict-data/index/' + scope.row.dictId" class="link-type">
                  <span>{{ scope.row.dictType }}</span>
               </router-link>
            </template>
         </el-table-column>
         <el-table-column label="状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dict:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dict:remove']">删除</el-button>
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
         <el-form ref="dictRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="字典名称" prop="dictName">
               <el-input v-model="form.dictName" placeholder="请输入字典名称" />
            </el-form-item>
            <el-form-item label="字典类型" prop="dictType">
               <el-input v-model="form.dictType" placeholder="请输入字典类型" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
               <el-radio-group v-model="form.status">
                  <el-radio
                     v-for="dict in sys_normal_disable"
                     :key="dict.value"
                     :value="dict.value"
                  >{{ dict.label }}</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
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

<script setup name="Dict">
/*
 * 字典类型管理页（列表 + 新增/编辑弹窗），读写 sys_dict_type。
 * 接口前缀 /system/dict/type：listType 分页查询、getType 详情、addType/updateType 写入、
 * delType 批量删除、refreshCache 刷新缓存。
 * 权限点：system:dict:add / system:dict:edit / system:dict:remove / system:dict:export。
 * 字段约定：status 取自 sys_normal_disable（'0'=正常 '1'=停用），新增默认 '0'；
 * dictType 全局唯一，修改它时后端会在同一事务里同步改写 sys_dict_data 的冗余 dict_type。
 */
import useDictStore from '@/store/modules/dict'
import { listType, getType, delType, addType, updateType, refreshCache } from "@/api/system/dict/type"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

// ===== 列表与弹窗状态 =====
// single / multiple 是「反义」禁用标志：single=true 表示未恰好选中 1 行（禁用工具栏修改），
// multiple=true 表示一行都没选（禁用删除）。
const typeList = ref([])
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
// rules 只校验必填，dictType 唯一性由后端校验并以「字典类型已存在」报错。
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  },
  rules: {
    dictName: [{ required: true, message: "字典名称不能为空", trigger: "blur" }],
    dictType: [{ required: true, message: "字典类型不能为空", trigger: "blur" }]
  },
})

const { queryParams, form, rules } = toRefs(data)

// 列表刷新统一入口：查询条件与创建时间区间经 proxy.addDateRange 合并后发出。
/** 查询字典类型列表 */
function getList() {
  loading.value = true
  listType(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    typeList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

// 清空 form 并复位校验状态；dictId 置空是「新增」的判定依据，status 预置 '0' 与后端默认值一致。
/** 表单重置 */
function reset() {
  form.value = {
    dictId: undefined,
    dictName: undefined,
    dictType: undefined,
    status: "0",
    remark: undefined
  }
  proxy.resetForm("dictRef")
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

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加字典类型"
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.dictId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

// row 只在行内按钮触发时存在；工具栏按钮传入的是点击事件，此时回退到勾选的 ids（数组，序列化后是单个编号）。
/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const dictId = row.dictId || ids.value
  getType(dictId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改字典类型"
  })
}

// 以 dictId 是否存在区分新增/修改；成功后不主动清前端字典缓存，需点工具栏「刷新缓存」才能看到新值。
/** 提交按钮 */
function submitForm() {
  proxy.$refs["dictRef"].validate(valid => {
    if (valid) {
      if (form.value.dictId != undefined) {
        updateType(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addType(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

// 行内/批量共用：未传 row 时删除勾选的全部编号；先二次确认，后端在该类型下仍有字典数据时整批拒绝。
/** 删除按钮操作 */
function handleDelete(row) {
  const dictIds = row.dictId || ids.value
  proxy.$modal.confirm('是否确认删除字典编号为"' + dictIds + '"的数据项？').then(function() {
    return delType(dictIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

// 走 proxy.download 下载文件流，沿用当前查询条件（含分页参数），文件名带时间戳避免浏览器覆盖。
/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/dict/type/export", {
    ...queryParams.value
  }, `dict_${new Date().getTime()}.xlsx`)
}

// 后端接口是空实现，真正生效的是随后的 useDictStore().cleanDict()（清空 Pinia 里的字典内存缓存）；
// 只调接口不清缓存时，页面上已渲染的字典标签不会变化。
/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess("刷新成功")
    useDictStore().cleanDict()
  })
}

getList()
</script>
