package cn.CDPersonal.tool.gen.service;

import cn.CDPersonal.tool.gen.domain.GenTable;
import cn.CDPersonal.tool.gen.domain.GenTableColumn;
import cn.CDPersonal.tool.gen.mapper.GenTableMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成（数据库侧）。
 *
 * 能力边界（重要）：
 *   - 表导入、列表、详情、修改、删除、同步：**完整实现**，界面可正常用。
 *   - 代码预览：**真实现**，直接根据表结构拼出 Java/XML/SQL 代码文本，
 *     不依赖 RuoYi 的 Velocity 模板文件（本仓库里没有 vm/ 模板目录）。
 *   - 生成到自定义路径（genType=1）：未实现，明确返回失败，避免假装成功。
 *     如需真正落盘生成，需要补模板引擎与模板文件。
 */
@Service
public class GenService {

    private final GenTableMapper genTableMapper;

    public GenService(GenTableMapper genTableMapper) {
        this.genTableMapper = genTableMapper;
    }

    /** 从数据源 URL 里取库名，用于查 information_schema */
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    private String schemaName() {
        // jdbc:mysql://localhost:3306/ry-cloud?...
        String url = datasourceUrl;
        int slash = url.indexOf("//");
        if (slash < 0) {
            return "ry-cloud";
        }
        int start = url.indexOf('/', slash + 2);
        if (start < 0) {
            return "ry-cloud";
        }
        int end = url.indexOf('?', start);
        String name = end < 0 ? url.substring(start + 1) : url.substring(start + 1, end);
        return name.isBlank() ? "ry-cloud" : name;
    }

    // ==================== 列表 / 详情 ====================

    public List<GenTable> selectGenTableList(GenTable query) {
        return genTableMapper.selectGenTableList(query);
    }

    public List<Map<String, Object>> selectDbTableList(String tableName, String tableComment) {
        return genTableMapper.selectDbTableList(schemaName(), tableName, tableComment);
    }

    public GenTable selectGenTableById(Long tableId) {
        GenTable table = genTableMapper.selectGenTableById(tableId);
        if (table != null) {
            List<GenTableColumn> columns = genTableMapper.selectGenTableColumnListByTableId(tableId);
            table.setColumns(columns);
            table.setPkColumn(columns.stream()
                    .filter(c -> "1".equals(c.getIsPk()))
                    .findFirst()
                    .orElse(columns.isEmpty() ? null : columns.get(0)));
        }
        return table;
    }

    // ==================== 导入 / 修改 / 删除 / 同步 ====================

    /** 从数据库导入表结构 */
    @Transactional
    public int importGenTable(List<String> tableNames, String operName) {
        String schema = schemaName();
        List<Map<String, Object>> dbTables = genTableMapper.selectDbTableListByNames(schema, tableNames);
        int count = 0;
        for (Map<String, Object> dbTable : dbTables) {
            String tableName = str(dbTable.get("tableName"));
            // 已导入过就跳过，避免重复
            if (genTableMapper.selectGenTableByName(tableName) != null) {
                continue;
            }
            GenTable table = buildGenTable(schema, tableName, str(dbTable.get("tableComment")), operName);
            genTableMapper.insertGenTable(table);
            for (GenTableColumn column : buildColumns(schema, tableName, table.getTableId(), operName)) {
                genTableMapper.insertGenTableColumn(column);
            }
            count++;
        }
        return count;
    }

    @Transactional
    public int updateGenTable(GenTable genTable) {
        genTableMapper.updateGenTable(genTable);
        if (genTable.getColumns() != null) {
            for (GenTableColumn column : genTable.getColumns()) {
                if (column.getColumnId() != null) {
                    // 列信息只更新可编辑的展示配置，保持表结构字段不被前端覆盖
                    genTableMapper.updateGenTableColumnConfig(column);
                }
            }
        }
        return 1;
    }

    @Transactional
    public int deleteGenTableByIds(Long[] tableIds) {
        genTableMapper.deleteGenTableColumnByTableIds(tableIds);
        return genTableMapper.deleteGenTableByIds(tableIds);
    }

    /** 同步数据库：按当前库表结构重建列信息 */
    @Transactional
    public void synchDb(String tableName) {
        GenTable table = genTableMapper.selectGenTableByName(tableName);
        if (table == null) {
            return;
        }
        genTableMapper.deleteGenTableColumnByTableId(table.getTableId());
        for (GenTableColumn column : buildColumns(schemaName(), tableName, table.getTableId(), table.getCreateBy())) {
            genTableMapper.insertGenTableColumn(column);
        }
    }

