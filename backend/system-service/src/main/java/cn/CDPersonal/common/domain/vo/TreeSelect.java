package cn.CDPersonal.common.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 前端 el-tree / el-tree-select 需要的树节点。
 *
 * 前端统一按 props `{ label: 'label', children: 'children' }` 渲染，
 * node-key 用 id；所以才要在 RuoYi 的原始实体之外再包一层：
 *   - /user/deptTree      → 部门树
 *   - /menu/treeselect    → 菜单树
 *
 * 同时保留原始实体字段（deptId/deptName、menuId/menuName），
 * 方便调试和以后改成用 deptId 当 node-key。
 */
public class TreeSelect implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点 id（部门 id / 菜单 id） */
    private Long id;

    /** 节点显示名（部门名 / 菜单名） */
    private String label;

    /** 父节点 id，0 为顶级 */
    private Long parentId;

    private List<TreeSelect> children = new ArrayList<>();

    /** 原始实体：部门 = deptId/deptName，菜单 = menuId/menuName */
    private Long deptId;
    private String deptName;
    private Long menuId;
    private String menuName;

    /** 停用的部门在前端被过滤掉（用户页 filterDisabledDept 读的是这个字段） */
    private Boolean disabled = Boolean.FALSE;

    public TreeSelect() {
    }

    public static TreeSelect ofDept(Long deptId, String deptName, Long parentId, boolean disabled) {
        TreeSelect node = new TreeSelect();
        node.setId(deptId);
        node.setDeptId(deptId);
        node.setLabel(deptName);
        node.setDeptName(deptName);
        node.setParentId(parentId);
        node.setDisabled(disabled);
        return node;
    }

    public static TreeSelect ofMenu(Long menuId, String menuName, Long parentId) {
        TreeSelect node = new TreeSelect();
        node.setId(menuId);
        node.setMenuId(menuId);
        node.setLabel(menuName);
        node.setMenuName(menuName);
        node.setParentId(parentId);
        node.setDisabled(Boolean.FALSE);
        return node;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<TreeSelect> getChildren() {
        return children;
    }

    public void setChildren(List<TreeSelect> children) {
        this.children = children;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
}
