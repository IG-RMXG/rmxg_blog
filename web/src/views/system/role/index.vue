<template>
   <div class="app-container">
      <!-- 查询区：按角色名称/权限字符/状态/创建时间过滤；v-show 由右侧工具栏控制收起，输入框回车即触发查询 -->
      <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="68px">
         <el-form-item label="角色名称" prop="roleName">
            <el-input
               v-model="queryParams.roleName"
               placeholder="请输入角色名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="权限字符" prop="roleKey">
            <el-input
               v-model="queryParams.roleKey"
               placeholder="请输入权限字符"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="角色状态"
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
      <!-- 工具栏：新增/修改/删除/导出按 system:role:* 权限点显隐；“修改”要求恰好选中一行，“删除”要求至少选中一行 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:role:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['system:role:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:role:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:role:export']"
            >导出</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 表格数据 -->
      <!-- 状态列用 el-switch 直改状态：字典 sys_normal_disable 为 0=正常、1=停用（与布尔开关取值相反），
           开关先改 UI 再调接口，取消或失败时由 handleStatusChange 的 catch 回滚显示 -->
      <el-table v-loading="loading" :data="roleList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="角色编号" prop="roleId" width="120" />
         <el-table-column label="角色名称" prop="roleName" :show-overflow-tooltip="true" width="150" />
         <el-table-column label="权限字符" prop="roleKey" :show-overflow-tooltip="true" width="150" />
         <el-table-column label="显示顺序" prop="roleSort" width="100" />
         <el-table-column label="状态" align="center" width="100">
            <template #default="scope">
               <el-switch
                  v-model="scope.row.status"
                  active-value="0"
                  inactive-value="1"
                  @change="handleStatusChange(scope.row)"
               ></el-switch>
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-tooltip content="修改" placement="top" v-if="scope.row.roleId !== 1">
                <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:role:edit']"></el-button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top" v-if="scope.row.roleId !== 1">
                <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:role:remove']"></el-button>
              </el-tooltip>
              <el-tooltip content="数据权限" placement="top" v-if="scope.row.roleId !== 1">
                <el-button link type="primary" icon="CircleCheck" @click="handleDataScope(scope.row)" v-hasPermi="['system:role:edit']"></el-button>
              </el-tooltip>
              <el-tooltip content="分配用户" placement="top" v-if="scope.row.roleId !== 1">
                <el-button link type="primary" icon="User" @click="handleAuthUser(scope.row)" v-hasPermi="['system:role:edit']"></el-button>
              </el-tooltip>
            </template>
         </el-table-column>
      </el-table>

      <!-- 分页：无数据时隐藏；页码/页大小与 queryParams 双向绑定，翻页沿用当前查询条件 -->
      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改角色配置对话框 -->
      <el-dialog :title="title" v-model="open" width="500px" append-to-body>
         <!-- 菜单权限树：:check-strictly 取“父子联动”勾选值的反值，勾选联动时严格模式为 false，由 el-tree 自动级联；
              提交时 getMenuAllCheckedKeys 会把半选的父节点一并带上，后端据此生成角色菜单授权 -->
         <el-form ref="roleRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="角色名称" prop="roleName">
               <el-input v-model="form.roleName" placeholder="请输入角色名称" />
            </el-form-item>
            <el-form-item prop="roleKey">
               <template #label>
                  <span>
                     <el-tooltip content="控制器中定义的权限字符，如：@PreAuthorize(`@ss.hasRole('admin')`)" placement="top">
                        <el-icon><question-filled /></el-icon>
                     </el-tooltip>
                     权限字符
                  </span>
               </template>
               <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
            </el-form-item>
            <el-form-item label="角色顺序" prop="roleSort">
               <el-input-number v-model="form.roleSort" controls-position="right" :min="0" />
            </el-form-item>
            <el-form-item label="状态">
               <el-radio-group v-model="form.status">
                  <el-radio
                     v-for="dict in sys_normal_disable"
                     :key="dict.value"
                     :value="dict.value"
                  >{{ dict.label }}</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="菜单权限">
               <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event, 'menu')">展开/折叠</el-checkbox>
               <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event, 'menu')">全选/全不选</el-checkbox>
               <el-checkbox v-model="form.menuCheckStrictly" @change="handleCheckedTreeConnect($event, 'menu')">父子联动</el-checkbox>
               <el-tree
                  class="tree-border"
                  :data="menuOptions"
                  show-checkbox
                  ref="menuRef"
                  node-key="id"
                  :check-strictly="!form.menuCheckStrictly"
                  empty-text="加载中，请稍候"
                  :props="{ label: 'label', children: 'children' }"
               ></el-tree>
            </el-form-item>
            <el-form-item label="备注">
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

      <!-- 分配角色数据权限对话框 -->
      <el-dialog :title="title" v-model="openDataScope" width="500px" append-to-body>
         <!-- 权限范围选“自定数据权限”(dataScope=2) 时才展开并提交部门树，其余范围只提交 dataScope 本身 -->
         <el-form :model="form" label-width="80px">
            <el-form-item label="角色名称">
               <el-input v-model="form.roleName" :disabled="true" />
            </el-form-item>
            <el-form-item label="权限字符">
               <el-input v-model="form.roleKey" :disabled="true" />
            </el-form-item>
            <el-form-item label="权限范围">
               <el-select v-model="form.dataScope" @change="dataScopeSelectChange">
                  <el-option
                     v-for="item in dataScopeOptions"
                     :key="item.value"
                     :label="item.label"
                     :value="item.value"
                  ></el-option>
               </el-select>
            </el-form-item>
            <el-form-item label="数据权限" v-show="form.dataScope == 2">
               <el-checkbox v-model="deptExpand" @change="handleCheckedTreeExpand($event, 'dept')">展开/折叠</el-checkbox>
               <el-checkbox v-model="deptNodeAll" @change="handleCheckedTreeNodeAll($event, 'dept')">全选/全不选</el-checkbox>
               <el-checkbox v-model="form.deptCheckStrictly" @change="handleCheckedTreeConnect($event, 'dept')">父子联动</el-checkbox>
               <el-tree
                  class="tree-border"
                  :data="deptOptions"
                  show-checkbox
                  default-expand-all
                  ref="deptRef"
                  node-key="id"
                  :check-strictly="!form.deptCheckStrictly"
                  empty-text="加载中，请稍候"
                  :props="{ label: 'label', children: 'children' }"
               ></el-tree>
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitDataScope">确 定</el-button>
               <el-button @click="cancelDataScope">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Role">
