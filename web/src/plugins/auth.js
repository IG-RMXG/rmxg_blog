// 权限/角色判定工具（RuoYi 体系）。数据源是 user store 里登录后由 getInfo() 写入的
// permissions（形如 'blog:article:add'）与 roles（形如 'admin' / 'common'）。
// 被 v-hasPermi / v-hasRole 指令、动态路由过滤、菜单渲染复用；
// 所有方法都返回布尔值，空参数一律 false（未声明即不放行）。
// 这里只是展示层判断，真正的鉴权在后端，绕过前端不等于有权限。
import useUserStore from '@/store/modules/user'

// 单条权限判定：拥有通配权限 '*:*:*'（超管）或精确匹配目标权限码即通过。
// 只支持全等比较，不支持 'system:user:*' 这类前缀通配。
function authPermission(permission) {
  const all_permission = "*:*:*"
  const permissions = useUserStore().permissions
  if (permission && permission.length > 0) {
    return permissions.some(v => {
      return all_permission === v || v === permission
    })
  } else {
    return false
  }
}

// 单条角色判定：拥有 'admin'（RuoYi 超级管理员角色标识）或精确匹配目标角色即通过。
// 超管绕过写在角色判定里，意味着前端无法用角色做「除超管外」的反向判断，这类场景请改用权限码。
function authRole(role) {
  const super_admin = "admin"
  const roles = useUserStore().roles
  if (role && role.length > 0) {
    return roles.some(v => {
      return super_admin === v || v === role
    })
  } else {
    return false
  }
}

export default {
  // 验证用户是否具备某权限
  hasPermi(permission) {
    return authPermission(permission)
  },
  // 验证用户是否含有指定权限，只需包含其中一个
  hasPermiOr(permissions) {
    return permissions.some(item => {
      return authPermission(item)
    })
  },
  // 验证用户是否含有指定权限，必须全部拥有
  hasPermiAnd(permissions) {
    return permissions.every(item => {
      return authPermission(item)
    })
  },
  // 验证用户是否具备某角色
  hasRole(role) {
    return authRole(role)
  },
  // 验证用户是否含有指定角色，只需包含其中一个
  hasRoleOr(roles) {
    return roles.some(item => {
      return authRole(item)
    })
  },
  // 验证用户是否含有指定角色，必须全部拥有
  hasRoleAnd(roles) {
    return roles.every(item => {
      return authRole(item)
    })
  }
}
