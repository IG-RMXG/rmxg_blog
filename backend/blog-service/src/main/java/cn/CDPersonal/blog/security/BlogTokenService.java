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

    /** 与 system-service 约定的 Redis 键前缀；StringRedisTemplate 写入，键名不带序列化前缀 */
    private static final String TOKEN_PREFIX = "login_tokens:";
    /** 标准鉴权头前缀；同时兼容不带前缀的裸 token（见 getToken） */
    private static final String BEARER_PREFIX = "Bearer ";
    /** 请求级缓存，避免一次请求内多次查库 */
    private static final String REQUEST_ATTR = "BLOG_LOGIN_USER";

    /**
     * 本服务续期用的有效期（分钟），默认 30，可用 BLOG_TOKEN_EXPIRE_MINUTES 覆盖。
     *
     * 必须与 system-service 的 TokenService.EXPIRE_MINUTES 保持一致，
     * 否则同一个 token 在两个服务里会被算出不同的过期时间。
     */
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
        // 容错：没有 Bearer 前缀时把整个头当作 token（历史前端直接塞 token）
        return header;
    }

    /** 当前请求的登录用户；未登录/已过期/账号停用返回 null */
    public BlogLoginUser currentUser() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 非 Web 线程（异步任务、定时任务）没有请求上下文，一律按未登录处理
        if (attrs == null) {
            return null;
        }
        Object cached = attrs.getAttribute(REQUEST_ATTR, RequestAttributes.SCOPE_REQUEST);
        // 命中请求级缓存：一次请求内多处调用 currentUser() 只查一次 sys_user / sys_role
        if (cached instanceof BlogLoginUser user) {
            return user;
        }
        BlogLoginUser user = resolve(attrs.getRequest());
        // 只在解析成功时写缓存，失败结果不缓存，避免同一次请求内后续调用读到旧结论
        if (user != null) {
            attrs.setAttribute(REQUEST_ATTR, user, RequestAttributes.SCOPE_REQUEST);
        }
        return user;
    }

    // ===== 以下为 Controller 用的便捷判定：未登录时统一给出安全默认值（null / 空串 / false） =====
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

    /**
     * 按 token 组装登录用户：Redis 取 userId → 查 sys_user → 查角色。
     *
     * 单次调用最多两条 SQL；调用方不要在 PageHelper.startPage() 之后再触发本方法，
     * 否则分页参数会被拼到这两条查询上（见 PageUtils 类注释）。
     */
    private BlogLoginUser resolve(HttpServletRequest request) {
        Long userId = readUserId(getToken(request));
        if (userId == null) {
            return null;
        }
        // status=0 为正常；已删除用户由 SQL 的 del_flag='0' 过滤，这里只需拦住停用账号
        BlogUser user = userMapper.selectByUserId(userId);
        if (user == null || !"0".equals(user.getStatus())) {
            return null;
        }
        BlogLoginUser login = new BlogLoginUser();
        login.setUserId(user.getUserId());
        login.setUserName(user.getUserName());
        login.setNickName(user.getNickName());
        login.setAvatar(user.getAvatar());
        // 角色标识精确等于 admin 才算管理员；其它角色（如 common）一律按普通用户处理
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
        // 滑动续期：每次成功读取都重置 TTL，等价于"只要访问间隔不超过有效期就一直在线"
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
        // 兼容更早的写入方式：值直接是 userId 数字字符串（没有 JSON 外壳）
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
