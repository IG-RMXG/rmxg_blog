<template>
  <!-- 导入表弹窗：从当前数据库中选择尚未导入的物理表，勾选后批量写入生成配置（表 + 字段） -->
  <!-- 已导入过的表会被后端跳过；全部被跳过时接口返回失败提示 -->
  <!-- 导入表 -->
  <el-dialog title="导入表" v-model="visible" width="800px" top="5vh" append-to-body>
    <!-- 弹窗内搜索区：按表名称 / 表描述过滤数据库中的候选表，回车即搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="表名称" prop="tableName">
        <el-input
          v-model="queryParams.tableName"
          placeholder="请输入表名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="表描述" prop="tableComment">
        <el-input
          v-model="queryParams.tableComment"
          placeholder="请输入表描述"
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
    <el-row>
      <!-- 候选表列表：单击整行等价于切换勾选；固定 260px 高并在内部滚动，避免弹窗跟着列表变长 -->
      <el-table @row-click="clickRow" ref="table" :data="dbTableList" @selection-change="handleSelectionChange" height="260px">
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="tableName" label="表名称" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="tableComment" label="表描述" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="createTime" label="创建时间"></el-table-column>
        <el-table-column prop="updateTime" label="更新时间"></el-table-column>
      </el-table>
      <!-- 服务端分页：pageNum/pageSize 直接作为查询参数，翻页时保留当前的表名/描述过滤条件 -->
      <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-row>
    <!-- 底部按钮：确定触发导入，成功后才关闭弹窗并通知父级刷新；取消只关弹窗，不额外清空已勾选状态 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleImportTable">确 定</el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
// listDbTable 分页查询数据库物理表，importTable 按逗号分隔的表名批量导入（参数走 query，字段名 tables）
import { listDbTable, importTable } from "@/api/tool/gen"

// visible 控制弹窗显隐（由父组件调用 show() 打开）；tables 只保存勾选行的表名，供导入接口使用
const total = ref(0)
const visible = ref(false)
const tables = ref([])
const dbTableList = ref([])
const { proxy } = getCurrentInstance()

// 查询条件：分页参数与后端 PageDomain 对齐；表名/描述为模糊匹配，留空表示不过滤
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined,
  tableComment: undefined
})

// 导入成功后通知父组件（列表页）重新查询生成表列表
const emit = defineEmits(["ok"])

/** 查询参数列表 */
// 由父组件通过 ref 调用：打开即拉取候选表，页码沿用上次的状态（需要从头查时点「重置」）
function show() {
  getList()
  visible.value = true
}

/** 单击选择行 */
// 用 toggleRowSelection 让整行点击也能勾选/取消，不必精确点中左侧复选框
function clickRow(row) {
  proxy.$refs.table.toggleRowSelection(row)
}

/** 多选框选中数据 */
// 只保留表名（不保留整行对象），因为导入接口的参数就是逗号分隔的表名
function handleSelectionChange(selection) {
  tables.value = selection.map(item => item.tableName)
}

/** 查询表数据 */
// 返回结构为 { rows, total }，只展示当前页；未做 loading 态，翻页较快时可能出现短暂空白
function getList() {
  listDbTable(queryParams).then(res => {
    dbTableList.value = res.rows
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

/** 导入按钮操作 */
// 未勾选任何表时直接提示，不发起请求；仅当返回 code === 200 才关闭弹窗并 emit("ok")，
// 否则（例如所选表全部已导入）保留弹窗，便于用户改选后重试
function handleImportTable() {
  const tableNames = tables.value.join(",")
  if (tableNames == "") {
    proxy.$modal.msgError("请选择要导入的表")
    return
  }
  importTable({ tables: tableNames }).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    if (res.code === 200) {
      visible.value = false
      emit("ok")
    }
  })
}

// 只向父组件暴露 show()：弹窗的打开时机由父级控制，其余状态对外不可见
defineExpose({
  show,
})
</script>
