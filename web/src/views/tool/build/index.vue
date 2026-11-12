<template>
  <div class="container">
    <!-- 左栏组件库：物料模板按输入型/选择型/布局型分三组渲染；每个 draggable 都是 pull:'clone'，
         拖出的是副本，左侧列表本身不会被消耗；点选则直接调 addComponent 追加 -->
    <div class="left-board">
      <div class="logo-wrapper">
        <div class="logo">
          <img :src="logo" alt="logo"> Form Generator
        </div>
      </div>
      <el-scrollbar class="left-scrollbar">
        <div class="components-list">
          <div class="components-title">
            <svg-icon icon-class="component" />输入型组件
          </div>
          <draggable class="components-draggable" :list="inputComponents"
            :group="{ name: 'componentsGroup', pull: 'clone', put: false }" :clone="cloneComponent"
            draggable=".components-item" :sort="false" @end="onEnd" item-key="label">
            <template #item="{ element, index }">
              <div :key="index" class="components-item" @click="addComponent(element)">
                <div class="components-body">
                  <svg-icon :icon-class="element.tagIcon" />
                  {{ element.label }}
                </div>
              </div>
            </template>
          </draggable>
          <div class="components-title">
            <svg-icon icon-class="component" />选择型组件
          </div>
          <draggable class="components-draggable" :list="selectComponents"
            :group="{ name: 'componentsGroup', pull: 'clone', put: false }" :clone="cloneComponent"
            draggable=".components-item" :sort="false" @end="onEnd" item-key="label">
            <template #item="{ element, index }">
              <div :key="index" class="components-item" @click="addComponent(element)">
                <div class="components-body">
                  <svg-icon :icon-class="element.tagIcon" />
                  {{ element.label }}
                </div>
              </div>
            </template>
          </draggable>
          <div class="components-title">
            <svg-icon icon-class="component" /> 布局型组件
          </div>
          <draggable class="components-draggable" :list="layoutComponents"
            :group="{ name: 'componentsGroup', pull: 'clone', put: false }" :clone="cloneComponent"
            draggable=".components-item" :sort="false" @end="onEnd" item-key="label">
            <template #item="{ element, index }">
              <div :key="index" class="components-item" @click="addComponent(element)">
                <div class="components-body">
                  <svg-icon :icon-class="element.tagIcon" />
                  {{ element.label }}
                </div>
              </div>
            </template>
          </draggable>
        </div>
      </el-scrollbar>
    </div>
    <!-- 中栏画布：顶部操作条 + 可拖拽排序的表单区域，画布内容即 drawingList，导出代码全部由它生成 -->
    <div class="center-board">
      <!-- 操作条：导出 vue 文件 / 复制代码 / 清空画布，前两者都要先选生成类型 -->
      <div class="action-bar">
        <el-button icon="Download" type="primary" text @click="download">
          导出vue文件
        </el-button>
        <el-button class="copy-btn-main" icon="DocumentCopy" type="primary" text @click="copy">
          复制代码
        </el-button>
        <el-button class="delete-btn" icon="Delete" text @click="empty" type="danger">
          清空
        </el-button>
      </div>
      <!-- 画布区：里面的 el-form 只作预览容器，尺寸/标签位置等属性全部取自 formConf -->
      <el-scrollbar class="center-scrollbar">
        <el-row class="center-board-row" :gutter="formConf.gutter">
          <el-form :size="formConf.size" :label-position="formConf.labelPosition" :disabled="formConf.disabled"
            :label-width="formConf.labelWidth + 'px'">
            <draggable class="drawing-board" :list="drawingList" :animation="340" group="componentsGroup"
              item-key="label">
              <template #item="{ element, index }">
                <draggable-item :key="element.renderKey" :drawing-list="drawingList" :element="element" :index="index"
                  :active-id="activeId" :form-conf="formConf" @activeItem="activeFormItem" @copyItem="drawingItemCopy"
                  @deleteItem="drawingItemDelete" />
              </template>
            </draggable>
            <!-- 空画布占位，画布有组件时隐藏 -->
            <div v-show="!drawingList.length" class="empty-info">
              从左侧拖入或点选组件进行表单设计
            </div>
          </el-form>
        </el-row>
      </el-scrollbar>
    </div>
    <!-- 右栏属性面板：编辑当前选中组件（activeData）或表单全局配置（formConf）；切换组件类型时向上抛 tag-change -->
    <right-panel :active-data="activeData" :form-conf="formConf" :show-field="!!drawingList.length"
      @tag-change="tagChange" />

    <!-- 生成类型弹窗：复制与导出两个入口共用，确认后回到 generate() 按 operationType 分派 -->
    <code-type-dialog v-model="dialogVisible" title="选择生成类型" :showFileName="showFileName" @confirm="generate" />
    <!-- ClipboardJS 的隐藏触发节点：无可见 UI，点击它才会执行 onMounted 里注册的 text 回调生成代码 -->
    <input id="copyNode" type="hidden">
  </div>
