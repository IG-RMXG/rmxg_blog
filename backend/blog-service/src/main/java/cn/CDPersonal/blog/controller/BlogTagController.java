package cn.CDPersonal.blog.controller;

import cn.CDPersonal.blog.domain.BlogTag;
import cn.CDPersonal.blog.security.BlogTokenService;
import cn.CDPersonal.blog.service.BlogTagService;
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
 * 博客标签接口（对外前缀 /blog/tag）。
 *
 * 管理类操作仅管理员可用；"all" 下拉给所有登录用户（文章编辑页选择标签用）。
 */
@RestController
@RequestMapping("/blog/tag")
public class BlogTagController extends BlogBaseController {

    private final BlogTagService tagService;

    public BlogTagController(BlogTokenService tokenService, BlogTagService tagService) {
        super(tokenService);
        this.tagService = tagService;
    }

    /** 标签管理列表（管理员，分页） */
    @GetMapping("/list")
    public TableDataInfo list(BlogTag query) {
        // 非管理员（含未登录）一律给空表：前端表格按 200 渲染，401/403 会打断页面；
        // 真正的数据隔离是"不查库"，写入权限由下面的增删改方法各自校验。
        if (!isAdmin()) {
            return PageUtils.getDataTable(null);
        }
        startPage();
        return PageUtils.getDataTable(tagService.selectTagList(query));
    }

    /** 全部标签下拉（登录用户均可） */
    @GetMapping("/all")
    public ResponseVO<List<BlogTag>> all() {
        // 一次返回全量、不分页：文章编辑页的标签多选需要完整候选集，标签表量级很小
        if (!isLogin()) {
            return ResponseVO.success(java.util.Collections.emptyList());
        }
        return ResponseVO.success(tagService.selectAllList());
    }

    /** 标签详情（管理员）：编辑弹窗回显用；articleCount 为 SQL 现算，不是 blog_tag 的字段 */
    @GetMapping("/{tagId}")
    public ResponseVO<BlogTag> getInfo(@PathVariable Long tagId) {
        if (!isAdmin()) {
            return forbidden("仅管理员可查看标签详情");
        }
        return ResponseVO.success(tagService.selectTagById(tagId));
    }

    /** 新增标签（管理员）：sort 缺省值由 Service 兜底；重名校验放在写入之前，避免插一半 */
    @PostMapping
    public ResponseVO<Void> add(@RequestBody BlogTag tag) {
        if (!isAdmin()) {
            return forbidden("仅管理员可新增标签");
        }
        // 新增时没有 tagId，判重不排除任何行
        if (!tagService.checkNameUnique(tag)) {
            return ResponseVO.error(500, "标签名称已存在");
        }
        tag.setCreateBy(currentUserName());
        return tagService.insertTag(tag) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    /** 修改标签（管理员）：整行覆盖语义，前端需回传完整对象，未传字段会被写成 NULL */
    @PutMapping
    public ResponseVO<Void> edit(@RequestBody BlogTag tag) {
        if (!isAdmin()) {
            return forbidden("仅管理员可修改标签");
        }
        if (tag.getTagId() == null) {
            return ResponseVO.error(500, "标签ID不能为空");
        }
        // tagId 非空时判重 SQL 会排除自身，改名以外的编辑不会被误判为重名
        if (!tagService.checkNameUnique(tag)) {
            return ResponseVO.error(500, "标签名称已存在");
        }
        tag.setUpdateBy(currentUserName());
        return tagService.updateTag(tag) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    /**
     * 批量删除标签（管理员）。标签没有状态/逻辑删除列，删了不可恢复；
     * 不在这里检查引用，是因为 Service 会先清 blog_article_tag 再删本体（同一事务）。
     */
    @DeleteMapping("/{tagIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] tagIds) {
        if (!isAdmin()) {
            return forbidden("仅管理员可删除标签");
        }
        return tagService.deleteTagByIds(tagIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
