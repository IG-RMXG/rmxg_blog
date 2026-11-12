package cn.CDPersonal.blog.security;

/**
 * 当前登录用户（由 token 解析 + sys_user/sys_role 组装）。
 */
public class BlogLoginUser {

    /** sys_user.user_id；也是文章/评论归属校验的依据 */
    private Long userId;
    /** 登录账号，写库时回填 create_by / update_by */
    private String userName;
    /** 昵称，列表与评论展示用；允许为空 */
    private String nickName;
    /** 头像地址（相对路径），允许为空 */
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

    /** 管理员标记在解析 token 时按 sys_role.role_key 一次性算出，同一请求内不会变化 */
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
