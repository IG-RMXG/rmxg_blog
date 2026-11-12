<template>
   <div class="app-container">
      <!-- 搜索区：菜单是树形数据、接口一次性返回全量，因此没有分页，只按名称/状态过滤 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="菜单名称" prop="menuName">
            <el-input
               v-model="queryParams.menuName"
               placeholder="请输入菜单名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="菜单状态" clearable style="width: 200px">
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

      <!-- 工具栏：新增受 system:menu:add 控制；展开/折叠只改前端展开状态，不重新请求后端 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:menu:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button 
               type="info"
               plain
               icon="Sort"
               @click="toggleExpandAll"
            >展开/折叠</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 树形表格：row-key=menuId 配合 tree-props 展开子级；default-expand-all 只在表格重建时生效 -->
      <el-table
         v-if="refreshTable"
         v-loading="loading"
         :data="menuList"
         row-key="menuId"
         :default-expand-all="isExpandAll"
         :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
         <el-table-column prop="menuName" label="菜单名称" :show-overflow-tooltip="true" width="160"></el-table-column>
         <el-table-column prop="icon" label="图标" align="center" width="100">
            <template #default="scope">
               <svg-icon :icon-class="scope.row.icon" />
            </template>
         </el-table-column>
         <el-table-column prop="orderNum" label="排序" width="60"></el-table-column>
         <el-table-column prop="perms" label="权限标识" :show-overflow-tooltip="true"></el-table-column>
         <el-table-column prop="component" label="组件路径" :show-overflow-tooltip="true"></el-table-column>
         <el-table-column prop="status" label="状态" width="80">
            <template #default="scope">
               <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" width="160" prop="createTime">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="210" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:menu:edit']">修改</el-button>
               <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['system:menu:add']">新增</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:menu:remove']">删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 添加或修改菜单对话框 -->
      <el-dialog :title="title" v-model="open" width="680px" append-to-body>
         <el-form ref="menuRef" :model="form" :rules="rules" label-width="100px">
            <!-- 菜单类型 M 目录 / C 菜单 / F 按钮 是下面各字段显隐的总开关：
                 目录只做分组；菜单才有路由名称、组件路径、缓存；按钮只保留名称、排序、权限字符与状态 -->
            <el-row>
               <el-col :span="24">
                  <el-form-item label="上级菜单">
                     <el-tree-select
                        v-model="form.parentId"
                        :data="menuOptions"
                        :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
                        value-key="menuId"
                        placeholder="选择上级菜单"
                        check-strictly
                     />
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="菜单类型" prop="menuType">
                     <el-radio-group v-model="form.menuType">
                        <el-radio value="M">目录</el-radio>
                        <el-radio value="C">菜单</el-radio>
                        <el-radio value="F">按钮</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType != 'F'">
                  <el-form-item label="菜单图标" prop="icon">
                     <el-popover
                        placement="bottom-start"
                        :width="540"
                        trigger="click"
                     >
                        <template #reference>
                           <el-input v-model="form.icon" placeholder="点击选择图标" @blur="showSelectIcon" readonly>
                              <template #prefix>
                                 <svg-icon
                                    v-if="form.icon"
                                    :icon-class="form.icon"
                                    class="el-input__icon"
                                    style="height: 32px;width: 16px;"
                                 />
                                 <el-icon v-else style="height: 32px;width: 16px;"><search /></el-icon>
                              </template>
                           </el-input>
                        </template>
                        <icon-select ref="iconSelectRef" @selected="selected" :active-icon="form.icon" />
                     </el-popover>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="显示排序" prop="orderNum">
                     <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="菜单名称" prop="menuName">
                     <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType == 'C'">
                  <el-form-item prop="routeName">
                     <template #label>
                        <span>
                           <el-tooltip content="默认不填则和路由地址相同：如地址为：`user`，则名称为`User`（注意：因为router会删除名称相同路由，为避免名字的冲突，特殊情况下请自定义，保证唯一性）" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           路由名称
                        </span>
                     </template>
                     <el-input v-model="form.routeName" placeholder="请输入路由名称" />
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType != 'F'">
                  <el-form-item>
                     <template #label>
                        <span>
                           <el-tooltip content="选择是外链则路由地址需要以`http(s)://`开头" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>是否外链
                        </span>
                     </template>
                     <el-radio-group v-model="form.isFrame">
                        <el-radio value="0">是</el-radio>
                        <el-radio value="1">否</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType != 'F'">
                  <el-form-item prop="path">
                     <template #label>
                        <span>
                           <el-tooltip content="访问的路由地址，如：`user`，如外网地址需内链访问则以`http(s)://`开头" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           路由地址
                        </span>
                     </template>
                     <el-input v-model="form.path" placeholder="请输入路由地址" />
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType == 'C'">
                  <el-form-item prop="component">
                     <template #label>
                        <span>
                           <el-tooltip content="访问的组件路径，如：`system/user/index`，默认在`views`目录下" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           组件路径
                        </span>
                     </template>
                     <el-input v-model="form.component" placeholder="请输入组件路径" />
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType != 'M'">
                  <el-form-item>
                     <el-input v-model="form.perms" placeholder="请输入权限标识" maxlength="100" />
                     <template #label>
                        <span>
                           <el-tooltip content="控制器中定义的权限字符，如：@PreAuthorize(`@ss.hasPermi('system:user:list')`)" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           权限字符
                        </span>
                     </template>
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType == 'C'">
                  <el-form-item>
                     <el-input v-model="form.query" placeholder="请输入路由参数" maxlength="255" />
                     <template #label>
                        <span>
                           <el-tooltip content='访问路由的默认传递参数，如：`{"id": 1, "name": "ry"}`' placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           路由参数
                        </span>
                     </template>
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType == 'C'">
                  <el-form-item>
                     <template #label>
                        <span>
                           <el-tooltip content="选择是则会被`keep-alive`缓存，需要匹配组件的`name`和地址保持一致" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           是否缓存
                        </span>
                     </template>
                     <el-radio-group v-model="form.isCache">
                        <el-radio value="0">缓存</el-radio>
                        <el-radio value="1">不缓存</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12" v-if="form.menuType != 'F'">
                  <el-form-item>
                     <template #label>
                        <span>
                           <el-tooltip content="选择隐藏则路由将不会出现在侧边栏，但仍然可以访问" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           显示状态
                        </span>
                     </template>
                     <el-radio-group v-model="form.visible">
                        <el-radio
                           v-for="dict in sys_show_hide"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item>
                     <template #label>
                        <span>
                           <el-tooltip content="选择停用则路由将不会出现在侧边栏，也不能被访问" placement="top">
                              <el-icon><question-filled /></el-icon>
                           </el-tooltip>
                           菜单状态
                        </span>
                     </template>
                     <el-radio-group v-model="form.status">
                        <el-radio
                           v-for="dict in sys_normal_disable"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</el-radio>
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
   </div>
