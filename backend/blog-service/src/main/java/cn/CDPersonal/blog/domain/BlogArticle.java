package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * 鍗氬鏂囩珷 blog_article
 *
 * 闈炴暟鎹簱鍒楋紙鍙鍥炴樉锛夛細
 *   - categoryName锛氭墍灞炲垎绫诲悕绉帮紙LEFT JOIN blog_category锛? *   - tagNames    锛氶€楀彿鍒嗛殧鐨勬爣绛惧悕锛堝瓙鏌ヨ GROUP_CONCAT锛屽垪琛ㄥ睍绀猴級
 *   - tagIds      锛氳鏂囩珷宸查€夋爣绛?id锛堢紪杈戝洖鏄撅級
 *   - authorNickName锛氫綔鑰呮樀绉帮紙LEFT JOIN sys_user锛屽垪琛ㄥ睍绀猴級
 */
public class BlogArticle extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 鏂囩珷ID */
    private Long articleId;

    /** 鏂囩珷鏍囬 */
    private String title;

    /** 鎽樿 */
    private String summary;

    /** 姝ｆ枃锛圡arkdown 婧愮爜锛?*/
    private String content;

    /** 灏侀潰鍦板潃 */
    private String coverUrl;

    /** 鍒嗙被ID */
    private Long categoryId;

    /** 鐘舵€侊細0鑽夌 1宸插彂甯?2寰呭鏍?3宸查┏鍥?*/
    private String status;

    /** 鏄惁缃《锛?鍚?1鏄?*/
    private String isTop;

    /** 鏄惁鎺ㄨ崘锛?鍚?1鏄?*/
    private String isRecommend;

    /** 娴忚閲?*/
    private Integer viewCount;

    /** 鐐硅禐鏁?*/
    private Integer likeCount;

    /** 璇勮鏁?*/
    private Integer commentCount;

    /** 浣滆€呯敤鎴稩D锛坰ys_user.user_id锛?*/
    private Long userId;

    /** 鍙戝竷鏃堕棿 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    // ==================== 闈炴暟鎹簱鍒?====================

    private String categoryName;
    private String tagNames;
    private String authorNickName;
    private Long[] tagIds;
    private List<BlogTag> tags;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIsTop() {
        return isTop;
    }

    public void setIsTop(String isTop) {
        this.isTop = isTop;
    }

    public String getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(String isRecommend) {
        this.isRecommend = isRecommend;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTagNames() {
        return tagNames;
    }

    public void setTagNames(String tagNames) {
        this.tagNames = tagNames;
    }

    public String getAuthorNickName() {
        return authorNickName;
    }

    public void setAuthorNickName(String authorNickName) {
        this.authorNickName = authorNickName;
    }

    public Long[] getTagIds() {
        return tagIds;
    }

    public void setTagIds(Long[] tagIds) {
        this.tagIds = tagIds;
    }

    public List<BlogTag> getTags() {
        return tags;
    }

    public void setTags(List<BlogTag> tags) {
        this.tags = tags;
    }
}