    // ==================== 代码预览 ====================

    /**
     * 生成预览文本。键名形如 vm/java/domain.java.vm，
     * 前端取最后一个 '/' 之后、'.vm' 之前作为页签名（domain.java）。
     */
    public Map<String, String> previewCode(Long tableId) {
        GenTable table = selectGenTableById(tableId);
        Map<String, String> result = new LinkedHashMap<>();
        if (table == null) {
            return result;
        }
        String className = table.getClassName() == null ? "Unknown" : table.getClassName();
        String packageName = table.getPackageName() == null ? "cn.CDPersonal" : table.getPackageName();
        String moduleName = table.getModuleName() == null ? "system" : table.getModuleName();
        String businessName = table.getBusinessName() == null ? "business" : table.getBusinessName();
        List<GenTableColumn> columns = table.getColumns() == null ? List.of() : table.getColumns();

        result.put("vm/java/domain.java.vm", genDomainJava(packageName, className, table, columns));
        result.put("vm/java/mapper.java.vm", genMapperJava(packageName, moduleName, className));
        result.put("vm/xml/mapper.xml.vm", genMapperXml(packageName, className, table, columns));
        result.put("vm/java/service.java.vm", genServiceJava(packageName, moduleName, className, businessName));
        result.put("vm/java/serviceImpl.java.vm", genServiceImplJava(packageName, moduleName, className, businessName));
        result.put("vm/java/controller.java.vm", genControllerJava(packageName, moduleName, className, businessName, table));
        result.put("vm/sql/sql.sql.vm", genSql(table, columns));
        result.put("vm/js/api.js.vm", genApiJs(businessName, className));
        return result;
    }

    // ==================== 内部：元数据构建 ====================

    private GenTable buildGenTable(String schema, String tableName, String tableComment, String operName) {
        GenTable table = new GenTable();
        table.setTableName(tableName);
        table.setTableComment(tableComment);
        table.setClassName(toPascalCase(stripPrefix(tableName)));
        table.setTplCategory("crud");
        table.setTplWebType("element-ui");
        table.setPackageName("cn.CDPersonal");
        table.setModuleName(guessModule(tableName));
        table.setBusinessName(toCamelCase(stripPrefix(tableName)));
        table.setFunctionName(tableComment == null || tableComment.isBlank() ? tableName : tableComment);
        table.setFunctionAuthor("rmxg");
        table.setGenType("0");
        table.setCreateBy(operName);
        return table;
    }

    private List<GenTableColumn> buildColumns(String schema, String tableName, Long tableId, String operName) {
        List<Map<String, Object>> dbColumns = genTableMapper.selectDbTableColumns(schema, tableName);
        List<GenTableColumn> columns = new ArrayList<>();
        int sort = 0;
        for (Map<String, Object> dbColumn : dbColumns) {
            String columnName = str(dbColumn.get("columnName"));
            String columnType = str(dbColumn.get("columnType"));
            String javaType = toJavaType(columnType);
            boolean isPk = "PRI".equalsIgnoreCase(str(dbColumn.get("columnKey")));
            boolean isIncrement = str(dbColumn.get("extra")).toLowerCase().contains("auto_increment");
            boolean notNull = "NO".equalsIgnoreCase(str(dbColumn.get("isNullable")));

            GenTableColumn column = new GenTableColumn();
            column.setTableId(tableId);
            column.setColumnName(columnName);
            column.setColumnComment(str(dbColumn.get("columnComment")));
            column.setColumnType(columnType);
            column.setJavaType(javaType);
            column.setJavaField(toCamelCase(columnName));
            column.setIsPk(isPk ? "1" : "0");
            column.setIsIncrement(isIncrement ? "1" : "0");
            column.setIsRequired(notNull ? "1" : "0");
            column.setIsInsert(isPk && isIncrement ? "0" : "1");
            column.setIsEdit(isPk && isIncrement ? "0" : "1");
            column.setIsList("1");
            column.setIsQuery("0");
            column.setQueryType("EQ");
            column.setHtmlType(toHtmlType(javaType, columnName));
            column.setSort(sort++);
            column.setCreateBy(operName);
            columns.add(column);
        }
        return columns;
    }

    private String guessModule(String tableName) {
        int idx = tableName.indexOf('_');
        return idx > 0 ? tableName.substring(0, idx) : "system";
    }

