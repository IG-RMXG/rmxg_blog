package cn.CDPersonal.auth.controller;

import cn.CDPersonal.auth.model.LoginParam;
import cn.CDPersonal.auth.model.LoginVO;
import cn.CDPersonal.auth.service.SysUserService;
import cn.CDPersonal.auth.service.TokenService;
import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。前端请求 /auth/login（dev 代理会把 /dev-api 前缀去掉），
 * 经网关转发到这里时路径是 /system/auth/login（system-service 有 /system 前缀）。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    // 依赖均为无状态单例；本类不缓存登录态，每次请求都按 token 现查
    private final SysUserService sysUserService;
    private final TokenService tokenService;

    public AuthController(SysUserService sysUserService, TokenService tokenService) {
        this.sysUserService = sysUserService;
        this.tokenService = tokenService;
    }

    /**
     * 登录入口。校验失败（账号不存在/停用/密码错）统一转 400 并把 message 原样透出，
     * 前端 request.js 据此弹提示；成功时 LoginVO 放在 ResponseVO.data 里，字段名已对齐前端 setToken。
     * 登录接口本身不校验图形验证码：滑块验证由前端调网关 /captcha 单独完成，
     * LoginParam 的 code/uuid 仅为兼容前端表单。
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseVO<LoginVO> login(@RequestBody LoginParam loginParam, HttpServletRequest request) {
        try {
            return ResponseVO.success(sysUserService.login(loginParam, request));
        } catch (IllegalArgumentException e) {
            return ResponseVO.error(ResponseStatus.BAD_REQUEST.getCode(), e.getMessage());
        }
    }

    /** 退出登录：只删 Redis 中的会话，token 本身无状态，无其它清理动作 */
    @RequestMapping(value = "/logout", method = RequestMethod.DELETE)
    public ResponseVO<Void> logout(HttpServletRequest request) {
        tokenService.deleteToken(request);
        return ResponseVO.success();
    }

    /** 预留：前端 api/login.js 里有 refreshToken()，暂未实现逻辑 */
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    public ResponseVO<Void> refresh() {
        return ResponseVO.error(ResponseStatus.NOT_IMPLEMENTED.getCode(), "暂未实现");
    }
}
