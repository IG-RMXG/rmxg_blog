package cn.CDPersonal.blog.controller;

import cn.CDPersonal.blog.domain.BlogCategory;
import cn.CDPersonal.blog.security.BlogTokenService;
import cn.CDPersonal.blog.service.BlogCategoryService;
import cn.CDPersonal.blog.support.PageUtils;
import cn.CDPersonal.blog.support.TableDataInfo;
import cn.CDPersonal.common.core.ResponseVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 博客分类接口（对外前缀 /blog/category）。
 *
 * 管理类操作（增删改查列表）仅管理员可用；"enabled" 下拉给所有登录用户
 * （文章编辑页选择分类用）。
 */
@RestController
@RequestMapping("/blog/category")
public class BlogCategoryController extends BlogBaseController {

    private final BlogCategoryService categoryService;

    public BlogCategoryController(BlogTokenService tokenService, BlogCategoryService categoryService) {
        super(tokenService);
        this.categoryService = categoryService;
    }

    /** 分类管理列表（管理员，分页） */
    @GetMapping("/list")
    public TableDataInfo list(BlogCategory query) {
        if (!isLogin()) {
            return PageUtils.getDataTable(null);
        }
        if (!isAdmin()) {
            return PageUtils.getDataTable(null);
        }
        startPage();
        return PageUtils.getDataTable(categoryService.selectCategoryList(query));
    }

    /** 启用分类下拉（登录用户均可） */
    @GetMapping("/enabled")
    public ResponseVO<List<BlogCategory>> enabled() {
        if (!isLogin()) {
            return ResponseVO.success(java.util.Collections.emptyList());
        }
        return ResponseVO.success(categoryService.selectEnabledList());
    }

    @GetMapping("/{categoryId}")
    public ResponseVO<BlogCategory> getInfo(@PathVariable Long categoryId) {
        if (!isAdmin()) {
            return forbidden("仅管理员可查看分类详情");
        }
        return ResponseVO.success(categoryService.selectCategoryById(categoryId));
    }

    @PostMapping
    public ResponseVO<Void> add(@RequestBody BlogCategory category) {
        if (!isAdmin()) {
            return forbidden("仅管理员可新增分类");
        }
        if (!categoryService.checkNameUnique(category)) {
            return ResponseVO.error(500, "分类名称已存在");
        }
        category.setCreateBy(currentUserName());
        return categoryService.insertCategory(category) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@RequestBody BlogCategory category) {
        if (!isAdmin()) {
            return forbidden("仅管理员可修改分类");
        }
        if (category.getCategoryId() == null) {
            return ResponseVO.error(500, "分类ID不能为空");
        }
        if (!categoryService.checkNameUnique(category)) {
            return ResponseVO.error(500, "分类名称已存在");
        }
        category.setUpdateBy(currentUserName());
        return categoryService.updateCategory(category) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    @DeleteMapping("/{categoryIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] categoryIds) {
        if (!isAdmin()) {
            return forbidden("仅管理员可删除分类");
        }
        for (Long categoryId : categoryIds) {
            if (categoryService.hasArticles(categoryId)) {
                return ResponseVO.error(500, "分类下存在文章，不能删除");
            }
        }
        return categoryService.deleteCategoryByIds(categoryIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
