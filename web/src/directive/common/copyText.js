/**
* v-copyText 复制文本内容
* Copyright (c) 2022 ruoyi
*/
// 指令契约（两条可写在同一个元素上，Vue 按 name+arg 区分，互不覆盖）：
//   v-copyText="text"              点击元素时复制 text
//   v-copyText:callback="handler"  复制成功后回调，入参就是被复制的文本
// arg 为 'callback' 时只登记回调、不重复绑定点击事件；回调在点击时才读取，
// 因此两条指令的书写顺序不影响结果。
export default {
  beforeMount(el, { value, arg }) {
    if (arg === "callback") {
      el.$copyCallback = value
    } else {
      el.$copyValue = value
      const handler = () => {
        copyTextToClipboard(el.$copyValue)
        if (el.$copyCallback) {
          el.$copyCallback(el.$copyValue)
        }
      }
      el.addEventListener("click", handler)
      // 暴露手动解绑方法。指令没有 unmounted 钩子，元素移除后监听会随 DOM 一起回收；
      // $destroyCopy 只留给「元素复用但需要停止响应点击」的特殊场景。
      el.$destroyCopy = () => el.removeEventListener("click", handler)
    }
  }
}

// 隐藏 textarea + document.execCommand('copy') 的兼容方案：不依赖 navigator.clipboard
// （后者要求安全上下文 HTTPS，且部分浏览器需要额外授权）。
// 临时插入 textarea 会夺走焦点与选区，因此结束后恢复原 Range 与原先的焦点元素；
// readonly 与 12pt 字号是移动端/iOS 的兼容处理（防软键盘弹出、防页面被放大）。
// 返回是否复制成功，但当前调用方并未检查返回值。
function copyTextToClipboard(input, { target = document.body } = {}) {
  const element = document.createElement('textarea')
  const previouslyFocusedElement = document.activeElement

  element.value = input

  // Prevent keyboard from showing on mobile
  element.setAttribute('readonly', '')

  element.style.contain = 'strict'
  element.style.position = 'absolute'
  element.style.left = '-9999px'
  element.style.fontSize = '12pt' // Prevent zooming on iOS

  const selection = document.getSelection()
  const originalRange = selection.rangeCount > 0 && selection.getRangeAt(0)

  target.append(element)
  element.select()

  // Explicit selection workaround for iOS
  element.selectionStart = 0
  element.selectionEnd = input.length

  let isSuccess = false
  try {
    isSuccess = document.execCommand('copy')
  } catch { }

  element.remove()

  if (originalRange) {
    selection.removeAllRanges()
    selection.addRange(originalRange)
  }

  // Get the focus back on the previously focused element, if any
  if (previouslyFocusedElement) {
    previouslyFocusedElement.focus()
  }

  return isSuccess
}
