package cn.CDPersonal.blog.controller;

import cn.CDPersonal.blog.domain.BlogArticle;
import cn.CDPersonal.blog.security.BlogTokenService;
import cn.CDPersonal.blog.service.BlogArticleService;
import cn.CDPersonal.blog.support.PageUtils;
import cn.CDPersonal.blog.support.TableDataInfo;
import cn.CDPersonal.common.core.ResponseVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 博客文章接口（对外前缀 /blog/article）。
 *
 * 分权要点（配合 BlogArticleService）：
 *   - 列表：管理员看全部，普通用户只看自己的。
 *   - 详情/浏览：普通用户看自己的或已发布文章。
 *   - 新增/修改/删除：普通用户只能操作自己的，且不能直接发布。
 *   - 状态流转（发布/审核/驳回）：仅管理员；普通用户只能提交审核。
 */
@RestController
@RequestMapping("/blog/article")
public class BlogArticleController extends BlogBaseController {

    private final BlogArticleService articleService;

    public BlogArticleController(BlogTokenService tokenService, BlogArticleService articleService) {
        super(tokenService);
        this.articleService = articleService;
    }

    /** 文章列表（分页） */
    @GetMapping("/list")
    public TableDataInfo list(BlogArticle query) {
        if (!isLogin()) {
            return PageUtils.getDataTable(null);
        }
        // 必须在 startPage() 之前取当前用户/角色，否则 PageHelper 会把它触发的
        // 登录态查询（selectByUserId）当成待分页 SQL，拼出 "LIMIT 1 LIMIT ?" 报错。
        Long userId = currentUserId();
        boolean admin = isAdmin();
        startPage();
        return PageUtils.getDataTable(articleService.selectArticleList(query, userId, admin));
    }

    /** 文章详情（编辑页加载，不增加浏览量） */
    @GetMapping("/{articleId}")
    public ResponseVO<BlogArticle> getInfo(@PathVariable Long articleId) {
        if (!isLogin()) {
            return unauthorized();
        }
        BlogArticle article = articleService.selectArticleById(articleId, currentUserId(), isAdmin());
        return article == null
                ? ResponseVO.error(403, "文章不存在或无权查看")
                : ResponseVO.success(article);
    }

    /** 文章浏览（详情页，已发布文章浏览量 +1） */
    @GetMapping("/view/{articleId}")
    public ResponseVO<BlogArticle> view(@PathVariable Long articleId) {
        if (!isLogin()) {
            return unauthorized();
        }
        BlogArticle article = articleService.viewArticle(articleId, currentUserId(), isAdmin());
        return article == null
                ? ResponseVO.error(403, "文章不存在或无权查看")
                : ResponseVO.success(article);
    }

    /** 新增文章 */
    @PostMapping
    public ResponseVO<Long> add(@RequestBody BlogArticle article) {
        if (!isLogin()) {
            return unauthorized();
        }
        Long articleId = articleService.insertArticle(article, article.getTagIds(), currentUserId(), isAdmin());
        return ResponseVO.success(articleId);
    }

    /** 修改文章 */
    @PutMapping
    public ResponseVO<Void> edit(@RequestBody BlogArticle article) {
        if (!isLogin()) {
            return unauthorized();
        }
        if (article.getArticleId() == null) {
            return ResponseVO.error(500, "文章ID不能为空");
        }
        article.setUpdateBy(currentUserName());
        String error = articleService.updateArticle(article, article.getTagIds(), currentUserId(), isAdmin());
        return error == null ? ResponseVO.success() : ResponseVO.error(403, error);
    }

    /** 删除文章（支持逗号分隔多个） */
    @DeleteMapping("/{articleIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] articleIds) {
        if (!isLogin()) {
            return unauthorized();
        }
        String error = articleService.deleteArticleByIds(articleIds, currentUserId(), isAdmin());
        return error == null ? ResponseVO.success() : ResponseVO.error(403, error);
    }

    /**
     * 状态流转：body {articleId, status}。status：0草稿 1已发布 2待审核 3已驳回。
     * 管理员任意切换；普通用户只能提交审核(2)。
     */
    @PutMapping("/status")
    public ResponseVO<Void> changeStatus(@RequestBody Map<String, Object> body) {
        if (!isLogin()) {
            return unauthorized();
        }
        Long articleId = asLong(body.get("articleId"));
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        if (articleId == null || status == null || status.isBlank()) {
            return ResponseVO.error(500, "参数不完整");
        }
        String error = articleService.changeStatus(articleId, status, currentUserId(), isAdmin());
        return error == null ? ResponseVO.success() : ResponseVO.error(403, error);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
