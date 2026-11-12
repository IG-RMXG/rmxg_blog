// 多页签（tagsView）状态：visitedViews 已打开的页签、cachedViews 参与 keep-alive 的组件 name 列表、
// iframeViews 内嵌外链页签。
// 两个列表必须同步维护：只从 visitedViews 删而没清 cachedViews，组件实例会留在内存里，
// 下次打开同一路由仍是旧数据 —— 这就是「页签关了但页面没刷新」的根因。
// 所有 del* 方法都返回 Promise 并在内部 resolve，方便 $tab 里 await 之后再决定跳转。
const useTagsViewStore = defineStore(
  'tags-view',
  {
    // visitedViews / iframeViews 存的是路由对象（额外挂了 title 字段）；cachedViews 只存组件 name，
    // 它们会作为 <keep-alive :include> 的值使用，所以必须是 name 而不是 path。
    state: () => ({
      visitedViews: [],
      cachedViews: [],
      iframeViews: []
    }),
    actions: {
      // 打开页签：同时登记「可见」与「可缓存」两个列表，任何新增页签都应走这里。
      addView(view) {
        this.addVisitedView(view)
        this.addCachedView(view)
      },
      addIframeView(view) {
        if (this.iframeViews.some(v => v.path === view.path)) return
        this.iframeViews.push(
          Object.assign({}, view, {
            title: view.meta.title || 'no-name'
          })
        )
      },
      // 以 path 去重：同一路由带不同 query 不会新开页签，页签的唯一标识就是 path（不含 query）。
      addVisitedView(view) {
        if (this.visitedViews.some(v => v.path === view.path)) return
        this.visitedViews.push(
          Object.assign({}, view, {
            title: view.meta.title || 'no-name'
          })
        )
      },
      // 只有写了 name 的组件才能被 <keep-alive include> 命中；meta.noCache 的页面
      // （编辑页等）不加入缓存，保证每次进入都是干净状态。
      addCachedView(view) {
        if (this.cachedViews.includes(view.name)) return
        if (!view.meta.noCache) {
          this.cachedViews.push(view.name)
        }
      },
      delView(view) {
        return new Promise(resolve => {
          this.delVisitedView(view)
          this.delCachedView(view)
          resolve({
            visitedViews: [...this.visitedViews],
            cachedViews: [...this.cachedViews]
          })
        })
      },
      // 从可见列表移除，并顺带清掉同路径的外链页签，避免外链页签残留在页签栏里。
      delVisitedView(view) {
        return new Promise(resolve => {
          for (const [i, v] of this.visitedViews.entries()) {
            if (v.path === view.path) {
              this.visitedViews.splice(i, 1)
              break
            }
          }
          this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
          resolve([...this.visitedViews])
        })
      },
      delIframeView(view) {
        return new Promise(resolve => {
          this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
          resolve([...this.iframeViews])
        })
      },
      delCachedView(view) {
        return new Promise(resolve => {
          const index = this.cachedViews.indexOf(view.name)
          index > -1 && this.cachedViews.splice(index, 1)
          resolve([...this.cachedViews])
        })
      },
      delOthersViews(view) {
        return new Promise(resolve => {
          this.delOthersVisitedViews(view)
          this.delOthersCachedViews(view)
          resolve({
            visitedViews: [...this.visitedViews],
            cachedViews: [...this.cachedViews]
          })
        })
      },
      delOthersVisitedViews(view) {
        return new Promise(resolve => {
          this.visitedViews = this.visitedViews.filter(v => {
            return v.meta.affix || v.path === view.path
          })
          this.iframeViews = this.iframeViews.filter(item => item.path === view.path)
          resolve([...this.visitedViews])
        })
      },
      // slice(index, index + 1) 只保留当前页签的缓存，其余全部丢弃；
      // 当前页签本就不在缓存里（noCache 页面）时直接清空。
      delOthersCachedViews(view) {
        return new Promise(resolve => {
          const index = this.cachedViews.indexOf(view.name)
          if (index > -1) {
            this.cachedViews = this.cachedViews.slice(index, index + 1)
          } else {
            this.cachedViews = []
          }
          resolve([...this.cachedViews])
        })
      },
      delAllViews(view) {
        return new Promise(resolve => {
          this.delAllVisitedViews(view)
          this.delAllCachedViews(view)
          resolve({
            visitedViews: [...this.visitedViews],
            cachedViews: [...this.cachedViews]
          })
        })
      },
      // 只保留 meta.affix 的固定页签（首页这类不允许关闭的），外链页签一并清空。
      delAllVisitedViews(view) {
        return new Promise(resolve => {
          const affixTags = this.visitedViews.filter(tag => tag.meta.affix)
          this.visitedViews = affixTags
          this.iframeViews = []
          resolve([...this.visitedViews])
        })
      },
      delAllCachedViews(view) {
        return new Promise(resolve => {
          this.cachedViews = []
          resolve([...this.cachedViews])
        })
      },
      // 就地合并新字段（一般只改 title），用于详情页拿到数据后回填页签标题。
      // 注意合并的是对象引用，调用方传进来的 view 会被一起改写。
      updateVisitedView(view) {
        for (let v of this.visitedViews) {
          if (v.path === view.path) {
            v = Object.assign(v, view)
            break
          }
        }
      },
      // 关闭右侧页签：保留当前页签及其左侧，固定页签（meta.affix）无论位置都保留；
      // 被关掉的页签同时从 cachedViews 与 iframeViews 中移除。
      // 如果当前路由根本不在页签列表里（index === -1），这里直接 return，
      // 返回的 Promise 会一直 pending —— 调用方不要依赖它来串联后续逻辑。
      delRightTags(view) {
        return new Promise(resolve => {
          const index = this.visitedViews.findIndex(v => v.path === view.path)
          if (index === -1) {
            return
          }
          this.visitedViews = this.visitedViews.filter((item, idx) => {
            if (idx <= index || (item.meta && item.meta.affix)) {
              return true
            }
            const i = this.cachedViews.indexOf(item.name)
            if (i > -1) {
              this.cachedViews.splice(i, 1)
            }
            if(item.meta.link) {
              const fi = this.iframeViews.findIndex(v => v.path === item.path)
              this.iframeViews.splice(fi, 1)
            }
            return false
          })
          resolve([...this.visitedViews])
        })
      },
      // 关闭左侧页签：保留当前页签及其右侧，固定页签（meta.affix）同样豁免；
      // 与 delRightTags 一样，目标页签不在列表时直接 return，Promise 会保持 pending。
      delLeftTags(view) {
        return new Promise(resolve => {
          const index = this.visitedViews.findIndex(v => v.path === view.path)
          if (index === -1) {
            return
          }
          this.visitedViews = this.visitedViews.filter((item, idx) => {
            if (idx >= index || (item.meta && item.meta.affix)) {
              return true
            }
            const i = this.cachedViews.indexOf(item.name)
            if (i > -1) {
              this.cachedViews.splice(i, 1)
            }
            if(item.meta.link) {
              const fi = this.iframeViews.findIndex(v => v.path === item.path)
              this.iframeViews.splice(fi, 1)
            }
            return false
          })
          resolve([...this.visitedViews])
        })
      }
    }
  })

export default useTagsViewStore
