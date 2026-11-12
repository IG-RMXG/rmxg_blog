<template>
  <!-- 代码生成配置编辑页：路由 /tool/gen-edit/index/:tableId，由列表页「修改」以多页签方式打开 -->
  <!-- 三个页签分别维护表元信息、字段级生成策略、模板与包路径等生成信息，最终由底部「提交」一次性保存 -->
  <el-card>
    <el-tabs v-model="activeName">
      <!-- 基本信息页签：表名称/表描述/实体类名称/作者，子组件直接读写 info 对象 -->
      <el-tab-pane label="基本信息" name="basic">
        <basic-info-form ref="basicInfo" :info="info" />
      </el-tab-pane>
      <!-- 字段信息页签：每行对应 gen_table_column 的一条记录，行内可直接编辑生成策略 -->
      <!-- 序号列与字段列名带 allowDrag 类，作为 sortablejs 的拖拽把手；row-key 用 columnId 保证拖拽重排后行标识稳定 -->
      <el-tab-pane label="字段信息" name="columnInfo">
        <el-table ref="dragTable" :data="columns" row-key="columnId" :max-height="tableHeight">
          <el-table-column label="序号" type="index" min-width="5%" class-name="allowDrag"/>
          <el-table-column label="字段列名" prop="columnName" min-width="10%" :show-overflow-tooltip="true" class-name="allowDrag"/>
          <el-table-column label="字段描述" min-width="10%">
            <template #default="scope">
              <el-input v-model="scope.row.columnComment"></el-input>
            </template>
          </el-table-column>
          <el-table-column
            label="物理类型"
            prop="columnType"
            min-width="10%"
            :show-overflow-tooltip="true"
          />
          <el-table-column label="Java类型" min-width="11%">
            <template #default="scope">
              <el-select v-model="scope.row.javaType">
                <el-option label="Long" value="Long" />
                <el-option label="String" value="String" />
                <el-option label="Integer" value="Integer" />
                <el-option label="Double" value="Double" />
                <el-option label="BigDecimal" value="BigDecimal" />
                <el-option label="Date" value="Date" />
                <el-option label="Boolean" value="Boolean" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="java属性" min-width="10%">
            <template #default="scope">
              <el-input v-model="scope.row.javaField"></el-input>
            </template>
          </el-table-column>

          <!-- 插入/编辑/列表/查询/必填都是 1/0 字符串枚举（true-value="1"），对应后端 char 类型字段，不要当布尔值用 -->
          <el-table-column label="插入" min-width="5%">
            <template #default="scope">
              <el-checkbox true-value="1" false-value="0" v-model="scope.row.isInsert"></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="编辑" min-width="5%">
            <template #default="scope">
              <el-checkbox true-value="1" false-value="0" v-model="scope.row.isEdit"></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="列表" min-width="5%">
            <template #default="scope">
              <el-checkbox true-value="1" false-value="0" v-model="scope.row.isList"></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="查询" min-width="5%">
            <template #default="scope">
              <el-checkbox true-value="1" false-value="0" v-model="scope.row.isQuery"></el-checkbox>
            </template>
          </el-table-column>
          <!-- 查询方式只在勾选「查询」后参与生成：EQ/NE/GT/GTE/LT/LTE/LIKE/BETWEEN，对应生成 Mapper 里的条件拼接 -->
          <el-table-column label="查询方式" min-width="10%">
            <template #default="scope">
              <el-select v-model="scope.row.queryType">
                <el-option label="=" value="EQ" />
                <el-option label="!=" value="NE" />
                <el-option label=">" value="GT" />
                <el-option label=">=" value="GTE" />
                <el-option label="<" value="LT" />
                <el-option label="<=" value="LTE" />
                <el-option label="LIKE" value="LIKE" />
                <el-option label="BETWEEN" value="BETWEEN" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" min-width="5%">
            <template #default="scope">
              <el-checkbox true-value="1" false-value="0" v-model="scope.row.isRequired"></el-checkbox>
            </template>
          </el-table-column>
          <!-- 显示类型决定生成页面上用哪种控件（文本框/下拉/上传/富文本等），选 imageUpload、fileUpload、editor 时
               生成代码会额外引入上传组件与富文本组件，需确认前端已注册对应组件 -->
          <el-table-column label="显示类型" min-width="12%">
            <template #default="scope">
              <el-select v-model="scope.row.htmlType">
                <el-option label="文本框" value="input" />
                <el-option label="文本域" value="textarea" />
                <el-option label="下拉框" value="select" />
                <el-option label="单选框" value="radio" />
                <el-option label="复选框" value="checkbox" />
                <el-option label="日期控件" value="datetime" />
                <el-option label="图片上传" value="imageUpload" />
                <el-option label="文件上传" value="fileUpload" />
                <el-option label="富文本控件" value="editor" />
              </el-select>
            </template>
          </el-table-column>
          <!-- 字典类型可留空；选中后生成的页面会用该字典编码拉取选项。下拉项同时展示「字典名称 + 字典类型」，
               因为系统里常出现同名字典，只显示名称无法区分 -->
          <el-table-column label="字典类型" min-width="12%">
            <template #default="scope">
              <el-select v-model="scope.row.dictType" clearable filterable placeholder="请选择">
                <el-option
                  v-for="dict in dictOptions"
                  :key="dict.dictType"
                  :label="dict.dictName"
                  :value="dict.dictType">
                  <span style="float: left">{{ dict.dictName }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ dict.dictType }}</span>
              </el-option>
              </el-select>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <!-- 生成信息页签：生成模板、包路径、上级菜单、生成方式等，由 genInfoForm 维护 -->
      <el-tab-pane label="生成信息" name="genInfo">
        <gen-info-form ref="genInfo" :info="info" :tables="tables" />
      </el-tab-pane>
    </el-tabs>
    <!-- 提交/返回独立于页签，放在 el-tabs 之外，三个页签共用同一组按钮 -->
    <el-form label-width="100px">
      <div style="text-align: center;margin-left:-100px;margin-top:10px;">
        <el-button type="primary" @click="submitForm()">提交</el-button>
        <el-button @click="close()">返回</el-button>
      </div>
    </el-form>
  </el-card>
