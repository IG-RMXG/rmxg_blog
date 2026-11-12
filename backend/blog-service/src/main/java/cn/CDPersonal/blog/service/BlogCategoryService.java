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

    // 只负责分类表的读写与默认值兜底；删除前的"分类下是否有文章"校验由 Controller 调 hasArticles 完成
    private final BlogCategoryMapper categoryMapper;

    public BlogCategoryService(BlogCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * 分类管理列表（分页由 Controller 的 startPage() 控制）。
     *
     * articleCount 是 SQL 子查询现算的统计列，不是 blog_category 的字段；
     * 排序为 sort ASC, category_id ASC，保证 sort 相同的分类顺序稳定。
     */
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

    /**
     * 分类名是否可用：Mapper 返回重名条数，0 条即通过。
     * 传了 categoryId 时 SQL 会排除自身，因此新增和编辑共用这一个校验。
     */
    public boolean checkNameUnique(BlogCategory category) {
        return categoryMapper.checkNameUnique(category) == 0;
    }

    /**
     * 新增分类：sort 缺省 0，status 缺省 "0"（启用）。
     * 重名校验由 Controller 先做，这里不重复查库。
     */
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

    /**
     * 更新分类：整行覆盖语义，UPDATE 会写回 category_name/sort/status/remark，
     * 未传的字段会被写成 NULL，因此前端编辑时必须回传完整对象。
     */
    @Transactional
    public int updateCategory(BlogCategory category) {
        return categoryMapper.updateCategory(category);
    }

    /**
     * 批量物理删除分类（blog_category 没有逻辑删除列）。
     *
     * 只删分类本身、不动其下文章：Controller 已先用 hasArticles() 拦住非空分类，
     * 直接调用本方法可能让文章留下指向已删分类的 category_id。
     */
    @Transactional
    public int deleteCategoryByIds(Long[] categoryIds) {
        return categoryMapper.deleteCategoryByIds(categoryIds);
    }

    /** 分类下是否有文章（删除前校验） */
    public boolean hasArticles(Long categoryId) {
        // 直接复用详情 SQL 里算好的 article_count，避免再发一条 COUNT 查询
        BlogCategory category = categoryMapper.selectCategoryById(categoryId);
        return category != null && category.getArticleCount() != null && category.getArticleCount() > 0;
    }
}
