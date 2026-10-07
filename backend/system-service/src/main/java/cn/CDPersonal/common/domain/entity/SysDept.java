package cn.CDPersonal.common.domain.entity;

import cn.CDPersonal.annotation.Excel;
import cn.CDPersonal.common.domain.model.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门表 sys_dept。
 *
 * 注意：sys_dept 没有 remark 列，所以 BaseEntity 里的 remark 不参与部门读写。
 * del_flag：0 正常，2 已删除（软删）。
 */
public class SysDept extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 部门 id */
    @Excel(name = "部门ID", cellType = Excel.ColumnType.NUMERIC)
    private Long deptId;

    /** 父部门 id（顶级为 0） */
    @Excel(name = "父部门ID", cellType = Excel.ColumnType.NUMERIC)
    private Long parentId;

    /** 祖级列表，如 0,100,101（不含自身） */
    private String ancestors;

    /** 部门名称 */
    @Excel(name = "部门名称")
    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 30, message = "部门名称长度不能超过30个字符")
    private String deptName;

    /** 显示顺序 */
    @Excel(name = "显示顺序", cellType = Excel.ColumnType.NUMERIC)
    private Integer orderNum;

    /** 负责人 */
    @Excel(name = "负责人")
    private String leader;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 部门状态（0正常 1停用） */
    @Excel(name = "部门状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 上级部门名称：非表字段，列表/详情用 LEFT JOIN 查出来给前端表格用 */
    private String parentName;

    /**
     * 子部门：刻意不填充（前端 handleTree 自己建树）。
     * 序列化成 [] 与 RuoYi 一致，前端 handleTree 会往这个数组里塞子节点。
     */
    private List<SysDept> children = new ArrayList<>();

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getAncestors() {
        return ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public List<SysDept> getChildren() {
        return children;
    }

    public void setChildren(List<SysDept> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("deptId", getDeptId())
                .append("parentId", getParentId())
                .append("ancestors", getAncestors())
                .append("deptName", getDeptName())
                .append("orderNum", getOrderNum())
                .append("leader", getLeader())
                .append("phone", getPhone())
                .append("email", getEmail())
                .append("status", getStatus())
                .append("delFlag", getDelFlag())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
