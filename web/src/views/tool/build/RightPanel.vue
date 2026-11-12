<template>
  <div class="right-board">
    <el-tabs v-model="currentTab" stretch class="center-tabs">
      <el-tab-pane label="组件属性" name="field" />
      <el-tab-pane label="表单属性" name="form" />
    </el-tabs>
    <!-- 属性面板主体：右上角是组件文档入口，下面按 Tab 切换「组件属性」与「表单属性」两个表单 -->
    <div class="field-box">
      <a class="document-link" target="_blank" :href="documentLink" title="查看组件文档">
        <el-icon>
          <Link />
        </el-icon>
      </a>
      <el-scrollbar class="right-scrollbar">
        <!-- 组件属性 -->
        <el-form v-show="currentTab === 'field' && showField" size="default" label-width="90px" label-position="top"
          style="">
          <!-- 只有 changeTag=true 的物料允许换类型；替换由父级 tagChange 完成，会尽量保留同名属性 -->
          <el-form-item v-if="activeData.changeTag" label="组件类型">
            <el-select v-model="activeData.tagIcon" placeholder="请选择组件类型" :style="{ width: '100%' }" @change="tagChange">
              <el-option-group v-for="group in tagList" :key="group.label" :label="group.label">
                <el-option v-for="item in group.options" :key="item.label" :label="item.label" :value="item.tagIcon">
                  <svg-icon class="node-icon" :icon-class="item.tagIcon" style="margin-right: 10px;" />
                  <span> {{ item.label }}</span>
                </el-option>
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.vModel !== undefined" label="字段名">
            <el-input v-model="activeData.vModel" placeholder="请输入字段名（v-model）" />
          </el-form-item>
          <el-form-item v-if="activeData.componentName !== undefined" label="组件名">
            {{ activeData.componentName }}
          </el-form-item>
          <el-form-item v-if="activeData.label !== undefined" label="标题">
            <el-input v-model="activeData.label" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item v-if="activeData.placeholder !== undefined" label="占位提示">
            <el-input v-model="activeData.placeholder" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="activeData['start-placeholder'] !== undefined" label="开始占位">
            <el-input v-model="activeData['start-placeholder']" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="activeData['end-placeholder'] !== undefined" label="结束占位">
            <el-input v-model="activeData['end-placeholder']" placeholder="请输入占位提示" />
          </el-form-item>
          <!-- 栅格 1~24（12 处有刻度）；改动会写回 formConf.span，作为之后新增组件的默认宽度 -->
          <el-form-item v-if="activeData.span !== undefined" label="表单栅格">
            <el-slider v-model="activeData.span" :max="24" :min="1" :marks="{ 12: '' }" @change="spanChange" />
          </el-form-item>
          <el-form-item v-if="activeData.layout === 'rowFormItem'" label="栅格间隔">
            <el-input-number v-model="activeData.gutter" :min="0" placeholder="栅格间隔" />
          </el-form-item>

          <el-form-item v-if="activeData.justify !== undefined" label="水平排列">
            <el-select v-model="activeData.justify" placeholder="请选择水平排列" :style="{ width: '100%' }">
              <el-option v-for="(item, index) in justifyOptions" :key="index" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.align !== undefined" label="垂直排列">
            <el-radio-group v-model="activeData.align">
              <el-radio-button label="top" />
              <el-radio-button label="middle" />
              <el-radio-button label="bottom" />
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.labelWidth !== undefined" label="标签宽度">
            <el-input v-model.number="activeData.labelWidth" type="number" placeholder="请输入标签宽度" />
          </el-form-item>
          <el-form-item v-if="activeData.style && activeData.style.width !== undefined" label="组件宽度">
            <el-input v-model="activeData.style.width" placeholder="请输入组件宽度" clearable />
          </el-form-item>
          <!-- 默认值：输入框里是文本形态，输入时由 onDefaultValueInput 按数组/布尔/数字回写 -->
          <el-form-item v-if="activeData.vModel !== undefined" label="默认值">
            <el-input :value="setDefaultValue(activeData.defaultValue)" placeholder="请输入默认值"
              @input="onDefaultValueInput" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-checkbox-group'" label="至少应选">
            <el-input-number :value="activeData.min" :min="0" placeholder="至少应选"
              @input="$set(activeData, 'min', $event ? $event : undefined)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-checkbox-group'" label="最多可选">
            <el-input-number :value="activeData.max" :min="0" placeholder="最多可选"
              @input="$set(activeData, 'max', $event ? $event : undefined)" />
          </el-form-item>
          <el-form-item v-if="activeData.prepend !== undefined" label="前缀">
            <el-input v-model="activeData.prepend" placeholder="请输入前缀" />
          </el-form-item>
          <el-form-item v-if="activeData.append !== undefined" label="后缀">
            <el-input v-model="activeData.append" placeholder="请输入后缀" />
          </el-form-item>
          <el-form-item v-if="activeData['prefix-icon'] !== undefined" label="前图标">
            <el-input v-model="activeData['prefix-icon']" placeholder="请输入前图标名称">
              <template #append>
                <el-button icon="Pointer" @click="openIconsDialog('prefix-icon')">
                  选择
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData['suffix-icon'] !== undefined" label="后图标">
            <el-input v-model="activeData['suffix-icon']" placeholder="请输入后图标名称">
              <template #append>
                <el-button icon="Pointer" @click="openIconsDialog('suffix-icon')">
                  选择
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="选项分隔符">
            <el-input v-model="activeData.separator" placeholder="请输入选项分隔符" />
          </el-form-item>
          <el-form-item v-if="activeData.autosize !== undefined" label="最小行数">
            <el-input-number v-model="activeData.autosize.minRows" :min="1" placeholder="最小行数" />
          </el-form-item>
          <el-form-item v-if="activeData.autosize !== undefined" label="最大行数">
            <el-input-number v-model="activeData.autosize.maxRows" :min="1" placeholder="最大行数" />
          </el-form-item>
          <el-form-item v-if="activeData.min !== undefined" label="最小值">
            <el-input-number v-model="activeData.min" placeholder="最小值" />
          </el-form-item>
          <el-form-item v-if="activeData.max !== undefined" label="最大值">
            <el-input-number v-model="activeData.max" placeholder="最大值" />
          </el-form-item>
          <el-form-item v-if="activeData.step !== undefined" label="步长">
            <el-input-number v-model="activeData.step" placeholder="步数" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="精度">
            <el-input-number v-model="activeData.precision" :min="0" placeholder="精度" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="按钮位置">
            <el-radio-group v-model="activeData['controls-position']">
              <el-radio-button label="">
                默认
              </el-radio-button>
              <el-radio-button label="right">
                右侧
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.maxlength !== undefined" label="最多输入">
            <el-input v-model="activeData.maxlength" placeholder="请输入字符长度">
              <template slot="append">
                个字符
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData['active-text'] !== undefined" label="开启提示">
            <el-input v-model="activeData['active-text']" placeholder="请输入开启提示" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-text'] !== undefined" label="关闭提示">
            <el-input v-model="activeData['inactive-text']" placeholder="请输入关闭提示" />
          </el-form-item>
          <el-form-item v-if="activeData['active-value'] !== undefined" label="开启值">
            <el-input :value="setDefaultValue(activeData['active-value'])" placeholder="请输入开启值"
              @input="onSwitchValueInput($event, 'active-value')" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-value'] !== undefined" label="关闭值">
            <el-input :value="setDefaultValue(activeData['inactive-value'])" placeholder="请输入关闭值"
              @input="onSwitchValueInput($event, 'inactive-value')" />
          </el-form-item>
          <el-form-item v-if="activeData.type !== undefined && 'el-date-picker' === activeData.tag" label="时间类型">
            <el-select v-model="activeData.type" placeholder="请选择时间类型" :style="{ width: '100%' }"
              @change="dateTypeChange">
              <el-option v-for="(item, index) in dateOptions" :key="index" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.name !== undefined" label="文件字段名">
            <el-input v-model="activeData.name" placeholder="请输入上传文件字段名" />
          </el-form-item>
          <el-form-item v-if="activeData.accept !== undefined" label="文件类型">
            <el-select v-model="activeData.accept" placeholder="请选择文件类型" :style="{ width: '100%' }" clearable>
              <el-option label="图片" value="image/*" />
              <el-option label="视频" value="video/*" />
              <el-option label="音频" value="audio/*" />
              <el-option label="excel" value=".xls,.xlsx" />
              <el-option label="word" value=".doc,.docx" />
              <el-option label="pdf" value=".pdf" />
              <el-option label="txt" value=".txt" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.fileSize !== undefined" label="文件大小">
            <el-input v-model.number="activeData.fileSize" placeholder="请输入文件大小">
              <el-select slot="append" v-model="activeData.sizeUnit" :style="{ width: '66px' }">
                <el-option label="KB" value="KB" />
                <el-option label="MB" value="MB" />
                <el-option label="GB" value="GB" />
              </el-select>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData.action !== undefined" label="上传地址">
            <el-input v-model="activeData.action" placeholder="请输入上传地址" clearable />
          </el-form-item>
          <el-form-item v-if="activeData['list-type'] !== undefined" label="列表类型">
            <el-radio-group v-model="activeData['list-type']" size="small">
              <el-radio-button label="text">
                text
              </el-radio-button>
              <el-radio-button label="picture">
                picture
              </el-radio-button>
              <el-radio-button label="picture-card">
                picture-card
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.buttonText !== undefined" v-show="'picture-card' !== activeData['list-type']"
            label="按钮文字">
            <el-input v-model="activeData.buttonText" placeholder="请输入按钮文字" />
          </el-form-item>
          <el-form-item v-if="activeData['range-separator'] !== undefined" label="分隔符">
            <el-input v-model="activeData['range-separator']" placeholder="请输入分隔符" />
          </el-form-item>
          <el-form-item v-if="activeData['picker-options'] !== undefined" label="时间段">
            <el-input v-model="activeData['picker-options'].selectableRange" placeholder="请输入时间段" />
          </el-form-item>
          <el-form-item v-if="activeData.format !== undefined" label="时间格式">
            <el-input :value="activeData.format" placeholder="请输入时间格式" @input="setTimeValue($event)" />
          </el-form-item>
          <!-- 选项编辑：仅多选/单选/下拉显示；可拖拽排序，选项值是否转数字由 isNumberStr 判断 -->
          <template v-if="['el-checkbox-group', 'el-radio-group', 'el-select'].indexOf(activeData.tag) > -1">
            <el-divider>选项</el-divider>
            <draggable :list="activeData.options" :animation="340" group="selectItem" handle=".option-drag"
              item-key="label">
              <template #item="{ element, index }">
                <div :key="index" class="select-item">
                  <div class="select-line-icon option-drag">
                    <i class="el-icon-s-operation" />
                  </div>
                  <el-input v-model="element.label" placeholder="选项名" size="small" />
                  <el-input placeholder="选项值" size="small" :value="element.value"
                    @input="setOptionValue(element, $event)" />
                  <div class="close-btn select-line-icon" @click="activeData.options.splice(index, 1)">
                    <el-icon>
                      <Remove />
                    </el-icon>
                  </div>
                </div>
              </template>
            </draggable>
            <div>
              <el-button icon="CirclePlus" style="margin-left: 8px; margin-top: 10px;" text bg type="primary"
                @click="addSelectItem">
                添加选项
              </el-button>
            </div>
            <el-divider />
          </template>

          <!-- 级联选择器：动态数据只配键名（数据由接口运行时提供），静态数据用 el-tree 直接维护层级 -->
          <template v-if="['el-cascader'].indexOf(activeData.tag) > -1">
            <el-divider>选项</el-divider>
            <el-form-item label="数据类型">
              <el-radio-group v-model="activeData.dataType" size="small">
                <el-radio-button label="dynamic">
                  动态数据
                </el-radio-button>
                <el-radio-button label="static">
                  静态数据
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <template v-if="activeData.dataType === 'dynamic'">
              <el-form-item label="标签键名">
                <el-input v-model="activeData.labelKey" placeholder="请输入标签键名" />
              </el-form-item>
              <el-form-item label="值键名">
                <el-input v-model="activeData.valueKey" placeholder="请输入值键名" />
              </el-form-item>
              <el-form-item label="子级键名">
                <el-input v-model="activeData.childrenKey" placeholder="请输入子级键名" />
              </el-form-item>
            </template>

            <el-tree v-if="activeData.dataType === 'static'" draggable :data="activeData.options" node-key="id"
              :expand-on-click-node="false" :render-content="renderContent" />
            <div v-if="activeData.dataType === 'static'">
              <el-button icon="CirclePlus" style="margin-left: 0; margin-top: 10px;" type="primary" text bg
                @click="addTreeItem">
                添加父级
              </el-button>
            </div>
            <el-divider />
          </template>

          <el-form-item v-if="activeData.optionType !== undefined" label="选项样式">
            <el-radio-group v-model="activeData.optionType">
              <el-radio-button label="default">
                默认
              </el-radio-button>
              <el-radio-button label="button">
                按钮
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData['active-color'] !== undefined" label="开启颜色">
            <el-color-picker v-model="activeData['active-color']" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-color'] !== undefined" label="关闭颜色">
            <el-color-picker v-model="activeData['inactive-color']" />
          </el-form-item>

          <el-form-item v-if="activeData['allow-half'] !== undefined" label="允许半选">
            <el-switch v-model="activeData['allow-half']" />
          </el-form-item>
          <el-form-item v-if="activeData['show-text'] !== undefined" label="辅助文字">
            <el-switch v-model="activeData['show-text']" @change="rateTextChange" />
          </el-form-item>
          <el-form-item v-if="activeData['show-score'] !== undefined" label="显示分数">
            <el-switch v-model="activeData['show-score']" @change="rateScoreChange" />
          </el-form-item>
          <el-form-item v-if="activeData['show-stops'] !== undefined" label="显示间断点">
            <el-switch v-model="activeData['show-stops']" />
          </el-form-item>
          <el-form-item v-if="activeData.range !== undefined" label="范围选择">
            <el-switch v-model="activeData.range" @change="rangeChange" />
          </el-form-item>
          <el-form-item v-if="activeData.border !== undefined && activeData.optionType === 'default'" label="是否带边框">
            <el-switch v-model="activeData.border" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-color-picker'" label="颜色格式">
            <el-select v-model="activeData['color-format']" placeholder="请选择颜色格式" :style="{ width: '100%' }"
              @change="colorFormatChange">
              <el-option v-for="(item, index) in colorFormatOptions" :key="index" :label="item.label"
                :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.size !== undefined &&
            (activeData.optionType === 'button' ||
              activeData.border ||
              activeData.tag === 'el-color-picker')" label="选项尺寸">
            <el-radio-group v-model="activeData.size">
              <el-radio-button label="large">
                较大
              </el-radio-button>
              <el-radio-button label="default">
                默认
              </el-radio-button>
              <el-radio-button label="small">
                较小
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData['show-word-limit'] !== undefined" label="输入统计">
            <el-switch v-model="activeData['show-word-limit']" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="严格步数">
            <el-switch v-model="activeData['step-strictly']" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="是否多选">
            <el-switch v-model="activeData.props.props.multiple" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="展示全路径">
            <el-switch v-model="activeData['show-all-levels']" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="可否筛选">
            <el-switch v-model="activeData.filterable" />
          </el-form-item>
          <el-form-item v-if="activeData.clearable !== undefined" label="能否清空">
            <el-switch v-model="activeData.clearable" />
          </el-form-item>
          <el-form-item v-if="activeData.showTip !== undefined" label="显示提示">
            <el-switch v-model="activeData.showTip" />
          </el-form-item>
          <el-form-item v-if="activeData.multiple !== undefined" label="多选文件">
            <el-switch v-model="activeData.multiple" />
          </el-form-item>
          <el-form-item v-if="activeData['auto-upload'] !== undefined" label="自动上传">
            <el-switch v-model="activeData['auto-upload']" />
          </el-form-item>
          <el-form-item v-if="activeData.readonly !== undefined" label="是否只读">
            <el-switch v-model="activeData.readonly" />
          </el-form-item>
          <el-form-item v-if="activeData.disabled !== undefined" label="是否禁用">
            <el-switch v-model="activeData.disabled" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-select'" label="是否可搜索">
            <el-switch v-model="activeData.filterable" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-select'" label="是否多选">
            <el-switch v-model="activeData.multiple" @change="multipleChange" />
          </el-form-item>
          <el-form-item v-if="activeData.required !== undefined" label="是否必填">
            <el-switch v-model="activeData.required" />
          </el-form-item>

          <!-- 布局结构树：把当前容器连同子级渲染成树，用于确认嵌套关系与组件名 -->
          <template v-if="activeData.layoutTree">
            <el-divider>布局结构树</el-divider>
            <el-tree :data="[activeData]" :props="layoutTreeProps" node-key="renderKey" default-expand-all draggable>
              <template #default="{ node, data }">
                <span class="node-label">
                  <svg-icon class="node-icon" :icon-class="data.tagIcon" style="margin-right: 5px;" />
                  {{ node.label }}
                </span>
              </template>
            </el-tree>
          </template>

          <!-- 正则校验：只给单表单项配置，生成代码时 pattern 会原样内联进 new RegExp(...)，这里不做语法校验 -->
          <template v-if="activeData.layout === 'colFormItem'">
            <el-divider>正则校验</el-divider>
            <div v-for="(item, index) in activeData.regList" :key="index" class="reg-item">
              <span class="close-btn" @click="activeData.regList.splice(index, 1)">
                <el-icon>
                  <Close />
                </el-icon>
              </span>
              <el-form-item label="表达式">
                <el-input v-model="item.pattern" placeholder="请输入正则" />
              </el-form-item>
              <el-form-item label="错误提示" style="margin-bottom:0">
                <el-input v-model="item.message" placeholder="请输入错误提示" />
              </el-form-item>
            </div>
            <div>
              <el-button icon="CirclePlus" style="margin-left: 0; margin-top: 10px;" type="primary" text bg
                @click="addReg">
                添加规则
              </el-button>
            </div>
          </template>
        </el-form>
        <!-- 表单属性 -->
        <el-form v-show="currentTab === 'form'" label-width="90px" label-position="top">
          <el-form-item label="表单名">
            <el-input v-model="formConf.formRef" placeholder="请输入表单名（ref）" />
          </el-form-item>
          <el-form-item label="表单模型">
            <el-input v-model="formConf.formModel" placeholder="请输入数据模型" />
          </el-form-item>
          <el-form-item label="校验模型">
            <el-input v-model="formConf.formRules" placeholder="请输入校验模型" />
          </el-form-item>
          <el-form-item label="表单尺寸">
            <el-radio-group v-model="formConf.size">
              <el-radio-button label="large" value="较大" />
              <el-radio-button label="default" value="默认" />
              <el-radio-button label="small" value="较小" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签对齐">
            <el-radio-group v-model="formConf.labelPosition">
              <el-radio-button label="left" value="左对齐" />
              <el-radio-button label="right" value="右对齐" />
              <el-radio-button label="top" value="顶部对齐" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签宽度">
            <el-input-number v-model="formConf.labelWidth" placeholder="标签宽度" />
          </el-form-item>
          <el-form-item label="栅格间隔">
            <el-input-number v-model="formConf.gutter" :min="0" placeholder="栅格间隔" />
          </el-form-item>
          <el-form-item label="禁用表单">
            <el-switch v-model="formConf.disabled" />
          </el-form-item>
          <el-form-item label="表单按钮">
            <el-switch v-model="formConf.formBtns" />
          </el-form-item>
          <el-form-item label="显示未选中组件边框">
            <el-switch v-model="formConf.unFocusedComponentBorder" />
          </el-form-item>
        </el-form>
      </el-scrollbar>
    </div>
    <!-- 两个纯 UI 弹窗：图标选择、树节点新增；结果都通过事件回写到 activeData / 选项树 -->
    <icons-dialog v-model="iconsVisible" :current="activeData[currentIconModel]" @select="setIcon" />
    <treeNode-dialog v-model="dialogVisible" @commit="addNode" />

  </div>
