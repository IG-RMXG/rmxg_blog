package cn.CDPersonal.dept.mapper;

import cn.CDPersonal.common.domain.entity.SysDept;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门 mapper。
 *
 * 列表刻意返回**扁平数组**（不组装 children），由前端 handleTree 建树。
 * del_flag = '0' 才算有效部门。
 */
@Mapper
public interface SysDeptMapper {

    /** 部门列表：扁平结构，parent_name 由自连接 LEFT JOIN 得到 */
    @Select("""
            <script>
            SELECT d.dept_id, d.parent_id, d.ancestors, d.dept_name, d.order_num,
                   d.leader, d.phone, d.email, d.status, d.del_flag,
                   d.create_by, d.create_time, d.update_by, d.update_time,
                   p.dept_name AS parent_name
            FROM sys_dept d
            LEFT JOIN sys_dept p ON p.dept_id = d.parent_id
            <where>
              d.del_flag = '0'
              <if test="deptName != null and deptName != ''">
                AND d.dept_name LIKE CONCAT('%', #{deptName}, '%')
              </if>
              <if test="status != null and status != ''">
                AND d.status = #{status}
              </if>
            </where>
            ORDER BY d.parent_id, d.order_num
            </script>
            """)
    List<SysDept> selectDeptList(SysDept query);

    /**
     * 部门列表（排除某个部门及其所有子孙），给"上级部门"下拉用。
     * ancestors 形如 0,100,101，所以用 FIND_IN_SET 判断 deptId 是否在祖级链里。
     */
    @Select("""
            SELECT d.dept_id, d.parent_id, d.ancestors, d.dept_name, d.order_num,
                   d.leader, d.phone, d.email, d.status, d.del_flag,
                   d.create_by, d.create_time, d.update_by, d.update_time,
                   p.dept_name AS parent_name
            FROM sys_dept d
            LEFT JOIN sys_dept p ON p.dept_id = d.parent_id
            WHERE d.del_flag = '0'
              AND d.dept_id != #{deptId}
              AND NOT FIND_IN_SET(#{deptId}, IFNULL(d.ancestors, ''))
            ORDER BY d.parent_id, d.order_num
            """)
    List<SysDept> selectDeptListExcludeChildren(@Param("deptId") Long deptId);

    @Select("""
            SELECT d.dept_id, d.parent_id, d.ancestors, d.dept_name, d.order_num,
                   d.leader, d.phone, d.email, d.status, d.del_flag,
                   d.create_by, d.create_time, d.update_by, d.update_time,
                   p.dept_name AS parent_name
            FROM sys_dept d
            LEFT JOIN sys_dept p ON p.dept_id = d.parent_id
            WHERE d.dept_id = #{deptId}
            """)
    SysDept selectDeptById(@Param("deptId") Long deptId);

    /** 是否存在未删除的下级部门 */
    @Select("SELECT COUNT(*) FROM sys_dept WHERE parent_id = #{deptId} AND del_flag = '0'")
    int countChildrenByParentId(@Param("deptId") Long deptId);

    /** 部门下是否还有用户（用于删除校验） */
    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId} AND del_flag = '0'")
    int countUserByDeptId(@Param("deptId") Long deptId);

    @Insert("""
            INSERT INTO sys_dept
              (parent_id, ancestors, dept_name, order_num, leader, phone, email,
               status, del_flag, create_by, create_time)
            VALUES
              (#{parentId}, #{ancestors}, #{deptName}, #{orderNum}, #{leader}, #{phone}, #{email},
               #{status}, '0', #{createBy}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "deptId")
    int insertDept(SysDept dept);

    @Update("""
            UPDATE sys_dept
            SET parent_id = #{parentId},
                ancestors = #{ancestors},
                dept_name = #{deptName},
                order_num = #{orderNum},
                leader = #{leader},
                phone = #{phone},
                email = #{email},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW()
            WHERE dept_id = #{deptId}
            """)
    int updateDept(SysDept dept);

    /** 软删：del_flag = '2' */
    @Update("""
            UPDATE sys_dept
            SET del_flag = '2', update_time = NOW()
            WHERE dept_id = #{deptId}
            """)
    int deleteDeptById(@Param("deptId") Long deptId);

    /**
     * 级联更新子孙部门的 ancestors。
     *
     * oldPrefix = 旧 ancestors + "," + 自身id，例如 0,100
     * newPrefix = 新 ancestors + "," + 自身id，例如 0,200
     * 子孙的 ancestors 都以 oldPrefix 开头，把前缀替换掉即可，不用逐条 CASE WHEN。
     */
    @Update("""
            UPDATE sys_dept
            SET ancestors = CONCAT(#{newPrefix}, SUBSTRING(ancestors, LENGTH(#{oldPrefix}) + 1)),
                update_time = NOW()
            WHERE dept_id != #{deptId}
              AND del_flag = '0'
              AND ancestors LIKE CONCAT(#{oldPrefix}, '%')
            """)
    int updateChildrenAncestors(@Param("oldPrefix") String oldPrefix,
                                @Param("newPrefix") String newPrefix,
                                @Param("deptId") Long deptId);
}
