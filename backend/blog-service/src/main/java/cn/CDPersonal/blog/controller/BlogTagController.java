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
        if (!isAdmin()) {
            return PageUtils.getDataTable(null);
        }
        startPage();
        return PageUtils.getDataTable(tagService.selectTagList(query));
    }

    /** 全部标签下拉（登录用户均可） */
    @GetMapping("/all")
    public ResponseVO<List<BlogTag>> all() {
        if (!isLogin()) {
            return ResponseVO.success(java.util.Collections.emptyList());
        }
        return ResponseVO.success(tagService.selectAllList());
    }

    @GetMapping("/{tagId}")
    public ResponseVO<BlogTag> getInfo(@PathVariable Long tagId) {
        if (!isAdmin()) {
            return forbidden("仅管理员可查看标签详情");
        }
        return ResponseVO.success(tagService.selectTagById(tagId));
    }

    @PostMapping
    public ResponseVO<Void> add(@RequestBody BlogTag tag) {
        if (!isAdmin()) {
            return forbidden("仅管理员可新增标签");
        }
        if (!tagService.checkNameUnique(tag)) {
            return ResponseVO.error(500, "标签名称已存在");
        }
        tag.setCreateBy(currentUserName());
        return tagService.insertTag(tag) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@RequestBody BlogTag tag) {
        if (!isAdmin()) {
            return forbidden("仅管理员可修改标签");
        }
        if (tag.getTagId() == null) {
            return ResponseVO.error(500, "标签ID不能为空");
        }
        if (!tagService.checkNameUnique(tag)) {
            return ResponseVO.error(500, "标签名称已存在");
        }
        tag.setUpdateBy(currentUserName());
        return tagService.updateTag(tag) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

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