</template>

<script setup>
/**
 * 表单构建器右侧属性面板。
 *
 * 两个 Tab 共用一块滚动区：
 * - 组件属性（field）：编辑 props.activeData。每个 el-form-item 都以「activeData 上是否存在该字段」
 *   作为 v-if 条件，所以组件能配哪些项完全由 utils/generator/config 的字段声明决定，本文件不写死组件名单；
 * - 表单属性（form）：直接双向编辑 props.formConf。
 * 传进来的 activeData / formConf 与父组件 ref 指向同一个对象，改属性即等于改画布数据，无需 emit 同步；
 * 只有「切换组件类型」例外，通过 tag-change 交回父级做整体替换。
 */
import draggable from "vuedraggable/dist/vuedraggable.common"
import { isNumberStr } from '@/utils/index'
import IconsDialog from './IconsDialog'
import TreeNodeDialog from './TreeNodeDialog'
import { inputComponents, selectComponents } from '@/utils/generator/config'

// 当前实例的 proxy，用于访问全局挂载的能力（如 $modal）以及实例上可写的属性
const { proxy } = getCurrentInstance()
// 各日期类型对应的 value-format。Element Plus 的日期控件 format 是显示格式、value-format 才是取值格式，
// 两者要分开设置；周选择器特殊，下面 setTimeValue 会单独处理
const dateTimeFormat = {
  date: 'YYYY-MM-DD',
  week: 'YYYY 第 ww 周',
  month: 'YYYY-MM',
  year: 'YYYY',
  datetime: 'YYYY-MM-DD HH:mm:ss',
  daterange: 'YYYY-MM-DD',
  monthrange: 'YYYY-MM',
  datetimerange: 'YYYY-MM-DD HH:mm:ss'
}
// showField：画布为空时不渲染「组件属性」表单；
// activeData / formConf：父组件 ref 里的同一个对象，直接改属性就等于改画布数据；
// 其中 formConf.formBtns 只在生成页面形态（type=file）时决定是否输出提交/重置按钮
const props = defineProps({
  showField: Boolean,
  activeData: Object,
  formConf: Object
})

