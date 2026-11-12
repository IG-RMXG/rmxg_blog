package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * 博客文章 blog_article。
 *
 * <p>状态机：0=草稿 1=已发布 2=待审核 3=已驳回；普通用户只能保存草稿或提交审核，
 * 管理员可任意切换（见 BlogArticleService）。publishTime 在转为已发布时写入，其它状态清空。</p>
 *
 * <p>以下字段不是数据库列，由 Mapper 联表/子查询填充，仅用于列表与编辑回显：</p>
 * <ul>
 *   <li>categoryName：所属分类名称（LEFT JOIN blog_category）</li>
 *   <li>tagNames：逗号分隔的标签名（子查询 GROUP_CONCAT）</li>
 *   <li>tagIds：该文章已选标签 id（编辑回显）</li>
 *   <li>authorNickName：作者昵称（LEFT JOIN sys_user）</li>
 * </ul>
 */
public class BlogArticle extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文章ID（主键，自增） */
    private Long articleId;

    /** 文章标题 */
    private String title;

    /** 摘要（列表与门户卡片展示，可为空） */
    private String summary;

    /** 正文（Markdown 源码） */
    private String content;

    /** 封面地址（为空时门户使用默认渐变封面） */
    private String coverUrl;

    /** 分类ID（关联 blog_category.category_id，可为空） */
    private Long categoryId;

    /** 状态：0草稿 1已发布 2待审核 3已驳回 */
    private String status;

    /** 是否置顶：0否 1是（仅管理员可改） */
    private String isTop;

    /** 是否推荐：0否 1是（仅管理员可改） */
    private String isRecommend;

    /** 浏览量（仅浏览详情接口会 +1） */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数（评论新增/删除时同步维护） */
    private Integer commentCount;

    /** 作者用户ID（sys_user.user_id） */
    private Long userId;

    /** 发布时间（转为已发布时写入，其它状态清空） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    // ==================== 非数据库列（只读回显） ====================

    /** 所属分类名称（联表回显，非表字段） */
    private String categoryName;
    /** 标签名，逗号分隔（聚合回显，非表字段） */
    private String tagNames;
    /** 作者昵称（联表回显，非表字段） */
    private String authorNickName;
    /** 已选标签ID集合（编辑回显，非表字段） */
    private Long[] tagIds;
    /** 标签对象列表（详情回显，非表字段） */
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
