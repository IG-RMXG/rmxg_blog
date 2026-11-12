/**
 * 平滑滚动到页面指定纵向位置（分页切换后回顶会用到，见 @/components/Pagination）。
 *
 * 实现是 20ms 步进 + easeInOutQuad 缓动，不依赖 CSS scroll-behavior；
 * 写入 scrollTop 时同时覆盖 documentElement / body.parentNode / body，
 * 因为不同浏览器和文档模式下的实际滚动元素不一致，逐个赋值比运行时探测更省事。
 */
Math.easeInOutQuad = function(t, b, c, d) {
  t /= d / 2
  if (t < 1) {
    return c / 2 * t * t + b
  }
  t--
  return -c / 2 * (t * (t - 2) - 1) + b
}

// requestAnimationFrame for Smart Animating http://goo.gl/sx5sts
// 逐级降级到 60fps 的 setTimeout，保证老浏览器也能滚动
var requestAnimFrame = (function() {
  return window.requestAnimationFrame || window.webkitRequestAnimationFrame || window.mozRequestAnimationFrame || function(callback) { window.setTimeout(callback, 1000 / 60) }
})()

/**
 * Because it's so fucking difficult to detect the scrolling element, just move them all
 * @param {number} amount
 */
// 三个滚动容器都写一遍，谁生效算谁（不同浏览器/文档模式的实际滚动元素不同）
function move(amount) {
  document.documentElement.scrollTop = amount
  document.body.parentNode.scrollTop = amount
  document.body.scrollTop = amount
}

// 读取当前滚动位置，取值顺序与 move() 一致，保证读写针对同一个滚动容器
function position() {
  return document.documentElement.scrollTop || document.body.parentNode.scrollTop || document.body.scrollTop
}

/**
 * @param {number} to
 * @param {number} duration
 * @param {Function} callback
 */
// to 是目标绝对 Y 像素；duration 默认 500ms、步进固定 20ms，实际帧数约等于 duration/20；
// callback 只在动画正常播完后调用，中途不会触发。
export function scrollTo(to, duration, callback) {
  const start = position()
  const change = to - start
  const increment = 20
  let currentTime = 0
  duration = (typeof (duration) === 'undefined') ? 500 : duration
  var animateScroll = function() {
    // increment the time
    currentTime += increment
    // find the value with the quadratic in-out easing function
    var val = Math.easeInOutQuad(currentTime, start, change, duration)
    // move the document.body
    move(val)
    // do the animation unless its over
    if (currentTime < duration) {
      requestAnimFrame(animateScroll)
    } else {
      if (callback && typeof (callback) === 'function') {
        // the animation is done so lets callback
        callback()
      }
    }
  }
  animateScroll()
}
