package cn.CDPersonal.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import cn.CDPersonal.common.domain.entity.SysDept;

/**
 * 用户模块里对 sys_dept 的只读查询。
 *
 * 为什么单独放一个 mapper 而不是往 cn.CDPersonal.dept.mapper.SysDeptMapper 里加：
 * 那个文件归部门模块（另一位同事在写），这里只做用户列表的"含下级部门"过滤，
 * 各写各的互不影响。
 *
 * ancestors 的格式是 "0,100,101"（不含自身），所以判断子孙用 FIND_IN_SET。
 */
@Mapper
public interface SysDeptQueryMapper {

    /**
     * 本部门 + 所有子孙部门的 id，用于用户列表按 deptId 递归过滤。
     * 以 selected 开头是为了排序稳定，方便日志排查。
     */
    @Select("""
            SELECT dept_id FROM sys_dept
            WHERE del_flag = '0'
              AND (dept_id = #{deptId} OR FIND_IN_SET(#{deptId}, IFNULL(ancestors, '')))
            ORDER BY (dept_id = #{deptId}) DESC, dept_id
            """)
    List<Long> selectDeptIdAndChildren(@Param("deptId") Long deptId);

    /**
     * 单个部门，供用户列表 / 详情用 @One 装配 SysUser.dept。
     *
     * 前端表格绑的是 scope.row.dept.deptName，所以这个对象必须有；
     * 但祖先 / 父级等字段用不到，只查必要的列。
     */
    @Select("""
            SELECT dept_id, parent_id, ancestors, dept_name, order_num,
                   leader, phone, email, status, del_flag
            FROM sys_dept
            WHERE dept_id = #{deptId}
            """)
    SysDept selectDeptForUser(@Param("deptId") Long deptId);
}
