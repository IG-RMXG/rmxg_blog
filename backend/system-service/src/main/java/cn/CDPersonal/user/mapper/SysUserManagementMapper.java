package cn.CDPersonal.user.mapper;

import cn.CDPersonal.common.domain.entity.SysDept;
import cn.CDPersonal.common.domain.entity.SysPost;
import cn.CDPersonal.common.domain.entity.SysRole;
import cn.CDPersonal.common.domain.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * 用户管理 mapper（/system/user/**）。
 *
 * 登录链路用的查询在 cn.CDPersonal.auth.mapper.SysUserMapper 里，那边不动；
 * 这里只放用户管理需要的列表 / 详情 / 增删改 / 关联表读写。
 *
 * 约定：
 *   - del_flag = '0' 有效，'2' 已软删；
 *   - 列表用 LEFT JOIN sys_dept 带出 dept.dept_name（前端表格绑 scope.row.dept.deptName）；
 *   - 关联表 sys_user_role / sys_user_post 一律"先删后插"。
 */
@Mapper
public interface SysUserManagementMapper {

    /**
     * 用户分页列表。
     *
     * deptId 走 params.deptIdScope（service 查 sys_dept 算出"本部门 + 所有子孙部门"的 id 集合），
     * 这样 deptId 能命中索引，也不用在 SQL 里写 FIND_IN_SET 子查询。
     *
     * dept 用 @One 子查询而不是 dept_xxx 列别名：
     * MyBatis 的嵌套自动映射只在 autoMappingBehavior=FULL 时才认 "dept.deptId"，
     * 本项目默认是 PARTIAL，实测 dept 直接是 null（前端"部门"列空白）。
     * 走 association 明确装配最稳，也不依赖全局配置。
     */
    @Select("""
            <script>
            SELECT u.user_id, u.dept_id, u.user_name, u.nick_name, u.user_type, u.email,
                   u.phonenumber, u.sex, u.avatar, u.status, u.del_flag,
                   u.login_ip, u.login_date, u.create_by, u.create_time,
                   u.update_by, u.update_time, u.remark
            FROM sys_user u
            <where>
              u.del_flag = '0'
              <if test="userName != null and userName != ''">
                AND u.user_name LIKE CONCAT('%', #{userName}, '%')
              </if>
              <if test="nickName != null and nickName != ''">
                AND u.nick_name LIKE CONCAT('%', #{nickName}, '%')
              </if>
              <if test="phonenumber != null and phonenumber != ''">
                AND u.phonenumber LIKE CONCAT('%', #{phonenumber}, '%')
              </if>
              <if test="status != null and status != ''">
                AND u.status = #{status}
              </if>
              <if test="email != null and email != ''">
                AND u.email LIKE CONCAT('%', #{email}, '%')
              </if>
              <if test="params.deptIdScope != null">
                AND u.dept_id IN
                <foreach collection="params.deptIdScope" item="scopeDeptId"
                         open="(" separator="," close=")">
                  #{scopeDeptId}
                </foreach>
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND u.create_time &gt;= #{params.beginTime}
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND u.create_time &lt;= #{params.endTime}
              </if>
            </where>
            ORDER BY u.user_id ASC
            </script>
            """)
    @Results(id = "sysUserWithDept", value = {
            @Result(property = "userId", column = "user_id", id = true),
            @Result(property = "deptId", column = "dept_id"),
            @Result(property = "userName", column = "user_name"),
            @Result(property = "nickName", column = "nick_name"),
            @Result(property = "userType", column = "user_type"),
            @Result(property = "phonenumber", column = "phonenumber"),
            @Result(property = "delFlag", column = "del_flag"),
            @Result(property = "loginIp", column = "login_ip"),
            @Result(property = "loginDate", column = "login_date"),
            @Result(property = "dept", column = "dept_id",
                    one = @One(select = "cn.CDPersonal.user.mapper.SysDeptQueryMapper.selectDeptForUser"))
    })
    List<SysUser> selectUserList(SysUser query);

