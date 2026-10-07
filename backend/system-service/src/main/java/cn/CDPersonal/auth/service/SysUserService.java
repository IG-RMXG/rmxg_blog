package cn.CDPersonal.auth.service;

import cn.CDPersonal.auth.domain.UserInfo;
import cn.CDPersonal.auth.mapper.SysMenuMapper;
import cn.CDPersonal.auth.mapper.SysUserMapper;
import cn.CDPersonal.auth.model.LoginParam;
import cn.CDPersonal.auth.model.LoginVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysMenuMapper sysMenuMapper;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SysUserService(SysUserMapper sysUserMapper, SysMenuMapper sysMenuMapper, TokenService tokenService) {
        this.sysUserMapper = sysUserMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.tokenService = tokenService;
    }

    /**
     * 登录校验，成功返回 token。
     *
     * @throws IllegalArgumentException 校验失败，message 直接回给前端
     */
    public LoginVO login(LoginParam param) {
        return login(param, null);
    }

    /**
     * 登录校验，成功返回 token。
     *
     * @param request 用于记录登录主机（在线用户页需要），可为 null
     * @throws IllegalArgumentException 校验失败，message 直接回给前端
     */
    public LoginVO login(LoginParam param, jakarta.servlet.http.HttpServletRequest request) {
        String username = param.getUsername() == null ? "" : param.getUsername().trim();
        String password = param.getPassword() == null ? "" : param.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        UserInfo user = sysUserMapper.selectByUserName(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在或已删除");
        }
        if (!"0".equals(user.getStatus())) {
            throw new IllegalArgumentException("账号已停用，请联系管理员");
        }
        if (!matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = tokenService.createToken(user.getUserId(), resolveClientIp(request));
        return new LoginVO(token, TokenService.EXPIRE_MINUTES * 60);
    }

    /** 取客户端真实 IP：请求经网关转发，优先读 X-Forwarded-For */
    private String resolveClientIp(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 兼容两种存储形式：
     *   - BCrypt 哈希（RuoYi 标准，形如 $2a$10$...）
     *   - 明文（本仓库 system 库原来的写法，方便本地调试）
     */
    private boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    public List<String> selectRoleKeys(Long userId) {
        return sysUserMapper.selectRoleKeysByUserId(userId);
    }

    public List<String> selectPerms(Long userId) {
        return sysUserMapper.selectPermsByUserId(userId);
    }

    /** 全部权限标识（给 admin 用，见 SysMenuMapper.selectAllPerms 的说明） */
    public List<String> selectAllPerms() {
        return sysMenuMapper.selectAllPerms();
    }

    /** admin 角色拥有全部权限和菜单 */
    public boolean isAdmin(Long userId) {
        List<String> roleKeys = sysUserMapper.selectRoleKeysByUserId(userId);
        return roleKeys != null && roleKeys.contains("admin");
    }
}
