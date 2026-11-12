<template>
  <!-- 代码生成「生成信息」子表单：维护模板类型、前端类型、包/模块/业务/功能命名、上级菜单与生成方式 -->
  <!-- 四个命名项共同决定生成文件的包路径、接口 URL 前缀与类注释，填错会直接影响生成结果，因此全部必填 -->
  <el-form ref="genInfoForm" :model="info" :rules="rules" label-width="150px">
    <el-row>
      <!-- 生成模板决定下方扩展区域是否出现：tree 显示树表三个字段，sub 显示主子表关联配置 -->
      <el-col :span="12">
        <el-form-item prop="tplCategory">
          <template #label>生成模板</template>
          <el-select v-model="info.tplCategory" @change="tplSelectChange">
            <el-option label="单表（增删改查）" value="crud" />
            <el-option label="树表（增删改查）" value="tree" />
            <el-option label="主子表（增删改查）" value="sub" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="tplWebType">
          <template #label>前端类型</template>
          <el-select v-model="info.tplWebType">
            <el-option label="Vue2 Element UI 模版" value="element-ui" />
            <el-option label="Vue3 Element Plus 模版" value="element-plus" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="packageName">
          <template #label>
            生成包路径
            <el-tooltip content="生成在哪个java包下，例如 com.ruoyi.system" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="info.packageName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="moduleName">
          <template #label>
            生成模块名
            <el-tooltip content="可理解为子系统名，例如 system" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="info.moduleName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="businessName">
          <template #label>
            生成业务名
            <el-tooltip content="可理解为功能英文名，例如 user" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="info.businessName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="functionName">
          <template #label>
            生成功能名
            <el-tooltip content="用作类描述，例如 用户" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="info.functionName" />
        </el-form-item>
      </el-col>

      <!-- 生成方式与列表页「生成」按钮的分支联动：0 下载 zip 压缩包，1 由服务端按下面填写的自定义路径直接写盘 -->
      <el-col :span="12">
        <el-form-item prop="genType">
          <template #label>
            生成代码方式
            <el-tooltip content="默认为zip压缩包下载，也可以自定义生成路径" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-radio v-model="info.genType" value="0">zip压缩包</el-radio>
          <el-radio v-model="info.genType" value="1">自定义路径</el-radio>
        </el-form-item>
      </el-col>

      <!-- 上级菜单用于生成菜单 SQL 时指定父节点；check-strictly 关闭父子联动，允许挂到任意层级，
               否则选中父级会自动勾选全部子菜单 -->
      <el-col :span="12">
        <el-form-item>
          <template #label>
            上级菜单
            <el-tooltip content="分配到指定菜单下，例如 系统管理" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-tree-select
            v-model="info.parentMenuId"
            :data="menuOptions"
            :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
            value-key="menuId"
            placeholder="请选择系统菜单"
            check-strictly
          />
        </el-form-item>
      </el-col>

      <!-- 自定义路径仅在生成方式选「自定义路径」时出现；留空表示生成到当前 Web 项目下，
               所以 rules 里没有把它设为必填 -->
      <el-col :span="24" v-if="info.genType == '1'">
        <el-form-item prop="genPath">
          <template #label>
            自定义路径
            <el-tooltip content="填写磁盘绝对路径，若不填写，则生成到当前Web项目下" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="info.genPath">
            <template #append>
              <el-dropdown>
                <el-button type="primary">
                  最近路径快速选择
                  <i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="info.genPath = '/'">恢复默认的生成基础路径</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>
    
    <template v-if="info.tplCategory == 'tree'">
      <!-- 树表三个字段都从当前表的已有字段中选择：编码字段作节点 id，父编码字段关联上级，名称字段作展示文本 -->
      <h4 class="form-header">其他信息</h4>
      <el-row v-show="info.tplCategory == 'tree'">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树编码字段
              <el-tooltip content="树显示的编码字段名， 如：dept_id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="info.treeCode" placeholder="请选择">
              <el-option
                v-for="(column, index) in info.columns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树父编码字段
              <el-tooltip content="树显示的父编码字段名， 如：parent_Id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="info.treeParentCode" placeholder="请选择">
              <el-option
                v-for="(column, index) in info.columns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树名称字段
              <el-tooltip content="树节点的显示名称字段名， 如：dept_name" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="info.treeName" placeholder="请选择">
              <el-option
                v-for="(column, index) in info.columns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </template>

    <template v-if="info.tplCategory == 'sub'">
      <!-- 主子表：先选子表，再从该子表的字段中选外键；切换子表会清空外键，避免残留上一个子表的列名 -->
      <h4 class="form-header">关联信息</h4>
      <el-row>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              关联子表的表名
              <el-tooltip content="关联子表的表名， 如：sys_user" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="info.subTableName" placeholder="请选择" @change="subSelectChange">
              <el-option
                v-for="(table, index) in tables"
                :key="index"
                :label="table.tableName + '：' + table.tableComment"
                :value="table.tableName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              子表关联的外键名
              <el-tooltip content="子表关联的外键名， 如：user_id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="info.subTableFkName" placeholder="请选择">
              <el-option
                v-for="(column, index) in subColumns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </template>

  </el-form>
