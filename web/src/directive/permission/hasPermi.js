 /**
 * v-hasPermi 操作权限处理
 * Copyright (c) 2019 ruoyi
 */
import useUserStore from '@/store/modules/user'

// 使用方式：v-hasPermi="['system:user:add']"，值必须是权限码数组，其他形态直接抛错（开发期约定）。
// 判定规则：用户权限里含 '*:*:*'（超管通配）或与数组中任一元素全等即保留元素；
// 不满足时在 mounted 阶段把元素从 DOM 移除，是「删除」而非隐藏，之后无法再恢复。
// 权限数据来自 user store，必须等路由守卫里的 getInfo() 完成后再渲染相关组件，否则会误删。
// 仅控制按钮可见性，接口权限仍由后端校验。
export default {
  mounted(el, binding, vnode) {
    const { value } = binding
    const all_permission = "*:*:*"
    const permissions = useUserStore().permissions

    if (value && value instanceof Array && value.length > 0) {
      const permissionFlag = value

      const hasPermissions = permissions.some(permission => {
        return all_permission === permission || permissionFlag.includes(permission)
      })

      if (!hasPermissions) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`请设置操作权限标签值`)
    }
  }
}
