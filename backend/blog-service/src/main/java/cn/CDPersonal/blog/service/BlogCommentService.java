package cn.CDPersonal.blog.service;

import cn.CDPersonal.blog.domain.BlogComment;
import cn.CDPersonal.blog.mapper.BlogArticleMapper;
import cn.CDPersonal.blog.mapper.BlogCommentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 博客评论管理。
 *
 * 普通用户可在文章详情页发表评论（默认正常展示）；管理员可审核（正常/隐藏）与删除。
 */
@Service
public class BlogCommentService {

    /** 评论状态：0=正常（前台展示） 1=隐藏（审核不通过，仅管理端可见） */
    public static final String STATUS_NORMAL = "0";
    public static final String STATUS_HIDDEN = "1";

    private final BlogCommentMapper commentMapper;
    private final BlogArticleMapper articleMapper;

    public BlogCommentService(BlogCommentMapper commentMapper, BlogArticleMapper articleMapper) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
    }

    /**
     * 评论列表（分页由 Controller 的 startPage() 控制）。
     *
     * 同一方法服务两端：管理端不传 status 看全部；文章详情页固定传 status="0" 只看正常评论。
     * 结果按 comment_id 倒序，新评论在前；返回的是扁平列表，父子关系由前端按 parentId 自行组装。
     */
    public List<BlogComment> selectCommentList(BlogComment query) {
        return commentMapper.selectCommentList(query);
    }

    /** 单条评论（带文章标题与评论人信息）；删除时先查它，用于拿到 articleId 回减计数 */
    public BlogComment selectCommentById(Long commentId) {
        return commentMapper.selectCommentById(commentId);
    }

    /**
     * 发表评论。commentId / userId 由服务端覆盖，parentId 缺省 0（顶级评论）。
     *
     * 状态缺省为正常(0)，即"先展示后审核"；插入成功后把所属文章的 comment_count +1，
     * 两条写操作在同一事务内，避免出现评论已存在但计数没加（或反之）。
     *
     * @return 受影响行数，小于等于 0 表示插入失败
     */
    @Transactional
    public int insertComment(BlogComment comment, Long userId) {
        comment.setCommentId(null);
        comment.setUserId(userId);
        // 没有父评论时用 0 占位（表结构约定 0=顶级评论，无外键约束）
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        if (comment.getStatus() == null || comment.getStatus().isBlank()) {
            comment.setStatus(STATUS_NORMAL);
        }
        int rows = commentMapper.insertComment(comment);
        // 评论数在同一事务里 +1，插入失败不会留下"计数加了、评论没进"的脏数据
        articleMapper.incrementCommentCount(comment.getArticleId());
        return rows;
    }

    /** 审核评论：只改 status（0 正常 / 1 隐藏）与 update_by，不改评论内容与归属 */
    @Transactional
    public int updateStatus(Long commentId, String status, String updateBy) {
        return commentMapper.updateStatus(commentId, status, updateBy);
    }

    /**
     * 批量删除评论（物理删除，blog_comment 没有逻辑删除列）。
     *
     * 逐条查询后再删，是为了拿到每条评论的 articleId 回减文章评论数；
     * 不存在的 id 直接跳过，因此返回的 rows 可能小于入参长度。
     * 只删选中的评论，不会级联处理 parent_id 指向它的回复。
     */
    @Transactional
    public int deleteCommentByIds(Long[] commentIds) {
        if (commentIds == null || commentIds.length == 0) {
            return 0;
        }
        int rows = 0;
        for (Long commentId : commentIds) {
            BlogComment comment = commentMapper.selectCommentById(commentId);
            // 可能已被其他管理员删除，跳过而不是报错
            if (comment != null) {
                rows += commentMapper.deleteCommentByIds(new Long[]{commentId});
                articleMapper.decrementCommentCount(comment.getArticleId());
            }
        }
        return rows;
    }
}
