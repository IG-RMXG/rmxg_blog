/**
 * 字典数据 hook：一次取多个字典类型，供下拉框、表格标签等渲染。
 *
 * 取数顺序：先查 pinia 的 useDictStore（纯内存，刷新页面即失效），未命中才调
 * GET /system/dict/data/type/{dictType}；响应 res.data 是字典数组，这里映射成
 * { label, value, elTagType, elTagClass } 后再回写缓存。
 *
 * 用法：setup 中同步调用 `const { sys_yes_no } = useDict('sys_yes_no')`，
 * 首次返回空数组，接口回来后由返回的 ref 驱动模板更新。
 * 注意 ref / toRefs 由 unplugin-auto-import 自动导入，本文件未显式 import。
 */
import useDictStore from '@/store/modules/dict'
import { getDicts } from '@/api/system/dict/data'

/**
 * 获取字典数据
 */
export function useDict(...args) {
  // 以 dictType 为键聚合，返回 toRefs 后解构出的仍是 ref，模板里可直接当数组用
  const res = ref({})
  // 立即执行一次：在 setup 阶段同步完成参数遍历、缓存查询与请求发起
  return (() => {
    args.forEach((dictType, index) => {
      res.value[dictType] = []
      const dicts = useDictStore().getDict(dictType)
      // 命中缓存（含空数组）就不再请求，避免同一字典在多个页面重复拉取
      if (dicts) {
        res.value[dictType] = dicts
      } else {
        getDicts(dictType).then(resp => {
          res.value[dictType] = resp.data.map(p => ({ label: p.dictLabel, value: p.dictValue, elTagType: p.listClass, elTagClass: p.cssClass }))
          useDictStore().setDict(dictType, res.value[dictType])
        })
      }
    })
    return toRefs(res.value)
  })()
}