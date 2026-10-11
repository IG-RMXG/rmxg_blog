package cn.CDPersonal.blog.security;

/**
 * sys_user 的最小映射（只取本服务鉴权需要的列）。
 */
public class BlogUser {

    private Long userId;
    private String userName;
    private String nickName;
    private String avatar;
    /** 0=正常 1=停用 */
    private String status;
    /** 0=存在 2=已删除 */
    private String delFlag;

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
}
