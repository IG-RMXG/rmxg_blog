package cn.CDPersonal.monitor.log.controller;

import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysLogininfor;
import cn.CDPersonal.monitor.log.service.SysLogService;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志。
 *
 * 前端契约（web/src/api/system/logininfor.js）：
 *   GET    /system/logininfor/list        分页，参数 ipaddr / userName / status
 *   DELETE /system/logininfor/{infoIds}   删除（逗号分隔多个）
 *   DELETE /system/logininfor/clean       清空
 *   PUT    /system/logininfor/unlock/{userName}  解锁（无账户锁定机制，直接成功）
 */
@RestController
@RequestMapping("/logininfor")
public class SysLogininforController {

    private final SysLogService logService;

    public SysLogininforController(SysLogService logService) {
        this.logService = logService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysLogininfor query) {
        PageUtils.startPage();
        return PageUtils.getDataTable(logService.selectLogininforList(query));
    }

    @DeleteMapping("/clean")
    public ResponseVO<Void> clean() {
        logService.cleanLogininfor();
        return ResponseVO.success();
    }

    @DeleteMapping("/{infoIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] infoIds) {
        return logService.deleteLogininforByIds(infoIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }

    /** 当前没有账户锁定机制，直接返回成功；预留接口避免前端报错 */
    @PutMapping("/unlock/{userName}")
    public ResponseVO<Void> unlock(@PathVariable String userName) {
        return ResponseVO.success();
    }
}