</template>

<script setup>
/**
 * 表单构建器（表单设计器）主页面。
 *
 * 三栏结构：左栏组件库 → 中栏画布 → 右栏属性面板。画布数据 drawingList 的元素结构由
 * @/utils/generator/config 定义，layout 区分 colFormItem（单个表单项）与 rowFormItem（栅格容器）。
 * 生成链路：AssembleFormData() 汇总 fields + formConf → makeUpHtml/makeUpJs/makeUpCss 拼装 →
 * vueTemplate/vueScript/cssStyle 包装成 SFC → js-beautify 统一格式，最后复制或另存为 .vue。
 * 本页是纯前端代码生成工具：不请求后端接口，也没有权限点，能否进入由菜单/路由权限决定。
 */
import draggable from "vuedraggable/dist/vuedraggable.common"
import ClipboardJS from 'clipboard'
import beautifier from 'js-beautify'
import logo from '@/assets/logo/logo.png'
import { inputComponents, selectComponents, layoutComponents, formConf as formConfData } from '@/utils/generator/config'
import { beautifierConf } from '@/utils/index'
import drawingDefalut from '@/utils/generator/drawingDefalut'
import { makeUpHtml, vueTemplate, vueScript, cssStyle } from '@/utils/generator/html'
import { makeUpJs } from '@/utils/generator/js'
import { makeUpCss } from '@/utils/generator/css'
import Download from '@/plugins/download'
import { ElNotification } from 'element-plus'
import DraggableItem from './DraggableItem'
import RightPanel from './RightPanel'
import CodeTypeDialog from './CodeTypeDialog'
import { onMounted, watch } from 'vue'

// 画布数据：初始内容取自模板默认配置（自带一个示例项），是整个设计器唯一的数据源
const drawingList = ref(drawingDefalut)
// 通过 proxy 访问全局挂载的 $modal（RuoYi 的确认框/消息提示插件）
const { proxy } = getCurrentInstance()
// 生成弹窗的显隐、是否要求输入文件名（仅导出时）、触发来源（copy=复制代码 / download=导出文件）
const dialogVisible = ref(false)
const showFileName = ref(false)
const operationType = ref('')
// 组件编号自增序列：formId、v-model 后缀、容器 componentName 后缀都取自它。
// 清空画布时会复位到 100，保证新组件的 v-model 不会和旧数据重名
const idGlobal = ref(100)
// 当前选中项及其 formId：activeData 驱动右侧属性面板，activeId 驱动画布高亮和悬浮操作按钮
const activeData = ref(drawingDefalut[0])
const activeId = ref(drawingDefalut[0].formId)
// generateConf 保存弹窗确认时的 { type, fileName }；formData 是生成代码时用的数据快照
const generateConf = ref(null)
const formData = ref({})
// 表单级配置，右栏「表单属性」直接双向编辑这个对象，导出时作为 el-form 的属性
const formConf = ref(formConfData)
// oldActiveId：上一次选中的 formId，供 label 监听判断是否仍在同一个组件内；
// tempActiveData：cloneComponent 的中转结果（克隆后会按 layout 再加工一次）
let oldActiveId
let tempActiveData

