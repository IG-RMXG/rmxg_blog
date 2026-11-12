/**
 * 按钮级权限判断，配合 v-hasPermi / v-hasRole 指令以及页面内的 v-if 使用。
 *
 * 数据来源：user store 的 permissions / roles，登录后由 getInfo 写入；
 * 因此必须在 store 初始化之后调用（组件 setup、指令 mounted 等时机），
 * 不能在模块顶层直接调用。
 */
import useUserStore from '@/store/modules/user'

// 传入权限标识数组（如 ['system:user:add']）时命中任意一项即通过；
// 超级权限 *:*:* 会短路所有判断。
/**
 * 字符权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkPermi(value) {
  if (value && value instanceof Array && value.length > 0) {
    const permissions = useUserStore().permissions
    const permissionDatas = value
    const all_permission = "*:*:*"

    const hasPermission = permissions.some(permission => {
      return all_permission === permission || permissionDatas.includes(permission)
    })

    if (!hasPermission) {
      return false
    }
    return true
  } else {
    console.error(`need roles! Like checkPermi="['system:user:add','system:user:edit']"`)
    return false
  }
}

// 传角色 key 数组（如 ['admin','editor']）；admin 视为拥有全部角色
/**
 * 角色权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkRole(value) {
  if (value && value instanceof Array && value.length > 0) {
    const roles = useUserStore().roles
    const permissionRoles = value
    const super_admin = "admin"

    const hasRole = roles.some(role => {
      return super_admin === role || permissionRoles.includes(role)
    })

    if (!hasRole) {
      return false
    }
    return true
  } else {
    console.error(`need roles! Like checkRole="['admin','editor']"`)
    return false
  }
}