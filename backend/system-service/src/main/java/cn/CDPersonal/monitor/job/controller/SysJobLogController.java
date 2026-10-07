package cn.CDPersonal.monitor.job.controller;

import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysJobLog;
import cn.CDPersonal.monitor.job.service.SysJobService;
import org.springframework.web.bind.annotation.*;

/**
 * 调度日志。
 *
 * 前端契约（web/src/api/monitor/jobLog.js）：
 *   GET    /schedule/job/log/list          分页
 *   DELETE /schedule/job/log/{jobLogIds}   删除
 *   DELETE /schedule/job/log/clean         清空
 */
@RestController
@RequestMapping("/schedule/job/log")
public class SysJobLogController {

    private final SysJobService jobService;

    public SysJobLogController(SysJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysJobLog query) {
        PageUtils.startPage();
        return PageUtils.getDataTable(jobService.selectJobLogList(query));
    }

    @DeleteMapping("/clean")
    public ResponseVO<Void> clean() {
        jobService.cleanJobLog();
        return ResponseVO.success();
    }

    @DeleteMapping("/{jobLogIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] jobLogIds) {
        return jobService.deleteJobLogByIds(jobLogIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
