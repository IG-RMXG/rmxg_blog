package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;

import java.io.Serial;

/**
 * 博客标签 blog_tag。
 *
 * <p>文章与标签是多对多关系，关联表为 blog_article_tag；标签本身不区分状态，
 * 删除标签时由 Mapper 一并清理关联。</p>
 */
public class BlogTag extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标签ID（主键，自增） */
    private Long tagId;

    /** 标签名称（门户标签云展示，建议不超过 50 字） */
    private String tagName;

    /** 显示顺序（升序，越小越靠前） */
    private Integer sort;

    /** 该标签下的文章数（只读，列表展示用，非表字段） */
    private Integer articleCount;

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
