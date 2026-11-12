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

    /** 当前登录用户名；未登录时返回空串而不是 null，方便直接写进 createBy / updateBy */
    protected String currentUserName() {
        return tokenService.currentUserName();
    }

    /** 是否管理员：只有 role_key 精确等于 admin 才为 true，其余角色（如 common）一律按普通用户降级处理 */
    protected boolean isAdmin() {
        return tokenService.isAdmin();
    }

    /** 是否已登录：Redis 里存在会话且账号未被停用才算，Redis 不可用时按未登录处理 */
    protected boolean isLogin() {
        return tokenService.isLogin();
    }

    /**
     * 统一的"未登录"响应。前端 axios 拦截器把 code=401 当成会话过期并弹出重新登录框，
     * 所以只有确实需要登录态的接口才用它；列表类接口宁可返回空表，避免首屏渲染就弹框。
     */
    protected <T> ResponseVO<T> unauthorized() {
        return ResponseVO.error(401, "登录状态已过期，请重新登录");
    }

    /** 统一的"越权"响应（403）：前端按普通错误提示处理，不会触发重新登录流程 */
    protected <T> ResponseVO<T> forbidden(String msg) {
        return ResponseVO.error(403, msg == null ? "无权执行该操作" : msg);
    }
}
