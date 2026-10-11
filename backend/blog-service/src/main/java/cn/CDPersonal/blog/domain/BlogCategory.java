package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;

import java.io.Serial;

/**
 * 鍗氬鍒嗙被 blog_category
 */
public class BlogCategory extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 鍒嗙被ID */
    private Long categoryId;

    /** 鍒嗙被鍚嶇О */
    private String categoryName;

    /** 鏄剧ず椤哄簭 */
    private Integer sort;

    /** 鐘舵€侊紙0姝ｅ父 1鍋滅敤锛?*/
    private String status;

    /** 璇ュ垎绫讳笅鐨勬枃绔犳暟锛堝彧璇伙紝鍒楄〃灞曠ず鐢級 */
    private Integer articleCount;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
