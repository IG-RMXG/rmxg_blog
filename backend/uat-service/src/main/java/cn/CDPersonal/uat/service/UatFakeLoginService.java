package cn.CDPersonal.uat.service;

import cn.CDPersonal.uat.mapper.UatUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * UAT 假登录：按角色签发与 system-service 完全同构的登录会话。
 *
 * ★ 跨服务契约（改这里必须同步 system-service 的 TokenService）：
 *   - Redis 键：login_tokens:<uuid>（本服务与 UAT 的 system-service 连同一个 Redis db）
 *   - 值格式：  {"userId":..,"loginTime":..,"ipaddr":".."}（TokenSession 的 JSON）
 *   - 有效期：  30 分钟（uat.token-expire-minutes）
 *
 * system-service 的 TokenService 读到这个键后即可解析出 userId，
 * 后续 /system/user/getInfo、/blog/** 等全部接口都认这个 token。
 */
@Service
public class UatFakeLoginService implements ApplicationRunner {

    private static final String TOKEN_PREFIX = "login_tokens:";
    private static final String IP_MARK = "uat-fake-login";

    /** 角色编码 -> 测试账号用户名 / 角色绑定（role_id 对应 dump 自 dev 的 ry-cloud-uat） */
    private static final Map<String, String> ROLE_USERS = Map.of(
            "admin", "uat_admin",
            "user", "uat_user"
    );

    @Value("${uat.token-expire-minutes:30}")
    private long expireMinutes;

    private final StringRedisTemplate redis;
    private final UatUserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UatFakeLoginService(StringRedisTemplate redis, UatUserMapper userMapper) {
        this.redis = redis;
        this.userMapper = userMapper;
    }

    /**
     * 假登录：为指定角色签发一个真实可用的 token。
     *
     * @param role admin=管理员（uat_admin）/ user=普通用户（uat_user）
     * @return token、userId、用户名、角色列表、有效期
     */
    public Map<String, Object> fakeLogin(String role) {
        String userName = ROLE_USERS.get(role == null ? "" : role.trim().toLowerCase());
        if (userName == null) {
            throw new IllegalArgumentException("role 仅支持 admin / user");
        }
        Map<String, Object> user = userMapper.selectByUserName(userName);
        if (user == null || !"0".equals(String.valueOf(user.get("status")))) {
            // 正常不会发生：启动时已 ensure；除非账号被人为停用
            throw new IllegalStateException("测试账号未就绪：" + userName);
        }
        Long userId = ((Number) user.get("userId")).longValue();

        String token = UUID.randomUUID().toString().replace("-", "");
        writeSession(token, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("expiresIn", expireMinutes * 60);
        result.put("userId", userId);
        result.put("userName", userName);
        result.put("nickName", user.get("nickName"));
        result.put("roles", userMapper.selectRoleKeys(userId));
        return result;
    }

    /** 把会话写进 Redis，格式与 system-service 的 TokenSession 序列化结果一致 */
    private void writeSession(String token, Long userId) {
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("loginTime", System.currentTimeMillis());
        session.put("ipaddr", IP_MARK);
        try {
            redis.opsForValue().set(TOKEN_PREFIX + token, objectMapper.writeValueAsString(session),
                    expireMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IllegalStateException("写入 UAT 会话失败：" + e.getMessage(), e);
        }
    }

    /**
     * 启动时确保两个测试账号存在且角色正确（幂等，重复启动不重复插）。
     * uat_admin -> admin(role_id=1)，uat_user -> common(role_id=2)。
     */
    @Override
    public void run(ApplicationArguments args) {
        ensureUser("uat_admin", "UAT管理员", 1L);
        ensureUser("uat_user", "UAT普通用户", 2L);
    }

    private void ensureUser(String userName, String nickName, Long roleId) {
        try {
            Map<String, Object> user = userMapper.selectByUserName(userName);
            if (user == null) {
                userMapper.insertUser(userName, nickName);
                user = userMapper.selectByUserName(userName);
            }
            Long userId = ((Number) user.get("userId")).longValue();
            if (userMapper.countUserRole(userId, roleId) == 0) {
                userMapper.insertUserRole(userId, roleId);
            }
        } catch (Exception e) {
            // UAT 库还没初始化（未跑 scripts/init-uat-db.ps1）时这里会失败，
            // 只打日志不中断启动：/uat/status 能把问题报出来
            System.err.println("[uat] 测试账号 ensure 失败（UAT 库是否已初始化？）：" + e.getMessage());
        }
    }

    /** /uat/status 用：两个测试账号的就绪情况 */
    public Map<String, Object> accountStatus() {
        Map<String, Object> status = new HashMap<>();
        for (Map.Entry<String, String> e : ROLE_USERS.entrySet()) {
            Map<String, Object> user = userMapper.selectByUserName(e.getValue());
            List<String> roles = user == null ? List.of()
                    : userMapper.selectRoleKeys(((Number) user.get("userId")).longValue());
            status.put(e.getKey(), Map.of(
                    "userName", e.getValue(),
                    "ready", user != null,
                    "roles", roles
            ));
        }
        return status;
    }

    /** /uat/status 用：Redis 连通性（一次真实 GET 即可证明连接可用，全版本兼容） */
    public boolean redisOk() {
        try {
            redis.opsForValue().get("uat:ping");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** /uat/status 用：MySQL 连通性 */
    public boolean dbOk() {
        try {
            return userMapper.ping() == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