// 切换选中项：两个 ref 必须同步改，否则会出现「高亮在 A、属性面板改的是 B」
function activeFormItem(element) {
  activeData.value = element
  activeId.value = element.formId
}
// 复制代码入口：不需要文件名，operationType 决定弹窗确认后走 execCopy
function copy() {
  dialogVisible.value = true
  showFileName.value = false
  operationType.value = 'copy'
}
// 导出文件入口：需要文件名，所以打开弹窗时把 showFileName 置 true
function download() {
  dialogVisible.value = true
  showFileName.value = true
  operationType.value = 'download'
}
// 清空画布不可撤销，必须二次确认。确认后只清 drawingList 并把编号复位，
// formConf（表单名/尺寸/标签宽度等）属于用户的显式配置，清空内容时保留
function empty() {
  proxy.$modal.confirm('确定要清空所有组件吗？', '提示', { type: 'warning' }).then(() => {
      idGlobal.value = 100
      drawingList.value = []
    }
  )
}

// sortablejs 的拖拽结束回调：from !== to 说明是「左栏 → 画布」的跨列表拖入，
// 此时把刚克隆出的 tempActiveData 设为选中项；画布内部重新排序则保持原选中项不动
function onEnd(obj, a) {
  if (obj.from !== obj.to) {
    activeData.value = tempActiveData
    activeId.value = idGlobal.value
  }
}

// 点击左栏组件直接追加到画布末尾，与拖拽共用 cloneComponent，保证编号与默认属性一致
function addComponent(item) {
  const clone = cloneComponent(item)
  drawingList.value.push(clone)
  activeFormItem(clone)
}

// 克隆物料模板：用 JSON 序列化做深拷贝，组件描述是纯数据（无函数、无循环引用），
// 拷贝后画布对象与 config 里的模板完全断开，后续修改不会污染组件库默认值。
// 再补三个运行时字段：formId（唯一编号，选中/查找用）、renderKey（变更后强制重建组件）、
// v-model 名（field + 序号，避免多个同类型组件生成同名字段）；容器型改用 row + 序号且没有 label
function cloneComponent(origin) {
  const clone = JSON.parse(JSON.stringify(origin))
  clone.formId = ++idGlobal.value
  clone.span = formConf.value.span
  clone.renderKey = +new Date() // 改变renderKey后可以实现强制更新组件
  if (!clone.layout) clone.layout = 'colFormItem'
  if (clone.layout === 'colFormItem') {
    clone.vModel = `field${idGlobal.value}`
    clone.placeholder !== undefined && (clone.placeholder += clone.label)
    tempActiveData = clone
  } else if (clone.layout === 'rowFormItem') {
    delete clone.label
    clone.componentName = `row${idGlobal.value}`
    clone.gutter = formConf.value.gutter
    tempActiveData = clone
  }
  return tempActiveData
}

// 复制画布节点：重新生成编号后插入同一父级末尾，并把选中项切到副本上
function drawingItemCopy(item, parent) {
  let clone = JSON.parse(JSON.stringify(item))
  clone = createIdAndKey(clone)
  parent.push(clone)
  activeFormItem(clone)
}


// 递归重置副本及整棵子树的编号与 v-model 名：不这样做子级会和源对象撞号，
// 导出的表单里会出现两个绑同一字段的控件
function createIdAndKey(item) {
  item.formId = ++idGlobal.value
  item.renderKey = +new Date()
  if (item.layout === 'colFormItem') {
    item.vModel = `field${idGlobal.value}`
  } else if (item.layout === 'rowFormItem') {
    item.componentName = `row${idGlobal.value}`
  }
  if (Array.isArray(item.children)) {
    item.children = item.children.map(childItem => createIdAndKey(childItem))
  }
  return item
}

