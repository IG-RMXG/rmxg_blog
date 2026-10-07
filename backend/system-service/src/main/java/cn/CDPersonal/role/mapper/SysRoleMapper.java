package cn.CDPersonal.role.mapper;

import cn.CDPersonal.common.domain.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 角色管理 mapper（/system/role/**）。
 *
 * 在 RuoYi 里角色还会带 sys_role_dept（数据权限）和 sys_role_menu（菜单权限），
 * 这两张关联表都归角色这边维护：
 *   - 角色列表/详情：不用 join 关联表；
 *   - 新增/修改角色：重写 sys_role_menu；
 *   - dataScope：重写 sys_role_dept；
 *   - 删除角色：清理 sys_role_menu / sys_role_dept / sys_user_role。
 *
 * del_flag = '0' 有效，'2' 已软删。
 */
@Mapper
public interface SysRoleMapper {

    @Select("""
            <script>
            SELECT role_id, role_name, role_key, role_sort, data_scope,
                   menu_check_strictly, dept_check_strictly, status, del_flag,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_role
            <where>
              del_flag = '0'
              <if test="roleName != null and roleName != ''">
                AND role_name LIKE CONCAT('%', #{roleName}, '%')
              </if>
              <if test="roleKey != null and roleKey != ''">
                AND role_key LIKE CONCAT('%', #{roleKey}, '%')
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND create_time &gt;= #{params.beginTime}
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND create_time &lt;= #{params.endTime}
              </if>
            </where>
            ORDER BY role_sort ASC, role_id ASC
            </script>
            """)
    List<SysRole> selectRoleList(SysRole query);

    @Select("""
            SELECT role_id, role_name, role_key, role_sort, data_scope,
                   menu_check_strictly, dept_check_strictly, status, del_flag,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_role
            WHERE role_id = #{roleId} AND del_flag = '0'
            """)
    SysRole selectRoleById(@Param("roleId") Long roleId);

    /** 全部有效角色，给"用户分配角色"下拉用 */
    @Select("""
            SELECT role_id, role_name, role_key, role_sort, status, del_flag, create_time
            FROM sys_role
            WHERE del_flag = '0'
            ORDER BY role_sort ASC, role_id ASC
            """)
    List<SysRole> selectRoleAll();

    /** 角色名称唯一性校验（调用方排除自身） */
    @Select("""
            SELECT role_id, role_name FROM sys_role
            WHERE role_name = #{roleName} AND del_flag = '0'
            LIMIT 1
            """)
    SysRole checkRoleNameUnique(@Param("roleName") String roleName);

    /** 权限字符唯一性校验（调用方排除自身） */
    @Select("""
            SELECT role_id, role_key FROM sys_role
            WHERE role_key = #{roleKey} AND del_flag = '0'
            LIMIT 1
            """)
    SysRole checkRoleKeyUnique(@Param("roleKey") String roleKey);

