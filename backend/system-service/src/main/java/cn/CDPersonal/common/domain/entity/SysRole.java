package cn.CDPersonal.common.domain.entity;

import cn.CDPersonal.common.domain.model.BaseEntity;

import java.io.Serial;
import java.util.List;

/**
 * 系统角色，对应 ry-cloud.sys_role。
 *
 * deptIds / menuIds 不是表里的列，是接口入参：
 *   - PUT /role/dataScope  用 deptIds 重写 sys_role_dept
 *   - POST/PUT /role       用 menuIds 重写 sys_role_menu
 */
public class SysRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long roleId;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    /** 1=全部 2=自定义 3=本部门 4=本部门及以下 5=仅本人 */
    private String dataScope;
    /** 菜单树选择时是否父子联动 */
    private Boolean menuCheckStrictly;
    /** 部门树选择时是否父子联动 */
    private Boolean deptCheckStrictly;
    /** 0=正常 1=停用 */
    private String status;
    /** 0=存在 2=已删除 */
    private String delFlag;

    private Long[] menuIds;
    private Long[] deptIds;

    /**
     * 该角色是否已分配给某个用户，只用于 /user/authRole/{userId} 回显勾选状态。
     * RuoYi 前端用 row.flag 做 el-table 的 toggleRowSelection 判断。
     */
    private Boolean flag;

    /** 只读关联：拥有该角色的用户（当前接口用不到，保留给扩展） */
    private List<SysUser> users;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public Integer getRoleSort() {
        return roleSort;
    }

    public void setRoleSort(Integer roleSort) {
        this.roleSort = roleSort;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public Boolean getMenuCheckStrictly() {
        return menuCheckStrictly;
    }

    public void setMenuCheckStrictly(Boolean menuCheckStrictly) {
        this.menuCheckStrictly = menuCheckStrictly;
    }

    public Boolean getDeptCheckStrictly() {
        return deptCheckStrictly;
    }

    public void setDeptCheckStrictly(Boolean deptCheckStrictly) {
        this.deptCheckStrictly = deptCheckStrictly;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public Long[] getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(Long[] menuIds) {
        this.menuIds = menuIds;
    }

    public Long[] getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(Long[] deptIds) {
        this.deptIds = deptIds;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }

    public List<SysUser> getUsers() {
        return users;
    }

    public void setUsers(List<SysUser> users) {
        this.users = users;
    }
}
