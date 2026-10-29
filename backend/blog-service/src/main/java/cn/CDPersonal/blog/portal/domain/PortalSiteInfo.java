package cn.CDPersonal.blog.portal.domain;

/**
 * 门户站点信息（首页 + 侧栏共用，一次请求取全）。
 *
 * 统计口径统一为「已发布文章」：
 *   articleCount  已发布文章数
 *   categoryCount 有已发布文章的分类数
 *   tagCount      有已发布文章的标签数
 *   commentCount  已发布文章下的正常评论数
 *   viewCount     已发布文章浏览量之和
 */
public class PortalSiteInfo {

    /** 站点标题，取自 blog.portal.title 配置 */
    private String title;

    /** 站点副标题，取自 blog.portal.subtitle 配置 */
    private String subtitle;

    /** 博主信息 */
    private PortalAuthor author;

    private int articleCount;
    private int categoryCount;
    private int tagCount;
    private int commentCount;
    private long viewCount;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public PortalAuthor getAuthor() {
        return author;
    }

    public void setAuthor(PortalAuthor author) {
        this.author = author;
    }

    public int getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    public int getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(int categoryCount) {
        this.categoryCount = categoryCount;
    }

    public int getTagCount() {
        return tagCount;
    }

    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }
}
