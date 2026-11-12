<template>
   <div class="app-container">
      <!-- 搜索区：公告标题与操作人员（对应 create_by）都是 LIKE 模糊匹配，公告类型按字典精确匹配 -->
      <!-- 后端列表还支持 status 与创建时间区间（按天比较），本页未提供这两个筛选入口 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="公告标题" prop="noticeTitle">
            <el-input
               v-model="queryParams.noticeTitle"
               placeholder="请输入公告标题"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="操作人员" prop="createBy">
            <el-input
               v-model="queryParams.createBy"
               placeholder="请输入操作人员"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="类型" prop="noticeType">
            <el-select v-model="queryParams.noticeType" placeholder="公告类型" clearable style="width: 200px">
               <el-option
                  v-for="dict in sys_notice_type"
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

      <!-- 工具栏：三个按钮都用 v-hasPermi 按权限点显隐；single / multiple 存的是「禁用条件」而非选中状态 -->
      <!-- 改（single）要求恰好选中 1 条，删（multiple）要求至少选中 1 条，未达标时按钮置灰 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:notice:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['system:notice:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:notice:remove']"
            >删除</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 数据表格：标题超长省略并悬浮提示，类型 / 状态交给 dict-tag 按字典渲染色块，创建时间只取到日 -->
      <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="序号" align="center" prop="noticeId" width="100" />
         <el-table-column
            label="公告标题"
            align="center"
            prop="noticeTitle"
            :show-overflow-tooltip="true"
         />
         <el-table-column label="公告类型" align="center" prop="noticeType" width="100">
            <template #default="scope">
               <dict-tag :options="sys_notice_type" :value="scope.row.noticeType" />
            </template>
         </el-table-column>
         <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="scope">
               <dict-tag :options="sys_notice_status" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="创建者" align="center" prop="createBy" width="100" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="100">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:notice:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:notice:remove']" >删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：pageNum / pageSize 与 queryParams 双向绑定，翻页直接复用当前查询条件，无需再点搜索 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改公告对话框 -->
      <!-- 新增与修改共用同一个弹窗，标题由 title 区分，noticeId 是否存在决定提交走 updateNotice 还是 addNotice -->
      <!-- 修改前必须先 getNotice 回填整张表单：后端 UPDATE 是全量覆盖，未回传的字段会被写成 null -->
      <!-- 内容用富文本 editor 组件，noticeContent 以 HTML 字符串原样提交，库里是 longblob 但按字符串读写 -->
      <el-dialog :title="title" v-model="open" width="780px" append-to-body>
         <el-form ref="noticeRef" :model="form" :rules="rules" label-width="80px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="公告标题" prop="noticeTitle">
                     <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="公告类型" prop="noticeType">
                     <el-select v-model="form.noticeType" placeholder="请选择">
                        <el-option
                           v-for="dict in sys_notice_type"
                           :key="dict.value"
                           :label="dict.label"
                           :value="dict.value"
                        ></el-option>
                     </el-select>
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="状态">
                     <el-radio-group v-model="form.status">
                        <el-radio
                           v-for="dict in sys_notice_status"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="内容">
                    <editor v-model="form.noticeContent" :min-height="192"/>
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
   </div>
</template>

<script setup name="Notice">
import { listNotice, getNotice, delNotice, addNotice, updateNotice } from "@/api/system/notice"

// 通知公告管理页：RuoYi 标准 CRUD（搜索区 / 工具栏 / 表格 / 分页 / 新增编辑共用弹窗）。
// 接口：@/api/system/notice 的 listNotice / getNotice / addNotice / updateNotice / delNotice。
// 权限点：system:notice:add、system:notice:edit、system:notice:remove；目前只由前端 v-hasPermi 控制显隐，
// 后端 SysNoticeController 没有声明接口级权限注解，绕过页面直接调接口不会被权限校验拦住。
// 字典：sys_notice_type（1=通知 2=公告）、sys_notice_status（0=正常 1=关闭）。
// 状态语义：status='1' 即关闭 / 下架，首页 listTop 只取 status='0' 的最近 5 条。
// 分页约定：pageNum / pageSize 由后端 PageUtils 从请求参数读取，响应固定为 { rows, total }。
// 初始化：script setup 顶层末尾直接调用 getList()，进入页面即拉第一页。

const { proxy } = getCurrentInstance()
const { sys_notice_status, sys_notice_type } = proxy.useDict("sys_notice_status", "sys_notice_type")

const noticeList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

// form 与 queryParams 收在同一个 reactive 里，便于新增 / 修改 / 取消时整体重置
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    noticeTitle: undefined,
    createBy: undefined,
    status: undefined
  },
  // 只校验标题与类型；公告内容不设必填，空内容也能保存（editor 没有对应的 prop 规则）
  rules: {
    noticeTitle: [{ required: true, message: "公告标题不能为空", trigger: "blur" }],
    noticeType: [{ required: true, message: "公告类型不能为空", trigger: "change" }]
  },
})

const { queryParams, form, rules } = toRefs(data)

/** 查询公告列表 */
function getList() {
  // 只在成功回调里关 loading，请求失败时会一直转圈（没有 finally 兜底）
  loading.value = true
  listNotice(queryParams.value).then(response => {
    noticeList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  // 新增默认 status='0'（正常），保存后立刻会出现在首页 listTop；后端在 status 为空时也会补 '0'
  form.value = {
    noticeId: undefined,
    noticeTitle: undefined,
    noticeType: undefined,
    noticeContent: undefined,
    status: "0"
  }
  // resetForm 只重置 el-form-item 上注册过 prop 的字段，富文本内容靠上面的整体赋值清空
  proxy.resetForm("noticeRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  // 条件变了必须回到第 1 页，否则可能停在新结果集之外的页码上显示空列表
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  // 抽出选中行主键，供工具栏的修改 / 删除在没传 row 时兜底；single / multiple 是禁用条件，与名字相反
  ids.value = selection.map(item => item.noticeId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加公告"
}

/**修改按钮操作 */
function handleUpdate(row) {
  reset()
  // 行内按钮直接传 row；工具栏按钮不传 row，回退到多选结果（按钮已限制只选中 1 条）
  const noticeId = row.noticeId || ids.value
  getNotice(noticeId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改公告"
  })
}

/** 提交按钮 */
function submitForm() {
  // 按钮本身没有 loading 锁，防重复提交靠 request.js 拦截器：1 秒内相同 url + data 的 POST / PUT 会被挡下
  proxy.$refs["noticeRef"].validate(valid => {
    if (valid) {
      if (form.value.noticeId != undefined) {
        updateNotice(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addNotice(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  // 删除是物理删除（DELETE /system/notice/{ids}，无软删列、无回收站），所以必须先二次确认
  // 未传 row 时用多选结果；数组拼进 URL 会自动变成逗号分隔，后端按 Long[] 接收
  const noticeIds = row.noticeId || ids.value
  proxy.$modal.confirm('是否确认删除公告编号为"' + noticeIds + '"的数据项？').then(function() {
    return delNotice(noticeIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  // 用户点「取消」会走 reject，这里吞掉，避免控制台出现未处理的 Promise 拒绝
  }).catch(() => {})
}

getList()
</script>