    /** 用户详情（含部门对象） */
    @Select("""
            SELECT u.user_id, u.dept_id, u.user_name, u.nick_name, u.user_type, u.email,
                   u.phonenumber, u.sex, u.avatar, u.password, u.status, u.del_flag,
                   u.login_ip, u.login_date, u.pwd_update_date, u.create_by, u.create_time,
                   u.update_by, u.update_time, u.remark
            FROM sys_user u
            WHERE u.user_id = #{userId}
            """)
    @Results(id = "sysUserDetail", value = {
            @Result(property = "userId", column = "user_id", id = true),
            @Result(property = "deptId", column = "dept_id"),
            @Result(property = "userName", column = "user_name"),
            @Result(property = "nickName", column = "nick_name"),
            @Result(property = "userType", column = "user_type"),
            @Result(property = "phonenumber", column = "phonenumber"),
            @Result(property = "delFlag", column = "del_flag"),
            @Result(property = "loginIp", column = "login_ip"),
            @Result(property = "loginDate", column = "login_date"),
            @Result(property = "pwdUpdateDate", column = "pwd_update_date"),
            @Result(property = "dept", column = "dept_id",
                    one = @One(select = "cn.CDPersonal.user.mapper.SysDeptQueryMapper.selectDeptForUser"))
    })
    SysUser selectUserById(@Param("userId") Long userId);

    /** 用户名是否已存在（调用方负责排除自身） */
    @Select("""
            SELECT user_id, user_name FROM sys_user
            WHERE user_name = #{userName} AND del_flag = '0'
            LIMIT 1
            """)
    SysUser checkUserNameUnique(@Param("userName") String userName);

    /** 手机号是否已存在（调用方负责排除自身） */
    @Select("""
            SELECT user_id, phonenumber FROM sys_user
            WHERE phonenumber = #{phonenumber} AND del_flag = '0'
            LIMIT 1
            """)
    SysUser checkPhoneUnique(@Param("phonenumber") String phonenumber);

    /** 邮箱是否已存在（调用方负责排除自身） */
    @Select("""
            SELECT user_id, email FROM sys_user
            WHERE email = #{email} AND del_flag = '0'
            LIMIT 1
            """)
    SysUser checkEmailUnique(@Param("email") String email);

    /**
     * 给 /role/authUser/allocatedList 与 unallocatedList 用的分页查询。
     * allocated=true  → 该角色已分配的用户
     * allocated=false → 该角色未分配的用户
     */
    @Select("""
            <script>
            SELECT u.user_id, u.dept_id, u.user_name, u.nick_name, u.user_type, u.email,
                   u.phonenumber, u.sex, u.avatar, u.status, u.del_flag,
                   u.create_by, u.create_time, u.update_by, u.update_time, u.remark
            FROM sys_user u
            WHERE u.del_flag = '0'
              <if test="query.userName != null and query.userName != ''">
                AND u.user_name LIKE CONCAT('%', #{query.userName}, '%')
              </if>
              <if test="query.phonenumber != null and query.phonenumber != ''">
                AND u.phonenumber LIKE CONCAT('%', #{query.phonenumber}, '%')
              </if>
              <choose>
                <when test="allocated">
                  AND EXISTS (SELECT 1 FROM sys_user_role ur
                              WHERE ur.user_id = u.user_id AND ur.role_id = #{roleId})
                </when>
                <otherwise>
                  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur
                                  WHERE ur.user_id = u.user_id AND ur.role_id = #{roleId})
                </otherwise>
              </choose>
            ORDER BY u.user_id ASC
            </script>
            """)
    @Results(id = "sysUserByRole", value = {
            @Result(property = "userId", column = "user_id", id = true),
            @Result(property = "deptId", column = "dept_id"),
            @Result(property = "userName", column = "user_name"),
            @Result(property = "nickName", column = "nick_name"),
            @Result(property = "userType", column = "user_type"),
            @Result(property = "phonenumber", column = "phonenumber"),
            @Result(property = "delFlag", column = "del_flag"),
            @Result(property = "dept", column = "dept_id",
                    one = @One(select = "cn.CDPersonal.user.mapper.SysDeptQueryMapper.selectDeptForUser"))
    })
    List<SysUser> selectUserListByRole(@Param("query") SysUser query,
                                       @Param("roleId") Long roleId,
                                       @Param("allocated") boolean allocated);

    // ==================== 增删改 ====================