</template>

<script setup>
// 上级菜单下拉需要全量菜单，listMenu 返回平铺列表后由 proxy.handleTree 按 parentId 组装成树
import { listMenu } from "@/api/system/menu"

// subColumns 是「子表外键」下拉的候选列，随选中的子表变化；menuOptions 是上级菜单树
const subColumns = ref([])
const menuOptions = ref([])
const { proxy } = getCurrentInstance()

// info 是父组件持有的生成配置对象（父子共用同一引用，改属性即写回）；tables 为可关联的子表及其字段列表
const props = defineProps({
  info: {
    type: Object,
    default: null
  },
  tables: {
    type: Array,
    default: null
  }
})

// 表单校验
// 这里没有校验 genPath：选「自定义路径」但留空时表示生成到当前 Web 项目下，由后端兜底
const rules = ref({
  tplCategory: [{ required: true, message: "请选择生成模板", trigger: "blur" }],
  packageName: [{ required: true, message: "请输入生成包路径", trigger: "blur" }],
  moduleName: [{ required: true, message: "请输入生成模块名", trigger: "blur" }],
  businessName: [{ required: true, message: "请输入生成业务名", trigger: "blur" }],
  functionName: [{ required: true, message: "请输入生成功能名", trigger: "blur" }]
})

// 切换关联子表后清空外键：外键必须是新子表里的字段，旧值一定无效
function subSelectChange(value) {
  props.info.subTableFkName = ""
}

// 模板切回单表/树表时清掉主子表配置，避免提交时残留 subTableName / subTableFkName
function tplSelectChange(value) {
  if (value !== "sub") {
    props.info.subTableName = ""
    props.info.subTableFkName = ""
  }
}

// 按表名在 tables 中查出该子表的字段列表，作为外键下拉项；未命中时保留上一次的结果（不清空）
function setSubTableColumns(value) {
  for (var item in props.tables) {
    const name = props.tables[item].tableName
    if (value === name) {
      subColumns.value = props.tables[item].columns
      break
    }
  }
}

/** 查询菜单下拉树结构 */
// 接口返回平铺菜单列表，handleTree 以 menuId 为主键、按 parentId 组装成树送给 el-tree-select
function getMenuTreeselect() {
  listMenu().then(response => {
    menuOptions.value = proxy.handleTree(response.data, "menuId")
  })
}

onMounted(() => {
  getMenuTreeselect()
})

// info 是父组件传入的对象引用，直接 watch 其属性（getter 形式）才能感知子表切换；
// 变化时同步刷新外键候选列
watch(() => props.info.subTableName, val => {
  setSubTableColumns(val)
})

// 兼容历史数据：tplWebType 变为空字符串时回填 element-plus，保证「前端类型」始终有明确取值。
// 判断条件是严格等于 ''，为 undefined / null 时不会触发回填
watch(() => props.info.tplWebType, val => {
  if (val === '') {
    props.info.tplWebType = "element-plus"
  }
})
</script>
