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

const site = ref({})
let fetchedAt = 0
let pending = null

export function usePortalSite() {
  const fresh = fetchedAt > 0 && Date.now() - fetchedAt < CACHE_MS
  if (!pending && !fresh) {
    pending = getPortalSite()
      .then(res => {
        site.value = res.data || {}
        fetchedAt = Date.now()
        return site.value
      })
      .catch(() => site.value)
      .finally(() => {
        pending = null
      })
  }
  return { site }
}

/** 强制重新拉取（例如后台改完站点配置后手动刷新） */
export function reloadPortalSite() {
  fetchedAt = 0
  pending = null
  return usePortalSite()
}
