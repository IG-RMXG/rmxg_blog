package cn.CDPersonal.common.core.utils;

import cn.CDPersonal.common.domain.entity.SysDept;
import cn.CDPersonal.common.domain.vo.TreeSelect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扁平列表 → 前端 el-tree 需要的嵌套树。
 *
 * 注意和 auth 模块的 MenuTreeUtils 的区别：
 *   - MenuTreeUtils：给 getRouters 用，输出的是 SysMenu（带 meta/hidden 等路由字段）；
 *   - 本类：给 deptTree / roleMenuTreeselect / treeselect 用，输出 TreeSelect
 *     （id + label + children，前端统一按 props {label:'label'} 渲染）。
 *
 * 节点顺序沿用入参顺序（调用方通常已按 order_num 排好），本类不再排序；
 * disabled=true 表示前端不可选，由部门状态推导。
 */
public final class TreeSelectUtils {

    private TreeSelectUtils() {
    }

    /**
     * 部门列表 → 部门树。
     * 停用的部门标 disabled=true，用户页会把它过滤掉。
     */
    // 入参为 null 时返回空列表而不是 null，前端可直接渲染，不必判空。
    public static List<TreeSelect> buildDeptTree(List<SysDept> depts) {
        List<TreeSelect> nodes = new ArrayList<>();
        if (depts == null) {
            return nodes;
        }
        for (SysDept dept : depts) {
            nodes.add(TreeSelect.ofDept(dept.getDeptId(), dept.getDeptName(),
                    dept.getParentId(), !"0".equals(dept.getStatus())));
        }
        return build(nodes);
    }

    /**
     * 把带 parentId 的扁平节点组装成树。
     * 父节点缺失（被权限过滤掉 / 数据脏）的节点当顶级，避免整棵子树消失。
     */
    // 只做一层挂载、不递归：跨层缺父的节点会升为根，但它已经挂上的子节点仍留在 children 里；
    // parentId 指向自身时按根处理，防止成环。
    public static List<TreeSelect> build(List<TreeSelect> nodes) {
        List<TreeSelect> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }
        Map<Long, TreeSelect> byId = new LinkedHashMap<>();
        for (TreeSelect node : nodes) {
            byId.put(node.getId(), node);
        }
        for (TreeSelect node : nodes) {
            Long parentId = node.getParentId();
            TreeSelect parent = parentId == null ? null : byId.get(parentId);
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
