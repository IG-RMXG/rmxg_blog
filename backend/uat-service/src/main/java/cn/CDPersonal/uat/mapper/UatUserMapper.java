package cn.CDPersonal.uat.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * UAT 测试账号的建号与查询（只操作 UAT 库 ry-cloud-uat）。
 *
 * 不复用 system-service 的用户体系代码——本服务保持最小 SQL 集合，
 * 只服务于假登录：ensure 测试账号、按用户名查 userId / 角色。
 */
@Mapper
public interface UatUserMapper {

    /** 按用户名查账号（sys_user），返回 userId / userName / nickName */
    @Select("""
            SELECT user_id AS userId, user_name AS userName, nick_name AS nickName, status, del_flag
            FROM sys_user
            WHERE user_name = #{userName}
              AND del_flag = '0'
            LIMIT 1
            """)
    Map<String, Object> selectByUserName(@Param("userName") String userName);

    /** 查某用户绑定的角色标识（sys_role / sys_user_role） */
    @Select("""
            SELECT r.role_key
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = '0'
              AND r.del_flag = '0'
            """)
    List<String> selectRoleKeys(@Param("userId") Long userId);

    /** 建 UAT 测试账号（密码字段填固定占位串，假登录不走密码） */
    @Insert("""
            INSERT INTO sys_user (user_name, nick_name, password, status, del_flag, create_by, remark)
            VALUES (#{userName}, #{nickName}, 'uat-only-no-login', '0', '0', 'uat', 'UAT 假登录专用账号')
            """)
    int insertUser(@Param("userName") String userName, @Param("nickName") String nickName);

    /** 给账号绑角色 */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 角色绑定是否存在 */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int countUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 库连通性探针 */
    @Select("SELECT 1")
    int ping();
}
