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

    public static final String STATUS_NORMAL = "0";
    public static final String STATUS_HIDDEN = "1";

    private final BlogCommentMapper commentMapper;
    private final BlogArticleMapper articleMapper;

    public BlogCommentService(BlogCommentMapper commentMapper, BlogArticleMapper articleMapper) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
    }

    public List<BlogComment> selectCommentList(BlogComment query) {
        return commentMapper.selectCommentList(query);
    }

    public BlogComment selectCommentById(Long commentId) {
        return commentMapper.selectCommentById(commentId);
    }

    @Transactional
    public int insertComment(BlogComment comment, Long userId) {
        comment.setCommentId(null);
        comment.setUserId(userId);
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        if (comment.getStatus() == null || comment.getStatus().isBlank()) {
            comment.setStatus(STATUS_NORMAL);
        }
        int rows = commentMapper.insertComment(comment);
        articleMapper.incrementCommentCount(comment.getArticleId());
        return rows;
    }

    @Transactional
    public int updateStatus(Long commentId, String status, String updateBy) {
        return commentMapper.updateStatus(commentId, status, updateBy);
    }

    @Transactional
    public int deleteCommentByIds(Long[] commentIds) {
        if (commentIds == null || commentIds.length == 0) {
            return 0;
        }
        int rows = 0;
        for (Long commentId : commentIds) {
            BlogComment comment = commentMapper.selectCommentById(commentId);
            if (comment != null) {
                rows += commentMapper.deleteCommentByIds(new Long[]{commentId});
                articleMapper.decrementCommentCount(comment.getArticleId());
            }
        }
        return rows;
    }
}
