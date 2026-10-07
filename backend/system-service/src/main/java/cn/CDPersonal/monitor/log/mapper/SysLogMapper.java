package cn.CDPersonal.monitor.log.mapper;

import cn.CDPersonal.common.domain.entity.SysLogininfor;
import cn.CDPersonal.common.domain.entity.SysOperLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysLogMapper {

    // ==================== 操作日志 ====================

    @Select("""
            <script>
            SELECT oper_id, title, business_type, method, request_method, operator_type,
                   oper_name, dept_name, oper_url, oper_ip, oper_location, oper_param,
                   json_result, status, error_msg, oper_time, cost_time
            FROM sys_oper_log
            <where>
              <if test="title != null and title != ''">
                AND title LIKE CONCAT('%', #{title}, '%')
              </if>
              <if test="operName != null and operName != ''">
                AND oper_name LIKE CONCAT('%', #{operName}, '%')
              </if>
              <if test="businessType != null">
                AND business_type = #{businessType}
              </if>
              <if test="status != null">
                AND status = #{status}
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND oper_time &gt;= #{params.beginTime}
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND oper_time &lt;= #{params.endTime}
              </if>
            </where>
            ORDER BY oper_id DESC
            </script>
            """)
    List<SysOperLog> selectOperLogList(SysOperLog query);

    @Delete("""
            <script>
            DELETE FROM sys_oper_log WHERE oper_id IN
            <foreach collection="array" item="operId" open="(" separator="," close=")">
              #{operId}
            </foreach>
            </script>
            """)
    int deleteOperLogByIds(@Param("array") Long[] operIds);

    @Delete("DELETE FROM sys_oper_log")
    int cleanOperLog();

    /** 供切面/业务记录日志用 */
    @Insert("""
            INSERT INTO sys_oper_log
              (title, business_type, method, request_method, operator_type, oper_name,
               dept_name, oper_url, oper_ip, oper_location, oper_param, json_result,
               status, error_msg, oper_time, cost_time)
            VALUES
              (#{title}, #{businessType}, #{method}, #{requestMethod}, #{operatorType}, #{operName},
               #{deptName}, #{operUrl}, #{operIp}, #{operLocation}, #{operParam}, #{jsonResult},
               #{status}, #{errorMsg}, NOW(), #{costTime})
            """)
    int insertOperLog(SysOperLog operLog);

    // ==================== 登录日志 ====================

    @Select("""
            <script>
            SELECT info_id, user_name, ipaddr, status, msg, access_time
            FROM sys_logininfor
            <where>
              <if test="userName != null and userName != ''">
                AND user_name LIKE CONCAT('%', #{userName}, '%')
              </if>
              <if test="ipaddr != null and ipaddr != ''">
                AND ipaddr LIKE CONCAT('%', #{ipaddr}, '%')
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND access_time &gt;= #{params.beginTime}
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND access_time &lt;= #{params.endTime}
              </if>
            </where>
            ORDER BY info_id DESC
            </script>
            """)
    List<SysLogininfor> selectLogininforList(SysLogininfor query);

    @Delete("""
            <script>
            DELETE FROM sys_logininfor WHERE info_id IN
            <foreach collection="array" item="infoId" open="(" separator="," close=")">
              #{infoId}
            </foreach>
            </script>
            """)
    int deleteLogininforByIds(@Param("array") Long[] infoIds);

    @Delete("DELETE FROM sys_logininfor")
    int cleanLogininfor();

    @Insert("""
            INSERT INTO sys_logininfor (user_name, ipaddr, status, msg, access_time)
            VALUES (#{userName}, #{ipaddr}, #{status}, #{msg}, NOW())
            """)
    int insertLogininfor(SysLogininfor logininfor);
}
