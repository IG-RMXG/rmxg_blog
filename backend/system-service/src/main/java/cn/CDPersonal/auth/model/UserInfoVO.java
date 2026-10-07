package cn.CDPersonal.auth.model;

import cn.CDPersonal.auth.domain.UserInfo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * /system/user/getInfo 返回体。
 *
 * 前端 store/modules/user.js 的 getInfo() 直接读顶层字段：
 *     const user = res.user
 *     this.roles = res.roles
 *     this.permissions = res.permissions
 *     if (res.isDefaultModifyPwd) ...
 * 所以这些必须是顶层属性，不能裹在 data 里。
 */
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UserInfo user;
    private List<String> roles;
    private List<String> permissions;
    private Boolean isDefaultModifyPwd;
    private Boolean isPasswordExpired;

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Boolean getIsDefaultModifyPwd() {
        return isDefaultModifyPwd;
    }

    public void setIsDefaultModifyPwd(Boolean isDefaultModifyPwd) {
        this.isDefaultModifyPwd = isDefaultModifyPwd;
    }

    public Boolean getIsPasswordExpired() {
        return isPasswordExpired;
    }

    public void setIsPasswordExpired(Boolean isPasswordExpired) {
        this.isPasswordExpired = isPasswordExpired;
    }
}
