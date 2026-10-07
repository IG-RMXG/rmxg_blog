package cn.CDPersonal.common.domain.entity;

import cn.CDPersonal.common.domain.model.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * 系统用户，对应 ry-cloud.sys_user。
 *
 * 除了表里的列，还带了几个"非数据库列"的关联字段：
 *   - dept：所属部门对象，前端表格直接绑 scope.row.dept.deptName
 *   - roles / roleIds / postIds：用户详情接口（/user/{userId}）要回显的权限数据
 *
 * password 只允许从请求体反序列化进来，永远不序列化回前端。
 */
public class SysUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long deptId;
    private String userName;
    private String nickName;
    /** 00=系统用户 */
    private String userType;
    private String email;
    private String phonenumber;
    /** 0=男 1=女 2=未知 */
    private String sex;
    private String avatar;
    /** BCrypt 哈希；WRITE_ONLY 保证不会出现在响应 JSON 里 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /** 0=正常 1=停用 */
    private String status;
    /** 0=存在 2=已删除 */
    private String delFlag;
    private String loginIp;
    private Date loginDate;
    private Date pwdUpdateDate;

    /** 所属部门（只读，来自 sys_dept 的 join） */
    private SysDept dept;
    /** 该用户的角色列表（只读） */
    private List<SysRole> roles;
    /** 该用户的角色 id 数组（新增/修改时入参） */
    private Long[] roleIds;
    /** 该用户的岗位 id 数组（新增/修改时入参） */
    private Long[] postIds;
    /** 该用户的岗位列表（只读） */
    private List<SysPost> posts;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public Date getPwdUpdateDate() {
        return pwdUpdateDate;
    }

    public void setPwdUpdateDate(Date pwdUpdateDate) {
        this.pwdUpdateDate = pwdUpdateDate;
    }

    public SysDept getDept() {
        return dept;
    }

    public void setDept(SysDept dept) {
        this.dept = dept;
    }

    public List<SysRole> getRoles() {
        return roles;
    }

    public void setRoles(List<SysRole> roles) {
        this.roles = roles;
    }

    public Long[] getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds) {
        this.roleIds = roleIds;
    }

    public Long[] getPostIds() {
        return postIds;
    }

    public void setPostIds(Long[] postIds) {
        this.postIds = postIds;
    }

    public List<SysPost> getPosts() {
        return posts;
    }

    public void setPosts(List<SysPost> posts) {
        this.posts = posts;
    }

    public boolean isAdmin() {
        return userId != null && userId == 1L;
    }
}