    @Insert("""
            INSERT INTO sys_user
              (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
               password, status, del_flag, pwd_update_date, create_by, create_time, remark)
            VALUES
              (#{deptId}, #{userName}, #{nickName}, #{userType}, #{email}, #{phonenumber}, #{sex}, #{avatar},
               #{password}, #{status}, '0', NOW(), #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insertUser(SysUser user);

    /**
     * 修改用户基础信息。
     * 刻意不写 password：改资料时前端不会传密码，用 COALESCE(NULLIF(...)) 反而容易踩空，
     * 密码走 resetPwd / updatePwd 两条独立通道。
     */
    @Update("""
            UPDATE sys_user
            SET dept_id = #{deptId},
                nick_name = #{nickName},
                email = #{email},
                phonenumber = #{phonenumber},
                sex = #{sex},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE user_id = #{userId}
            """)
    int updateUser(SysUser user);

    /** 个人资料修改：只允许改昵称 / 手机 / 邮箱 / 性别 / 头像 */
    @Update("""
            UPDATE sys_user
            SET nick_name = #{nickName},
                email = #{email},
                phonenumber = #{phonenumber},
                sex = #{sex},
                avatar = #{avatar},
                update_time = NOW()
            WHERE user_id = #{userId}
            """)
    int updateUserProfile(SysUser user);

    /** 改密（BCrypt 哈希由 service 传入） */
    @Update("""
            UPDATE sys_user
            SET password = #{password},
                pwd_update_date = NOW(),
                update_time = NOW()
            WHERE user_id = #{userId}
            """)
    int updateUserPassword(@Param("userId") Long userId, @Param("password") String password);

    @Update("""
            UPDATE sys_user
            SET status = #{status}, update_by = #{updateBy}, update_time = NOW()
            WHERE user_id = #{userId}
            """)
    int updateUserStatus(SysUser user);

    /** 软删：del_flag = '2' */
    @Update("""
            <script>
            UPDATE sys_user
            SET del_flag = '2', update_time = NOW()
            WHERE user_id IN
            <foreach collection="array" item="userId" open="(" separator="," close=")">
              #{userId}
            </foreach>
            </script>
            """)
    int softDeleteUserByIds(@Param("array") Long[] userIds);

    // ==================== sys_user_role ====================

    @Select("""
            SELECT r.role_id, r.role_name, r.role_key, r.role_sort, r.data_scope,
                   r.menu_check_strictly, r.dept_check_strictly, r.status, r.del_flag,
                   r.create_by, r.create_time, r.update_by, r.update_time, r.remark
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.role_id
            WHERE ur.user_id = #{userId}
              AND r.del_flag = '0'
            ORDER BY r.role_sort
            """)
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT role_id FROM sys_user_role WHERE user_id = #{userId}
            """)
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRolesByUserId(@Param("userId") Long userId);

    @Delete("""
            <script>
            DELETE FROM sys_user_role WHERE user_id IN
            <foreach collection="array" item="userId" open="(" separator="," close=")">
              #{userId}
            </foreach>
            </script>
            """)
    int deleteUserRolesByUserIds(@Param("array") Long[] userIds);

    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="roleIds" item="roleId" separator=",">
              (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int batchInsertUserRoles(@Param("userId") Long userId,
                             @Param("roleIds") Long[] roleIds);

    // ==================== sys_user_post ====================

    @Select("""
            SELECT p.post_id, p.post_code, p.post_name, p.post_sort, p.status,
                   p.create_by, p.create_time, p.update_by, p.update_time, p.remark
            FROM sys_post p
            JOIN sys_user_post up ON up.post_id = p.post_id
            WHERE up.user_id = #{userId}
            ORDER BY p.post_sort
            """)
    List<SysPost> selectPostsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT post_id FROM sys_user_post WHERE user_id = #{userId}
            """)
    List<Long> selectPostIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    int deleteUserPostsByUserId(@Param("userId") Long userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_post (user_id, post_id) VALUES
            <foreach collection="postIds" item="postId" separator=",">
              (#{userId}, #{postId})
            </foreach>
            </script>
            """)
    int batchInsertUserPosts(@Param("userId") Long userId,
                             @Param("postIds") Long[] postIds);

    /** 批量查用户（导出 / 其它模块用） */
    @Select("""
            <script>
            SELECT u.user_id, u.dept_id, u.user_name, u.nick_name, u.email,
                   u.phonenumber, u.sex, u.avatar, u.status, u.del_flag,
                   u.create_time, u.remark
            FROM sys_user u
            WHERE u.del_flag = '0'
              AND u.user_id IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            </script>
            """)
    List<SysUser> selectUserByIds(@Param("userIds") Collection<Long> userIds);
}
