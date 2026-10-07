package cn.CDPersonal.notice.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysNotice;
import cn.CDPersonal.notice.service.SysNoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知公告管理。
 *
 * 前端契约（web/src/api/system/notice.js）：
 *   GET    /system/notice/list          分页列表 -> {total, rows}
 *   GET    /system/notice/{noticeId}    详情
 *   GET    /system/notice/listTop       最近 5 条已发布公告（首页用）
 *   POST   /system/notice               新增
 *   PUT    /system/notice               修改
 *   DELETE /system/notice/{noticeIds}   删除（支持逗号分隔多个）
 *
 * 注意：server.servlet.context-path 已是 /system，这里只写 /notice。
 * /listTop 是字面量路径，优先级高于 /{noticeId}，不会被当成 noticeId 解析。
 */
@RestController
@RequestMapping("/notice")
public class SysNoticeController extends BaseController {

    private final SysNoticeService noticeService;

    public SysNoticeController(SysNoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysNotice query) {
        startPage();
        return PageUtils.getDataTable(noticeService.selectNoticeList(query));
    }

    @GetMapping("/listTop")
    public ResponseVO<List<SysNotice>> listTop() {
        return ResponseVO.success(noticeService.selectNoticeTop());
    }

    @GetMapping("/{noticeId}")
    public ResponseVO<SysNotice> getInfo(@PathVariable Long noticeId) {
        return ResponseVO.success(noticeService.selectNoticeById(noticeId));
    }

    @PostMapping
    public ResponseVO<Void> add(@Valid @RequestBody SysNotice notice) {
        if (notice.getStatus() == null) {
            notice.setStatus("0");
        }
        notice.setCreateBy(currentUserName());
        return noticeService.insertNotice(notice) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@Valid @RequestBody SysNotice notice) {
        notice.setUpdateBy(currentUserName());
        return noticeService.updateNotice(notice) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "修改失败");
    }

    @DeleteMapping("/{noticeIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] noticeIds) {
        return noticeService.deleteNoticeByIds(noticeIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "删除失败");
    }

    /** 当前登录用户名，用于 create_by / update_by 留痕 */
    private String currentUserName() {
        return LoginUserHolder.getUserName();
    }
}
