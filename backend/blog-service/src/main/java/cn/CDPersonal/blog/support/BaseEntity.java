package cn.CDPersonal.blog.support;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// blog_* 各实体都继承本类；公共列由各 Mapper 的 SQL 显式处理（create_time/update_time 用 NOW()），
// 没有注册 MyBatis 自动填充处理器，别指望框架自动补。
/**
 * 博客服务自带的实体基类（create_by / create_time / update_by / update_time / remark 等公共列）。
 *
 * 为什么不复用 system-service 里的同名类：本服务要能整体拷走单独部署，
 * 因此把这类基础支撑代码留在自己模块内，只对外依赖 cn.CDPersonal:core 的响应体。
 */
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 搜索值 */
    @JsonIgnore
    private String searchValue;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 请求参数（列表查询的时间范围等走 params.beginTime / params.endTime） */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> params;

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 懒初始化：返回前把 params 补成空 Map，调用方可以直接 put 而不用判空；
     * 空 Map 因字段上的 @JsonInclude(NON_EMPTY) 不会出现在 JSON 响应里。
     */
    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
