package cn.CDPersonal.role.vo;

import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.domain.vo.TreeSelect;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /system/menu/roleMenuTreeselect/{roleId} 的响应体。
 *
 * 前端（views/system/role/index.vue）：
 *   menuOptions.value = response.menus          // 完整菜单树（含按钮 F 类型）
 *   let checkedKeys = response.checkedKeys      // 该角色已勾选的 menuId
 *
 * 不是 { code, data: ... }，menus / checkedKeys 都在顶层。
 * 另外多带一份 data 镜像，兼容只读 data 的调用方 —— 多一个字段不影响前端。
 */
public class RoleMenuTreeSelectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    /** 完整菜单树（父子已嵌套） */
    private List<TreeSelect> menus = new ArrayList<>();

    /** 该角色已勾选的菜单 id */
    private List<Long> checkedKeys = new ArrayList<>();

    /** menus 的镜像，便于按 ResponseVO 习惯读取 */
    private List<TreeSelect> data = new ArrayList<>();

    private Long timestamp;

    public RoleMenuTreeSelectVO() {
        this.code = ResponseStatus.SUCCESS.getCode();
        this.msg = ResponseStatus.SUCCESS.getMessage();
        this.timestamp = System.currentTimeMillis();
    }

    public static RoleMenuTreeSelectVO success(List<TreeSelect> menus, List<Long> checkedKeys) {
        RoleMenuTreeSelectVO vo = new RoleMenuTreeSelectVO();
        vo.setMenus(menus == null ? new ArrayList<>() : menus);
        vo.setCheckedKeys(checkedKeys == null ? new ArrayList<>() : checkedKeys);
        vo.setData(vo.getMenus());
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

    public List<TreeSelect> getMenus() {
        return menus;
    }

    public void setMenus(List<TreeSelect> menus) {
        this.menus = menus;
    }

    public List<Long> getCheckedKeys() {
        return checkedKeys;
    }

    public void setCheckedKeys(List<Long> checkedKeys) {
        this.checkedKeys = checkedKeys;
    }

    public List<TreeSelect> getData() {
        return data;
    }

    public void setData(List<TreeSelect> data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
