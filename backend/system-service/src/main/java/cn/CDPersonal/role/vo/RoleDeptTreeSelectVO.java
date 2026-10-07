package cn.CDPersonal.role.vo;

import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.domain.vo.TreeSelect;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /system/role/deptTree/{roleId} 的响应体。
 *
 * ⚠️ 这里有个前端版本差异，两边都兼容才稳：
 *   - 任务书 / 新前端：读 response.data + response.checkedKeys
 *   - 仓库里当前这份 views/system/role/index.vue：getDeptTree() 读的是 response.depts
 * 所以 depts 和 data 同时返回同一棵树，checkedKeys 给勾选状态。
 */
public class RoleDeptTreeSelectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    /** 部门树 */
    private List<TreeSelect> depts = new ArrayList<>();

    /** depts 的镜像 */
    private List<TreeSelect> data = new ArrayList<>();

    /** 该角色已勾选的部门 id（data_scope = '2' 时才非空） */
    private List<Long> checkedKeys = new ArrayList<>();

    private Long timestamp;

    public RoleDeptTreeSelectVO() {
        this.code = ResponseStatus.SUCCESS.getCode();
        this.msg = ResponseStatus.SUCCESS.getMessage();
        this.timestamp = System.currentTimeMillis();
    }

    public static RoleDeptTreeSelectVO success(List<TreeSelect> depts, List<Long> checkedKeys) {
        RoleDeptTreeSelectVO vo = new RoleDeptTreeSelectVO();
        vo.setDepts(depts == null ? new ArrayList<>() : depts);
        vo.setData(vo.getDepts());
        vo.setCheckedKeys(checkedKeys == null ? new ArrayList<>() : checkedKeys);
        return vo;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<TreeSelect> getDepts() {
        return depts;
    }

    public void setDepts(List<TreeSelect> depts) {
        this.depts = depts;
    }

    public List<TreeSelect> getData() {
        return data;
    }

    public void setData(List<TreeSelect> data) {
        this.data = data;
    }

    public List<Long> getCheckedKeys() {
        return checkedKeys;
    }

    public void setCheckedKeys(List<Long> checkedKeys) {
        this.checkedKeys = checkedKeys;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
