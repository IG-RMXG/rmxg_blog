package cn.CDPersonal.common.domain.entity;

import cn.CDPersonal.common.domain.model.BaseEntity;

/**
 * 定时任务调度 sys_job。
 * 注意：quartz 的 qrtz_* 表存在，但当前没有接入 Quartz 调度器，
 * 所以这里只做数据库 CRUD，"立即执行一次"不会真正触发。
 */
public class SysJob extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long jobId;
    /** 任务名称 */
    private String jobName;
    /** 任务组名 */
    private String jobGroup;
    /** 调用目标字符串 */
    private String invokeTarget;
    /** cron 执行表达式 */
    private String cronExpression;
    /** 计划执行错误策略 1立即执行 2执行一次 3放弃执行 */
    private String misfirePolicy;
    /** 是否并发执行 0允许 1禁止 */
    private String concurrent;
    /** 状态 0正常 1暂停 */
    private String status;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getInvokeTarget() {
        return invokeTarget;
    }

    public void setInvokeTarget(String invokeTarget) {
        this.invokeTarget = invokeTarget;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getMisfirePolicy() {
        return misfirePolicy;
    }

    public void setMisfirePolicy(String misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    public String getConcurrent() {
        return concurrent;
    }

    public void setConcurrent(String concurrent) {
        this.concurrent = concurrent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
