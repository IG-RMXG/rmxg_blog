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
        // 非管理员（含未登录）返回空表：审核数据不对外暴露，同时避免表格组件因 401/403 弹错
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
        // 这里没有 startPage()，所以是一次性返回该文章的全部正常评论，不分页；
        // 父子回复由前端按 parentId 自行组装（Service 返回的是扁平列表）。
        // 边界：不校验文章是否存在/是否可见，登录用户传任意 articleId 都能读到该文章的正常评论。
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
        // 这里只挡明显非法的入参；parentId 缺省 0、"先展示后审核"的默认状态、
        // 以及所属文章 comment_count +1 都在 Service 的同一个事务里完成。
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
        // status：0=正常（前台展示） 1=隐藏（审核不通过，仅管理端可见）；其余取值不校验，直接落库
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
        // 物理删除且不级联清理 parentId 指向它的回复；评论数回减由 Service 逐条处理，
        // 因此入参里不存在的 id 会被静默跳过。
        return commentService.deleteCommentByIds(commentIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }

    /**
     * 宽松解析 JSON 数字：Map 反序列化后小整数是 Integer、大整数是 Long，前端也可能传字符串；
     * 统一收敛为 Long，解析失败返回 null，由调用方按"参数不完整"处理。
     */
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