</template>

<script setup name="Menu">
/**
 * 菜单管理页（树形、无分页）。
 * 职责：维护 sys_menu 里的目录 / 菜单 / 按钮节点，只负责录入配置，角色授权在角色管理页完成。
 * 接口：@/api/system/menu（/system/menu 前缀）。list 返回扁平数组，由 handleTree 在前端组装成树，
 *       不能按分页结构取 res.rows。
 * 权限点：system:menu:add / system:menu:edit / system:menu:remove，无权限时按钮由 v-hasPermi 直接不渲染。
 * 约定：增删改成功后统一重拉整棵树，不在本地增删节点，保证 parentId 变化后层级正确。
 */
import { addMenu, delMenu, getMenu, listMenu, updateMenu } from "@/api/system/menu"
import SvgIcon from "@/components/SvgIcon"
import IconSelect from "@/components/IconSelect"

// 取 app 级全局能力：useDict、resetForm、handleTree、$modal、$refs 都挂在 proxy 上
const { proxy } = getCurrentInstance()
// 字典：sys_show_hide = 显示状态（0 显示 / 1 隐藏），sys_normal_disable = 通用状态（0 正常 / 1 停用）
const { sys_show_hide, sys_normal_disable } = proxy.useDict("sys_show_hide", "sys_normal_disable")

