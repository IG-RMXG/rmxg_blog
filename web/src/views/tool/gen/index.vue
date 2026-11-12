<template>
  <!-- 代码生成列表页：展示已导入 gen_table 的表配置，提供预览、编辑、删除、同步表结构、生成代码等入口 -->
  <!-- 权限点：tool:gen:code 生成、tool:gen:import 导入、tool:gen:edit 修改/同步、tool:gen:remove 删除、tool:gen:preview 预览 -->
  <div class="app-container">
    <!-- 搜索区：表名称/表描述为模糊匹配；创建时间用 dateRange 承载，交给 addDateRange 转成 beginTime/endTime 参数 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="表名称" prop="tableName">
        <el-input
          v-model="queryParams.tableName"
          placeholder="请输入表名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="表描述" prop="tableComment">
        <el-input
          v-model="queryParams.tableComment"
          placeholder="请输入表描述"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
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

    <!-- 工具栏：批量按钮的可用状态由 single/multiple 控制——修改要求恰好选中一行，删除与生成至少选中一行 -->
    <!-- 各按钮再叠一层 v-hasPermi 控制显隐；后端接口同样做权限校验，前端只是入口收敛 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Download"
          @click="handleGenTable"
          v-hasPermi="['tool:gen:code']"
        >生成</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Upload"
          @click="openImportTable"
          v-hasPermi="['tool:gen:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleEditTable"
          v-hasPermi="['tool:gen:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['tool:gen:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格：创建/更新时间用 sortable="custom" 走服务端排序，sort-orders 限定只有降序、升序两态（无「无序」） -->
    <el-table ref="genRef" v-loading="loading" :data="tableList" @selection-change="handleSelectionChange" :default-sort="defaultSort" @sort-change="handleSortChange">
      <el-table-column type="selection" align="center" width="55"></el-table-column>
      <!-- 序号按「(当前页 - 1) * 每页条数 + 行下标 + 1」计算，保证翻页后连续，而不是每页从 1 重排 -->
      <el-table-column label="序号" type="index" width="50" align="center">
        <template #default="scope">
          <span>{{(queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1}}</span>
        </template>
      </el-table-column>
      <el-table-column label="表名称" align="center" prop="tableName" :show-overflow-tooltip="true" />
      <el-table-column label="表描述" align="center" prop="tableComment" :show-overflow-tooltip="true" />
      <el-table-column label="实体" align="center" prop="className" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <!-- 操作列：每行按钮与工具栏同源，区别是行内按钮直接把 scope.row 传给处理函数，无需依赖多选结果 -->
      <el-table-column label="操作" align="center" width="330" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="预览" placement="top">
            <el-button link type="primary" icon="View" @click="handlePreview(scope.row)" v-hasPermi="['tool:gen:preview']"></el-button>
          </el-tooltip>
          <el-tooltip content="编辑" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleEditTable(scope.row)" v-hasPermi="['tool:gen:edit']"></el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['tool:gen:remove']"></el-button>
          </el-tooltip>
          <el-tooltip content="同步" placement="top">
            <el-button link type="primary" icon="Refresh" @click="handleSynchDb(scope.row)" v-hasPermi="['tool:gen:edit']"></el-button>
          </el-tooltip>
          <el-tooltip content="生成代码" placement="top">
            <el-button link type="primary" icon="Download" @click="handleGenTable(scope.row)" v-hasPermi="['tool:gen:code']"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页：pageNum/pageSize 双向绑定到 queryParams，翻页事件触发重新请求；无数据时隐藏分页条 -->
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
    <!-- 预览界面：返回的是一组「虚拟文件路径 -> 代码文本」，形如 vm/java/domain.java.vm；
        每个键一个页签，label 取路径最后一段并截掉 .vm 后缀（如 domain.java），内容只读展示并可一键复制 -->
    <!-- 预览界面 -->
    <el-dialog :title="preview.title" v-model="preview.open" width="80%" top="5vh" append-to-body class="scrollbar">
      <el-tabs v-model="preview.activeName">
        <el-tab-pane
          v-for="(value, key) in preview.data"
          :label="key.substring(key.lastIndexOf('/')+1,key.indexOf('.vm'))"
          :name="key.substring(key.lastIndexOf('/')+1,key.indexOf('.vm'))"
          :key="value"
        >
          <el-link :underline="false" icon="DocumentCopy" v-copyText="value" v-copyText:callback="copyTextSuccess" style="float:right">&nbsp;复制</el-link>
          <pre>{{ value }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
    <!-- 导入表弹窗：子组件通过 defineExpose 暴露 show()，导入成功后 emit("ok")，此处直接复用查询逻辑回到列表重新拉取 -->
    <import-table ref="importRef" @ok="handleQuery" />
  </div>
</template>

<script setup name="Gen">
// 生成相关的全部后端接口：listTable 分页查询、previewTable 取模板渲染内容、delTable 删除配置、
// genCode 生成到自定义路径（服务端直接写盘）、synchDb 用数据库最新结构覆盖字段配置
import { listTable, previewTable, delTable, genCode, synchDb } from "@/api/tool/gen"
import router from "@/router"
import importTable from "./importTable"

const route = useRoute()
const { proxy } = getCurrentInstance()

// ===== 列表与选择状态 =====
const tableList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const tableNames = ref([])
const dateRange = ref([])
const uniqueId = ref("")
const defaultSort = ref({ prop: "createTime", order: "descending" })

// ===== 查询条件与预览配置 =====
// pageNum / pageSize 与后端 PageDomain 约定同名；orderByColumn + isAsc 为服务端排序约定，
// 默认按创建时间倒序，与表格的 :default-sort 保持同一份取值（defaultSort）
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    tableName: undefined,
    tableComment: undefined,
    orderByColumn: defaultSort.value.prop,
    isAsc: defaultSort.value.order
  },
  preview: {
    open: false,
    title: "代码预览",
    data: {},
    activeName: "domain.java"
  }
})

