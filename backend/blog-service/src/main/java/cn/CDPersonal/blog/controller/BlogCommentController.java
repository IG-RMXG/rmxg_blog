package cn.CDPersonal.blog.controller;

import cn.CDPersonal.blog.domain.BlogComment;
import cn.CDPersonal.blog.security.BlogTokenService;
import cn.CDPersonal.blog.service.BlogCommentService;
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
 * 博客评论接口（对外前缀 /blog/comment）。
 *
 * 普通用户可在文章详情页发表评论并查看已展示评论；管理员负责审核与删除。
 */
@RestController
@RequestMapping("/blog/comment")
public class BlogCommentController extends BlogBaseController {

    private final BlogCommentService commentService;

    public BlogCommentController(BlogTokenService tokenService, BlogCommentService commentService) {
        super(tokenService);
        this.commentService = commentService;
    }

    /** 评论管理列表（管理员，分页） */
    @GetMapping("/list")
    public TableDataInfo list(BlogComment query) {
        if (!isAdmin()) {
            return PageUtils.getDataTable(null);
        }
        startPage();
        return PageUtils.getDataTable(commentService.selectCommentList(query));
    }

    /** 某篇文章的评论（登录用户均可，仅展示正常状态） */
    @GetMapping("/article/{articleId}")
    public ResponseVO<java.util.List<BlogComment>> articleComments(@PathVariable Long articleId) {
        if (!isLogin()) {
            return unauthorized();
        }
        BlogComment query = new BlogComment();
        query.setArticleId(articleId);
        query.setStatus(BlogCommentService.STATUS_NORMAL);
        return ResponseVO.success(commentService.selectCommentList(query));
    }

    /** 发表评论（登录用户均可） */
    @PostMapping
    public ResponseVO<Void> add(@RequestBody BlogComment comment) {
        if (!isLogin()) {
            return unauthorized();
        }
        if (comment.getArticleId() == null) {
            return ResponseVO.error(500, "文章ID不能为空");
        }
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            return ResponseVO.error(500, "评论内容不能为空");
        }
        comment.setCreateBy(currentUserName());
        return commentService.insertComment(comment, currentUserId()) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "评论失败");
    }

    /** 审核评论（管理员）：body {commentId, status} */
    @PutMapping("/status")
    public ResponseVO<Void> changeStatus(@RequestBody Map<String, Object> body) {
        if (!isAdmin()) {
            return forbidden("仅管理员可审核评论");
        }
        Long commentId = asLong(body.get("commentId"));
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        if (commentId == null || status == null || status.isBlank()) {
            return ResponseVO.error(500, "参数不完整");
        }
        return commentService.updateStatus(commentId, status, currentUserName()) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "操作失败");
    }

    /** 删除评论（管理员） */
    @DeleteMapping("/{commentIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] commentIds) {
        if (!isAdmin()) {
            return forbidden("仅管理员可删除评论");
        }
        return commentService.deleteCommentByIds(commentIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
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
