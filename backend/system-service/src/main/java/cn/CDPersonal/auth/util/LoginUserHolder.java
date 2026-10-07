package cn.CDPersonal.auth.util;

import cn.CDPersonal.auth.domain.UserInfo;
import cn.CDPersonal.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 静态获取当前登录用户，供各 controller 记录 create_by / update_by 使用。
 *
 * 为什么不做成工具类直接注入：RuoYi 风格的 controller 数量多，
 * 每个都注入 TokenService 只为了取个用户名太重，这里用一次性注册的静态引用。
 */
@Component
public class LoginUserHolder {

    private static TokenService tokenService;

    public LoginUserHolder(TokenService service) {
        LoginUserHolder.tokenService = service;
    }

    /** 当前登录用户；未登录或非 Web 线程返回 null */
    public static UserInfo getUser() {
        if (tokenService == null) {
            return null;
        }
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return tokenService.getLoginUser(request);
    }

    /** 当前登录用户名；取不到时返回空串，避免写库时 NOT NULL 报错 */
    public static String getUserName() {
        UserInfo user = getUser();
        return user == null || user.getUserName() == null ? "" : user.getUserName();
    }

    /** 当前登录用户 id；取不到返回 null */
    public static Long getUserId() {
        UserInfo user = getUser();
        return user == null ? null : user.getUserId();
    }
}
