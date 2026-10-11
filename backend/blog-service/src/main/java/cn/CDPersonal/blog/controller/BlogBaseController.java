package cn.CDPersonal.blog.controller;

import cn.CDPersonal.blog.security.BlogLoginUser;
import cn.CDPersonal.blog.security.BlogTokenService;
import cn.CDPersonal.blog.support.BaseController;
import cn.CDPersonal.common.core.ResponseVO;

/**
 * 博客模块控制器基类：提供登录态与角色判定。
 *
 * 鉴权不依赖 Spring Security：登录 token 由 system-service 签发并写入 Redis，
 * 本服务通过 BlogTokenService 读取校验，再按 sys_role.role_key 判定是否 admin。
 */
public abstract class BlogBaseController extends BaseController {

    private final BlogTokenService tokenService;

    protected BlogBaseController(BlogTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /** 当前登录用户，未登录返回 null */
    protected BlogLoginUser currentUser() {
        return tokenService.currentUser();
    }

    /** 当前登录用户 id，未登录返回 null */
    protected Long currentUserId() {
        return tokenService.currentUserId();
    }

    protected String currentUserName() {
        return tokenService.currentUserName();
    }

    protected boolean isAdmin() {
        return tokenService.isAdmin();
    }

    protected boolean isLogin() {
        return tokenService.isLogin();
    }

    protected <T> ResponseVO<T> unauthorized() {
        return ResponseVO.error(401, "登录状态已过期，请重新登录");
    }

    protected <T> ResponseVO<T> forbidden(String msg) {
        return ResponseVO.error(403, msg == null ? "无权执行该操作" : msg);
    }
}
