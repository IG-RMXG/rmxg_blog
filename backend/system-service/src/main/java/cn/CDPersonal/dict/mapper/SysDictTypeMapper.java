package cn.CDPersonal.dict.mapper;

import cn.CDPersonal.common.domain.entity.SysDictType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 字典类型 Mapper（sys_dict_type）。
 *
 * 表没有逻辑删除列，deleteDictTypeByIds 是物理删除；调用方需先确认该类型
 * 是否还被 sys_dict_data 引用，否则会留下挂在不存在类型上的字典项。
 * 列表按 dict_id 倒序（新建的在前），分页由 controller 在查询前调用 startPage() 开启。
 * dict_type 在库上有唯一索引，checkDictTypeUnique 只能挡住常规重复，并发插入仍可能撞索引。
 */
@Mapper
public interface SysDictTypeMapper {

    @Select("""
            <script>
            SELECT dict_id, dict_name, dict_type, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_dict_type
            <where>
              <if test="dictName != null and dictName != ''">
                AND dict_name LIKE CONCAT('%', #{dictName}, '%')
              </if>
              <if test="dictType != null and dictType != ''">
                AND dict_type LIKE CONCAT('%', #{dictType}, '%')
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND create_time &gt;= #{params.beginTime}
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND create_time &lt;= #{params.endTime}
              </if>
            </where>
            ORDER BY dict_id DESC
            </script>
            """)
    List<SysDictType> selectDictTypeList(SysDictType query);

    @Select("""
            SELECT dict_id, dict_name, dict_type, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_dict_type
            WHERE dict_id = #{dictId}
            """)
    SysDictType selectDictTypeById(@Param("dictId") Long dictId);

    @Select("""
            SELECT dict_id, dict_name, dict_type, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_dict_type
            ORDER BY dict_id DESC
            """)
    List<SysDictType> selectDictTypeAll();

    /** 校验字典类型唯一 */
    @Select("""
            SELECT dict_id, dict_name, dict_type, status
            FROM sys_dict_type
            WHERE dict_type = #{dictType}
            LIMIT 1
            """)
    SysDictType checkDictTypeUnique(@Param("dictType") String dictType);

    @Insert("""
            INSERT INTO sys_dict_type
              (dict_name, dict_type, status, create_by, create_time, remark)
            VALUES
              (#{dictName}, #{dictType}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "dictId")
    int insertDictType(SysDictType dictType);

    @Update("""
            UPDATE sys_dict_type
            SET dict_name = #{dictName},
                dict_type = #{dictType},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE dict_id = #{dictId}
            """)
    int updateDictType(SysDictType dictType);

    @Delete("DELETE FROM sys_dict_type WHERE dict_id = #{dictId}")
    int deleteDictTypeById(@Param("dictId") Long dictId);

    @Delete("""
            <script>
            DELETE FROM sys_dict_type WHERE dict_id IN
            <foreach collection="array" item="dictId" open="(" separator="," close=")">
              #{dictId}
            </foreach>
            </script>
            """)
    int deleteDictTypeByIds(@Param("array") Long[] dictIds);

    /** 字典类型被修改时，同步更新字典数据里冗余的 dict_type */
    @Update("""
            UPDATE sys_dict_data SET dict_type = #{newDictType} WHERE dict_type = #{oldDictType}
            """)
    int updateDictDataType(@Param("oldDictType") String oldDictType,
                           @Param("newDictType") String newDictType);
}