// 面板本地的 UI 状态与各类下拉选项常量（当前 Tab、弹窗开关、选项列表），不参与画布数据
const data = reactive({
  currentTab: 'field',
  currentNode: null,
  dialogVisible: false,
  iconsVisible: false,
  currentIconModel: null,
  dateTypeOptions: [
    {
      label: '日(date)',
      value: 'date'
    },
    {
      label: '周(week)',
      value: 'week'
    },
    {
      label: '月(month)',
      value: 'month'
    },
    {
      label: '年(year)',
      value: 'year'
    },
    {
      label: '日期时间(datetime)',
      value: 'datetime'
    }
  ],
  dateRangeTypeOptions: [
    {
      label: '日期范围(daterange)',
      value: 'daterange'
    },
    {
      label: '月范围(monthrange)',
      value: 'monthrange'
    },
    {
      label: '日期时间范围(datetimerange)',
      value: 'datetimerange'
    }
  ],
  colorFormatOptions: [
    {
      label: 'hex',
      value: 'hex'
    },
    {
      label: 'rgb',
      value: 'rgb'
    },
    {
      label: 'rgba',
      value: 'rgba'
    },
    {
      label: 'hsv',
      value: 'hsv'
    },
    {
      label: 'hsl',
      value: 'hsl'
    }
  ],
  justifyOptions: [
    {
      label: 'start',
      value: 'start'
    },
    {
      label: 'end',
      value: 'end'
    },
    {
      label: 'center',
      value: 'center'
    },
    {
      label: 'space-around',
      value: 'space-around'
    },
    {
      label: 'space-between',
      value: 'space-between'
    }
  ],
  layoutTreeProps: {
    label(data, node) {
      return data.componentName || `${data.label}: ${data.vModel}`
    }
  }
})

