package cn.CDPersonal.blog.security;

/**
 * 当前登录用户（由 token 解析 + sys_user/sys_role 组装）。
 */
public class BlogLoginUser {

    private Long userId;
    private String userName;
    private String nickName;
    private String avatar;
    /** 是否拥有 admin 角色 */
    private boolean admin;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
