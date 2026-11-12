package cn.CDPersonal.monitor.log.mapper;

import cn.CDPersonal.common.domain.entity.SysLogininfor;
import cn.CDPersonal.common.domain.entity.SysOperLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志（sys_oper_log）与登录日志（sys_logininfor）的注解式 Mapper。
 *
 * 约定：
 *   - 两张表都没有逻辑删除列，delete / clean 是物理删除，清空后不可恢复；
 *   - 列表按主键倒序（最新在前），SQL 本身不写 LIMIT，分页由调用方先 PageUtils.startPage()；
 *   - 时间范围取 BaseEntity.params 的 beginTime / endTime（前端 daterange 传入），
 *     以字符串直接与 DATETIME 比较，格式需为 yyyy-MM-dd HH:mm:ss；
 *   - 批量删除用 foreach 拼 IN，传入空数组会生成非法 SQL，调用方需先判空。
 */
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

    /** 物理清空整张操作日志表，不可恢复 */
    @Delete("DELETE FROM sys_oper_log")
    int cleanOperLog();

    /** 供切面/业务记录日志用 */
    // oper_time 由 SQL 里的 NOW() 生成，入参 operTime 不生效；
    // oper_param / json_result 按原样入库，超长内容需调用方自行截断。
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

    // 登录日志的 status 是字符串（'0'=成功 '1'=失败），与操作日志的 Integer status 不同；
    // userName / ipaddr 模糊匹配，status 精确匹配。
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

    /** 物理清空整张登录日志表，不可恢复 */
    @Delete("DELETE FROM sys_logininfor")
    int cleanLogininfor();

    // access_time 取数据库 NOW()，入参 accessTime 不生效。
    @Insert("""
            INSERT INTO sys_logininfor (user_name, ipaddr, status, msg, access_time)
            VALUES (#{userName}, #{ipaddr}, #{status}, #{msg}, NOW())
            """)
    int insertLogininfor(SysLogininfor logininfor);
}
