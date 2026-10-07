package cn.CDPersonal.dict.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysDictData;
import cn.CDPersonal.dict.service.SysDictService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据管理。
 *
 * 前端契约（web/src/api/system/dict/data.js）：
 *   GET    /system/dict/data/list           分页列表 -> {total, rows}
 *   GET    /system/dict/data/{dictCode}     详情
 *   GET    /system/dict/data/type/{dictType} 按类型取可用数据（前端 useDict 用）
 *   POST   /system/dict/data                新增
 *   PUT    /system/dict/data                修改
 *   DELETE /system/dict/data/{dictCodes}    删除（支持逗号分隔多个）
 */
@RestController
@RequestMapping("/dict/data")
public class SysDictDataController extends BaseController {

    private final SysDictService dictService;

    public SysDictDataController(SysDictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysDictData query) {
        startPage();
        return PageUtils.getDataTable(dictService.selectDataList(query));
    }

    /** 按字典类型取可用数据，返回结构需要是 {code,msg,data:[...]} */
    @GetMapping("/type/{dictType}")
    public ResponseVO<List<SysDictData>> dictType(@PathVariable String dictType) {
        return ResponseVO.success(dictService.selectDataByType(dictType));
    }

    @GetMapping("/{dictCode}")
    public ResponseVO<SysDictData> getInfo(@PathVariable Long dictCode) {
        return ResponseVO.success(dictService.selectDataById(dictCode));
    }

    @PostMapping
    public ResponseVO<Void> add(@Validated @RequestBody SysDictData dictData) {
        if (dictData.getStatus() == null) {
            dictData.setStatus("0");
        }
        if (dictData.getIsDefault() == null) {
            dictData.setIsDefault("N");
        }
        dictData.setCreateBy(LoginUserHolder.getUserName());
        return dictService.insertData(dictData) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@Validated @RequestBody SysDictData dictData) {
        dictData.setUpdateBy(LoginUserHolder.getUserName());
        return dictService.updateData(dictData) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "修改失败");
    }

    @DeleteMapping("/{dictCodes}")
    public ResponseVO<Void> remove(@PathVariable Long[] dictCodes) {
        return dictService.deleteDataByIds(dictCodes) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "删除失败");
    }
}
