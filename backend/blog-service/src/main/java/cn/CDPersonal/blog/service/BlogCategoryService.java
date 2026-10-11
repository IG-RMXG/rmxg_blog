package cn.CDPersonal.blog.service;

import cn.CDPersonal.blog.domain.BlogCategory;
import cn.CDPersonal.blog.mapper.BlogCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 博客分类管理
 */
@Service
public class BlogCategoryService {

    private final BlogCategoryMapper categoryMapper;

    public BlogCategoryService(BlogCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<BlogCategory> selectCategoryList(BlogCategory query) {
        return categoryMapper.selectCategoryList(query);
    }

    /** 启用的分类（文章编辑页下拉） */
    public List<BlogCategory> selectEnabledList() {
        return categoryMapper.selectEnabledList();
    }

    public BlogCategory selectCategoryById(Long categoryId) {
        return categoryMapper.selectCategoryById(categoryId);
    }

    public boolean checkNameUnique(BlogCategory category) {
        return categoryMapper.checkNameUnique(category) == 0;
    }

    @Transactional
    public int insertCategory(BlogCategory category) {
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null || category.getStatus().isBlank()) {
            category.setStatus("0");
        }
        return categoryMapper.insertCategory(category);
    }

    @Transactional
    public int updateCategory(BlogCategory category) {
        return categoryMapper.updateCategory(category);
    }

    @Transactional
    public int deleteCategoryByIds(Long[] categoryIds) {
        return categoryMapper.deleteCategoryByIds(categoryIds);
    }

    /** 分类下是否有文章（删除前校验） */
    public boolean hasArticles(Long categoryId) {
        BlogCategory category = categoryMapper.selectCategoryById(categoryId);
        return category != null && category.getArticleCount() != null && category.getArticleCount() > 0;
    }
}
