package cn.CDPersonal.blog.support;

import java.io.Serializable;
import java.util.List;

// 与 ResponseVO 是两套响应结构：列表接口直接返回本类（前端读 res.rows / res.total），
// 其余接口返回 ResponseVO（数据在 res.data）；本类的 code 由 PageUtils.getDataTable 固定写 200。
/**
 * 表格分页数据对象，前端统一读 { total, rows, code, msg }。
 */
public class TableDataInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 列表数据 */
    private List<?> rows;

    /** 消息状态码 */
    private int code;

    /** 消息内容 */
    private String msg;

    /** 供 Jackson 反序列化与 PageUtils 逐个 set 字段使用 */
    public TableDataInfo() {
    }

    /** 便捷构造：rows 为当前页数据，total 是过滤后的总记录数（不是当前页条数） */
    public TableDataInfo(List<?> list, long total) {
        this.rows = list;
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<?> getRows() {
        return rows;
    }

    public void setRows(List<?> rows) {
        this.rows = rows;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
