package cn.CDPersonal.dict.mapper;

import cn.CDPersonal.common.domain.entity.SysDictData;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysDictDataMapper {

    @Select("""
            <script>
            SELECT dict_code, dict_sort, dict_label, dict_value, dict_type,
                   css_class, list_class, is_default, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_dict_data
            <where>
              <if test="dictType != null and dictType != ''">
                AND dict_type = #{dictType}
              </if>
              <if test="dictLabel != null and dictLabel != ''">
                AND dict_label LIKE CONCAT('%', #{dictLabel}, '%')
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
            </where>
            ORDER BY dict_sort ASC
            </script>
            """)
    List<SysDictData> selectDictDataList(SysDictData query);

    @Select("""
            SELECT dict_code, dict_sort, dict_label, dict_value, dict_type,
                   css_class, list_class, is_default, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_dict_data
            WHERE dict_code = #{dictCode}
            """)
    SysDictData selectDictDataById(@Param("dictCode") Long dictCode);

    /** 根据字典类型查可用字典数据，前端 getDicts 用 */
    @Select("""
            SELECT dict_code, dict_sort, dict_label, dict_value, dict_type,
                   css_class, list_class, is_default, status, remark
            FROM sys_dict_data
            WHERE dict_type = #{dictType} AND status = '0'
            ORDER BY dict_sort ASC
            """)
    List<SysDictData> selectDictDataByType(@Param("dictType") String dictType);

    @Select("SELECT COUNT(*) FROM sys_dict_data WHERE dict_type = #{dictType}")
    int countDictDataByType(@Param("dictType") String dictType);

    @Insert("""
            INSERT INTO sys_dict_data
              (dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
               is_default, status, create_by, create_time, remark)
            VALUES
              (#{dictSort}, #{dictLabel}, #{dictValue}, #{dictType}, #{cssClass}, #{listClass},
               #{isDefault}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "dictCode")
    int insertDictData(SysDictData dictData);

    @Update("""
            UPDATE sys_dict_data
            SET dict_sort = #{dictSort},
                dict_label = #{dictLabel},
                dict_value = #{dictValue},
                dict_type = #{dictType},
                css_class = #{cssClass},
                list_class = #{listClass},
                is_default = #{isDefault},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE dict_code = #{dictCode}
            """)
    int updateDictData(SysDictData dictData);

    @Delete("DELETE FROM sys_dict_data WHERE dict_code = #{dictCode}")
    int deleteDictDataById(@Param("dictCode") Long dictCode);

    @Delete("""
            <script>
            DELETE FROM sys_dict_data WHERE dict_code IN
            <foreach collection="array" item="dictCode" open="(" separator="," close=")">
              #{dictCode}
            </foreach>
            </script>
            """)
    int deleteDictDataByIds(@Param("array") Long[] dictCodes);
}
