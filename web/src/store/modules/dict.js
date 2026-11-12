// 字典缓存：把「字典类型 → 字典项数组」缓存在内存，避免同一页面多个组件重复请求字典接口
// （请求本身由 utils/dict.js 的 useDict 发起，命中缓存就不再调接口）。
// 只存内存，刷新即失效；dict 用数组而不是对象，因此 getDict 是线性查找。
const useDictStore = defineStore(
  'dict',
  {
    // 元素形如 { key: 'sys_normal_disable', value: [{ label, value, elTagType, elTagClass }] }，
    // value 的结构由 utils/dict.js 的 useDict 决定，字典组件按 label/value 渲染。
    state: () => ({
      dict: new Array()
    }),
    actions: {
      // 获取字典
      // 返回字典项数组；未命中返回 undefined（useDict 据此判断要不要发请求）。
      // 注意空值判断写成了 &&（_key == null && _key == ""），条件恒不成立，空 key 仍会进入下面的遍历。
      getDict(_key) {
        if (_key == null && _key == "") {
          return null
        }
        try {
          for (let i = 0; i < this.dict.length; i++) {
            if (this.dict[i].key == _key) {
              return this.dict[i].value
            }
          }
        } catch (e) {
          return null
        }
      },
      // 设置字典
      // 追加写入且不做去重：重复 set 同一 key 会多出一份数据，getDict 只返回第一个匹配项。
      // 并发场景（同一类型多个组件同时首次请求）就可能写出重复项，缓存命中逻辑只认第一个。
      setDict(_key, value) {
        if (_key !== null && _key !== "") {
          this.dict.push({
            key: _key,
            value: value
          })
        }
      },
      // 删除字典
      // 按 key 移除并返回是否命中；未命中返回 false，调用方一般忽略返回值。
      removeDict(_key) {
        var bln = false
        try {
          for (let i = 0; i < this.dict.length; i++) {
            if (this.dict[i].key == _key) {
              this.dict.splice(i, 1)
              return true
            }
          }
        } catch (e) {
          bln = false
        }
        return bln
      },
      // 清空字典
      // 退出登录、切换账号时调用，避免上一个账号的字典内容残留到当前会话。
      cleanDict() {
        this.dict = new Array()
      },
      // 初始字典
      // 预留的初始化钩子，当前是空实现，调用不产生任何副作用；保留它是为了让调用方无需判空。
      initDict() {
      }
    }
  })

export default useDictStore