// reactive 解构会丢响应性，统一用 toRefs 转成 ref，模板里可直接当变量用
const { currentTab, currentNode, dialogVisible, iconsVisible, currentIconModel, dateTypeOptions, dateRangeTypeOptions, colorFormatOptions, justifyOptions, layoutTreeProps } = toRefs(data)

// 组件文档入口：activeData.document 未配置时兜底到 Element Plus 文档，保证链接始终可点
const documentLink = computed(() => props.activeData.document || 'https://element-plus.org/zh-CN/guide/installation')

// 日期类型下拉：带 start-placeholder 说明是按范围选择的控件，给范围类型；否则只给单值类型。
// 非 el-date-picker 时返回空数组，下拉自然就不渲染
const dateOptions = computed(() => {
  if (props.activeData.type !== undefined && props.activeData.tag === 'el-date-picker') {
    if (props.activeData['start-placeholder'] === undefined) {
      return dateTypeOptions.value
    }
    return dateRangeTypeOptions.value
  }
  return []
})

// 类型切换的可选物料：只允许输入型与选择型互转，布局型容器不参与
const tagList = ref([
  {
    label: '输入型组件',
    options: inputComponents
  },
  {
    label: '选择型组件',
    options: selectComponents
  }
])

// 只上抛「类型切换」，画布节点怎么替换由父级决定，本组件不直接改 drawingList
const emit = defineEmits(['tag-change'])

