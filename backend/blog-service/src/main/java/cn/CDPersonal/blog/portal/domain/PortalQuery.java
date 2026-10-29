package cn.CDPersonal.blog.portal.domain;

/**
 * 门户（/portal 前端）的文章检索条件。
 *
 * 只用于已发布文章的公开查询：
 *   - categoryId / tagId：分类、标签筛选
 *   - keyword：标题 + 摘要模糊匹配
 *   - month：归档筛选，格式 yyyy-MM
 *   - orderBy：latest（默认）/ hot（按浏览量）/ comments（按评论数）
 *
 * 分页不走这个对象：PageHelper 直接在 PageUtils.startPage() 里读 request 的 pageNum/pageSize。
 */
public class PortalQuery {

    private Long categoryId;
    private Long tagId;
    private String keyword;
    private String month;
    private String orderBy;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
}
