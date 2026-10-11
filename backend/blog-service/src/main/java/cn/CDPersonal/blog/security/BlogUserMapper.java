package cn.CDPersonal.blog.security;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 鉴权用的只读查询（sys_user / sys_role / sys_user_role）。
 *
 * 这些表由 system-service 维护，本服务只读；库与 system-service 共用同一套
 * （ry-cloud），因此不重复造用户体系。
 */
@Mapper
public interface BlogUserMapper {

    @Select("""
            SELECT user_id, user_name, nick_name, avatar, status, del_flag
            FROM sys_user
            WHERE user_id = #{userId}
              AND del_flag = '0'
            LIMIT 1
            """)
    BlogUser selectByUserId(@Param("userId") Long userId);

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
}