    /** 去掉常见业务前缀，如 sys_user -> user */
    private String stripPrefix(String tableName) {
        for (String prefix : List.of("sys_", "gen_", "biz_", "t_")) {
            if (tableName.startsWith(prefix)) {
                return tableName.substring(prefix.length());
            }
        }
        return tableName;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public static String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = false;
        }
        return sb.toString();
    }

    public static String toPascalCase(String name) {
        String camel = toCamelCase(name);
        return camel == null || camel.isEmpty()
                ? camel
                : Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    private String toJavaType(String columnType) {
        String t = columnType.toLowerCase();
        if (t.startsWith("bigint") || t.startsWith("int") || t.startsWith("smallint")
                || t.startsWith("tinyint") || t.startsWith("mediumint")) {
            return "Long";
        }
        if (t.startsWith("decimal") || t.startsWith("numeric") || t.startsWith("double")
                || t.startsWith("float")) {
            return "BigDecimal";
        }
        if (t.startsWith("date") || t.startsWith("timestamp") || t.startsWith("datetime")) {
            return "Date";
        }
        if (t.startsWith("time")) {
            return "Date";
        }
        if (t.startsWith("bit") || t.startsWith("bool")) {
            return "Boolean";
        }
        return "String";
    }

    private String toHtmlType(String javaType, String columnName) {
        if ("Date".equals(javaType)) {
            return "datetime";
        }
        if ("BigDecimal".equals(javaType) || "Long".equals(javaType)) {
            return "input";
        }
        String lower = columnName.toLowerCase();
        if (lower.endsWith("status") || lower.endsWith("type") || lower.endsWith("sex")) {
            return "select";
        }
        if (lower.contains("content") || lower.contains("remark") || lower.contains("description")) {
            return "textarea";
        }
        return "input";
    }

    // ==================== 内部：代码文本生成 ====================

    private String genDomainJava(String pkg, String className, GenTable table, List<GenTableColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".domain;\n\n");
        boolean hasDate = columns.stream().anyMatch(c -> "Date".equals(c.getJavaType()));
        boolean hasBigDecimal = columns.stream().anyMatch(c -> "BigDecimal".equals(c.getJavaType()));
        if (hasDate) {
            sb.append("import java.util.Date;\n");
        }
        if (hasBigDecimal) {
            sb.append("import java.math.BigDecimal;\n");
        }
        sb.append("\n/**\n * ").append(nvl(table.getFunctionName())).append(" 对象 ")
          .append(table.getTableName()).append("\n */\n");
        sb.append("public class ").append(className).append(" {\n\n");
        for (GenTableColumn c : columns) {
            if (c.getColumnComment() != null && !c.getColumnComment().isBlank()) {
                sb.append("    /** ").append(c.getColumnComment()).append(" */\n");
            }
            sb.append("    private ").append(c.getJavaType()).append(' ').append(c.getJavaField()).append(";\n\n");
        }
        for (GenTableColumn c : columns) {
            String type = c.getJavaType();
            String field = c.getJavaField();
            String cap = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            sb.append("    public ").append(type).append(" get").append(cap).append("() {\n")
              .append("        return ").append(field).append(";\n    }\n\n");
            sb.append("    public void set").append(cap).append('(').append(type).append(' ').append(field).append(") {\n")
              .append("        this.").append(field).append(" = ").append(field).append(";\n    }\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String genMapperJava(String pkg, String module, String className) {
        return "package " + pkg + "." + module + ".mapper;\n\n"
                + "import " + pkg + ".domain." + className + ";\n"
                + "import org.apache.ibatis.annotations.Mapper;\n\n"
                + "import java.util.List;\n\n"
                + "@Mapper\npublic interface " + className + "Mapper {\n\n"
                + "    List<" + className + "> select" + className + "List(" + className + " query);\n\n"
                + "    " + className + " select" + className + "ById(Long id);\n\n"
                + "    int insert" + className + "(" + className + " entity);\n\n"
                + "    int update" + className + "(" + className + " entity);\n\n"
                + "    int delete" + className + "ByIds(Long[] ids);\n}\n";
    }

    private String genMapperXml(String pkg, String className, GenTable table, List<GenTableColumn> columns) {
        GenTableColumn pk = table.getPkColumn();
        String pkName = pk == null ? "id" : pk.getColumnName();
        String pkField = pk == null ? "id" : pk.getJavaField();
        String tbl = table.getTableName();
        String resultMapId = className + "Result";
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        sb.append("<mapper namespace=\"").append(pkg).append(".mapper.").append(className).append("Mapper\">\n\n");
        sb.append("    <resultMap type=\"").append(pkg).append(".domain.").append(className)
          .append("\" id=\"").append(resultMapId).append("\">\n");
        for (GenTableColumn c : columns) {
            sb.append("        <result property=\"").append(c.getJavaField())
              .append("\" column=\"").append(c.getColumnName()).append("\"/>\n");
        }
        sb.append("    </resultMap>\n\n");
        sb.append("    <sql id=\"select").append(className).append("Vo\">\n        ");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(columns.get(i).getColumnName());
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("\n    </sql>\n\n");
        sb.append("    <select id=\"select").append(className).append("List\" resultMap=\"").append(resultMapId).append("\">\n");
        sb.append("        select <include refid=\"select").append(className).append("Vo\"/> from ").append(tbl).append("\n");
        sb.append("    </select>\n\n");
        sb.append("    <select id=\"select").append(className).append("ById\" resultMap=\"").append(resultMapId).append("\">\n");
        sb.append("        select <include refid=\"select").append(className).append("Vo\"/> from ").append(tbl)
          .append(" where ").append(pkName).append(" = #{").append(pkField).append("}\n");
        sb.append("    </select>\n\n");

        List<GenTableColumn> inserts = columns.stream().filter(c -> "1".equals(c.getIsInsert())).toList();
        sb.append("    <insert id=\"insert").append(className).append("\" useGeneratedKeys=\"true\" keyProperty=\"")
          .append(pkField).append("\">\n");
        sb.append("        insert into ").append(tbl).append(" (\n            ");
        for (int i = 0; i < inserts.size(); i++) {
            sb.append(inserts.get(i).getColumnName());
            if (i < inserts.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("\n        ) values (\n            ");
        for (int i = 0; i < inserts.size(); i++) {
            sb.append("#{").append(inserts.get(i).getJavaField()).append("}");
            if (i < inserts.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("\n        )\n    </insert>\n\n");

        List<GenTableColumn> edits = columns.stream().filter(c -> "1".equals(c.getIsEdit())).toList();
        sb.append("    <update id=\"update").append(className).append("\">\n        update ").append(tbl).append("\n        <set>\n");
        for (GenTableColumn c : edits) {
            sb.append("            <if test=\"").append(c.getJavaField()).append(" != null\">")
              .append(c.getColumnName()).append(" = #{").append(c.getJavaField()).append("},</if>\n");
        }
        sb.append("        </set>\n        where ").append(pkName).append(" = #{").append(pkField).append("}\n    </update>\n\n");

        sb.append("    <delete id=\"delete").append(className).append("ByIds\">\n");
        sb.append("        delete from ").append(tbl).append(" where ").append(pkName).append(" in\n");
        sb.append("        <foreach collection=\"array\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach>\n");
        sb.append("    </delete>\n\n</mapper>\n");
        return sb.toString();
    }

    private String genServiceJava(String pkg, String module, String className, String business) {
        return "package " + pkg + "." + module + ".service;\n\n"
                + "import " + pkg + ".domain." + className + ";\n\n"
                + "import java.util.List;\n\n"
                + "public interface I" + className + "Service {\n\n"
                + "    List<" + className + "> select" + className + "List(" + className + " query);\n\n"
                + "    " + className + " select" + className + "ById(Long id);\n\n"
                + "    int insert" + className + "(" + className + " entity);\n\n"
                + "    int update" + className + "(" + className + " entity);\n\n"
                + "    int delete" + className + "ByIds(Long[] ids);\n}\n";
    }

    private String genServiceImplJava(String pkg, String module, String className, String business) {
        return "package " + pkg + "." + module + ".service.impl;\n\n"
                + "import " + pkg + ".domain." + className + ";\n"
                + "import " + pkg + "." + module + ".mapper." + className + "Mapper;\n"
                + "import " + pkg + "." + module + ".service.I" + className + "Service;\n"
                + "import org.springframework.stereotype.Service;\n\n"
                + "import java.util.List;\n\n"
                + "@Service\npublic class " + className + "ServiceImpl implements I" + className + "Service {\n\n"
                + "    private final " + className + "Mapper " + toCamelCase(className) + "Mapper;\n\n"
                + "    public " + className + "ServiceImpl(" + className + "Mapper " + toCamelCase(className) + "Mapper) {\n"
                + "        this." + toCamelCase(className) + "Mapper = " + toCamelCase(className) + "Mapper;\n    }\n\n"
                + "    @Override\n    public List<" + className + "> select" + className + "List(" + className + " query) {\n"
                + "        return " + toCamelCase(className) + "Mapper.select" + className + "List(query);\n    }\n\n"
                + "    @Override\n    public " + className + " select" + className + "ById(Long id) {\n"
                + "        return " + toCamelCase(className) + "Mapper.select" + className + "ById(id);\n    }\n\n"
                + "    @Override\n    public int insert" + className + "(" + className + " entity) {\n"
                + "        return " + toCamelCase(className) + "Mapper.insert" + className + "(entity);\n    }\n\n"
                + "    @Override\n    public int update" + className + "(" + className + " entity) {\n"
                + "        return " + toCamelCase(className) + "Mapper.update" + className + "(entity);\n    }\n\n"
                + "    @Override\n    public int delete" + className + "ByIds(Long[] ids) {\n"
                + "        return " + toCamelCase(className) + "Mapper.delete" + className + "ByIds(ids);\n    }\n}\n";
    }

    private String genControllerJava(String pkg, String module, String className, String business, GenTable table) {
        String var = toCamelCase(className);
        return "package " + pkg + "." + module + ".controller;\n\n"
                + "import " + pkg + ".domain." + className + ";\n"
                + "import " + pkg + "." + module + ".service.I" + className + "Service;\n"
                + "import org.springframework.web.bind.annotation.*;\n\n"
                + "import java.util.List;\n\n"
                + "@RestController\n@RequestMapping(\"/" + module + "/" + business + "\")\n"
                + "public class " + className + "Controller {\n\n"
                + "    private final I" + className + "Service " + var + "Service;\n\n"
                + "    public " + className + "Controller(I" + className + "Service " + var + "Service) {\n"
                + "        this." + var + "Service = " + var + "Service;\n    }\n\n"
                + "    @GetMapping(\"/list\")\n    public List<" + className + "> list(" + className + " query) {\n"
                + "        return " + var + "Service.select" + className + "List(query);\n    }\n\n"
                + "    @GetMapping(\"/{id}\")\n    public " + className + " getInfo(@PathVariable Long id) {\n"
                + "        return " + var + "Service.select" + className + "ById(id);\n    }\n\n"
                + "    @PostMapping\n    public int add(@RequestBody " + className + " entity) {\n"
                + "        return " + var + "Service.insert" + className + "(entity);\n    }\n\n"
                + "    @PutMapping\n    public int edit(@RequestBody " + className + " entity) {\n"
                + "        return " + var + "Service.update" + className + "(entity);\n    }\n\n"
                + "    @DeleteMapping(\"/{ids}\")\n    public int remove(@PathVariable Long[] ids) {\n"
                + "        return " + var + "Service.delete" + className + "ByIds(ids);\n    }\n}\n";
    }

    private String genSql(GenTable table, List<GenTableColumn> columns) {
        GenTableColumn pk = table.getPkColumn();
        StringBuilder sb = new StringBuilder();
        sb.append("-- 菜单 SQL\n");
        sb.append("-- 主子菜单需按实际 parent_id 调整\n");
        sb.append("insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)\n");
        sb.append("values ('").append(nvl(table.getFunctionName())).append("管理', 3, 1, '")
          .append(table.getBusinessName()).append("', '").append(table.getModuleName()).append("/")
          .append(table.getBusinessName()).append("/index', 1, 0, 'C', '0', '0', '")
          .append(table.getModuleName()).append(":").append(table.getBusinessName()).append(":list', '#', 'admin', sysdate());\n\n");
        sb.append("-- 按钮 SQL 省略，可用 sys_menu 对照生成\n");
        if (pk != null) {
            sb.append("\n-- 主键：").append(pk.getColumnName()).append('\n');
        }
        return sb.toString();
    }

    private String genApiJs(String business, String className) {
        String var = toCamelCase(className);
        return "import request from '@/utils/request'\n\n"
                + "// 查询" + business + "列表\nexport function list" + className + "(query) {\n"
                + "  return request({ url: '/" + business + "/list', method: 'get', params: query })\n}\n\n"
                + "// 查询" + business + "详细\nexport function get" + className + "(id) {\n"
                + "  return request({ url: '/" + business + "/' + id, method: 'get' })\n}\n\n"
                + "// 新增" + business + "\nexport function add" + className + "(data) {\n"
                + "  return request({ url: '/" + business + "', method: 'post', data })\n}\n\n"
                + "// 修改" + business + "\nexport function update" + className + "(data) {\n"
                + "  return request({ url: '/" + business + "', method: 'put', data })\n}\n\n"
                + "// 删除" + business + "\nexport function del" + className + "(id) {\n"
                + "  return request({ url: '/" + business + "/' + id, method: 'delete' })\n}\n";
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
