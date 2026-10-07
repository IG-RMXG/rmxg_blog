package cn.CDPersonal.auth.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录请求体，对应前端 store/modules/user.js 里 login(username, password, code, uuid) 发出的 JSON
 */
public class LoginParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    /** 验证码，当前登录流程未强制校验，保留字段以兼容前端 */
    private String code;
    private String uuid;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
