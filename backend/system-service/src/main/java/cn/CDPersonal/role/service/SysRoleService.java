package cn.CDPersonal.role.service;

import cn.CDPersonal.auth.domain.SysMenu;
import cn.CDPersonal.common.core.utils.TreeSelectUtils;
import cn.CDPersonal.common.domain.entity.SysDept;
import cn.CDPersonal.common.domain.entity.SysRole;
import cn.CDPersonal.common.domain.entity.SysUser;
import cn.CDPersonal.common.domain.vo.TreeSelect;
import cn.CDPersonal.dept.mapper.SysDeptMapper;
import cn.CDPersonal.role.mapper.SysMenuQueryMapper;
import cn.CDPersonal.role.mapper.SysRoleMapper;
import cn.CDPersonal.user.mapper.SysUserManagementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色管理业务（/system/role/** + 菜单授权相关的两个 tree 接口）。
 *
 * 一个角色牵扯 4 张表：
 *   sys_role           主表
 *   sys_role_menu      菜单权限（新增/修改角色时重写）
 *   sys_role_dept      数据权限（dataScope='2' 自定时重写）
 *   sys_user_role      用户授权（分配用户 / 删除角色时清理）
 */
@Service
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysMenuQueryMapper menuQueryMapper;
    private final SysUserManagementMapper userMapper;
    private final SysDeptMapper deptMapper;

    public SysRoleService(SysRoleMapper roleMapper,
                          SysMenuQueryMapper menuQueryMapper,
                          SysUserManagementMapper userMapper,
                          SysDeptMapper deptMapper) {
        this.roleMapper = roleMapper;
        this.menuQueryMapper = menuQueryMapper;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
    }

    // ==================== 角色 CRUD ====================

    public List<SysRole> selectRoleList(SysRole query) {
        return roleMapper.selectRoleList(query);
    }

    public SysRole selectRoleById(Long roleId) {
        return roleMapper.selectRoleById(roleId);
    }

    /** 角色名称唯一（新增时 roleId 为空；修改时命中的必须是自己） */
    public boolean checkRoleNameUnique(SysRole role) {
        SysRole exist = roleMapper.checkRoleNameUnique(role.getRoleName());
        return exist == null || exist.getRoleId().equals(role.getRoleId());
    }

    /** 权限字符唯一 */
    public boolean checkRoleKeyUnique(SysRole role) {
        SysRole exist = roleMapper.checkRoleKeyUnique(role.getRoleKey());
        return exist == null || exist.getRoleId().equals(role.getRoleId());
    }

    /** 新增角色，同时写 sys_role_menu */
    @Transactional
    public int insertRole(SysRole role) {
        fillDefaults(role);
        int rows = roleMapper.insertRole(role);
        insertRoleMenu(role.getRoleId(), role.getMenuIds());
        return rows;
    }

    /** 修改角色，重写 sys_role_menu；dataScope='2' 时同时重写 sys_role_dept */
    @Transactional
    public int updateRole(SysRole role) {
        fillDefaults(role);
        int rows = roleMapper.updateRole(role);
        roleMapper.deleteRoleMenusByRoleId(role.getRoleId());
        insertRoleMenu(role.getRoleId(), role.getMenuIds());
        return rows;
    }

    /**
     * 软删角色：del_flag='2'，并清理三张关联表。
     *
     * 事务内先清关联再软删主表，避免残留脏关联数据。
     * 是否允许删（admin / 已分配用户）由 controller 校验并给出友好提示。
     */
    @Transactional
    public int deleteRoleByIds(Long[] roleIds) {
        roleMapper.deleteRoleMenusByRoleIds(roleIds);
        roleMapper.deleteRoleDeptsByRoleIds(roleIds);
        roleMapper.deleteUserRolesByRoleIds(roleIds);
        return roleMapper.softDeleteRoleByIds(roleIds);
    }

    /** 角色是否已分配给用户（删除前校验） */
    public boolean hasUserRoleByRoleId(Long roleId) {
        return roleMapper.countUserRoleByRoleId(roleId) > 0;
    }

    @Transactional
    public int updateRoleStatus(SysRole role) {
        return roleMapper.updateRoleStatus(role);
    }

    // ==================== 数据权限 ====================

    /**
     * 修改数据权限。
     *
     * dataScope：
     *   1 = 全部数据权限 → sys_role_dept 清空（到处都能看，不需要白名单）
     *   2 = 自定数据权限 → 按 deptIds 重写 sys_role_dept
     *   3/4/5            → 按部门/本人推导，同样清空白名单
     *
     * 统一"先删后插"，前端不传 deptIds 就等于清空。
     */
    @Transactional
    public int updateRoleDataScope(SysRole role) {
        roleMapper.deleteRoleDeptsByRoleId(role.getRoleId());
        String dataScope = role.getDataScope();
        if ("2".equals(dataScope) && role.getDeptIds() != null && role.getDeptIds().length > 0) {
            roleMapper.batchInsertRoleDepts(role.getRoleId(), role.getDeptIds());
        }
        return roleMapper.updateRoleDataScope(role);
    }

    // ==================== 菜单树 ====================

    /**
     * GET /system/menu/treeselect：完整菜单树（M 目录 + C 菜单 + F 按钮）。
     * 角色配权限时必须看到按钮级节点，否则已勾选的按钮会"消失"。
     */
    public List<TreeSelect> selectMenuTree() {
        return buildMenuTree(menuQueryMapper.selectMenuListByTypes(
                List.of("M", "C", "F")));
    }

    /**
     * GET /system/menu/roleMenuTreeselect/{roleId}：
     * 返回完整菜单树 + 该角色已勾选的 menuId。
     */
    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        List<Long> ids = menuQueryMapper.selectMenuIdsByRoleId(roleId);
        return ids == null ? new ArrayList<>() : ids;
    }

    public List<TreeSelect> selectMenuTreeAll() {
        return selectMenuTree();
    }

    private List<TreeSelect> buildMenuTree(List<SysMenu> menus) {
        List<TreeSelect> nodes = new ArrayList<>();
        if (menus == null) {
            return nodes;
        }
        for (SysMenu menu : menus) {
            nodes.add(TreeSelect.ofMenu(menu.getMenuId(), menu.getMenuName(), menu.getParentId()));
        }
        return TreeSelectUtils.build(nodes);
    }

    // ==================== 部门树 ====================

    /**
     * GET /system/role/deptTree/{roleId}：部门树 + 该角色已勾选的 deptId。
     * 注：前端角色页读的 key 有版本差异（depts / data），VO 里两个都给了。
     */
    public List<TreeSelect> selectDeptTree() {
        List<SysDept> depts = deptMapper.selectDeptList(new SysDept());
        return TreeSelectUtils.buildDeptTree(depts);
    }

    public List<Long> selectDeptIdsByRoleId(Long roleId) {
        List<Long> ids = roleMapper.selectDeptIdsByRoleId(roleId);
        return ids == null ? new ArrayList<>() : ids;
    }

    // ==================== 用户授权 ====================

    /**
     * 分配用户列表。
     * allocated=true → 该角色已分配；false → 未分配（"添加用户"弹窗）。
     */
    public List<SysUser> selectUserListByRole(SysUser query, Long roleId, boolean allocated) {
        return userMapper.selectUserListByRole(query, roleId, allocated);
    }

    /** 取消单个用户的角色授权 */
    @Transactional
    public int deleteAuthUser(Long userId, Long roleId) {
        return roleMapper.deleteUserRole(userId, roleId);
    }

    /** 批量取消授权 */
    @Transactional
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return 0;
        }
        return roleMapper.deleteUserRolesByRoleIdAndUserIds(roleId, userIds);
    }

    /** 批量授权（INSERT IGNORE，重复勾选不会 500） */
    @Transactional
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return 0;
        }
        return roleMapper.batchInsertUserRoles(roleId, userIds);
    }

    // ==================== 内部 ====================

    private void insertRoleMenu(Long roleId, Long[] menuIds) {
        if (menuIds != null && menuIds.length > 0) {
            roleMapper.batchInsertRoleMenus(roleId, menuIds);
        }
    }

    private void fillDefaults(SysRole role) {
        if (role.getRoleSort() == null) {
            role.setRoleSort(0);
        }
        if (role.getStatus() == null || role.getStatus().isBlank()) {
            role.setStatus("0");
        }
        if (role.getDataScope() == null || role.getDataScope().isBlank()) {
            role.setDataScope("1");
        }
        if (role.getMenuCheckStrictly() == null) {
            role.setMenuCheckStrictly(Boolean.TRUE);
        }
        if (role.getDeptCheckStrictly() == null) {
            role.setDeptCheckStrictly(Boolean.TRUE);
        }
    }
}