// 删除后等 v-for 更新完成（nextTick）再补选画布最后一项，避免 activeData 指向已删除对象；
// 全部删空时不设置选中项，右栏「组件属性」会随 showField 一起隐藏
function drawingItemDelete(index, parent) {
  parent.splice(index, 1)
  nextTick(() => {
    const len = drawingList.value.length
    if (len) {
      activeFormItem(drawingList.value[len - 1])
    }
  })
}

// 切换组件类型：按「同名且同类型」的规则尽量保留原属性，其余丢弃。
// 先删掉 tag/tagIcon/document 这类与具体类型强绑定的字段，避免把 el-input 的配置带到 el-select；
// 随后保持原 formId/vModel/span 不变，让它在画布中的位置与字段名都不变
function tagChange(newTag) {
  newTag = cloneComponent(newTag)
  newTag.vModel = activeData.value.vModel
  newTag.formId = activeId.value
  newTag.span = activeData.value.span
  delete activeData.value.tag
  delete activeData.value.tagIcon
  delete activeData.value.document
  Object.keys(newTag).forEach(key => {
    if (activeData.value[key] !== undefined
      && typeof activeData.value[key] === typeof newTag[key]) {
      newTag[key] = activeData.value[key]
    }
  })
  activeData.value = newTag
  updateDrawingList(newTag, drawingList.value)
}


// 就地替换：先在本层按 formId 查找，找不到再下钻 rowFormItem 的 children（容器可多层嵌套）
function updateDrawingList(newTag, list) {
  const index = list.findIndex(item => item.formId === activeId.value)
  if (index > -1) {
    list.splice(index, 1, newTag)
  } else {
    list.forEach(item => {
      if (Array.isArray(item.children)) updateDrawingList(newTag, item.children)
    })
  }
}
// 弹窗确认后的分派：nextTick 等弹窗收起、DOM 更新完再执行，避免生成/下载动作与关闭动画抢时序
function generate(data) {
  generateConf.value = data
  nextTick(() => {
    switch (operationType.value) {
      case 'copy':
        execCopy(data)
        break
      case 'download':
        execDownload(data)
        break
      default:
        break
    }
  })
}

// 以 text/plain 包成 Blob 交给 Download 插件保存，文件名取弹窗里输入的值
function execDownload(data) {
  const codeStr = generateCode()
  const blob = new Blob([codeStr], { type: 'text/plain;charset=utf-8' })
  Download.saveAs(blob, data.fileName)
}

// 不直接调剪贴板 API，而是点击隐藏节点，由 ClipboardJS 的 text 回调现算代码：
// 复制与导出因此共用同一套生成逻辑，也能借它的降级方案兼容受限环境
function execCopy(data) {
  document.getElementById('copyNode').click()
}
// 生成前汇总数据：fields 是画布深拷贝（生成过程会归一化类型，不能写回画布对象），外加 formConf
function AssembleFormData() {
  formData.value = { fields: JSON.parse(JSON.stringify(drawingList.value)), ...formConf.value }
}
// 生成 SFC 文本：type 决定页面还是弹窗形态；html/script/css 三段各自拼装后拼在一起统一美化
function generateCode() {
  const { type } = generateConf.value
  AssembleFormData()
  const script = vueScript(makeUpJs(formData.value, type))
  const html = vueTemplate(makeUpHtml(formData.value, type))
  const css = cssStyle(makeUpCss(formData.value))
  return beautifier.html(html + script + css, beautifierConf.html)
}
// 标题改动时同步占位提示：把旧标题从 placeholder 里替换成新标题，保持「请输入+标题」的默认文案。
// 三个前置条件缺一不可：组件有 placeholder、是表单控件（有 tag）、且没有切换过选中项，
// 否则切换选中项时会把上一个组件的文案改到新组件上
watch(() => activeData.value.label, (val, oldVal) => {
  if (
    activeData.value.placeholder === undefined
    || !activeData.value.tag
    || oldActiveId !== activeId.value
  ) {
    return
  }
  activeData.value.placeholder = activeData.value.placeholder.replace(oldVal, '') + val
})
// 记录上一次选中的 formId（immediate 保证首屏就有值），供上面的 label 监听判断是否跨组件
watch(activeId, (val) => {
  oldActiveId = val
}, { immediate: true })