function addReg() {
  props.activeData.regList.push({
    pattern: '',
    message: ''
  })
}
function addSelectItem() {
  props.activeData.options.push({
    label: '',
    value: ''
  })
}

// 打开添加弹窗：currentNode 指向待插入的那个数组（根级就是 options），
// 弹窗只收集数据，真正的插入在 addNode 里做
function addTreeItem() {
  ++proxy.idGlobal
  dialogVisible.value = true
  currentNode.value = props.activeData.options
}

// el-tree 的自定义节点渲染（用 h 函数而不是模板）：左侧节点名，右侧「追加 / 删除」两个操作，
// 追加会打开弹窗并把结果压进该节点的 children
function renderContent(h, { node, data, store }) {
  return h('div', {
    class: "custom-tree-node"
  }, [
    h('span', node.label),
    h('span', {
      class: "node-operation"
    }, [
      h(resolveComponent('el-link'), {
        type: "primary",
        icon: "Plus",
        underline: false,
        onClick: () => {
          append(data)

        }
      }),
      h(resolveComponent('el-link'), {
        type: "danger",
        icon: "Delete",
        underline: false,
        style: "margin-left: 5px;",
        onClick: () => {
          remove(node, data)
        }
      })
    ])
  ])
}
// 给节点追加子级：没有 children 先补数组，再把插入目标指向它
function append(data) {
  if (!data.children) {
    data.children = []
  }
  dialogVisible.value = true
  currentNode.value = data.children
}
// 删除节点：父级可能直接是根数组，也可能是带 children 的对象，两种都要兼容；
// 按 id 定位而不是按下标，避免拖拽排序后删错节点
function remove(node, data) {
  const { parent } = node
  const children = parent.data.children || parent.data
  const index = children.findIndex(d => d.id === data.id)
  children.splice(index, 1)
}
// 弹窗提交后压入 currentNode 指向的数组，el-tree 会随数据变化自动刷新
function addNode(data) {
  currentNode.value.push(data)
}

