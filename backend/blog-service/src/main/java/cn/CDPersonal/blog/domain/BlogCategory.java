package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;

import java.io.Serial;

/**
 * 博客分类 blog_category。
 *
 * <p>后台「博客管理 - 分类管理」与门户「分类筛选」共用；status 为停用(1)时不出现在门户的
 * 分类列表里，但历史文章仍保留该 categoryId。</p>
 */
public class BlogCategory extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类ID（主键，自增） */
    private Long categoryId;

    /** 分类名称（门户按此展示，建议不超过 50 字） */
    private String categoryName;

    /** 显示顺序（升序，越小越靠前） */
    private Integer sort;

    /** 状态：0正常 1停用 */
    private String status;

    /** 该分类下的文章数（只读，列表展示用，非表字段） */
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
