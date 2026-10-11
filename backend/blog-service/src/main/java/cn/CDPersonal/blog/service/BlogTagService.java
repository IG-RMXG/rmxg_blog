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

    private final BlogTagMapper tagMapper;

    public BlogTagService(BlogTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public List<BlogTag> selectTagList(BlogTag query) {
        return tagMapper.selectTagList(query);
    }

    /** 全部标签（文章编辑页下拉） */
    public List<BlogTag> selectAllList() {
        return tagMapper.selectAllList();
    }

    public BlogTag selectTagById(Long tagId) {
        return tagMapper.selectTagById(tagId);
    }

    public boolean checkNameUnique(BlogTag tag) {
        return tagMapper.checkNameUnique(tag) == 0;
    }

    @Transactional
    public int insertTag(BlogTag tag) {
        if (tag.getSort() == null) {
            tag.setSort(0);
        }
        return tagMapper.insertTag(tag);
    }

    @Transactional
    public int updateTag(BlogTag tag) {
        return tagMapper.updateTag(tag);
    }

    @Transactional
    public int deleteTagByIds(Long[] tagIds) {
        tagMapper.deleteArticleTagByTagIds(tagIds);
        return tagMapper.deleteTagByIds(tagIds);
    }
}
