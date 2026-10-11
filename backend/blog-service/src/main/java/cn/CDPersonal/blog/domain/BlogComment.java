package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;

import java.io.Serial;

/**
 * 鍗氬璇勮 blog_comment
 */
public class BlogComment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 璇勮ID */
    private Long commentId;

    /** 鏂囩珷ID */
    private Long articleId;

    /** 鐖惰瘎璁篒D锛? 琛ㄧず椤剁骇璇勮锛?*/
    private Long parentId;

    /** 璇勮鍐呭 */
    private String content;

    /** 璇勮鑰呯敤鎴稩D */
    private Long userId;

    /** 鐘舵€侊細0姝ｅ父 1闅愯棌 */
    private String status;

    // ==================== 闈炴暟鎹簱鍒楋紙鍥炴樉锛?====================

    private String articleTitle;
    private String nickName;
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