const { queryParams, preview } = toRefs(data)

// 列表页被 keep-alive 缓存：从编辑页返回时路由会带上新的 t（时间戳）与 pageNum。
// 只有时间戳与上次记录不同才重新查询，避免同一页签反复激活时重复请求；
// 刷新时把页码恢复成来源页并清空日期区间。
// 注意此处 resetForm 传的是 "queryForm"，而上方表单的 ref 是 queryRef，因此该调用不会真正复位输入框
onActivated(() => {
  const time = route.query.t
  if (time != null && time != uniqueId.value) {
    uniqueId.value = time
    queryParams.value.pageNum = Number(route.query.pageNum)
    dateRange.value = []
    proxy.resetForm("queryForm")
    getList()
  }
})

/** 查询表集合 */
// addDateRange 会把 dateRange 数组转成 params[beginTime] / params[endTime]，后端按创建时间区间过滤；
// 返回值约定为 { rows, total }，total 供分页组件使用
function getList() {
  loading.value = true
  listTable(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    tableList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 生成代码操作 */
// 行内按钮传 row，工具栏按钮不传参时取多选结果 tableNames（逗号拼接后在下方拼进下载地址）。
// genType === "1" 表示该表配置为「自定义路径」：由服务端 genCode 直接写盘，所以没有文件下载；
// 其余情况统一请求后端批量生成接口，以浏览器下载方式拿到 zip
// 注意：genCode 对应的后端能力目前未实现（缺少模板文件），走「自定义路径」分支会返回 501，页面表现为接口异常提示
function handleGenTable(row) {
  const tbNames = row.tableName || tableNames.value
  if (tbNames == "") {
    proxy.$modal.msgError("请选择要生成的数据")
    return
  }
  if (row.genType === "1") {
    genCode(row.tableName).then(response => {
      proxy.$modal.msgSuccess("成功生成到自定义路径：" + row.genPath)
    })
  } else {
    proxy.$download.zip("/code/gen/batchGenCode?tables=" + tbNames, "ruoyi.zip")
  }
}

/** 同步数据库操作 */
// 需要二次确认的原因：同步会用数据库当前表结构覆盖已手工调整过的字段配置（描述、查询方式、显示类型等），不可撤销
function handleSynchDb(row) {
  const tableName = row.tableName
  proxy.$modal.confirm('确认要强制同步"' + tableName + '"表结构吗？').then(function () {
    return synchDb(tableName)
  }).then(() => {
    proxy.$modal.msgSuccess("同步成功")
  }).catch(() => {})
}

/** 打开导入表弹窗 */
function openImportTable() {
  proxy.$refs["importRef"].show()
}

/** 重置按钮操作 */
// 重置查询表单与日期区间，并把表格排序恢复成默认的创建时间倒序；
// 调用 el-table 的 sort 会连带触发 sort-change，因此排序恢复本身就会重新发起查询
function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  queryParams.value.pageNum = 1
  proxy.$refs["genRef"].sort(defaultSort.value.prop, defaultSort.value.order)
}

/** 预览按钮 */
// 按 tableId 实时拼装代码文本（不落盘、不依赖模板文件），拿到后打开弹窗并固定选中 domain.java 页签
function handlePreview(row) {
  previewTable(row.tableId).then(response => {
    preview.value.data = response.data
    preview.value.open = true
    preview.value.activeName = "domain.java"
  })
}

/** 复制代码成功 */
function copyTextSuccess() {
  proxy.$modal.msgSuccess("复制成功")
}

// 多选框选中数据
// single / multiple 是「取反」语义：single=true 表示未选中恰好一条、multiple=true 表示一条都没选，
// 直接绑到按钮的 :disabled 上；同时缓存 id 与表名两个数组，供批量操作和下载地址拼接使用
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.tableId)
  tableNames.value = selection.map(item => item.tableName)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 排序触发事件 */
// 只改排序字段与方向、不重置页码，保证在深翻页时切换排序仍停留在当前页；
// column.order 取 ascending / descending，与后端 isAsc 约定一致
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop
  queryParams.value.isAsc = column.order
  getList()
}

/** 修改按钮操作 */
// 行内按钮传 row；工具栏按钮不传参，此时取多选结果的第 0 项（single 已保证只选中一条）。
// 用多页签方式打开配置页并把当前页码带过去，返回列表时用于定位回原来那一页
function handleEditTable(row) {
  const tableId = row.tableId || ids.value[0]
  const tableName = row.tableName || tableNames.value[0]
  const params = { pageNum: queryParams.value.pageNum }
  proxy.$tab.openPage("修改[" + tableName + "]生成配置", '/tool/gen-edit/index/' + tableId, params)
}

/** 删除按钮操作 */
// tableIds 可能是单个 id 也可能是 id 数组，后端按路径变量 Long[] 接收，数组会被拼成 "1,2" 形式；
// 二次确认的文案里直接拼接该值，删除只清生成配置，不会 DROP 数据库表
function handleDelete(row) {
  const tableIds = row.tableId || ids.value
  proxy.$modal.confirm('是否确认删除表编号为"' + tableIds + '"的数据项？').then(function () {
    return delTable(tableIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>
