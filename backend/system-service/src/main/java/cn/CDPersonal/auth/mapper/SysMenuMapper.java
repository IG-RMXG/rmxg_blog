package cn.CDPersonal.auth.mapper;

import cn.CDPersonal.auth.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单查询 Mapper（注解式 SQL，无 XML）。
 *
 * 约定：路由类查询只取 status='0' 且 menu_type IN ('M','C') 的记录——按钮类型 F 只作权限点，
 * 不进路由；排序固定 parent_id, order_num，保证前端建树后同级顺序稳定。
 */
@Mapper
public interface SysMenuMapper {

    /**
     * 管理员：取全部正常菜单（目录 M + 菜单 C），按父级和排序号排列
     */
    @Select("""
            SELECT menu_id, menu_name, parent_id, order_num, path, component,
                   query, is_frame, is_cache, menu_type, visible, status, perms, icon
            FROM sys_menu
            WHERE menu_type IN ('M', 'C')
              AND status = '0'
            ORDER BY parent_id, order_num
            """)
    List<SysMenu> selectAllMenus();

    /**
     * 普通用户：只取该用户角色被授权的菜单
     */
    @Select("""
            SELECT DISTINCT m.menu_id, m.menu_name, m.parent_id, m.order_num, m.path,
                   m.component, m.query, m.is_frame, m.is_cache, m.menu_type,
                   m.visible, m.status, m.perms, m.icon
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId}
              AND m.menu_type IN ('M', 'C')
              AND m.status = '0'
            ORDER BY m.parent_id, m.order_num
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 全部权限标识。admin 角色在 sys_role_menu 里没有记录，
     * 所以不能按角色查权限，否则管理员会拿到空 permissions 数组。
     */
    @Select("""
            SELECT DISTINCT perms
            FROM sys_menu
            WHERE status = '0'
              AND perms IS NOT NULL
              AND perms <> ''
            """)
    List<String> selectAllPerms();
}
