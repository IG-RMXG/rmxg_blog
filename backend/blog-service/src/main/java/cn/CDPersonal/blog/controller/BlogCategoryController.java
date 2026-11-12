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
        // 未登录与非管理员都返回空表：表格组件按 200 + {rows,total} 渲染，抛 401/403 会让页面弹错；
        // 这里的数据保护方式是"根本不查库"，写入权限由下面各方法逐个校验。
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
        // 未登录给空数组：该下拉只在文章编辑页使用，外层路由已保证登录态，这里不必弹 401
        if (!isLogin()) {
            return ResponseVO.success(java.util.Collections.emptyList());
        }
        return ResponseVO.success(categoryService.selectEnabledList());
    }

    /** 分类详情（管理员）：编辑弹窗回显用，返回值里的 articleCount 是 SQL 现算的统计列，不是表字段 */
    @GetMapping("/{categoryId}")
    public ResponseVO<BlogCategory> getInfo(@PathVariable Long categoryId) {
        if (!isAdmin()) {
            return forbidden("仅管理员可查看分类详情");
        }
        return ResponseVO.success(categoryService.selectCategoryById(categoryId));
    }

    /** 新增分类（管理员）：sort / status 的缺省值由 Service 兜底，重名校验在插入前完成 */
    @PostMapping
    public ResponseVO<Void> add(@RequestBody BlogCategory category) {
        if (!isAdmin()) {
            return forbidden("仅管理员可新增分类");
        }
        // 新增时请求体没有 categoryId，判重 SQL 不会排除任何行；与编辑共用同一个 Service 方法
        if (!categoryService.checkNameUnique(category)) {
            return ResponseVO.error(500, "分类名称已存在");
        }
        category.setCreateBy(currentUserName());
        return categoryService.insertCategory(category) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    /** 修改分类（管理员）：整行覆盖语义，前端需回传完整对象，未传字段会被写成 NULL */
    @PutMapping
    public ResponseVO<Void> edit(@RequestBody BlogCategory category) {
        if (!isAdmin()) {
            return forbidden("仅管理员可修改分类");
        }
        if (category.getCategoryId() == null) {
            return ResponseVO.error(500, "分类ID不能为空");
        }
        // 这里 categoryId 一定非空，判重 SQL 会排除自身，所以"不改名只改排序"不会被误判为重名
        if (!categoryService.checkNameUnique(category)) {
            return ResponseVO.error(500, "分类名称已存在");
        }
        category.setUpdateBy(currentUserName());
        return categoryService.updateCategory(category) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    /**
     * 批量删除分类（管理员）：逐条检查分类下是否还有文章，只要有一个非空就整体拒绝，
     * 避免前端出现"删了一半"的中间状态。
     */
    @DeleteMapping("/{categoryIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] categoryIds) {
        if (!isAdmin()) {
            return forbidden("仅管理员可删除分类");
        }
        // 分类是物理删除且没有外键级联，删掉后文章的 category_id 会变成悬空引用，所以必须先拦
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
