package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;

import java.io.Serial;

/**
 * 博客评论 blog_comment。
 *
 * <p>用 parentId 表示两级结构：0 为顶级评论，非 0 指向顶级评论的 commentId；
 * status 为隐藏(1)时门户不展示，但后台仍可见可恢复。</p>
 */
public class BlogComment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评论ID（主键，自增） */
    private Long commentId;

    /** 所属文章ID（blog_article.article_id） */
    private Long articleId;

    /** 父评论ID：0 表示顶级评论，非 0 表示回复某条顶级评论 */
    private Long parentId;

    /** 评论内容（纯文本，门户用文本插值渲染，不解析 HTML） */
    private String content;

    /** 评论者用户ID（sys_user.user_id） */
    private Long userId;

    /** 状态：0正常 1隐藏 */
    private String status;

    // ==================== 非数据库列（回显） ====================

    /** 文章标题（联表回显，非表字段） */
    private String articleTitle;
    /** 评论者昵称（联表回显，非表字段） */
    private String nickName;
    /** 评论者账号（联表回显，非表字段） */
    private String userName;

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