// 纯数字字符串转成 number：否则选项值是字符串，与数字型默认值、接口数据比较时会出现不相等
function setOptionValue(item, val) {
  item.value = isNumberStr(val) ? +val : val
}
// 内部默认值转成输入框可编辑的文本：数组用逗号拼接，布尔转成字符串，其余原样返回
function setDefaultValue(val) {
  if (Array.isArray(val)) {
    return val.join(',')
  }
  if (['string', 'number'].indexOf(val) > -1) {
    return val
  }
  if (typeof val === 'boolean') {
    return `${val}`
  }
  return val
}

// 输入时反向解析成真实类型回写：数组按逗号拆分并逐项转数字，'true'/'false' 转布尔，其余做数字探测
function onDefaultValueInput(str) {
  if (Array.isArray(props.activeData.defaultValue)) {
    // 数组
    props.activeData.defaultValue = str.split(',').map(val => (isNumberStr(val) ? +val : val))
  } else if (['true', 'false'].indexOf(str) > -1) {
    // 布尔
    props.activeData.defaultValue = JSON.parse(str)
  } else {
    // 字符串和数字
    props.activeData.defaultValue = isNumberStr(str) ? +str : str
  }
}

// 开关的开启值/关闭值可能是布尔也可能是数字，按输入内容推断类型后回写，避免类型不一致开关失效
function onSwitchValueInput(val, name) {
  if (['true', 'false'].indexOf(val) > -1) {
    props.activeData[name] = JSON.parse(val)
  } else {
    props.activeData[name] = isNumberStr(val) ? +val : val
  }
}