// ===== 页面状态 =====
const menuList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const title = ref("")
const menuOptions = ref([])
// isExpandAll 是目标展开状态，refreshTable 用来卸载/重建表格让它真正生效（见 toggleExpandAll）
const isExpandAll = ref(false)
const refreshTable = ref(true)
// 图标选择组件实例，输入框失焦时用来清掉上一次的选中态
const iconSelectRef = ref(null)

// ===== 查询条件 / 表单 / 校验 =====
// queryParams 只声明了 menuName 与 visible（visible 当前模板未绑定，保留给接口按需使用）；
// 状态筛选靠 v-model 在运行时补出 status 字段，所以这里没声明也会随请求发出
const data = reactive({
  form: {},
  queryParams: {
    menuName: undefined,
    visible: undefined
  },
  // 规则只对已渲染的 form-item 生效：F(按钮) 类型下路由地址输入框被 v-if 卸载，其 required 也随之失效
  rules: {
    menuName: [{ required: true, message: "菜单名称不能为空", trigger: "blur" }],
    orderNum: [{ required: true, message: "菜单顺序不能为空", trigger: "blur" }],
    path: [{ required: true, message: "路由地址不能为空", trigger: "blur" }]
  },
})

const { queryParams, form, rules } = toRefs(data)

/** 查询菜单列表 */
// 后端给的是扁平列表，handleTree 按 menuId/parentId 就地挂上 children 后返回根节点数组
function getList() {
  loading.value = true
  listMenu(queryParams.value).then(response => {
    menuList.value = proxy.handleTree(response.data, "menuId")
    loading.value = false
  })
}

/** 查询菜单下拉树结构 */
// 补一个 menuId=0 的虚拟根「主类目」，让顶级菜单（parentId=0）在树里有个可选父节点；
// check-strictly 允许选任意层级；下拉未排除当前节点及子孙，后端只拦「上级选自己」，改层级时需人工避免倒挂
function getTreeselect() {
  menuOptions.value = []
  listMenu().then(response => {
    const menu = { menuId: 0, menuName: "主类目", children: [] }
    menu.children = proxy.handleTree(response.data, "menuId")
    menuOptions.value.push(menu)
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
// 默认值等价于「新增一个目录」：类型 M、parentId=0、非外链、使用缓存、显示、正常
function reset() {
  form.value = {
    menuId: undefined,
    parentId: 0,
    menuName: undefined,
    icon: undefined,
    menuType: "M",
    orderNum: undefined,
    isFrame: "1",
    isCache: "0",
    visible: "0",
    status: "0"
  }
  proxy.resetForm("menuRef")
}

/** 展示下拉图标 */
// 输入框失焦时重置图标面板的选中态，避免再次打开仍高亮上一次的选择
function showSelectIcon() {
  iconSelectRef.value.reset()
}

/** 选择图标 */
function selected(name) {
  form.value.icon = name
}

/** 搜索按钮操作 */
function handleQuery() {
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 新增按钮操作 */
// 行内「新增」以当前菜单为上级；工具栏入口拿不到 menuId，落到 else 分支挂为顶层(parentId=0)
function handleAdd(row) {
  reset()
  getTreeselect()
  if (row != null && row.menuId) {
    form.value.parentId = row.menuId
  } else {
    form.value.parentId = 0
  }
  open.value = true
  title.value = "添加菜单"
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
  })
}

/** 修改按钮操作 */
// 先 reset 再拉详情：详情整体替换 form，避免上一次编辑的残留字段被带到这次提交；
// 注意 getTreeselect() 内部没有 return 请求 Promise，这里的 await 并不会等下拉树就绪
async function handleUpdate(row) {
  reset()
  await getTreeselect()
  getMenu(row.menuId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改菜单"
  })
}

/** 提交按钮 */
// 用 menuId 是否存在区分新增/修改（reset 后为 undefined）；成功后整棵树重拉，不在本地插入节点
function submitForm() {
  proxy.$refs["menuRef"].validate(valid => {
    if (valid) {
      if (form.value.menuId != undefined) {
        updateMenu(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMenu(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
// 不可逆，先二次确认；有子菜单或已分配给角色时后端会拒绝删除并提示原因
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.menuName + '"的数据项?').then(function() {
    return delMenu(row.menuId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

// 页面挂载即拉取整棵菜单树（接口无分页参数）
getList()
</script>
