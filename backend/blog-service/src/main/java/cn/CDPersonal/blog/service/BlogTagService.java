package cn.CDPersonal.blog.service;

import cn.CDPersonal.blog.domain.BlogTag;
import cn.CDPersonal.blog.mapper.BlogTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 博客标签管理
 */
@Service
public class BlogTagService {

    // 标签没有状态/逻辑删除列：删除即物理删除，并同步清理 blog_article_tag 中的关联
    private final BlogTagMapper tagMapper;

    public BlogTagService(BlogTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    /**
     * 标签管理列表（分页由 Controller 的 startPage() 控制）。
     *
     * articleCount 由 SQL 关联 blog_article_tag 现算，不是 blog_tag 的字段；
     * 排序为 sort ASC, tag_id ASC，保证 sort 相同的标签顺序稳定。
     */
    public List<BlogTag> selectTagList(BlogTag query) {
        return tagMapper.selectTagList(query);
    }

    /** 全部标签（文章编辑页下拉） */
    public List<BlogTag> selectAllList() {
        // 不分页、不筛选：文章编辑页的标签多选需要一次取全量（标签量级很小）
        return tagMapper.selectAllList();
    }

    public BlogTag selectTagById(Long tagId) {
        return tagMapper.selectTagById(tagId);
    }

    /**
     * 标签名是否可用：Mapper 返回重名条数，0 条即通过。
     * 传了 tagId 时 SQL 会排除自身，所以新增和编辑共用一个校验。
     */
    public boolean checkNameUnique(BlogTag tag) {
        return tagMapper.checkNameUnique(tag) == 0;
    }

    /** 新增标签：sort 缺省 0（升序排最前）；重名校验由 Controller 先行完成 */
    @Transactional
    public int insertTag(BlogTag tag) {
        if (tag.getSort() == null) {
            tag.setSort(0);
        }
        return tagMapper.insertTag(tag);
    }

    /**
     * 更新标签：UPDATE 会整行写回 tag_name/sort/remark，
     * 未传字段会被写成 NULL，前端编辑时需要回传完整对象。
     */
    @Transactional
    public int updateTag(BlogTag tag) {
        return tagMapper.updateTag(tag);
    }

    /**
     * 批量物理删除标签。
     *
     * 先删 blog_article_tag 中的关联再删标签本体：两表之间没有外键级联，
     * 顺序反了会留下指向不存在标签的中间表记录，导致文章详情的标签回显出错。
     */
    @Transactional
    public int deleteTagByIds(Long[] tagIds) {
        tagMapper.deleteArticleTagByTagIds(tagIds);
        return tagMapper.deleteTagByIds(tagIds);
    }
}