// 改日期格式时连带重置默认值并写回 value-format：旧默认值是按老格式存的，留着会和控件新格式冲突；
// 周选择器特殊，value-format 固定取日期格式，否则取不到值
function setTimeValue(val, type) {
  const valueFormat = type === 'week' ? dateTimeFormat.date : val
  props.activeData.defaultValue = null
  props.activeData['value-format'] = valueFormat
  props.activeData.format = val
}

// 栅格改动同时记到 formConf.span，之后新增的组件会沿用这个宽度
function spanChange(val) {
  props.formConf.span = val
}

// 单选/多选切换后默认值类型要跟着变：多选用数组，单选用字符串，否则控件回显会异常
function multipleChange(val) {
  props.activeData.defaultValue = val ? [] : ''
}

// 时间类型切换：按映射表把新格式套到 format / value-format 上，并清掉旧默认值
function dateTypeChange(val) {
  setTimeValue(dateTimeFormat[val], val)
}

// 范围开关：打开时默认值变成 [min, max] 双值，关闭时回退成单值
function rangeChange(val) {
  props.activeData.defaultValue = val ? [props.activeData.min, props.activeData.max] : props.activeData.min
}

// 评分组件的「辅助文字」与「显示分数」互斥：打开一个就必须关掉另一个（rateScoreChange 是反向处理）
function rateTextChange(val) {
  if (val) props.activeData['show-score'] = false
}