/**
 * 角色管理列表页（路由 /system/role）
 * 职责：角色增删改查 + 菜单权限分配（弹窗内 el-tree）+ 数据权限范围分配 + 跳转“分配用户”子页。
 * 依赖接口：@/api/system/role 下的 listRole/getRole/addRole/updateRole/delRole/changeRoleStatus/dataScope/deptTreeSelect；
 *          @/api/system/menu 下 menuTreeselect（全量菜单树，新增用）与 roleMenuTreeselect（附带该角色已勾选的菜单）。
 * 权限点：system:role:add / edit / remove / export，由 v-hasPermi 控制按钮显隐。
 * 约定：roleId=1 为超级管理员角色，行内不提供修改、删除、数据权限与分配用户入口。
 */
import { addRole, changeRoleStatus, dataScope, delRole, getRole, listRole, updateRole, deptTreeSelect } from "@/api/system/role"
import { roleMenuTreeselect, treeselect as menuTreeselect } from "@/api/system/menu"

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const roleList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
// single/multiple 是“禁用按钮”的反向标记：single=true 表示修改按钮不可点，multiple=true 表示删除按钮不可点
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
// ===== 菜单树 / 部门树：选项数据、展开与全选状态、以及 el-tree 实例 =====
const menuOptions = ref([])
const menuExpand = ref(false)
const menuNodeAll = ref(false)
const deptExpand = ref(true)
const deptNodeAll = ref(false)
const deptOptions = ref([])
const openDataScope = ref(false)
const menuRef = ref(null)
const deptRef = ref(null)

/** 数据范围选项*/
const dataScopeOptions = ref([
  { value: "1", label: "全部数据权限" },
  { value: "2", label: "自定数据权限" },
  { value: "3", label: "本部门数据权限" },
  { value: "4", label: "本部门及以下数据权限" },
  { value: "5", label: "仅本人数据权限" }
])

// ===== 查询条件 / 表单模型 / 校验规则 =====
// 分页约定：pageNum/pageSize 由 pagination 组件双向绑定，后端返回 { rows, total }；
// 创建时间区间不在 queryParams 中，而是由 dateRange 经 proxy.addDateRange 拼成 beginTime/endTime。
// 校验规则：角色名称、权限字符、角色顺序必填，触发方式均为 blur；权限字符即后端 @ss.hasRole('xxx') 使用的标识。
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    roleName: undefined,
    roleKey: undefined,
    status: undefined
  },
  rules: {
    roleName: [{ required: true, message: "角色名称不能为空", trigger: "blur" }],
    roleKey: [{ required: true, message: "权限字符不能为空", trigger: "blur" }],
    roleSort: [{ required: true, message: "角色顺序不能为空", trigger: "blur" }]
  },
})

const { queryParams, form, rules } = toRefs(data)

/** 查询角色列表 */
// addDateRange 会把 dateRange 追加成 beginTime/endTime 参数；rows 为当前页数据、total 为总条数，分页在后端完成
function getList() {
  loading.value = true
  listRole(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    roleList.value = response.rows
    total.value = response.total
    loading.value = false
  })
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

