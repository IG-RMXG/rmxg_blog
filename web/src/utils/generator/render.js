import { defineComponent, h } from 'vue'
import { makeMap } from '@/utils/index'

/**
 * 设计器画布的单元素渲染器（render 函数写法）：把一个 schema 元素 conf 直接渲染成真实的
 * Element Plus 组件，供 views/tool/build/DraggableItem.vue 中的
 * <render :conf="element" v-model="element.defaultValue" /> 做实时预览。
 * 它不参与导出代码的生成（那条链路是 html.js / js.js 的字符串拼接），改动时两边要各自维护。
 */

// HTML 原生属性白名单：命中则作为 attrs 透传，否则当作组件 prop 传递。
// 注意末尾的 'prefix-icon' 是直接拼接、缺少分隔逗号，makeMap 按逗号切分后得到的是 wrapprefix-icon，
// 因此 wrap 与 prefix-icon 两项都匹配不到（既有写法，未改动）。
const isAttr = makeMap(
  'accept,accept-charset,accesskey,action,align,alt,async,autocomplete,' +
  'autofocus,autoplay,autosave,bgcolor,border,buffered,challenge,charset,' +
  'checked,cite,class,code,codebase,color,cols,colspan,content,http-equiv,' +
  'name,contenteditable,contextmenu,controls,coords,data,datetime,default,' +
  'defer,dir,dirname,disabled,download,draggable,dropzone,enctype,method,for,' +
  'form,formaction,headers,height,hidden,high,href,hreflang,http-equiv,' +
  'icon,id,ismap,itemprop,keytype,kind,label,lang,language,list,loop,low,' +
  'manifest,max,maxlength,media,method,GET,POST,min,multiple,email,file,' +
  'muted,name,novalidate,open,optimum,pattern,ping,placeholder,poster,' +
  'preload,radiogroup,readonly,rel,required,reversed,rows,rowspan,sandbox,' +
  'scope,scoped,seamless,selected,shape,size,type,text,password,sizes,span,' +
  'spellcheck,src,srcdoc,srclang,srcset,start,step,style,summary,tabindex,' +
  'target,title,type,usemap,value,width,wrap' + 'prefix-icon'
)
// 设计器自身的字段，只服务于生成逻辑，不能透传给组件（否则会变成无效 prop 甚至落到 DOM 上）
const isNotProps = makeMap(
  'layout,prepend,regList,tag,document,changeTag,defaultValue'
)

// 模型绑定辅助：把元素的 defaultValue 映射成 modelValue 并转发 update:modelValue。
// 当前文件内没有调用点（实际绑定在 render 里从 this.$attrs.modelValue 读取）
function useVModel(props, emit) {
  return {
    modelValue: props.defaultValue,
    'onUpdate:modelValue': (val) => emit('update:modelValue', val),
  }
}

// 子节点生成器：tag -> { 元素属性名: 生成函数 }。
// key 借用元素上的属性名（如 options / list-type / default），靠「该属性有值」来触发，
// 所以新增一种子节点既要在物料里加字段，也要在这里补同名 key。
const componentChild = {
  'el-button': {
    default(h, conf, key) {
      return conf[key]
    },
  },
  'el-select': {
    options(h, conf, key) {
      return conf.options.map(item => h(resolveComponent('el-option'), {
        label: item.label,
        value: item.value,
      }))
    }
  },
  'el-radio-group': {
    options(h, conf, key) {
      return conf.optionType === 'button' ? conf.options.map(item => h(resolveComponent('el-checkbox-button'), {
        label: item.value,
      }, () => item.label)) : conf.options.map(item => h(resolveComponent('el-radio'), {
        label: item.value,
        border: conf.border,
      }, () => item.label))
    }
  },
  'el-checkbox-group': {
    options(h, conf, key) {
      return conf.optionType === 'button' ? conf.options.map(item => h(resolveComponent('el-checkbox-button'), {
        label: item.value,
      }, () => item.label)) : conf.options.map(item => h(resolveComponent('el-checkbox'), {
        label: item.value,
        border: conf.border,
      }, () => item.label))
    }
  },
  'el-upload': {
    'list-type': (h, conf, key) => {
      const option = {}
      // if (conf.showTip) {
      //   tip = h('div', {
      //     class: "el-upload__tip"
      //   }, () => '只能上传不超过' + conf.fileSize + conf.sizeUnit + '的' + conf.accept + '文件')
      // }
      if (conf['list-type'] === 'picture-card') {
        return h(resolveComponent('el-icon'), option, () => h(resolveComponent('Plus')))
      } else {
        // option.size = "small"
        option.type = "primary"
        option.icon = "Upload"
        return h(resolveComponent('el-button'), option, () => conf.buttonText)
      }
    },

  }
}

// 具名插槽生成器：返回值仍是「生成插槽的函数」而不是 VNode，由 render 里赋给 slot[key]
const componentSlot = {
  'el-upload': {
    'tip': (h, conf, key) => {
      if (conf.showTip) {
        return () => h('div', {
          class: "el-upload__tip"
        }, '只能上传不超过' + conf.fileSize + conf.sizeUnit + '的' + conf.accept + '文件')
      }
    },
  }
}
export default defineComponent({

  // 使用 render 函数
  render() {
    const dataObject = {
      attrs: {},
      props: {},
      on: {},
      style: {}
    }
    const confClone = JSON.parse(JSON.stringify(this.conf))
    const children = []
    const slot = {}
    const childObjs = componentChild[confClone.tag]
    if (childObjs) {
      Object.keys(childObjs).forEach(key => {
        const childFunc = childObjs[key]
        if (confClone[key]) {
          children.push(childFunc(h, confClone, key))
        }
      })
    }
    const slotObjs = componentSlot[confClone.tag]
    if (slotObjs) {
      Object.keys(slotObjs).forEach(key => {
        const childFunc = slotObjs[key]
        if (confClone[key]) {
          slot[key] = childFunc(h, confClone, key)
        }
      })
    }
    // 分流规则：与 dataObject 已有的四个键（attrs / props / on / style）同名的直接整块覆盖，
    // 其余按 isAttr 白名单进 attrs，再用 isNotProps 排除设计器字段，最后才落到 props
    Object.keys(confClone).forEach(key => {
      const val = confClone[key]
      if (dataObject[key]) {
        dataObject[key] = val
      } else if (isAttr(key)) {
        dataObject.attrs[key] = val
      } else if (!isNotProps(key)) {
        dataObject.props[key] = val
      }
    })
    // 有子节点时统一挂到默认插槽，作为 h() 的第三个参数传入
    if(children.length > 0){
      slot.default = () => children
    }
    
    // modelValue 从 $attrs 透传（父组件 v-model 绑的其实是 defaultValue），
    // props 与 attrs 分开收集后再合并，style 仅做浅合并
    return h(resolveComponent(this.conf.tag),
      {
        modelValue: this.$attrs.modelValue,
        ...dataObject.props,
        ...dataObject.attrs,
        style: {
          ...dataObject.style
        },
      }
      , slot ?? null)
  },
  props: {
    // 单个元素的完整 schema，结构与 config.js 的物料、drawingDefalut.js 的初始元素一致
    conf: {
      type: Object,
      required: true,
    },
  }
})