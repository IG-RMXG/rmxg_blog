package cn.CDPersonal.tool.gen.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 代码生成业务表字段 gen_table_column。
 *
 * 一行对应所属表的一个物理列，是"表结构元数据 + 前端展示配置"的混合体：
 *   - 结构类（columnName/columnType/isPk/isIncrement）来自 information_schema，编辑页不可改，
 *     同步数据库时会整体删除重建；
 *   - 展示类（javaType/javaField/isInsert/isEdit/isList/isQuery/queryType/htmlType/dictType）
 *     决定生成代码与表单控件，可通过 updateGenTableColumnConfig 单独修改。
 *
 * 布尔语义统一用字符串 "1"/"0"（与前端 gen 页提交格式一致），不是 boolean。
 */
public class GenTableColumn implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long columnId;
    private Long tableId;
    private String columnName;
    private String columnComment;
    private String columnType;
    private String javaType;
    private String javaField;
    /** 是否主键 1是 */
    private String isPk;
    /** 是否自增 1是 */
    private String isIncrement;
    /** 是否必填 1是 */
    private String isRequired;
    /** 是否为插入字段 1是 */
    private String isInsert;
    /** 是否编辑字段 1是 */
    private String isEdit;
    /** 是否列表字段 1是 */
    private String isList;
    /** 是否查询字段 1是 */
    private String isQuery;
    /** 查询方式（EQ等于 / NE不等于 / GT大于 / LIKE模糊） */
    private String queryType;
    /** 显示类型（input / textarea / select / radio / checkbox / datetime / imageUpload） */
    private String htmlType;
    /** 字典类型，htmlType 为 select/radio 时前端据此取字典项，可为空 */
    private String dictType;
    /** 展示顺序：导入时按物理列序（ordinal_position）递增，列表查询按它升序 */
    private Integer sort;

    // 审计字段：create_time 由插入 SQL 的 NOW() 生成，update_time 在更新展示配置时写 NOW()。
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getColumnId() {
        return columnId;
    }

    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getJavaField() {
        return javaField;
    }

    public void setJavaField(String javaField) {
        this.javaField = javaField;
    }

    public String getIsPk() {
        return isPk;
    }

    public void setIsPk(String isPk) {
        this.isPk = isPk;
    }

    public String getIsIncrement() {
        return isIncrement;
    }

    public void setIsIncrement(String isIncrement) {
        this.isIncrement = isIncrement;
    }

    public String getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(String isRequired) {
        this.isRequired = isRequired;
    }

    public String getIsInsert() {
        return isInsert;
    }

    public void setIsInsert(String isInsert) {
        this.isInsert = isInsert;
    }

    public String getIsEdit() {
        return isEdit;
    }

    public void setIsEdit(String isEdit) {
        this.isEdit = isEdit;
    }

    public String getIsList() {
        return isList;
    }

    public void setIsList(String isList) {
        this.isList = isList;
    }

    public String getIsQuery() {
        return isQuery;
    }

    public void setIsQuery(String isQuery) {
        this.isQuery = isQuery;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getHtmlType() {
        return htmlType;
    }

    public void setHtmlType(String htmlType) {
        this.htmlType = htmlType;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
