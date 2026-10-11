package cn.CDPersonal.blog.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 博客服务的登录态解析。
 *
 * 与 system-service 的约定（跨服务契约，改一处要同步另一处）：
 *   - 登录成功后 system-service 会把会话写到 Redis：键 login_tokens:&lt;uuid&gt;
 *   - 值是一段 JSON：{"userId":1,"loginTime":...,"ipaddr":"..."}
 *   - 请求头 Authorization: Bearer &lt;uuid&gt;
 *
 * 本服务不负责签发/注销 token，只读取校验，并顺带续期（有效期与登录服务一致）。
 */
@Service
public class BlogTokenService {

    private static final String TOKEN_PREFIX = "login_tokens:";
    private static final String BEARER_PREFIX = "Bearer ";
    /** 请求级缓存，避免一次请求内多次查库 */
    private static final String REQUEST_ATTR = "BLOG_LOGIN_USER";

    @Value("${blog.auth.token-expire-minutes:30}")
    private long expireMinutes = 30L;

    private final StringRedisTemplate redis;
    private final BlogUserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BlogTokenService(StringRedisTemplate redis, BlogUserMapper userMapper) {
        this.redis = redis;
        this.userMapper = userMapper;
    }

    /** 从 Authorization 头取 token */
    public String getToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return header;
    }

    /** 当前请求的登录用户；未登录/已过期/账号停用返回 null */
    public BlogLoginUser currentUser() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object cached = attrs.getAttribute(REQUEST_ATTR, RequestAttributes.SCOPE_REQUEST);
        if (cached instanceof BlogLoginUser user) {
            return user;
        }
        BlogLoginUser user = resolve(attrs.getRequest());
        if (user != null) {
            attrs.setAttribute(REQUEST_ATTR, user, RequestAttributes.SCOPE_REQUEST);
        }
        return user;
    }

    public Long currentUserId() {
        BlogLoginUser user = currentUser();
        return user == null ? null : user.getUserId();
    }

    public String currentUserName() {
        BlogLoginUser user = currentUser();
        return user == null || user.getUserName() == null ? "" : user.getUserName();
    }

    public boolean isLogin() {
        return currentUser() != null;
    }

    public boolean isAdmin() {
        BlogLoginUser user = currentUser();
        return user != null && user.isAdmin();
    }

    // ==================== 内部 ====================

    private BlogLoginUser resolve(HttpServletRequest request) {
        Long userId = readUserId(getToken(request));
        if (userId == null) {
            return null;
        }
        BlogUser user = userMapper.selectByUserId(userId);
        if (user == null || !"0".equals(user.getStatus())) {
            return null;
        }
        BlogLoginUser login = new BlogLoginUser();
        login.setUserId(user.getUserId());
        login.setUserName(user.getUserName());
        login.setNickName(user.getNickName());
        login.setAvatar(user.getAvatar());
        List<String> roles = userMapper.selectRoleKeysByUserId(userId);
        login.setAdmin(roles != null && roles.contains("admin"));
        return login;
    }

    /** 读 Redis 会话取 userId，并把有效期续上 */
    private Long readUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String key = TOKEN_PREFIX + token;
        String raw;
        try {
            raw = redis.opsForValue().get(key);
        } catch (Exception e) {
            // Redis 不可用时视为未登录，不要抛 500 让页面崩
            return null;
        }
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Long userId = parseUserId(raw);
        if (userId == null) {
            return null;
        }
        try {
            redis.expire(key, expireMinutes, TimeUnit.MINUTES);
        } catch (Exception ignored) {
            // 续期失败不影响本次请求
        }
        return userId;
    }

    /** 兼容两种存储形式：JSON 会话对象 / 裸 userId */
    private Long parseUserId(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            JsonNode id = node.get("userId");
            if (id != null && !id.isNull()) {
                return id.asLong();
            }
        } catch (Exception ignored) {
            // 落到下面的裸数字分支
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
