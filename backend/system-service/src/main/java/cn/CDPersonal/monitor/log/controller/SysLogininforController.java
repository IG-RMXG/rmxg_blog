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
 *
 * 查询条件：userName / ipaddr 模糊匹配，status 精确匹配且为字符串（'0'=成功、'1'=失败，
 * 字典 sys_common_status；与操作日志的 Integer status 不同，不能按数字 0 比较）。
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

    // 同操作日志：字面量 /clean 优先于 /{infoIds}，清空为物理删除。
    @DeleteMapping("/clean")
    public ResponseVO<Void> clean() {
        logService.cleanLogininfor();
        return ResponseVO.success();
    }

    // 路径变量按逗号拆成数组批量删除；删除 0 条返回失败，提示数据可能已被清理。
    @DeleteMapping("/{infoIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] infoIds) {
        return logService.deleteLogininforByIds(infoIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }

    // 无任何副作用：不查库、不写日志、不清 Redis，仅满足前端按钮的接口约定。
    /** 当前没有账户锁定机制，直接返回成功；预留接口避免前端报错 */
    @PutMapping("/unlock/{userName}")
    public ResponseVO<Void> unlock(@PathVariable String userName) {
        return ResponseVO.success();
    }
}
