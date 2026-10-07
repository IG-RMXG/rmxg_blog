package cn.CDPersonal.dict.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysDictType;
import cn.CDPersonal.dict.service.SysDictService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典类型管理。
 *
 * 前端契约（web/src/api/system/dict/type.js）：
 *   GET    /system/dict/type/list           分页列表 -> {total, rows}
 *   GET    /system/dict/type/{dictId}       详情
 *   POST   /system/dict/type                新增
 *   PUT    /system/dict/type                修改
 *   DELETE /system/dict/type/{dictIds}      删除（支持逗号分隔多个）
 *   DELETE /system/dict/type/refreshCache   刷新缓存
 *   GET    /system/dict/type/optionselect   下拉列表
 *
 * 注意：server.servlet.context-path 已是 /system，这里只写 /dict/type。
 */
@RestController
@RequestMapping("/dict/type")
public class SysDictTypeController extends BaseController {

    private final SysDictService dictService;

    public SysDictTypeController(SysDictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysDictType query) {
        startPage();
        return PageUtils.getDataTable(dictService.selectTypeList(query));
    }

    /** 下拉列表：只返回正常状态的类型 */
    @GetMapping("/optionselect")
    public ResponseVO<List<SysDictType>> optionselect() {
        List<SysDictType> all = dictService.selectTypeAll();
        List<SysDictType> enabled = all.stream()
                .filter(t -> "0".equals(t.getStatus()))
                .toList();
        return ResponseVO.success(enabled);
    }

    @GetMapping("/{dictId}")
    public ResponseVO<SysDictType> getInfo(@PathVariable Long dictId) {
        return ResponseVO.success(dictService.selectTypeById(dictId));
    }

    @PostMapping
    public ResponseVO<Void> add(@Valid @RequestBody SysDictType dictType) {
        if (!dictService.checkTypeUnique(dictType)) {
            return ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "新增字典'" + dictType.getDictName() + "'失败，字典类型已存在");
        }
        if (dictType.getStatus() == null) {
            dictType.setStatus("0");
        }
        dictType.setCreateBy(currentUserName());
        return dictService.insertType(dictType) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@Valid @RequestBody SysDictType dictType) {
        if (!dictService.checkTypeUnique(dictType)) {
            return ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "修改字典'" + dictType.getDictName() + "'失败，字典类型已存在");
        }
        dictType.setUpdateBy(currentUserName());
        return dictService.updateType(dictType) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "修改失败");
    }

    @DeleteMapping("/refreshCache")
    public ResponseVO<Void> refreshCache() {
        // 当前未引入字典缓存（每次直接查库），无需真正刷新
        return ResponseVO.success();
    }

    @DeleteMapping("/{dictIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] dictIds) {
        for (Long dictId : dictIds) {
            if (dictService.hasDataType(dictId)) {
                SysDictType type = dictService.selectTypeById(dictId);
                String name = type == null ? String.valueOf(dictId) : type.getDictName();
                return ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                        name + "已分配，不能删除");
            }
        }
        return dictService.deleteTypeByIds(dictIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "删除失败");
    }

    /** 当前登录用户名，用于 create_by / update_by 留痕 */
    private String currentUserName() {
        return LoginUserHolder.getUserName();
    }
}
