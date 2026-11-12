// 页签（tagsView）操作封装（$tab）：把「开/关/刷新页签」与路由跳转组合起来。
// 页签数据在 tagsView store，真正跳转依赖 router；凡返回 Promise 的方法都已等待 store 更新完成，
// 调用方可以 await 之后再执行后续动作。
// refreshPage 依赖 constantRoutes 里的 /redirect/:path(.*) 空壳路由：
// 先跳到 /redirect/原路径 再 replace 回来，借路由重新挂载绕过 keep-alive 缓存。
import useTagsViewStore from '@/store/modules/tagsView'
import router from '@/router'

export default {
  // 刷新当前tab页签
  // 不传 obj 时从 matched 里找最内层的业务组件（跳过 Layout/ParentView，
  // 它们的 name 不是页面组件），用它的 name 清掉 keep-alive 缓存，再经 /redirect 重新进入。
  // 依赖页面组件都写了 name，否则缓存清不掉、刷新等于没刷。
  refreshPage(obj) {
    const { path, query, matched } = router.currentRoute.value
    if (obj === undefined) {
      matched.forEach((m) => {
        if (m.components && m.components.default && m.components.default.name) {
          if (!['Layout', 'ParentView'].includes(m.components.default.name)) {
            obj = { name: m.components.default.name, path: path, query: query }
          }
        }
      })
    }
    return useTagsViewStore().delCachedView(obj).then(() => {
      const { path, query } = obj
      router.replace({
        path: '/redirect' + path,
        query: query
      })
    })
  },
  // 关闭当前tab页签，打开新页签
  closeOpenPage(obj) {
    useTagsViewStore().delView(router.currentRoute.value)
    if (obj !== undefined) {
      return router.push(obj)
    }
  },
  // 关闭指定tab页签
  // 不传 obj 时关闭当前页签并跳到剩余页签里最右侧的一个，一个不剩则回首页；
  // 传了 obj 就只从已访问列表移除，不做跳转（典型用途：编辑页关闭后由调用方自己决定去哪）。
  closePage(obj) {
    if (obj === undefined) {
      return useTagsViewStore().delView(router.currentRoute.value).then(({ visitedViews }) => {
        const latestView = visitedViews.slice(-1)[0]
        if (latestView) {
          return router.push(latestView.fullPath)
        }
        return router.push('/')
      })
    }
    return useTagsViewStore().delView(obj)
  },
  // 关闭所有tab页签
  closeAllPage() {
    return useTagsViewStore().delAllViews()
  },
  // 关闭左侧tab页签
  closeLeftPage(obj) {
    return useTagsViewStore().delLeftTags(obj || router.currentRoute.value)
  },
  // 关闭右侧tab页签
  closeRightPage(obj) {
    return useTagsViewStore().delRightTags(obj || router.currentRoute.value)
  },
  // 关闭其他tab页签
  closeOtherPage(obj) {
    return useTagsViewStore().delOthersViews(obj || router.currentRoute.value)
  },
  // 打开tab页签
  // 用于页面间带标题跳转（如列表页跳详情/编辑）：先登记页签，再 push 到目标地址。
  // params 作为 query 传递；标题写进 meta.title，页签栏直接读它显示。
  openPage(title, url, params) {
    const obj = { path: url, meta: { title: title } }
    useTagsViewStore().addView(obj)
    return router.push({ path: url, query: params })
  },
  // 修改tab页签
  // 覆盖已存在的页签信息（通常是异步拿到详情后回填标题），不改变已访问顺序。
  updatePage(obj) {
    return useTagsViewStore().updateVisitedView(obj)
  }
}
