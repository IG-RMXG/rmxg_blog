package cn.CDPersonal.monitor.job.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysJob;
import cn.CDPersonal.monitor.job.service.SysJobService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 定时任务调度。
 *
 * 前端契约（web/src/api/monitor/job.js）——注意路径是 /schedule 而不是 /system：
 *   GET    /schedule/job/list           分页
 *   GET    /schedule/job/{jobId}        详情
 *   POST   /schedule/job                新增
 *   PUT    /schedule/job                修改
 *   DELETE /schedule/job/{jobIds}       删除
 *   PUT    /schedule/job/changeStatus   改状态
 *   PUT    /schedule/job/run            立即执行一次
 *
 * context-path 是 /system，所以对外完整路径是 /system/schedule/job/**，
 * 需要网关额外转发 /schedule/**（见 SystemServiceForwardController）。
 */
@RestController
@RequestMapping("/schedule/job")
public class SysJobController {

    private final SysJobService jobService;

    public SysJobController(SysJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysJob query) {
        PageUtils.startPage();
        return PageUtils.getDataTable(jobService.selectJobList(query));
    }

    @GetMapping("/{jobId}")
    public ResponseVO<SysJob> getInfo(@PathVariable Long jobId) {
        return ResponseVO.success(jobService.selectJobById(jobId));
    }

    @PostMapping
    public ResponseVO<Void> add(@RequestBody SysJob job) {
        if (!jobService.checkJobUnique(job)) {
            return ResponseVO.error(500,
                    "新增任务'" + job.getJobName() + "'失败，任务名称已存在");
        }
        job.setCreateBy(LoginUserHolder.getUserName());
        if (job.getStatus() == null) {
            job.setStatus("1");
        }
        return jobService.insertJob(job) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@RequestBody SysJob job) {
        if (!jobService.checkJobUnique(job)) {
            return ResponseVO.error(500,
                    "修改任务'" + job.getJobName() + "'失败，任务名称已存在");
        }
        job.setUpdateBy(LoginUserHolder.getUserName());
        return jobService.updateJob(job) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    @PutMapping("/changeStatus")
    public ResponseVO<Void> changeStatus(@RequestBody SysJob job) {
        return jobService.updateJobStatus(job.getJobId(), job.getStatus()) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "状态修改失败");
    }

    @PutMapping("/run")
    public ResponseVO<Void> run(@RequestBody Map<String, Object> body) {
        Long jobId = body.get("jobId") == null ? null : Long.valueOf(String.valueOf(body.get("jobId")));
        String jobGroup = body.get("jobGroup") == null ? null : String.valueOf(body.get("jobGroup"));
        if (jobId == null) {
            return ResponseVO.error(500, "任务ID不能为空");
        }
        return jobService.runJobOnce(jobId, jobGroup)
                ? ResponseVO.success()
                : ResponseVO.error(500, "执行失败，任务不存在");
    }

    @DeleteMapping("/{jobIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] jobIds) {
        return jobService.deleteJobByIds(jobIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