/** 删除按钮操作 */
// 行内删除取 row.roleId，工具栏删除取勾选的 ids；确认框被取消时进入 catch，静默不做提示
function handleDelete(row) {
  const roleIds = row.roleId || ids.value
  proxy.$modal.confirm('是否确认删除角色编号为"' + roleIds + '"的数据项?').then(function () {
    return delRole(roleIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
// 导出复用当前查询条件；文件名带毫秒时间戳，避免多次下载互相覆盖（proxy.download 内部负责 blob 与鉴权头）
function handleExport() {
  proxy.download("system/role/export", {
    ...queryParams.value,
  }, `role_${new Date().getTime()}.xlsx`)
}

/** 多选框选中数据 */
// 选中恰好 1 行才放开“修改”，选中 0 行则禁用“删除”；两个标记都是“禁用条件”的反向表达
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.roleId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 角色状态修改 */
// el-switch 已先改掉 status 再触发本方法，因此取消确认或接口失败时必须在 catch 里取反回滚，保证开关与后端一致；
// 提交的是切换后的目标值（0=正常 / 1=停用），不是旧值
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用"
  proxy.$modal.confirm('确认要"' + text + '""' + row.roleName + '"角色吗?').then(function () {
    return changeRoleStatus(row.roleId, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功")
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0"
  })
}

/** 更多操作 */
// 旧版“更多操作”下拉的命令分发入口；当前模板已改为行内按钮直连 handleDataScope / handleAuthUser，此处保留
function handleCommand(command, row) {
  switch (command) {
    case "handleDataScope":
      handleDataScope(row)
      break
    case "handleAuthUser":
      handleAuthUser(row)
      break
    default:
      break
  }
}

/** 分配用户 */
// 跳转隐藏路由 /system/role-auth/user/:roleId，roleId 走路径参数，目标页从 route.params.roleId 读取
function handleAuthUser(row) {
  router.push("/system/role-auth/user/" + row.roleId)
}

/** 查询菜单树结构 */
// 注意接口返回结构差异：全量菜单树取 response.data，而按角色查询时树在 response.menus
function getMenuTreeselect() {
  menuTreeselect().then(response => {
    menuOptions.value = response.data
  })
}

/** 所有部门节点数据 */
// 半选的父部门必须一并提交：只传全选节点会让部门链断档，后端回显数据范围时树会缺层级
function getDeptAllCheckedKeys() {
  // 目前被选中的部门节点
  let checkedKeys = deptRef.value.getCheckedKeys()
  // 半选中的部门节点
  let halfCheckedKeys = deptRef.value.getHalfCheckedKeys()
  checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys)
  return checkedKeys
}

/** 重置新增的表单以及其他数据  */
// 复位为“新增”默认值：status=0（正常）、roleSort=0、菜单/部门父子联动默认开启；
// 菜单树实例要等弹窗首次渲染后才存在，故先判空再清空勾选
function reset() {
  if (menuRef.value != undefined) {
    menuRef.value.setCheckedKeys([])
  }
  menuExpand.value = false
  menuNodeAll.value = false
  deptExpand.value = true
  deptNodeAll.value = false
  form.value = {
    roleId: undefined,
    roleName: undefined,
    roleKey: undefined,
    roleSort: 0,
    status: "0",
    menuIds: [],
    deptIds: [],
    menuCheckStrictly: true,
    deptCheckStrictly: true,
    remark: undefined
  }
  proxy.resetForm("roleRef")
}

/** 添加角色 */
function handleAdd() {
  reset()
  getMenuTreeselect()
  open.value = true
  title.value = "添加角色"
}

/** 修改角色 */
// 回显要点：菜单勾选必须等弹窗内 el-tree 渲染完成（nextTick）后再逐个 setChecked；
// 第三个参数 false 表示不联动子节点，否则勾选父节点会级联勾上它下面本不该授权的子菜单。
// roleSort 后端返回字符串，而 el-input-number 需要数字，故显式做 Number 转换。
function handleUpdate(row) {
  reset()
  const roleId = row.roleId || ids.value
  const roleMenu = getRoleMenuTreeselect(roleId)
  getRole(roleId).then(response => {
    form.value = response.data
    form.value.roleSort = Number(form.value.roleSort)
    open.value = true
    nextTick(() => {
      roleMenu.then((res) => {
        let checkedKeys = res.checkedKeys
        checkedKeys.forEach((v) => {
          nextTick(() => {
            menuRef.value.setChecked(v, true, false)
          })
        })
      })
    })
  })
  title.value = "修改角色"
}

/** 根据角色ID查询菜单树结构 */
// 一次请求同时返回 menus（全量菜单树）与该角色的 checkedKeys（已勾选菜单），弹窗据此回显
function getRoleMenuTreeselect(roleId) {
  return roleMenuTreeselect(roleId).then(response => {
    menuOptions.value = response.menus
    return response
  })
}

/** 根据角色ID查询部门树结构 */
function getDeptTree(roleId) {
  return deptTreeSelect(roleId).then(response => {
    deptOptions.value = response.depts
    return response
  })
}

/** 树权限（展开/折叠）*/
// 这里直接改 el-tree 内部 store.nodesMap 的 expanded 属于非公开 API（element-plus 未导出批量展开方法），
// 升级组件库后需回归验证；循环只覆盖顶层节点，更深层节点保持各自的展开状态
function handleCheckedTreeExpand(value, type) {
  if (type == "menu") {
    let treeList = menuOptions.value
    for (let i = 0; i < treeList.length; i++) {
      menuRef.value.store.nodesMap[treeList[i].id].expanded = value
    }
  } else if (type == "dept") {
    let treeList = deptOptions.value
    for (let i = 0; i < treeList.length; i++) {
      deptRef.value.store.nodesMap[treeList[i].id].expanded = value
    }
  }
}

/** 树权限（全选/全不选） */
function handleCheckedTreeNodeAll(value, type) {
  if (type == "menu") {
    menuRef.value.setCheckedNodes(value ? menuOptions.value : [])
  } else if (type == "dept") {
    deptRef.value.setCheckedNodes(value ? deptOptions.value : [])
  }
}

/** 树权限（父子联动） */
function handleCheckedTreeConnect(value, type) {
  if (type == "menu") {
    form.value.menuCheckStrictly = value ? true : false
  } else if (type == "dept") {
    form.value.deptCheckStrictly = value ? true : false
  }
}

/** 所有菜单节点数据 */
// 与部门树同理：半选的父菜单也要回传，否则父级链路缺失会导致菜单权限树回显断层
function getMenuAllCheckedKeys() {
  // 目前被选中的菜单节点
  let checkedKeys = menuRef.value.getCheckedKeys()
  // 半选中的菜单节点
  let halfCheckedKeys = menuRef.value.getHalfCheckedKeys()
  checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys)
  return checkedKeys
}

/** 提交按钮 */
// 新增与修改共用一个弹窗和同一个提交入口，以 form.roleId 是否存在区分；
// menuIds 不在 form 初始值里维护，而是提交前从 el-tree 实时读取（含半选父节点）
function submitForm() {
  proxy.$refs["roleRef"].validate(valid => {
    if (valid) {
      if (form.value.roleId != undefined) {
        form.value.menuIds = getMenuAllCheckedKeys()
        updateRole(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        form.value.menuIds = getMenuAllCheckedKeys()
        addRole(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 选择角色权限范围触发 */
// 从“自定数据权限”切换到其他范围时清空部门树勾选，避免把上一次的 deptIds 带入其它范围提交；
// 部门树容器用的是 v-show（始终挂载），因此这里可以直接调用 deptRef，不必判空
function dataScopeSelectChange(value) {
  if (value !== "2") {
    deptRef.value.setCheckedKeys([])
  }
}

/** 分配数据权限操作 */
// 两个请求并行发出：getDeptTree 取部门树和该角色已选部门，getRole 取角色主体（含 dataScope）；
// 回显要等弹窗渲染完成（nextTick）后 setCheckedKeys，再嵌一层 nextTick 兜住树数据晚到的情况
function handleDataScope(row) {
  reset()
  const deptTreeSelect = getDeptTree(row.roleId)
  getRole(row.roleId).then(response => {
    form.value = response.data
    openDataScope.value = true
    nextTick(() => {
      deptTreeSelect.then(res => {
        nextTick(() => {
          if (deptRef.value) {
            deptRef.value.setCheckedKeys(res.checkedKeys)
          }
        })
      })
    })
  })
  title.value = "分配数据权限"
}

/** 提交按钮（数据权限） */
// 只有 form 里带 roleId（即从行内“数据权限”进入）才提交；deptIds 从部门树实时读取，含半选父部门，
// 若权限范围不是“自定”则树已被清空，提交的部门集合为空数组
function submitDataScope() {
  if (form.value.roleId != undefined) {
    form.value.deptIds = getDeptAllCheckedKeys()
    dataScope(form.value).then(response => {
      proxy.$modal.msgSuccess("修改成功")
      openDataScope.value = false
      getList()
    })
  }
}

/** 取消按钮（数据权限）*/
function cancelDataScope() {
  openDataScope.value = false
  reset()
}

// 页面创建即加载第一页角色数据；后续查询、翻页、增删改成功后的刷新都复用 getList
getList()
</script>
