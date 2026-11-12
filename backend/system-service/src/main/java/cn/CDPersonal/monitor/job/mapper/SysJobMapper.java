package cn.CDPersonal.monitor.job.mapper;

import cn.CDPersonal.common.domain.entity.SysJob;
import cn.CDPersonal.common.domain.entity.SysJobLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 定时任务（sys_job）与调度日志（sys_job_log）的注解式 Mapper。
 *
 * 约定：
 *   - 两张表都没有逻辑删除列，delete / clean 为物理删除；deleteJobByIds 不级联删调度日志；
 *   - 动态条件统一用 <script> 包裹；列表按主键倒序，SQL 不写 LIMIT，分页由 PageUtils 拦截；
 *   - 新增依赖 @Options 回填自增主键，插入后可直接读 jobId / jobLogId；
 *   - create_time 一律取数据库 NOW()，update_time 只在 update 语句里刷新；
 *   - 批量删除用 foreach 拼 IN，空数组会产生非法 SQL，调用方需先判空。
 */
@Mapper
public interface SysJobMapper {

    // ==================== 任务 ====================

    @Select("""
            <script>
            SELECT job_id, job_name, job_group, invoke_target, cron_expression,
                   misfire_policy, concurrent, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_job
            <where>
              <if test="jobName != null and jobName != ''">
                AND job_name LIKE CONCAT('%', #{jobName}, '%')
              </if>
              <if test="jobGroup != null and jobGroup != ''">
                AND job_group = #{jobGroup}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
            </where>
            ORDER BY job_id DESC
            </script>
            """)
    List<SysJob> selectJobList(SysJob query);

    @Select("""
            SELECT job_id, job_name, job_group, invoke_target, cron_expression,
                   misfire_policy, concurrent, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_job WHERE job_id = #{jobId}
            """)
    SysJob selectJobById(@Param("jobId") Long jobId);

    /** 校验 任务名称+任务组 唯一 */
    @Select("""
            SELECT job_id, job_name, job_group FROM sys_job
            WHERE job_name = #{jobName} AND job_group = #{jobGroup}
            LIMIT 1
            """)
    SysJob checkJobUnique(@Param("jobName") String jobName, @Param("jobGroup") String jobGroup);

    @Insert("""
            INSERT INTO sys_job
              (job_name, job_group, invoke_target, cron_expression, misfire_policy,
               concurrent, status, create_by, create_time, remark)
            VALUES
              (#{jobName}, #{jobGroup}, #{invokeTarget}, #{cronExpression}, #{misfirePolicy},
               #{concurrent}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "jobId")
    int insertJob(SysJob job);

    // 全字段覆盖式更新（含 remark）：调用方须传完整对象，未传字段会被写成 NULL。
    @Update("""
            UPDATE sys_job
            SET job_name = #{jobName},
                job_group = #{jobGroup},
                invoke_target = #{invokeTarget},
                cron_expression = #{cronExpression},
                misfire_policy = #{misfirePolicy},
                concurrent = #{concurrent},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE job_id = #{jobId}
            """)
    int updateJob(SysJob job);

    // 列表开关只改 status 与 update_time；不校验取值（0 正常 / 1 暂停），也不触发调度器。
    @Update("UPDATE sys_job SET status = #{status}, update_time = NOW() WHERE job_id = #{jobId}")
    int updateJobStatus(@Param("jobId") Long jobId, @Param("status") String status);

    @Delete("""
            <script>
            DELETE FROM sys_job WHERE job_id IN
            <foreach collection="array" item="jobId" open="(" separator="," close=")">
              #{jobId}
            </foreach>
            </script>
            """)
    int deleteJobByIds(@Param("array") Long[] jobIds);

    // ==================== 调度日志 ====================

    @Select("""
            <script>
            SELECT job_log_id, job_name, job_group, invoke_target, job_message,
                   status, exception_info, start_time, end_time, create_time
            FROM sys_job_log
            <where>
              <if test="jobName != null and jobName != ''">
                AND job_name LIKE CONCAT('%', #{jobName}, '%')
              </if>
              <if test="jobGroup != null and jobGroup != ''">
                AND job_group = #{jobGroup}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
            </where>
            ORDER BY job_log_id DESC
            </script>
            """)
    List<SysJobLog> selectJobLogList(SysJobLog query);

    // start_time / end_time 由调用方传入，create_time 取 DB NOW()。
    @Insert("""
            INSERT INTO sys_job_log
              (job_name, job_group, invoke_target, job_message, status,
               exception_info, start_time, end_time, create_time)
            VALUES
              (#{jobName}, #{jobGroup}, #{invokeTarget}, #{jobMessage}, #{status},
               #{exceptionInfo}, #{startTime}, #{endTime}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "jobLogId")
    int insertJobLog(SysJobLog jobLog);

    @Delete("""
            <script>
            DELETE FROM sys_job_log WHERE job_log_id IN
            <foreach collection="array" item="jobLogId" open="(" separator="," close=")">
              #{jobLogId}
            </foreach>
            </script>
            """)
    int deleteJobLogByIds(@Param("array") Long[] jobLogIds);

    /** 物理清空整张调度日志表，不可恢复 */
    @Delete("DELETE FROM sys_job_log")
    int cleanJobLog();
}
