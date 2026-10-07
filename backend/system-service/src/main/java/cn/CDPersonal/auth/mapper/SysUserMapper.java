package cn.CDPersonal.auth.mapper;

import cn.CDPersonal.auth.domain.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("""
            SELECT u.user_id, u.user_name, u.nick_name, u.email, u.phonenumber,
                   u.sex, u.avatar, u.password, u.status, u.del_flag
            FROM sys_user u
            WHERE u.user_name = #{userName}
              AND u.del_flag = '0'
            LIMIT 1
            """)
    UserInfo selectByUserName(@Param("userName") String userName);

    @Select("""
            SELECT u.user_id, u.user_name, u.nick_name, u.email, u.phonenumber,
                   u.sex, u.avatar, u.password, u.status, u.del_flag
            FROM sys_user u
            WHERE u.user_id = #{userId}
              AND u.del_flag = '0'
            LIMIT 1
            """)
    UserInfo selectByUserId(@Param("userId") Long userId);

    /** 用户的角色标识，如 admin / common */
    @Select("""
            SELECT r.role_key
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = '0'
              AND r.del_flag = '0'
            """)
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /** 用户的权限标识，如 system:user:list */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId}
              AND m.status = '0'
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 按 id 批量查用户（在线用户列表要拼用户名和部门） */
    @Select("""
            <script>
            SELECT u.user_id, u.user_name, u.nick_name, u.email, u.phonenumber,
                   u.sex, u.avatar, u.password, u.status, u.del_flag
            FROM sys_user u
            WHERE u.del_flag = '0'
              AND u.user_id IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            </script>
            """)
    List<UserInfo> selectByIds(@Param("userIds") java.util.Collection<Long> userIds);
}