</template>

<script setup name="GenEdit">
// getGenTable 一次返回 { info: 表配置, rows: 字段列表, tables: 可关联的子表 }；updateGenTable 整体提交表配置与全部字段配置
import { getGenTable, updateGenTable } from "@/api/tool/gen"
import { optionselect as getDictOptionselect } from "@/api/system/dict/type"
import basicInfoForm from "./basicInfoForm"
import genInfoForm from "./genInfoForm"
import Sortable from 'sortablejs'

// 路由参数 tableId 来自 /tool/gen-edit/index/:tableId，取不到时不做任何初始化请求
const route = useRoute()
const { proxy } = getCurrentInstance()

// ===== 页面状态 =====
// 默认停在「字段信息」页签，因为该页是生成配置里最常调整的部分
// 表格高度按窗口滚动高度减去页头/页签/按钮区估算而来（245 为经验值），且只在创建时计算一次，
// 窗口尺寸变化后不会自动重算，属于已知的静态高度处理
const activeName = ref("columnInfo")
const tableHeight = ref(document.documentElement.scrollHeight - 245 + "px")
// tables 是后端返回的可关联子表（含其字段），columns 是当前表的字段配置，info 是表级配置
const tables = ref([])
const columns = ref([])
const dictOptions = ref([])
const info = ref({})

/** 提交按钮 */
// 基本信息与生成信息是两个独立的子表单，必须分别校验后合并结果；任一未通过则整体拦截，不发起请求
function submitForm() {
  const basicForm = proxy.$refs.basicInfo.$refs.basicInfoForm
  const genForm = proxy.$refs.genInfo.$refs.genInfoForm
  Promise.all([basicForm, genForm].map(getFormPromise)).then(res => {
    const validateResult = res.every(item => !!item)
    if (validateResult) {
      const genTable = Object.assign({}, info.value)
      genTable.columns = columns.value
      // 树表/上级菜单等扩展配置走 params：后端从 GenTable 的 params 里取 treeCode、treeName、
      // treeParentCode、parentMenuId，平铺在顶层不会生效
      genTable.params = {
        treeCode: info.value.treeCode,
        treeName: info.value.treeName,
        treeParentCode: info.value.treeParentCode,
        parentMenuId: info.value.parentMenuId
      }
      updateGenTable(genTable).then(res => {
        proxy.$modal.msgSuccess(res.msg)
        if (res.code === 200) {
          close()
        }
      })
    } else {
      proxy.$modal.msgError("表单校验未通过，请重新检查提交内容")
    }
  })
}

// 把 el-form validate(callback) 的回调式 API 包装成 Promise，回调参数为 true 表示校验通过
function getFormPromise(form) {
  return new Promise(resolve => {
    form.validate(res => {
      resolve(res)
    })
  })
}

// 关闭当前页签并跳回列表页：t 用当前时间戳，触发列表页 onActivated 里的刷新判断；
// pageNum 透传来源页码，使返回后仍停在离开时的那一页
function close() {
  const obj = { path: "/tool/gen", query: { t: Date.now(), pageNum: route.query.pageNum } }
  proxy.$tab.closeOpenPage(obj)
}

// 立即执行初始化：缺少 tableId（直接访问或参数丢失）时跳过全部请求，避免用空 id 拉数据；
// 字典下拉只在需要编辑时拉取一次，后续切换页签不会重复请求
(() => {
  const tableId = route.params && route.params.tableId
  if (tableId) {
    // 获取表详细信息
    getGenTable(tableId).then(res => {
      columns.value = res.data.rows
      info.value = res.data.info
      tables.value = res.data.tables
    })
    /** 查询字典下拉列表 */
    getDictOptionselect().then(response => {
      dictOptions.value = response.data
    })
  }
})()

// 拖动排序
// 拖拽结束后本地重排 columns，并按新下标回写 sort（从 1 开始）；该顺序即生成代码时字段出现的顺序
onMounted(() => {
  // 用全局选择器取字段表格的 tbody 作为拖拽容器，handle 限制为带 allowDrag 的单元格，避免误拖输入框
  const element = document.querySelector('.el-table__body > tbody')
  Sortable.create(element, {
    handle: ".allowDrag",
    // evt.oldIndex / evt.newIndex 为拖拽前后的行下标：先摘出被拖行，再插到新位置，最后统一重编 sort
    onEnd: (evt) => {
      const targetRow = columns.value.splice(evt.oldIndex, 1)[0]
      columns.value.splice(evt.newIndex, 0, targetRow)
      for (const index in columns.value) {
        columns.value[index].sort = parseInt(index) + 1
      }
    }
  })
})
</script>