function rateScoreChange(val) {
  if (val) props.activeData['show-text'] = false
}

// 颜色格式决定是否支持透明度：rgba/hsla 这类带 a 的格式才打开 show-alpha；
// 换格式后旧颜色值可能不合法，清空默认值并改 renderKey 强制重建控件
function colorFormatChange(val) {
  props.activeData.defaultValue = null
  props.activeData['show-alpha'] = val.indexOf('a') > -1
  props.activeData.renderKey = +new Date() // 更新renderKey,重新渲染该组件
}

// 先记住要回写的图标字段名（prefix-icon / suffix-icon），弹窗选中后按它写值
function openIconsDialog(model) {
  iconsVisible.value = true
  currentIconModel.value = model
}

function setIcon(val) {
  props.activeData[currentIconModel.value] = val
}

// 从两类物料里找出目标定义后上抛；下拉选项本来就用这两个数组渲染，正常情况下不会返回 undefined
function tagChange(tagIcon) {
  let target = inputComponents.find(item => item.tagIcon === tagIcon)
  if (!target) target = selectComponents.find(item => item.tagIcon === tagIcon)
  emit('tag-change', target)
}
</script>

<style lang="scss" scoped>
.right-board {
  width: 350px;
  position: absolute;
  right: 0;
  top: 0;
  padding-top: 3px;

  &:deep() {
    .el-tabs__header {
      margin: 0;
    }

    .el-input-group__append .el-button {
      display: inline-flex;
    }
  }

  /* 高度扣掉顶部 Tab 条 42px，让滚动条只出现在属性表单区域 */
  .field-box {
    position: relative;
    height: calc(100vh - 50px - 40px - 42px);
    box-sizing: border-box;
    overflow: hidden;
  }

  .el-scrollbar {
    height: 100%;

    &:deep() {
      .el-scrollbar__view {
        padding: 30px 20px;
      }

    }
  }
}

.reg-item {
  padding: 12px 6px;
  background: var(--el-border-color-extra-light);
  position: relative;
  border-radius: 4px;

  .close-btn {
    position: absolute;
    right: -6px;
    top: -6px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    line-height: 16px;
    background: rgba(0, 0, 0, .2);
    border-radius: 50%;
    color: #fff;
    z-index: 1;
    cursor: pointer;
    font-size: 12px;
  }
}

.select-item {
  display: flex;
  border: 1px dashed #fff;
  box-sizing: border-box;

  & .close-btn {
    cursor: pointer;
    color: #f56c6c;
  }

  & .el-input+.el-input {
    margin-left: 4px;
  }
}

.select-item+.select-item {
  margin-top: 4px;
}

.select-item.sortable-chosen {
  border: 1px dashed #409eff;
}

.select-line-icon {
  line-height: 32px;
  font-size: 22px;
  padding: 0 4px;
  color: #777;
}

.option-drag {
  cursor: move;
}

.time-range {
  .el-date-editor {
    width: 227px;
  }

  :deep() {
    .el-icon-time {
      display: none;
    }
  }
}

/* 固定在面板左上角，z-index 保证浮在滚动内容之上 */
.document-link {
  position: absolute;
  display: flex;
  width: 26px;
  height: 26px;
  top: 0;
  left: 0;
  cursor: pointer;
  background: #409eff;
  z-index: 1;
  border-radius: 0 0 6px 0;
  justify-content: center;
  align-items: center;
  color: #fff;
  font-size: 18px;
}

.node-label {
  font-size: 14px;
}

.node-icon {
  color: #bebfc3;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
</style>