package cn.CDPersonal.blog.domain;

import cn.CDPersonal.blog.support.BaseEntity;

import java.io.Serial;

/**
 * 鍗氬鏍囩 blog_tag
 */
public class BlogTag extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 鏍囩ID */
    private Long tagId;

    /** 鏍囩鍚嶇О */
    private String tagName;

    /** 鏄剧ず椤哄簭 */
    private Integer sort;

    /** 璇ユ爣绛句笅鐨勬枃绔犳暟锛堝彧璇伙紝鍒楄〃灞曠ず鐢級 */
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
