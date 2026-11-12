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

    /** 会话 token（纯 UUID，非 JWT）；字段名保留下划线是为了匹配前端 setToken(data.access_token) */
    private String access_token;
    /** 固定 Bearer，前端据此拼 Authorization 头 */
    private String token_type;
    /** 有效期（秒），由 TokenService.EXPIRE_MINUTES 换算而来 */
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
