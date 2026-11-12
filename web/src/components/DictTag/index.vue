<template>
  <div>
    <!-- 只渲染命中 value 的选项：默认类型且无自定义 class 时用 span，其余用 el-tag 上色 -->
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span
          v-if="(item.elTagType == 'default' || item.elTagType == '') && (item.elTagClass == '' || item.elTagClass == null)"
          :key="item.value"
          :index="index"
          :class="item.elTagClass"
        >{{ item.label + " " }}</span>
        <el-tag
          v-else
          :disable-transitions="true"
          :key="item.value + ''"
          :index="index"
          :type="item.elTagType"
          :class="item.elTagClass"
        >{{ item.label + " " }}</el-tag>
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ unmatchArray | handleArray }}
    </template>
  </div>
</template>

<script setup>
/**
 * 字典标签渲染：按 options 里的 { value, label, elTagType, elTagClass } 把后端字典值翻译成带颜色的 el-tag。
 *
 * value 支持三种入参形态：数组、分隔符字符串（默认 ","）、单值，统一在 values 里归一成字符串数组再比对。
 * elTagType 为 default/空 且 elTagClass 也为空时退化成普通 <span>，避免出现无意义的空标签边框；
 * 未匹配到的值由 showValue 控制是否直接显示原值，便于发现「后端新增字典项、前端没配」的情况。
 */
// 记录未匹配的项
const unmatchArray = ref([])

const props = defineProps({
  // 数据
  options: {
    type: Array,
    default: null,
  },
  // 当前的值
  value: [Number, String, Array],
  // 当未找到匹配的数据时，显示value
  showValue: {
    type: Boolean,
    default: true,
  },
  // value 为字符串（多选值拼接）时按此分隔符切分
  separator: {
    type: String,
    default: ",",
  }
})

// 归一化后参与比对的字符串数组：空值返回空数组（否则空串会被当成一个有效值），
// 数组逐项转字符串，字符串按 separator 切开，保证与字典项的 value 类型对齐。
const values = computed(() => {
  if (props.value === null || typeof props.value === 'undefined' || props.value === '') return []
  return Array.isArray(props.value) ? props.value.map(item => '' + item) : String(props.value).split(props.separator)
})

// 一次性做两件事：把没命中 options 的值收集到 unmatchArray，并返回「是否存在未匹配项」。
// 值为空、options 不是数组或为空时直接返回 false，不报警也不显示原值。
const unmatch = computed(() => {
  unmatchArray.value = []
  // 没有value不显示
  if (props.value === null || typeof props.value === 'undefined' || props.value === '' || !Array.isArray(props.options) || props.options.length === 0) return false
  // 传入值为数组
  let unmatch = false // 添加一个标志来判断是否有未匹配项
  values.value.forEach(item => {
    if (!props.options.some(v => v.value === item)) {
      unmatchArray.value.push(item)
      unmatch = true // 如果有未匹配项，将标志设置为true
    }
  })
  return unmatch // 返回标志的值
})

// 把未匹配的值拼成空格分隔的字符串（首项前也带一个空格）。
// 注意：模板里是按 Vue2 filter 语法调用的（unmatchArray | handleArray），
// Vue 3 已移除 filter，该表达式实际会按位或求值而不会走到这里——保留原实现不动。
function handleArray(array) {
  if (array.length === 0) return ""
  return array.reduce((pre, cur) => {
    return pre + " " + cur
  })
}
</script>

<style scoped>
/* 多个标签时的横向间隔；span 形态不在此规则内，需要间距请配 elTagClass */
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
