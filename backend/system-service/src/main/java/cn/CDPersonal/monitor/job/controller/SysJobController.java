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
 *
 * 任务唯一性由应用层 checkJobUnique 校验（job_name + job_group），校验与写库是两步、无并发控制；
 * 状态值为字符串：'0' 正常、'1' 暂停，改状态与删除都不触碰调度器（当前未接入 Quartz）。
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

    // 唯一性由应用层 checkJobUnique 校验；未传 status 时默认 '1'（暂停），与前端新增表单默认的 '0'（正常）不同。
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

    // 修改同样先校验唯一性（checkJobUnique 会排除自身 jobId），再整对象覆盖更新。
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

    // 列表开关入口：只更新 status，不校验取值范围，也不注册/移除触发器。
    @PutMapping("/changeStatus")
    public ResponseVO<Void> changeStatus(@RequestBody SysJob job) {
        return jobService.updateJobStatus(job.getJobId(), job.getStatus()) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "状态修改失败");
    }

    // 用 Map 接收是因为前端可能把 jobId 传成数字或字符串；jobGroup 可选。
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

    // 逗号分隔多个 jobId；删除 0 条视为失败（id 不存在或已被并发删除）。
    @DeleteMapping("/{jobIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] jobIds) {
        return jobService.deleteJobByIds(jobIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
