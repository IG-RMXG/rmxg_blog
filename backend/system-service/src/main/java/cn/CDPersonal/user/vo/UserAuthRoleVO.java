package cn.CDPersonal.user.vo;

import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.domain.entity.SysRole;
import cn.CDPersonal.common.domain.entity.SysUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /system/user/authRole/{userId} 的响应体。
 *
 * 前端（views/system/user/authRole.vue）读的是**顶层** user 和 roles：
 *   form.value  = response.user
 *   roles.value = response.roles
 *   // roles 每项带 flag，true 表示该用户已有这个角色，用来 toggleRowSelection
 *
 * 注意不是 { code, data: {...} }，包进 data 页面就空白。
 */
public class UserAuthRoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    /** 用户基本信息（昵称 / 登录账号 / userId） */
    private SysUser user;

    /** 全部角色，每项带 flag = 该用户是否已分配 */
    private List<SysRole> roles = new ArrayList<>();

    private Long timestamp;

    public UserAuthRoleVO() {
        this.code = ResponseStatus.SUCCESS.getCode();
        this.msg = ResponseStatus.SUCCESS.getMessage();
        this.timestamp = System.currentTimeMillis();
    }

    public static UserAuthRoleVO success(SysUser user, List<SysRole> roles) {
        UserAuthRoleVO vo = new UserAuthRoleVO();
        vo.setUser(user);
        vo.setRoles(roles == null ? new ArrayList<>() : roles);
        return vo;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public SysUser getUser() {
        return user;
    }

    public void setUser(SysUser user) {
        this.user = user;
    }

    public List<SysRole> getRoles() {
        return roles;
    }

    public void setRoles(List<SysRole> roles) {
        this.roles = roles;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
