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

    private final SysUserService sysUserService;
    private final TokenService tokenService;

    public AuthController(SysUserService sysUserService, TokenService tokenService) {
        this.sysUserService = sysUserService;
        this.tokenService = tokenService;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseVO<LoginVO> login(@RequestBody LoginParam loginParam, HttpServletRequest request) {
        try {
            return ResponseVO.success(sysUserService.login(loginParam, request));
        } catch (IllegalArgumentException e) {
            return ResponseVO.error(ResponseStatus.BAD_REQUEST.getCode(), e.getMessage());
        }
    }

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
