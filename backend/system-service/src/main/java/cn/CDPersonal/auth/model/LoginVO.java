package cn.CDPersonal.auth.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录返回体。
 *
 * 前端 store/modules/user.js 里是这么取的：
 *     let data = res.data
 *     setToken(data.access_token)
 * 所以 access_token 必须放在 ResponseVO 的 data 里。
 */
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String access_token;
    private String token_type;
    private Long expires_in;

    public LoginVO() {
    }

    public LoginVO(String accessToken, Long expiresIn) {
        this.access_token = accessToken;
        this.token_type = "Bearer";
        this.expires_in = expiresIn;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public Long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Long expires_in) {
        this.expires_in = expires_in;
    }
}
