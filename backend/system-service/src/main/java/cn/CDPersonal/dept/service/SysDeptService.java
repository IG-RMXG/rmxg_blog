package cn.CDPersonal.dept.service;

import cn.CDPersonal.common.domain.entity.SysDept;
import cn.CDPersonal.dept.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 部门管理。
 *
 * ancestors 维护规则（与 RuoYi 一致）：
 *   顶级部门 ancestors = "0"；子部门 ancestors = 父.ancestors + "," + 父.deptId。
 *   ancestors **不含自身**，所以子孙判断可以用 FIND_IN_SET / LIKE。
 */
@Service
public class SysDeptService {

    private final SysDeptMapper deptMapper;

    public SysDeptService(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    /** 部门列表（扁平数组，前端自己建树） */
    public List<SysDept> selectDeptList(SysDept query) {
        return deptMapper.selectDeptList(query);
    }

    /** 部门列表，排除指定部门及其所有子孙（"上级部门"下拉） */
    public List<SysDept> selectDeptListExcludeChildren(Long deptId) {
        return deptMapper.selectDeptListExcludeChildren(deptId);
    }

    public SysDept selectDeptById(Long deptId) {
        return deptMapper.selectDeptById(deptId);
    }

    @Transactional
    public int insertDept(SysDept dept) {
        fillDefaults(dept);
        dept.setAncestors(buildAncestors(dept.getParentId()));
        return deptMapper.insertDept(dept);
    }

    /**
     * 修改部门。父部门变了要重算 ancestors，并级联更新子孙的 ancestors。
     * 调用前 controller 已校验父部门不是自己/自己的下级。
     */
    @Transactional
    public int updateDept(SysDept dept) {
        SysDept old = deptMapper.selectDeptById(dept.getDeptId());
        if (old == null) {
            return 0;
        }
        fillDefaults(dept);
        String newAncestors = buildAncestors(dept.getParentId());
        dept.setAncestors(newAncestors);

        int rows = deptMapper.updateDept(dept);

        String oldPrefix = old.getAncestors() + "," + old.getDeptId();
        String newPrefix = newAncestors + "," + old.getDeptId();
        if (!oldPrefix.equals(newPrefix)) {
            deptMapper.updateChildrenAncestors(oldPrefix, newPrefix, old.getDeptId());
        }
        return rows;
    }

    /** 软删部门（del_flag = '2'） */
    @Transactional
    public int deleteDeptById(Long deptId) {
        return deptMapper.deleteDeptById(deptId);
    }

    /** 是否存在下级部门 */
    public boolean hasChildByDeptId(Long deptId) {
        return deptMapper.countChildrenByParentId(deptId) > 0;
    }

    /** 部门下是否存在用户 */
    public boolean hasUserByDeptId(Long deptId) {
        return deptMapper.countUserByDeptId(deptId) > 0;
    }

    /**
     * 新的上级部门是否非法（是自己，或自己的子孙）。
     * 子孙判断：父部门的 ancestors 链里出现了自身 id。
     */
    public boolean isParentIllegal(Long deptId, Long parentId) {
        if (deptId == null || parentId == null || parentId == 0L) {
            return false;
        }
        if (deptId.equals(parentId)) {
            return true;
        }
        SysDept parent = deptMapper.selectDeptById(parentId);
        if (parent == null || parent.getAncestors() == null || parent.getAncestors().isEmpty()) {
            return false;
        }
        for (String id : parent.getAncestors().split(",")) {
            if (String.valueOf(deptId).equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 父部门 ancestors 拼上父 id；顶级部门（parentId 为空或 0）为 "0" */
    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = deptMapper.selectDeptById(parentId);
        if (parent == null) {
            return "0";
        }
        String parentAncestors = parent.getAncestors() == null || parent.getAncestors().isEmpty()
                ? "0"
                : parent.getAncestors();
        return parentAncestors + "," + parentId;
    }

    /** 补全前端可能没传、但数据库 NOT NULL / 有默认语义的字段 */
    private void fillDefaults(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getOrderNum() == null) {
            dept.setOrderNum(0);
        }
        if (dept.getStatus() == null || dept.getStatus().isEmpty()) {
            dept.setStatus("0");
        }
    }
}