    @Insert("""
            INSERT INTO sys_role
              (role_name, role_key, role_sort, data_scope, menu_check_strictly,
               dept_check_strictly, status, del_flag, create_by, create_time, remark)
            VALUES
              (#{roleName}, #{roleKey}, #{roleSort}, #{dataScope}, #{menuCheckStrictly},
               #{deptCheckStrictly}, #{status}, '0', #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "roleId")
    int insertRole(SysRole role);

    @Update("""
            UPDATE sys_role
            SET role_name = #{roleName},
                role_key = #{roleKey},
                role_sort = #{roleSort},
                data_scope = #{dataScope},
                menu_check_strictly = #{menuCheckStrictly},
                dept_check_strictly = #{deptCheckStrictly},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE role_id = #{roleId}
            """)
    int updateRole(SysRole role);

    /** 软删角色 */
    @Update("""
            <script>
            UPDATE sys_role
            SET del_flag = '2', update_time = NOW()
            WHERE role_id IN
            <foreach collection="array" item="roleId" open="(" separator="," close=")">
              #{roleId}
            </foreach>
            </script>
            """)
    int softDeleteRoleByIds(@Param("array") Long[] roleIds);

    /** 只改状态（前端表格里的开关） */
    @Update("""
            UPDATE sys_role
            SET status = #{status}, update_by = #{updateBy}, update_time = NOW()
            WHERE role_id = #{roleId}
            """)
    int updateRoleStatus(SysRole role);

    /** 只改数据权限（roleId + dataScope），sys_role_dept 由 service 单独重写 */
    @Update("""
            UPDATE sys_role
            SET data_scope = #{dataScope},
                update_by = #{updateBy},
                update_time = NOW()
            WHERE role_id = #{roleId}
            """)
    int updateRoleDataScope(SysRole role);

    // ==================== sys_role_menu ====================

    @Select("""
            SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}
            """)
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenusByRoleId(@Param("roleId") Long roleId);

    @Delete("""
            <script>
            DELETE FROM sys_role_menu WHERE role_id IN
            <foreach collection="array" item="roleId" open="(" separator="," close=")">
              #{roleId}
            </foreach>
            </script>
            """)
    int deleteRoleMenusByRoleIds(@Param("array") Long[] roleIds);

    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="menuIds" item="menuId" separator=",">
              (#{roleId}, #{menuId})
            </foreach>
            </script>
            """)
    int batchInsertRoleMenus(@Param("roleId") Long roleId,
                             @Param("menuIds") Long[] menuIds);

    // ==================== sys_role_dept ====================

    @Select("""
            SELECT dept_id FROM sys_role_dept WHERE role_id = #{roleId}
            """)
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_dept WHERE role_id = #{roleId}")
    int deleteRoleDeptsByRoleId(@Param("roleId") Long roleId);

    @Delete("""
            <script>
            DELETE FROM sys_role_dept WHERE role_id IN
            <foreach collection="array" item="roleId" open="(" separator="," close=")">
              #{roleId}
            </foreach>
            </script>
            """)
    int deleteRoleDeptsByRoleIds(@Param("array") Long[] roleIds);

    @Insert("""
            <script>
            INSERT INTO sys_role_dept (role_id, dept_id) VALUES
            <foreach collection="deptIds" item="deptId" separator=",">
              (#{roleId}, #{deptId})
            </foreach>
            </script>
            """)
    int batchInsertRoleDepts(@Param("roleId") Long roleId,
                             @Param("deptIds") Long[] deptIds);

    // ==================== sys_user_role ====================

    /** 角色下是否还有用户（删除角色前校验） */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countUserRoleByRoleId(@Param("roleId") Long roleId);

    @Delete("""
            DELETE FROM sys_user_role
            WHERE user_id = #{userId} AND role_id = #{roleId}
            """)
    int deleteUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("""
            <script>
            DELETE FROM sys_user_role
            WHERE role_id = #{roleId} AND user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">
              #{userId}
            </foreach>
            </script>
            """)
    int deleteUserRolesByRoleIdAndUserIds(@Param("roleId") Long roleId,
                                          @Param("userIds") Long[] userIds);

    @Delete("""
            <script>
            DELETE FROM sys_user_role WHERE role_id IN
            <foreach collection="array" item="roleId" open="(" separator="," close=")">
              #{roleId}
            </foreach>
            </script>
            """)
    int deleteUserRolesByRoleIds(@Param("array") Long[] roleIds);

    /**
     * 批量授权。
     *
     * 用 INSERT IGNORE 而不是 INSERT：sys_user_role 是联合主键，
     * 前端"添加用户"弹窗里如果重复勾了已授权的用户，普通 INSERT 会整批 500。
     */
    @Insert("""
            <script>
            INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="userIds" item="userId" separator=",">
              (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int batchInsertUserRoles(@Param("roleId") Long roleId,
                             @Param("userIds") Long[] userIds);
}
