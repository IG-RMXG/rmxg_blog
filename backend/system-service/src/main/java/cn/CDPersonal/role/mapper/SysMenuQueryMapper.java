package cn.CDPersonal.role.mapper;

import cn.CDPersonal.auth.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 ⇄ 菜单（sys_menu / sys_role_menu）的只读查询。
 *
 * 为什么不写进 auth.mapper.SysMenuMapper：那个文件归"菜单路由"用，
 * 而且 menu 模块的 CRUD 由另一位同事在同一个文件里加方法，分开放能少冲突。
 *
 * 注意：这里的菜单查询**不带 status='0' 过滤** —— 角色配菜单时要看到全部
 * （含停用菜单和 F 类型的按钮），否则编辑角色时已勾选的按钮会"消失"。
 */
@Mapper
public interface SysMenuQueryMapper {

    // menuTypes 传 null 或空集合时不会拼 WHERE，等价于查全表；这里也不带 status / visible 过滤，
    // 取舍见类头说明。
    /**
     * 按菜单类型查全部菜单（不组装树，service 自己建）。
     */
    @Select("""
            <script>
            SELECT menu_id, menu_name, parent_id, order_num, path, component,
                   query, is_frame, is_cache, menu_type, visible, status, perms, icon
            FROM sys_menu
            <if test="menuTypes != null and menuTypes.size() > 0">
              WHERE menu_type IN
              <foreach collection="menuTypes" item="menuType" open="(" separator="," close=")">
                #{menuType}
              </foreach>
            </if>
            ORDER BY parent_id, order_num
            </script>
            """)
    List<SysMenu> selectMenuListByTypes(@Param("menuTypes") List<String> menuTypes);

    /** 该角色已勾选的菜单 id */
    @Select("""
            SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}
            """)
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 菜单是否被任一角色引用（菜单模块删除校验用，这里顺手提供） */
    @Select("SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = #{menuId}")
    int countRoleMenuByMenuId(@Param("menuId") Long menuId);

    // ==================== 菜单管理 CRUD（菜单管理页用） ====================

    /** 菜单列表，带 menuName / visible / status 过滤 */
    @Select("""
            <script>
            SELECT menu_id, menu_name, parent_id, order_num, path, component,
                   query, is_frame, is_cache, menu_type, visible, status, perms, icon
            FROM sys_menu
            <where>
              <if test="menuName != null and menuName != ''">
                AND menu_name LIKE CONCAT('%', #{menuName}, '%')
              </if>
              <if test="visible != null and visible != ''">
                AND visible = #{visible}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
            </where>
            ORDER BY parent_id, order_num
            </script>
            """)
    List<SysMenu> selectMenuList(SysMenu query);

    @Select("""
            SELECT menu_id, menu_name, parent_id, order_num, path, component,
                   query, is_frame, is_cache, menu_type, visible, status, perms, icon
            FROM sys_menu WHERE menu_id = #{menuId}
            """)
    SysMenu selectMenuById(@Param("menuId") Long menuId);

    /** 是否存在子菜单（删除前校验） */
    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{menuId}")
    int countChildrenByParentId(@Param("menuId") Long menuId);

    /**
     * 同级（同 parentId）下菜单名是否重复。
     * menuId 传 null 表示新增场景；传值时会排除自身，用于修改时校验。
     */
    @Select("""
            SELECT COUNT(*) FROM sys_menu
            WHERE parent_id = #{parentId} AND menu_name = #{menuName}
              AND (#{menuId} IS NULL OR menu_id != #{menuId})
            """)
    int countMenuNameDuplicate(@Param("parentId") Long parentId,
                               @Param("menuName") String menuName,
                               @Param("menuId") Long menuId);

    /**
     * 新增菜单。create_time 由数据库 NOW() 生成；useGeneratedKeys 会把
     * 自增主键回填到 menu.getMenuId()，后续角色授权依赖这个值。
     */
    @org.apache.ibatis.annotations.Insert("""
            INSERT INTO sys_menu
              (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
               menu_type, visible, status, perms, icon, create_by, create_time)
            VALUES
              (#{menuName}, #{parentId}, #{orderNum}, #{path}, #{component}, #{query},
               #{isFrame}, #{isCache}, #{menuType}, #{visible}, #{status}, #{perms}, #{icon},
               #{createBy}, NOW())
            """)
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "menuId")
    int insertMenu(SysMenu menu);

    @org.apache.ibatis.annotations.Update("""
            UPDATE sys_menu
            SET menu_name = #{menuName},
                parent_id = #{parentId},
                order_num = #{orderNum},
                path = #{path},
                component = #{component},
                query = #{query},
                is_frame = #{isFrame},
                is_cache = #{isCache},
                menu_type = #{menuType},
                visible = #{visible},
                status = #{status},
                perms = #{perms},
                icon = #{icon},
                update_by = #{updateBy},
                update_time = NOW()
            WHERE menu_id = #{menuId}
            """)
    int updateMenu(SysMenu menu);

    /**
     * 物理删除：sys_menu 没有 del_flag，删掉即不可恢复。
     * 因此调用方需先用 countChildrenByParentId / countRoleMenuByMenuId 挡住误删。
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_menu WHERE menu_id = #{menuId}")
    int deleteMenuById(@Param("menuId") Long menuId);

    /** 删除菜单时清理角色关联，避免 sys_role_menu 留下脏数据 */
    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteRoleMenuByMenuId(@Param("menuId") Long menuId);
}
