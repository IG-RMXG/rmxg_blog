package cn.CDPersonal.auth.mapper;

import cn.CDPersonal.auth.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
