package cn.CDPersonal.post.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysPost;
import cn.CDPersonal.post.service.SysPostService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位管理。
 *
 * 前端契约（web/src/api/system/post.js）：
 *   GET    /system/post/list        分页 -> {total, rows}
 *   GET    /system/post/{postId}    详情
 *   POST   /system/post             新增
 *   PUT    /system/post             修改
 *   DELETE /system/post/{postIds}   删除（支持逗号分隔多个）
 */
@RestController
@RequestMapping("/post")
public class SysPostController extends BaseController {

    private final SysPostService postService;

    public SysPostController(SysPostService postService) {
        this.postService = postService;
    }

    /** 分页列表，前端读 response.rows / response.total */
    @GetMapping("/list")
    public TableDataInfo list(SysPost query) {
        startPage();
        return PageUtils.getDataTable(postService.selectPostList(query));
    }

    @GetMapping("/{postId}")
    public ResponseVO<SysPost> getInfo(@PathVariable Long postId) {
        return ResponseVO.success(postService.selectPostById(postId));
    }

    @PostMapping
    public ResponseVO<Void> add(@Validated @RequestBody SysPost post) {
        if (!postService.checkPostCodeUnique(post)) {
            return ResponseVO.error(500, "新增岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        if (!postService.checkPostNameUnique(post)) {
            return ResponseVO.error(500, "新增岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        post.setCreateBy(LoginUserHolder.getUserName());
        return postService.insertPost(post) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@Validated @RequestBody SysPost post) {
        if (post.getPostId() == null) {
            return ResponseVO.error(500, "岗位ID不能为空");
        }
        if (!postService.checkPostCodeUnique(post)) {
            return ResponseVO.error(500, "修改岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        if (!postService.checkPostNameUnique(post)) {
            return ResponseVO.error(500, "修改岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        post.setUpdateBy(LoginUserHolder.getUserName());
        return postService.updatePost(post) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    /** 批量删除；已被用户引用的岗位不允许删 */
    @DeleteMapping("/{postIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] postIds) {
        if (postService.hasUserPostByIds(postIds)) {
            return ResponseVO.error(500, "岗位已分配用户,不允许删除");
        }
        return postService.deletePostByIds(postIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
