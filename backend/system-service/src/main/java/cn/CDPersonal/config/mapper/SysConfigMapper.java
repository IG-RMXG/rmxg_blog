package cn.CDPersonal.config.mapper;

import cn.CDPersonal.common.domain.entity.SysConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 参数配置 Mapper（sys_config）。
 *
 * 库里没有 status 列，所以查询条件只有 configName / configKey / configType / 时间范围。
 */
@Mapper
public interface SysConfigMapper {

    @Select("""
            <script>
            SELECT config_id, config_name, config_key, config_value, config_type,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_config
            <where>
              <if test="configName != null and configName != ''">
                AND config_name LIKE CONCAT('%', #{configName}, '%')
              </if>
              <if test="configKey != null and configKey != ''">
                AND config_key LIKE CONCAT('%', #{configKey}, '%')
              </if>
              <if test="configValue != null and configValue != ''">
                AND config_value = #{configValue}
              </if>
              <if test="configType != null and configType != ''">
                AND config_type = #{configType}
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND DATE(create_time) &gt;= DATE(#{params.beginTime})
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND DATE(create_time) &lt;= DATE(#{params.endTime})
              </if>
            </where>
            ORDER BY config_id DESC
            </script>
            """)
    List<SysConfig> selectConfigList(SysConfig query);

    @Select("""
            SELECT config_id, config_name, config_key, config_value, config_type,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_config
            WHERE config_id = #{configId}
            """)
    SysConfig selectConfigById(@Param("configId") Long configId);

    /** 校验参数键名唯一 */
    @Select("""
            SELECT config_id, config_name, config_key, config_value, config_type
            FROM sys_config
            WHERE config_key = #{configKey}
            LIMIT 1
            """)
    SysConfig checkConfigKeyUnique(@Param("configKey") String configKey);

    @Insert("""
            INSERT INTO sys_config
              (config_name, config_key, config_value, config_type, create_by, create_time, remark)
            VALUES
              (#{configName}, #{configKey}, #{configValue}, #{configType}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "configId")
    int insertConfig(SysConfig config);

    @Update("""
            UPDATE sys_config
            SET config_name = #{configName},
                config_key = #{configKey},
                config_value = #{configValue},
                config_type = #{configType},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE config_id = #{configId}
            """)
    int updateConfig(SysConfig config);

    @Delete("""
            <script>
            DELETE FROM sys_config WHERE config_id IN
            <foreach collection="array" item="configId" open="(" separator="," close=")">
              #{configId}
            </foreach>
            </script>
            """)
    int deleteConfigByIds(@Param("array") Long[] configIds);
}
