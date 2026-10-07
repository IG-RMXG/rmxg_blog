package cn.CDPersonal.monitor.job.service;

import cn.CDPersonal.common.domain.entity.SysJob;
import cn.CDPersonal.common.domain.entity.SysJobLog;
import cn.CDPersonal.monitor.job.mapper.SysJobMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 定时任务调度。
 *
 * 注意：当前未接入 Quartz 调度器（qrtz_* 表为空），所以：
 *   - 增删改查都是纯数据库操作，界面可用；
 *   - runJob 只写一条调度日志留痕，不会真正执行目标任务；
 *   - 任务状态切换也只改库，不注册/移除触发器。
 * 待接入真正的调度器时，在这里补 Scheduler 调用即可。
 */
@Service
public class SysJobService {

    private final SysJobMapper jobMapper;

    public SysJobService(SysJobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    // ==================== 任务 ====================

    public List<SysJob> selectJobList(SysJob query) {
        return jobMapper.selectJobList(query);
    }

    public SysJob selectJobById(Long jobId) {
        return jobMapper.selectJobById(jobId);
    }

    /** 任务名称+任务组 是否唯一（新增时 jobId 为空，修改时排除自身） */
    public boolean checkJobUnique(SysJob job) {
        SysJob exist = jobMapper.checkJobUnique(job.getJobName(), job.getJobGroup());
        if (exist == null) {
            return true;
        }
        return exist.getJobId() != null && exist.getJobId().equals(job.getJobId());
    }

    @Transactional
    public int insertJob(SysJob job) {
        return jobMapper.insertJob(job);
    }

    @Transactional
    public int updateJob(SysJob job) {
        return jobMapper.updateJob(job);
    }

    @Transactional
    public int updateJobStatus(Long jobId, String status) {
        return jobMapper.updateJobStatus(jobId, status);
    }

    @Transactional
    public int deleteJobByIds(Long[] jobIds) {
        return jobMapper.deleteJobByIds(jobIds);
    }

    @Transactional
    public int deleteJobLogByIds(Long[] jobLogIds) {
        return jobMapper.deleteJobLogByIds(jobLogIds);
    }

    @Transactional
    public int cleanJobLog() {
        return jobMapper.cleanJobLog();
    }

    // ==================== 调度日志 ====================

    public List<SysJobLog> selectJobLogList(SysJobLog query) {
        return jobMapper.selectJobLogList(query);
    }

    /**
     * 立即执行一次。当前无调度器，仅记录一条日志，返回是否成功。
     */
    @Transactional
    public boolean runJobOnce(Long jobId, String jobGroup) {
        SysJob job = jobMapper.selectJobById(jobId);
        if (job == null) {
            return false;
        }
        SysJobLog log = new SysJobLog();
        log.setJobName(job.getJobName());
        log.setJobGroup(jobGroup == null ? job.getJobGroup() : jobGroup);
        log.setInvokeTarget(job.getInvokeTarget());
        log.setJobMessage("手动触发成功（当前未接入 Quartz 调度器，仅记录日志未实际执行）");
        log.setStatus("0");
        log.setStartTime(new Date());
        log.setEndTime(new Date());
        return jobMapper.insertJobLog(log) > 0;
    }
}
