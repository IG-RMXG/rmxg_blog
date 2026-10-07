package cn.CDPersonal.monitor.log.controller;

import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysOperLog;
import cn.CDPersonal.monitor.log.service.SysLogService;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志。
 *
 * 前端契约（web/src/api/system/operlog.js）：
 *   GET    /system/operlog/list         分页，参数 title / operName / businessType / status
 *   DELETE /system/operlog/{operIds}    删除（逗号分隔多个）
 *   DELETE /system/operlog/clean        清空
 */
@RestController
@RequestMapping("/operlog")
public class SysOperlogController {

    private final SysLogService logService;

    public SysOperlogController(SysLogService logService) {
        this.logService = logService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysOperLog query) {
        PageUtils.startPage();
        return PageUtils.getDataTable(logService.selectOperLogList(query));
    }

    @DeleteMapping("/clean")
    public ResponseVO<Void> clean() {
        logService.cleanOperLog();
        return ResponseVO.success();
    }

    @DeleteMapping("/{operIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] operIds) {
        return logService.deleteOperLogByIds(operIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
