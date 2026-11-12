<template>
  <component :is="type" v-bind="linkProps()">
    <slot />
  </component>
</template>

<script setup>
// 菜单链接适配器：站内路由渲染 router-link，站外地址渲染普通 <a>，
// 让 SidebarItem 在拼装菜单时不必关心链接是内链还是外链。
import { isExternal } from '@/utils/validate'

// to：站内为路由路径或路由对象；站外为 http(s)/mailto/tel 地址（判定规则见 utils/validate.isExternal）
const props = defineProps({
  to: {
    type: [String, Object],
    required: true
  }
})

const isExt = computed(() => {
  return isExternal(props.to)
})

const type = computed(() => {
  if (isExt.value) {
    return 'a'
  }
  return 'router-link'
})

// 外链一律新窗口打开；rel="noopener" 阻断目标页通过 window.opener 反向操作当前页
function linkProps() {
  if (isExt.value) {
    return {
      href: props.to,
      target: '_blank',
      rel: 'noopener'
    }
  }
  return {
    to: props.to
  }
}
</script>
