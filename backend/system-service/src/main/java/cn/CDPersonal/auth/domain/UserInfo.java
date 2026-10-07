package cn.CDPersonal.auth.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录用户，对应 ry-cloud.sys_user 的子集。
 *
 * 注意：ry-cloud 是 RuoYi-Cloud 的库，没有 is_default_modify_pwd /
 * pwd_update_date 之类字段，所以这里只映射确实存在的列。
 */
public class UserInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    private String sex;
    private String avatar;
    private String password;
    private String status;
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
}
