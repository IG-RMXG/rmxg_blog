package cn.CDPersonal.auth.service;

import cn.CDPersonal.auth.domain.TokenSession;
import cn.CDPersonal.auth.domain.UserInfo;
import cn.CDPersonal.auth.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录 token 管理。
 *
 * token 是一个 UUID，会话信息以 JSON 存在 Redis 的 login_tokens:&lt;uuid&gt;。
 *
 * 为什么直接用 StringRedisTemplate 而不是 common.redis 的 RedisService：
 * 那个 RedisService 注入的是通用 RedisTemplate，实际生效的是 JDK 序列化，
 * 键名会变成 "\xac\xed\x00\x05t\x00-login_tokens:xxx"，
 * 导致 keys("login_tokens:*") 永远扫不到（在线用户页因此一直是空的）。
 * StringRedisTemplate 的键值都是纯字符串，行为可预期。
 */
@Service
public class TokenService {

    /** token 有效期（分钟） */
    public static final long EXPIRE_MINUTES = 30L;

    private static final String TOKEN_PREFIX = "login_tokens:";
    private static final String BEARER_PREFIX = "Bearer ";

    private final StringRedisTemplate redis;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenService(StringRedisTemplate redis, SysUserMapper sysUserMapper) {
        this.redis = redis;
        this.sysUserMapper = sysUserMapper;
    }

    /** 创建 token 并写入 Redis */
    public String createToken(Long userId) {
        return createToken(userId, null);
    }

    /** 创建 token，记录登录主机（供"在线用户"页使用） */
    public String createToken(Long userId, String ipaddr) {
        String token = UUID.randomUUID().toString().replace("-", "");
        TokenSession session = new TokenSession(userId, System.currentTimeMillis(), ipaddr);
        writeSession(TOKEN_PREFIX + token, session, EXPIRE_MINUTES);
        return token;
    }

    /** 从请求头 Authorization: Bearer xxx 取 token */
    public String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return header;
    }

    /** 取当前 token 对应的会话，未登录或过期返回 null */
    public TokenSession getSession(HttpServletRequest request) {
        return getSessionByToken(getToken(request));
    }

    public TokenSession getSessionByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return readSession(TOKEN_PREFIX + token);
    }

    /** 取当前登录用户，未登录或已过期返回 null */
    public UserInfo getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenSession session = readSession(TOKEN_PREFIX + token);
        if (session == null || session.getUserId() == null) {
            return null;
        }
        // 续期
        redis.expire(TOKEN_PREFIX + token, EXPIRE_MINUTES, TimeUnit.MINUTES);
        return sysUserMapper.selectByUserId(session.getUserId());
    }

    /** 在线会话列表（tokenId + 会话信息），供"在线用户"页使用 */
    public List<TokenEntry> listSessions() {
        List<TokenEntry> result = new ArrayList<>();
        Set<String> keys = redis.keys(TOKEN_PREFIX + "*");
        if (keys == null) {
            return result;
        }
        for (String key : keys) {
            TokenSession session = readSession(key);
            if (session == null) {
                continue;
            }
            result.add(new TokenEntry(key.substring(TOKEN_PREFIX.length()), session));
        }
        return result;
    }

    /** 强退：删除指定 token */
    public boolean deleteSession(String tokenId) {
        if (tokenId == null || tokenId.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redis.delete(TOKEN_PREFIX + tokenId));
    }

    /** 退出登录 */
    public void deleteToken(HttpServletRequest request) {
        String token = getToken(request);
        if (token != null && !token.isEmpty()) {
            redis.delete(TOKEN_PREFIX + token);
        }
    }

    // ==================== 内部 ====================

    private void writeSession(String key, TokenSession session, long minutes) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(session),
                    minutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IllegalStateException("写入登录会话失败", e);
        }
    }

    private TokenSession readSession(String key) {
        String raw = redis.opsForValue().get(key);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, TokenSession.class);
        } catch (Exception e) {
            // 兜底：万一存的是裸 userId（历史数据），至少保留 userId
            try {
                return new TokenSession(Long.valueOf(raw.trim()), null, null);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

    /** 一条在线会话：tokenId + 会话内容 */
    public record TokenEntry(String tokenId, TokenSession session) {
    }
}
