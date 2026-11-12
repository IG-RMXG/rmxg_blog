 /**
 * v-hasRole 角色权限处理
 * Copyright (c) 2019 ruoyi
 */
import useUserStore from '@/store/modules/user'

// 使用方式：v-hasRole="['admin']"，值必须是角色标识数组，其他形态直接抛错（开发期约定）。
// 判定规则：用户角色含 'admin'（超管）或与数组中任一元素全等即保留元素；
// 不满足时在 mounted 阶段从 DOM 移除，不可恢复。
// 角色是粗粒度身份，接口级管控请用 v-hasPermi（权限码），业务按钮优先用后者。
export default {
  mounted(el, binding, vnode) {
    const { value } = binding
    const super_admin = "admin"
    const roles = useUserStore().roles

    if (value && value instanceof Array && value.length > 0) {
      const roleFlag = value

      const hasRole = roles.some(role => {
        return super_admin === role || roleFlag.includes(role)
      })

      if (!hasRole) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`请设置角色权限标签值`)
    }
  }
}
