package cn.CDPersonal.auth.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录令牌对应的会话信息，存在 Redis 的 login_tokens:&lt;token&gt; 里。
 *
 * 之前只存了一个 userId，导致"在线用户"页面拿不到主机地址和登录时间。
 * 用 POJO 存储（GenericJackson2JsonRedisSerializer 会写成带 @class 的 JSON）。
 * 新增字段请保持无参构造 + getter/setter，否则反序列化会失败。
 */
public class TokenSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    /** 登录时间戳（毫秒） */
    private Long loginTime;
    /** 登录主机地址 */
    private String ipaddr;

    public TokenSession() {
    }

    public TokenSession(Long userId, Long loginTime, String ipaddr) {
        this.userId = userId;
        this.loginTime = loginTime;
        this.ipaddr = ipaddr;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }
}
