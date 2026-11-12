<template>
   <div class="app-container">
      <!-- 检索区：字典类型下拉来自 optionselect（后端只返回 status='0' 的类型），进入页面时默认锁定为路由带过来的 dictType -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="字典名称" prop="dictType">
            <el-select v-model="queryParams.dictType" style="width: 200px">
               <el-option
                  v-for="item in typeOptions"
                  :key="item.dictId"
                  :label="item.dictName"
                  :value="item.dictType"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="字典标签" prop="dictLabel">
            <el-input
               v-model="queryParams.dictLabel"
               placeholder="请输入字典标签"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="数据状态" clearable style="width: 200px">
               <el-option
                  v-for="dict in sys_normal_disable"
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

      <!-- 工具栏：增删改导出按 system:dict:* 权限点控制；「关闭」无权限点，只关闭当前页签回到字典类型页 -->
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
               type="warning"
               plain
               icon="Close"
               @click="handleClose"
            >关闭</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 列表：字典标签列按 listClass/cssClass 决定样式，两者都为空或 default 时才按纯文本渲染，否则套 el-tag -->
      <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="字典编码" align="center" prop="dictCode" />
         <el-table-column label="字典标签" align="center" prop="dictLabel">
            <template #default="scope">
               <span v-if="(scope.row.listClass == '' || scope.row.listClass == 'default') && (scope.row.cssClass == '' || scope.row.cssClass == null)">{{ scope.row.dictLabel }}</span>
               <el-tag v-else :type="scope.row.listClass == 'primary' ? '' : scope.row.listClass" :class="scope.row.cssClass">{{ scope.row.dictLabel }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="字典键值" align="center" prop="dictValue" />
         <el-table-column label="字典排序" align="center" prop="dictSort" />
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

      <!-- 分页：pageNum/pageSize 双向绑定查询条件，翻页触发 getList；total 为 0 时整块隐藏 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改参数配置对话框 -->
      <el-dialog :title="title" v-model="open" width="500px" append-to-body>
         <el-form ref="dataRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="字典类型">
               <el-input v-model="form.dictType" :disabled="true" />
            </el-form-item>
            <el-form-item label="数据标签" prop="dictLabel">
               <el-input v-model="form.dictLabel" placeholder="请输入数据标签" />
            </el-form-item>
            <el-form-item label="数据键值" prop="dictValue">
               <el-input v-model="form.dictValue" placeholder="请输入数据键值" />
            </el-form-item>
            <el-form-item label="样式属性" prop="cssClass">
               <el-input v-model="form.cssClass" placeholder="请输入样式属性" />
            </el-form-item>
            <el-form-item label="显示排序" prop="dictSort">
               <el-input-number v-model="form.dictSort" controls-position="right" :min="0" />
            </el-form-item>
            <el-form-item label="回显样式" prop="listClass">
               <el-select v-model="form.listClass">
                  <el-option
                     v-for="item in listClassOptions"
                     :key="item.value"
                     :label="item.label + '(' + item.value + ')'"
                     :value="item.value"
                  ></el-option>
               </el-select>
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

<script setup name="Data">
/*
 * 字典数据管理页（某个字典类型下的数据项列表 + 新增/编辑弹窗），入口路由 /system/dict-data/index/:dictId。
 * 接口前缀 /system/dict/data：listData 分页查询、getData 详情、addData/updateData 写入、delData 批量删除；
 * 类型下拉与入口类型分别走 /system/dict/type/optionselect 和 getType。
 * 权限点：system:dict:add / edit / remove / export（与字典类型页共用）。
 * 缓存一致性：写操作成功后必须用 useDictStore().removeDict(dictType) 让该类型的 Pinia 缓存失效，
 * 否则已加载过该字典的页面会继续显示旧数据。
 */
import useDictStore from '@/store/modules/dict'
import { optionselect as getDictOptionselect, getType } from "@/api/system/dict/type"
import { listData, getData, delData, addData, updateData } from "@/api/system/dict/data"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

// ===== 列表、弹窗与类型下拉状态 =====
// single / multiple 是「反义」禁用标志：single=true 表示未恰好选中 1 行，multiple=true 表示一行都没选。
const dataList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const defaultDictType = ref("")
const typeOptions = ref([])
const route = useRoute()
// 下面 value 直接作为 el-tag 的 type 使用，default 表示不套标签、按纯文本渲染；label 只用于下拉文案。
// 数据标签回显样式
const listClassOptions = ref([
  { value: "default", label: "默认" }, 
  { value: "primary", label: "主要" }, 
  { value: "success", label: "成功" },
  { value: "info", label: "信息" },
  { value: "warning", label: "警告" },
  { value: "danger", label: "危险" }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictType: undefined,
    dictLabel: undefined,
    status: undefined
  },
  rules: {
    dictLabel: [{ required: true, message: "数据标签不能为空", trigger: "blur" }],
    dictValue: [{ required: true, message: "数据键值不能为空", trigger: "blur" }],
    dictSort: [{ required: true, message: "数据顺序不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 用路由里的 dictId 反查 dictType，拿到后再串行拉列表；defaultDictType 记住入口类型，供「重置」恢复。
/** 查询字典类型详细 */
function getTypes(dictId) {
  getType(dictId).then(response => {
    queryParams.value.dictType = response.data.dictType
    defaultDictType.value = response.data.dictType
    getList()
  })
}

// 类型下拉只用于切换查看范围；optionselect 只返回启用中的类型，切换后不会自动查询，需再点「搜索」。
/** 查询字典类型列表 */
function getTypeList() {
  getDictOptionselect().then(response => {
    typeOptions.value = response.data
  })
}

// 直接透传 queryParams（本页没有日期区间筛选），列表返回 { rows, total }。
/** 查询字典数据列表 */
function getList() {
  loading.value = true
  listData(queryParams.value).then(response => {
    dataList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

// 清空表单并复位校验状态；dictCode 置空用于判定「新增」，listClass/status/dictSort 的初值与后端默认值对齐。
/** 表单重置 */
function reset() {
  form.value = {
    dictCode: undefined,
    dictLabel: undefined,
    dictValue: undefined,
    cssClass: undefined,
    listClass: "default",
    dictSort: 0,
    status: "0",
    remark: undefined
  }
  proxy.resetForm("dataRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

// 关闭当前页签回到字典类型列表，用页签管理而非路由跳转，避免详情页残留在多页签里。
/** 返回按钮操作 */
function handleClose() {
  const obj = { path: "/system/dict" }
  proxy.$tab.closeOpenPage(obj)
}

// 重置后把 dictType 拉回入口类型，否则会「重置」成不带类型条件、看到全部字典数据而丢失上下文。
/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.dictType = defaultDictType.value
  handleQuery()
}

// 弹窗里的字典类型是只读的，必须由当前查询条件回填，否则新增的数据不知道挂在哪个类型下。
/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加字典数据"
  form.value.dictType = queryParams.value.dictType
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.dictCode)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

// 工具栏触发时 row 是点击事件，回退到勾选的 ids（数组，序列化后是单个 dictCode）。
/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const dictCode = row.dictCode || ids.value
  getData(dictCode).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改字典数据"
  })
}

// 以 dictCode 区分新增/修改；写入成功后清掉当前类型的 Pinia 字典缓存，下次 useDict 会重新拉取。
/** 提交按钮 */
function submitForm() {
  proxy.$refs["dataRef"].validate(valid => {
    if (valid) {
      if (form.value.dictCode != undefined) {
        updateData(form.value).then(response => {
          useDictStore().removeDict(queryParams.value.dictType)
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addData(form.value).then(response => {
          useDictStore().removeDict(queryParams.value.dictType)
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

// 物理删除且无回收站，先二次确认；批量时编号逗号拼接给后端，删完同样要清该类型的字典缓存。
/** 删除按钮操作 */
function handleDelete(row) {
  const dictCodes = row.dictCode || ids.value
  proxy.$modal.confirm('是否确认删除字典编码为"' + dictCodes + '"的数据项？').then(function() {
    return delData(dictCodes)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
    useDictStore().removeDict(queryParams.value.dictType)
  }).catch(() => {})
}

// 导出当前筛选条件下的字典数据（含分页参数），文件名带时间戳避免与上次下载重名。
/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/dict/data/export", {
    ...queryParams.value
  }, `dict_data_${new Date().getTime()}.xlsx`)
}

// 首屏：getTypes 拿到 dictType 后会自行触发 getList，因此这里不直接调 getList；类型下拉并行加载。
getTypes(route.params && route.params.dictId)
getTypeList()
</script>
