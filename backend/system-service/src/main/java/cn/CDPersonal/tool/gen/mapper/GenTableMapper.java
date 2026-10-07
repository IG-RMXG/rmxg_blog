package cn.CDPersonal.tool.gen.mapper;

import cn.CDPersonal.tool.gen.domain.GenTable;
import cn.CDPersonal.tool.gen.domain.GenTableColumn;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface GenTableMapper {

    // ==================== gen_table ====================

    @Select("""
            <script>
            SELECT table_id, table_name, table_comment, sub_table_name, sub_table_fk_name,
                   class_name, tpl_category, tpl_web_type, package_name, module_name,
                   business_name, function_name, function_author, form_col_num,
                   gen_type, gen_path, options, create_by, create_time, update_by, update_time, remark
            FROM gen_table
            <where>
              <if test="tableName != null and tableName != ''">
                AND table_name LIKE CONCAT('%', #{tableName}, '%')
              </if>
              <if test="tableComment != null and tableComment != ''">
                AND table_comment LIKE CONCAT('%', #{tableComment}, '%')
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND create_time &gt;= #{params.beginTime}
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND create_time &lt;= #{params.endTime}
              </if>
            </where>
            ORDER BY table_id DESC
            </script>
            """)
    List<GenTable> selectGenTableList(GenTable query);

    @Select("""
            SELECT table_id, table_name, table_comment, sub_table_name, sub_table_fk_name,
                   class_name, tpl_category, tpl_web_type, package_name, module_name,
                   business_name, function_name, function_author, form_col_num,
                   gen_type, gen_path, options, create_by, create_time, update_by, update_time, remark
            FROM gen_table WHERE table_id = #{tableId}
            """)
    GenTable selectGenTableById(@Param("tableId") Long tableId);

    @Select("""
            SELECT table_id, table_name, table_comment, class_name, tpl_category, module_name, business_name
            FROM gen_table WHERE table_name = #{tableName} LIMIT 1
            """)
    GenTable selectGenTableByName(@Param("tableName") String tableName);

    @Insert("""
            INSERT INTO gen_table
              (table_name, table_comment, sub_table_name, sub_table_fk_name, class_name,
               tpl_category, tpl_web_type, package_name, module_name, business_name,
               function_name, function_author, form_col_num, gen_type, gen_path,
               options, create_by, create_time, remark)
            VALUES
              (#{tableName}, #{tableComment}, #{subTableName}, #{subTableFkName}, #{className},
               #{tplCategory}, #{tplWebType}, #{packageName}, #{moduleName}, #{businessName},
               #{functionName}, #{functionAuthor}, #{formColNum}, #{genType}, #{genPath},
               #{options}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "tableId")
    int insertGenTable(GenTable genTable);

    @Update("""
            UPDATE gen_table
            SET table_name = #{tableName},
                table_comment = #{tableComment},
                sub_table_name = #{subTableName},
                sub_table_fk_name = #{subTableFkName},
                class_name = #{className},
                tpl_category = #{tplCategory},
                tpl_web_type = #{tplWebType},
                package_name = #{packageName},
                module_name = #{moduleName},
                business_name = #{businessName},
                function_name = #{functionName},
                function_author = #{functionAuthor},
                form_col_num = #{formColNum},
                gen_type = #{genType},
                gen_path = #{genPath},
                options = #{options},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE table_id = #{tableId}
            """)
    int updateGenTable(GenTable genTable);

    @Delete("""
            <script>
            DELETE FROM gen_table WHERE table_id IN
            <foreach collection="array" item="tableId" open="(" separator="," close=")">
              #{tableId}
            </foreach>
            </script>
            """)
    int deleteGenTableByIds(@Param("array") Long[] tableIds);

    // ==================== gen_table_column ====================

    @Select("""
            SELECT column_id, table_id, column_name, column_comment, column_type,
                   java_type, java_field, is_pk, is_increment, is_required,
                   is_insert, is_edit, is_list, is_query, query_type, html_type,
                   dict_type, sort, create_by, create_time, update_by, update_time
            FROM gen_table_column
            WHERE table_id = #{tableId}
            ORDER BY sort
            """)
    List<GenTableColumn> selectGenTableColumnListByTableId(@Param("tableId") Long tableId);

    @Insert("""
            INSERT INTO gen_table_column
              (table_id, column_name, column_comment, column_type, java_type, java_field,
               is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query,
               query_type, html_type, dict_type, sort, create_by, create_time)
            VALUES
              (#{tableId}, #{columnName}, #{columnComment}, #{columnType}, #{javaType}, #{javaField},
               #{isPk}, #{isIncrement}, #{isRequired}, #{isInsert}, #{isEdit}, #{isList}, #{isQuery},
               #{queryType}, #{htmlType}, #{dictType}, #{sort}, #{createBy}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "columnId")
    int insertGenTableColumn(GenTableColumn column);

    @Delete("DELETE FROM gen_table_column WHERE table_id = #{tableId}")
    int deleteGenTableColumnByTableId(@Param("tableId") Long tableId);

    /**
     * 只更新列的"展示配置"，不动列名/类型/主键等表结构字段。
     * 前端 gen 编辑页只允许改这些配置项。
     */
    @Update("""
            UPDATE gen_table_column
            SET column_comment = #{columnComment},
                java_type = #{javaType},
                java_field = #{javaField},
                is_required = #{isRequired},
                is_insert = #{isInsert},
                is_edit = #{isEdit},
                is_list = #{isList},
                is_query = #{isQuery},
                query_type = #{queryType},
                html_type = #{htmlType},
                dict_type = #{dictType},
                update_by = #{updateBy},
                update_time = NOW()
            WHERE column_id = #{columnId}
            """)
    int updateGenTableColumnConfig(GenTableColumn column);

    @Delete("""
            <script>
            DELETE FROM gen_table_column WHERE table_id IN
            <foreach collection="array" item="tableId" open="(" separator="," close=")">
              #{tableId}
            </foreach>
            </script>
            """)
    int deleteGenTableColumnByTableIds(@Param("array") Long[] tableIds);

    // ==================== 读取 information_schema ====================

    /** 查指定库里的表（"导入表"对话框用，排除已导入的） */
    @Select("""
            <script>
            SELECT table_name AS tableName, table_comment AS tableComment,
                   create_time AS createTime, update_time AS updateTime
            FROM information_schema.tables
            WHERE table_schema = #{schemaName}
              AND table_type = 'BASE TABLE'
              AND table_name NOT IN (SELECT table_name FROM gen_table)
              <if test="tableName != null and tableName != ''">
                AND table_name LIKE CONCAT('%', #{tableName}, '%')
              </if>
              <if test="tableComment != null and tableComment != ''">
                AND table_comment LIKE CONCAT('%', #{tableComment}, '%')
              </if>
            ORDER BY create_time DESC
            </script>
            """)
    List<Map<String, Object>> selectDbTableList(@Param("schemaName") String schemaName,
                                                @Param("tableName") String tableName,
                                                @Param("tableComment") String tableComment);

    /** 按表名列表查库表元数据（导入时用） */
    @Select("""
            <script>
            SELECT table_name AS tableName, table_comment AS tableComment
            FROM information_schema.tables
            WHERE table_schema = #{schemaName}
              AND table_type = 'BASE TABLE'
              AND table_name IN
              <foreach collection="tableNames" item="name" open="(" separator="," close=")">
                #{name}
              </foreach>
            </script>
            """)
    List<Map<String, Object>> selectDbTableListByNames(@Param("schemaName") String schemaName,
                                                       @Param("tableNames") List<String> tableNames);

    /** 查某张表的列元数据 */
    @Select("""
            SELECT column_name AS columnName, column_comment AS columnComment,
                   column_type AS columnType, column_key AS columnKey,
                   extra AS extra, is_nullable AS isNullable, ordinal_position AS ordinalPosition
            FROM information_schema.columns
            WHERE table_schema = #{schemaName} AND table_name = #{tableName}
            ORDER BY ordinal_position
            """)
    List<Map<String, Object>> selectDbTableColumns(@Param("schemaName") String schemaName,
                                                   @Param("tableName") String tableName);
}
