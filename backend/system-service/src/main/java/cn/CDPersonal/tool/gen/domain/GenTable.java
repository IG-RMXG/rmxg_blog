package cn.CDPersonal.tool.gen.domain;

import cn.CDPersonal.common.domain.model.BaseEntity;

import java.util.List;

/**
 * 代码生成业务表 gen_table。
 */
public class GenTable extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long tableId;
    private String tableName;
    private String tableComment;
    /** 关联子表的表名 */
    private String subTableName;
    /** 子表关联的外键名 */
    private String subTableFkName;
    /** 实体类名称 */
    private String className;
    /** 使用的模板（crud 单表 / tree 树表 / sub 主子表） */
    private String tplCategory;
    /** 前端模板类型（element-ui 等） */
    private String tplWebType;
    private String packageName;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String functionAuthor;
    private Integer formColNum;
    /** 生成方式 0zip压缩包 1自定义路径 */
    private String genType;
    private String genPath;
    private String options;

    /** 列信息 */
    private List<GenTableColumn> columns;
    /** 主键列 */
    private GenTableColumn pkColumn;

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public String getSubTableName() {
        return subTableName;
    }

    public void setSubTableName(String subTableName) {
        this.subTableName = subTableName;
    }

    public String getSubTableFkName() {
        return subTableFkName;
    }

    public void setSubTableFkName(String subTableFkName) {
        this.subTableFkName = subTableFkName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTplCategory() {
        return tplCategory;
    }

    public void setTplCategory(String tplCategory) {
        this.tplCategory = tplCategory;
    }

    public String getTplWebType() {
        return tplWebType;
    }

    public void setTplWebType(String tplWebType) {
        this.tplWebType = tplWebType;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionAuthor() {
        return functionAuthor;
    }

    public void setFunctionAuthor(String functionAuthor) {
        this.functionAuthor = functionAuthor;
    }

    public Integer getFormColNum() {
        return formColNum;
    }

    public void setFormColNum(Integer formColNum) {
        this.formColNum = formColNum;
    }

    public String getGenType() {
        return genType;
    }

    public void setGenType(String genType) {
        this.genType = genType;
    }

    public String getGenPath() {
        return genPath;
    }

    public void setGenPath(String genPath) {
        this.genPath = genPath;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public List<GenTableColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<GenTableColumn> columns) {
        this.columns = columns;
    }

    public GenTableColumn getPkColumn() {
        return pkColumn;
    }

    public void setPkColumn(GenTableColumn pkColumn) {
        this.pkColumn = pkColumn;
    }
}
