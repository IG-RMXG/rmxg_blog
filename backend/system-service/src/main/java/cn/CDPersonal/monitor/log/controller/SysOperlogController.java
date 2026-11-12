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
 *
 * 查询条件：title / operName 模糊匹配，businessType / status 精确匹配（status 为 Integer：0 正常、1 异常）。
 * 分页由 PageUtils.startPage() 开启，PageHelper 拦截紧接着的那一条 SQL，中间不能再查库。
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

    // 字面量路径 /clean 优先于 /{operIds} 匹配，不会被当成 operIds 解析；
    // 清空是物理删除，前端已做二次确认。
    @DeleteMapping("/clean")
    public ResponseVO<Void> clean() {
        logService.cleanOperLog();
        return ResponseVO.success();
    }

    // @PathVariable Long[] 由 Spring 按逗号拆分（如 /operlog/1,2,3），一次删除多条记录。
    @DeleteMapping("/{operIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] operIds) {
        return logService.deleteOperLogByIds(operIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