// 初始化剪贴板：text 回调里现算待复制的代码字符串，写入动作交给 ClipboardJS；
// error 一般是浏览器不支持或被安全策略拦截，这里只提示失败，不抛异常
onMounted(() => {
  const clipboard = new ClipboardJS('#copyNode', {
    text: trigger => {
      const codeStr = generateCode()
      ElNotification({ title: '成功', message: '代码已复制到剪切板，可粘贴。', type: 'success' })
      return codeStr
    }
  })
  clipboard.on('error', e => {
    proxy.$modal.msgError('代码复制失败')
  })
})
</script>

<style lang='scss'>
$lighterBlue: #409EFF;

/* 样式块未加 scoped，所有规则都挂在 .container 下，靠这层前缀收敛作用范围；
   高度按「视口 - 顶部导航 50px - 内容区上下留白 40px」计算，让滚动条只出现在内部面板上 */
.container {
  position: relative;
  width: 100%;
  background-color: var(--el-bg-color-overlay);
  height: calc(100vh - 50px - 40px);
  overflow: hidden;

  .left-board {
    width: 260px;
    position: absolute;
    left: 0;
    top: 0;
    height: calc(100vh - 50px - 40px);

    .logo-wrapper {
      position: relative;
      height: 42px;
      border-bottom: 1px solid var(--el-border-color-extra-light);
      box-sizing: border-box;

      .logo {
        position: absolute;
        left: 12px;
        top: 6px;
        line-height: 30px;
        color: #00afff;
        font-weight: 600;
        font-size: 17px;
        white-space: nowrap;

        >img {
          width: 30px;
          height: 30px;
          vertical-align: top;
        }

        .github {
          display: inline-block;
          vertical-align: sub;
          margin-left: 15px;

          >img {
            height: 22px;
          }
        }
      }
    }

    .left-scrollbar {
      .el-scrollbar__wrap {
        box-sizing: border-box;
        overflow-x: hidden !important;
        margin-bottom: 0 !important;

        .components-list {
          padding: 8px;
          box-sizing: border-box;
          height: 100%;

          .components-title {
            font-size: 14px;
            // color: #222;
            margin: 6px 2px;

            .svg-icon {
              // color: #666;
              font-size: 18px;
              margin-right: 5px;
            }
          }

          .components-draggable {
            padding-bottom: 20px;

            .components-item {
              display: inline-block;
              width: 48%;
              margin: 1%;
              transition: transform 0ms !important;

              .components-body {
                padding: 8px 10px;
                background: var(--el-border-color-extra-light);
                font-size: 12px;
                cursor: move;
                border: 1px dashed var(--el-border-color-extra-light);
                border-radius: 3px;

                .svg-icon {
                  // color: #777;
                  font-size: 15px;
                  margin-right: 5px;
                }

                &:hover {
                  border: 1px dashed #787be8;
                  color: #787be8;

                  .svg-icon {
                    color: #787be8;
                  }
                }
              }
            }
          }


        }
      }
    }
  }

  .center-board {
    height: calc(100vh - 50px - 40px);
    width: auto;
    margin: 0 350px 0 260px;
    box-sizing: border-box;

    .action-bar {
      position: relative;
      height: 42px;
      padding: 0 15px;
      box-sizing: border-box;
      ;
      border: 1px solid var(--el-border-color-extra-light);
      border-top: none;
      border-left: none;
      display: flex;
      align-items: center;
      justify-content: flex-end;

      u .delete-btn {
        color: #F56C6C;
      }
    }

    .center-scrollbar {
      height: calc(100vh - 50px - 40px - 42px);
      overflow: hidden;
      border-left: 1px solid var(--el-border-color-extra-light);
      border-right: 1px solid var(--el-border-color-extra-light);
      box-sizing: border-box;

      .el-scrollbar__view {
        overflow-x: hidden;
      }

      .center-board-row {
        padding: 12px 12px 15px 12px;
        box-sizing: border-box;

        &>.el-form {
          // 69 = 12+15+42
          height: calc(100vh - 50px - 40px - 69px);
          flex: 1;

          .drawing-board {
            height: 100%;
            position: relative;

            .components-body {
              padding: 0;
              margin: 0;
              font-size: 0;
            }

            /* 拖拽经过时的占位：顶部 3px 横线是伪元素画出的插入位置指示，z-index 保证盖在组件之上 */
            .sortable-ghost {
              position: relative;
              display: block;
              overflow: hidden;

              &::before {
                content: " ";
                position: absolute;
                left: 0;
                right: 0;
                top: 0;
                height: 3px;
                background: rgb(89, 89, 223);
                z-index: 2;
              }
            }

            .components-item.sortable-ghost {
              width: 100%;
              height: 60px;
              background: var(--el-border-color-extra-light);
            }

            .active-from-item {
              &>.el-form-item {
                background: var(--el-border-color-extra-light);
                border-radius: 6px;
              }

              &>.drawing-item-copy,
              &>.drawing-item-delete {
                display: initial;
              }

              &>.component-name {
                color: $lighterBlue;
              }

              .el-input__wrapper {
                box-shadow: 0 0 0 1px var(--el-input-hover-border-color) inset;
              }
            }

            .el-form-item {
              margin-bottom: 15px;
            }
          }

          .drawing-item {
            position: relative;
            cursor: move;

            &.unfocus-bordered:not(.activeFromItem)>div:first-child {
              border: 1px dashed #ccc;
            }

            .el-form-item {
              padding: 12px 10px;
            }
          }

          .drawing-row-item {
            position: relative;
            cursor: move;
            box-sizing: border-box;
            border: 1px dashed #ccc;
            border-radius: 3px;
            padding: 0 2px;
            margin-bottom: 15px;

            .drawing-row-item {
              margin-bottom: 2px;
            }

            .el-col {
              margin-top: 22px;
            }

            .el-form-item {
              margin-bottom: 0;
            }

            .drag-wrapper {
              min-height: 80px;
              flex: 1;
              display: flex;
              flex-wrap: wrap;
            }

            &.active-from-item {
              border: 1px dashed $lighterBlue;
            }

            .component-name {
              position: absolute;
              top: 0;
              left: 0;
              font-size: 12px;
              color: #bbb;
              display: inline-block;
              padding: 0 6px;
            }
          }

          .drawing-item,
          .drawing-row-item {
            &:hover {
              &>.el-form-item {
                background: var(--el-border-color-extra-light);
                border-radius: 6px;
              }

              &>.drawing-item-copy,
              &>.drawing-item-delete {
                display: initial;
              }
            }

            &>.drawing-item-copy,
            &>.drawing-item-delete {
              display: none;
              position: absolute;
              top: -10px;
              width: 22px;
              height: 22px;
              line-height: 22px;
              text-align: center;
              border-radius: 50%;
              font-size: 12px;
              border: 1px solid;
              cursor: pointer;
              z-index: 1;
            }

            &>.drawing-item-copy {
              right: 56px;
              border-color: $lighterBlue;
              color: $lighterBlue;
              background: #fff;

              &:hover {
                background: $lighterBlue;
                color: #fff;
              }
            }

            &>.drawing-item-delete {
              right: 24px;
              border-color: #F56C6C;
              color: #F56C6C;
              background: #fff;

              &:hover {
                background: #F56C6C;
                color: #fff;
              }
            }
          }

          /* 空画布提示：绝对定位到中部、左右撑满后文字居中，脱离文档流不影响画布高度 */
          .empty-info {
            position: absolute;
            top: 46%;
            left: 0;
            right: 0;
            text-align: center;
            font-size: 18px;
            color: #ccb1ea;
            letter-spacing: 4px;
          }

        }
      }
    }
  }
}
</style>
