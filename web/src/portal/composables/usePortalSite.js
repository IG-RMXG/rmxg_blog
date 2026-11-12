import { ref } from 'vue'
import { getPortalSite } from '@/api/portal'

/**
 * 门户站点信息的共享缓存。
 *
 * 顶栏、页脚、首页、登录页、侧栏都要站点标题和统计，但它们分散在不同组件里。
 * 用一个模块级 ref + 单飞（in-flight）Promise 保证同一时刻只请求一次；
 * 再给 60 秒的软过期：管理员刚发完文章切回门户时，统计还能自己刷新过来。
 */
const CACHE_MS = 60 * 1000

// 模块级共享状态：所有调用 usePortalSite() 的组件拿到的是同一个 ref，
// 任一组件触发刷新后，顶栏/页脚/侧栏会一起更新，不需要各自维护副本。
const site = ref({})
// 上次成功写入的时间戳；0 表示从未成功拉取过，此时即使没有在途请求也必须再请求一次。
let fetchedAt = 0
// 单飞（in-flight）Promise：同一时刻只允许一个请求，后来的调用方复用它的结果。
let pending = null

/**
 * 获取站点信息（全站共享的单例）。
 *
 * - 缓存未过期且没有在途请求：直接返回已有的 site；
 * - 缓存过期或首次调用：发起请求，成功后写入 site 与时间戳；
 * - 请求失败：保留上一次的值（首次失败则保持空对象）且不向调用方抛错——
 *   门户是只读展示，宁可显示旧数据，也不要让整个页面因为站点接口挂掉而空白。
 *
 * 注意这里不 await：调用方拿到的是响应式 ref，数据回来会自动触发渲染。
 *
 * @returns {{ site: import('vue').Ref<object> }} site：站点信息（title/subtitle/author/各项统计等）
 */
export function usePortalSite() {
  // 只有缓存新鲜才跳过请求；pending 存在时也要走下面的分支（复用同一个 Promise）
  const fresh = fetchedAt > 0 && Date.now() - fetchedAt < CACHE_MS
  if (!pending && !fresh) {
    pending = getPortalSite()
      .then(res => {
        site.value = res.data || {}
        fetchedAt = Date.now()
        return site.value
      })
      // 失败时把上一次的 site 值当作结果返回，调用方无需再写 try/catch
      .catch(() => site.value)
      .finally(() => {
        // 无论成功失败都要清空，否则一次失败会让后续所有调用都复用这个已失败的单飞 Promise
        pending = null
      })
  }
  return { site }
}

/** 强制重新拉取（例如后台改完站点配置后手动刷新） */
export function reloadPortalSite() {
  // 只把缓存标记为过期、丢弃单飞引用，并不会取消已经发出的请求：
  // 若确有请求在途，它返回时仍会写入 site（数据同源，晚到的结果最多多覆盖一次，可接受）。
  fetchedAt = 0
  pending = null
  return usePortalSite()
}
