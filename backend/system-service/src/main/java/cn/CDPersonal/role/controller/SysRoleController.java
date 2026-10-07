package cn.CDPersonal.role.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysRole;
import cn.CDPersonal.common.domain.entity.SysUser;
import cn.CDPersonal.common.domain.vo.TreeSelect;
import cn.CDPersonal.role.service.SysRoleService;
import cn.CDPersonal.role.vo.RoleDeptTreeSelectVO;
import cn.CDPersonal.role.vo.RoleMenuTreeSelectVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 角色接口（对外前缀 /system/role）。
 *
 * ⚠️ 权限分配相关的两个 tree 接口路径在 /system/menu 下（前端 api/system/menu.js 写死的），
 * 但它们读的是角色数据，所以也放在这个 controller 里：
 *   GET /system/menu/treeselect
 *   GET /system/menu/roleMenuTreeselect/{roleId}
 * 菜单本身的 CRUD 由另一位同事负责，不在这里。
 *
 * 参数来源三种，混了页面就报错：
 *   - @RequestBody          ：POST/PUT /role、/role/dataScope、/role/changeStatus、/role/authUser/cancel
 *   - @RequestParam(query)  ：PUT /role/authUser/cancelAll、/role/authUser/selectAll
 *   - @PathVariable         ：/{roleId}、/deptTree/{roleId}、DELETE /{roleIds}
 */
@RestController
public class SysRoleController extends BaseController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    // ==================== 角色 CRUD ====================

    /** GET /system/role/list —— 分页，前端读 rows / total */
    @GetMapping("/role/list")
    public TableDataInfo list(SysRole query) {
        startPage();
        return PageUtils.getDataTable(roleService.selectRoleList(query));
    }

    /** GET /system/role/{roleId} —— 详情，前端读 response.data */
    @GetMapping("/role/{roleId}")
    public ResponseVO<SysRole> getInfo(@PathVariable Long roleId) {
        return ResponseVO.success(roleService.selectRoleById(roleId));
    }

    /** POST /system/role —— 新增，同时写 sys_role_menu */
    @PostMapping("/role")
    public ResponseVO<Void> add(@RequestBody SysRole role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return ResponseVO.error(500, "新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return ResponseVO.error(500, "新增角色'" + role.getRoleName() + "'失败，权限字符已存在");
        }
        role.setCreateBy(LoginUserHolder.getUserName());
        return roleService.insertRole(role) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    /** PUT /system/role —— 修改，重写 sys_role_menu */
    @PutMapping("/role")
    public ResponseVO<Void> edit(@RequestBody SysRole role) {
        if (role.getRoleId() == null) {
            return ResponseVO.error(500, "角色ID不能为空");
        }
        if (!roleService.checkRoleNameUnique(role)) {
            return ResponseVO.error(500, "修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return ResponseVO.error(500, "修改角色'" + role.getRoleName() + "'失败，权限字符已存在");
        }
        role.setUpdateBy(LoginUserHolder.getUserName());
        return roleService.updateRole(role) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    /**
     * PUT /system/role/dataScope —— JSON body {roleId, dataScope, deptIds:[]}
     * dataScope='1'（全部数据）时清空 sys_role_dept。
     */
    @PutMapping("/role/dataScope")
    public ResponseVO<Void> dataScope(@RequestBody SysRole role) {
        if (role.getRoleId() == null) {
            return ResponseVO.error(500, "角色ID不能为空");
        }
        role.setUpdateBy(LoginUserHolder.getUserName());
        return roleService.updateRoleDataScope(role) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改数据权限失败");
    }

    /** PUT /system/role/changeStatus —— JSON body {roleId, status} */
    @PutMapping("/role/changeStatus")
    public ResponseVO<Void> changeStatus(@RequestBody SysRole role) {
        if (role.getRoleId() == null) {
            return ResponseVO.error(500, "角色ID不能为空");
        }
        if (role.getStatus() == null || role.getStatus().isBlank()) {
            return ResponseVO.error(500, "状态不能为空");
        }
        role.setUpdateBy(LoginUserHolder.getUserName());
        return roleService.updateRoleStatus(role) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改状态失败");
    }

    /** DELETE /system/role/{roleIds} —— 批量软删，admin(roleId=1) 不允许删 */
    @DeleteMapping("/role/{roleIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] roleIds) {
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            if (roleId == 1L) {
                return ResponseVO.error(500, "不允许删除超级管理员角色");
            }
            if (roleService.hasUserRoleByRoleId(roleId)) {
                SysRole role = roleService.selectRoleById(roleId);
                String name = role == null ? String.valueOf(roleId) : role.getRoleName();
                return ResponseVO.error(500, name + "已分配,不能删除");
            }
        }
        return roleService.deleteRoleByIds(roleIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }

    // ==================== 分配用户 ====================

    /** GET /system/role/authUser/allocatedList —— 该角色已分配的用户（分页） */
    @GetMapping("/role/authUser/allocatedList")
    public TableDataInfo allocatedList(SysUser query, @RequestParam("roleId") Long roleId) {
        startPage();
        return PageUtils.getDataTable(roleService.selectUserListByRole(query, roleId, true));
    }

    /** GET /system/role/authUser/unallocatedList —— 未分配的用户（分页，"添加用户"弹窗） */
    @GetMapping("/role/authUser/unallocatedList")
    public TableDataInfo unallocatedList(SysUser query, @RequestParam("roleId") Long roleId) {
        startPage();
        return PageUtils.getDataTable(roleService.selectUserListByRole(query, roleId, false));
    }

    /** PUT /system/role/authUser/cancel —— JSON body {userId, roleId}（前端用 data 提交） */
    @PutMapping("/role/authUser/cancel")
    public ResponseVO<Void> cancelAuthUser(@RequestBody Map<String, Object> body) {
        Long userId = asLong(body.get("userId"));
        Long roleId = asLong(body.get("roleId"));
        if (userId == null || roleId == null) {
            return ResponseVO.error(500, "用户ID和角色ID不能为空");
        }
        return roleService.deleteAuthUser(userId, roleId) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "取消授权失败");
    }

    /** PUT /system/role/authUser/cancelAll —— query string: roleId + userIds（逗号分隔） */
    @PutMapping("/role/authUser/cancelAll")
    public ResponseVO<Void> cancelAuthUserAll(@RequestParam("roleId") Long roleId,
                                              @RequestParam("userIds") String userIds) {
        roleService.deleteAuthUsers(roleId, parseLongArray(userIds));
        return ResponseVO.success();
    }

    /** PUT /system/role/authUser/selectAll —— query string: roleId + userIds（逗号分隔） */
    @PutMapping("/role/authUser/selectAll")
    public ResponseVO<Void> selectAuthUserAll(@RequestParam("roleId") Long roleId,
                                              @RequestParam("userIds") String userIds) {
        roleService.insertAuthUsers(roleId, parseLongArray(userIds));
        return ResponseVO.success();
    }

    // ==================== 树形 ====================

    /** GET /system/role/deptTree/{roleId} —— 部门树 + 已勾选 deptId（depts/data 两个 key 都回） */
    @GetMapping("/role/deptTree/{roleId}")
    public RoleDeptTreeSelectVO deptTree(@PathVariable Long roleId) {
        return RoleDeptTreeSelectVO.success(roleService.selectDeptTree(),
                roleService.selectDeptIdsByRoleId(roleId));
    }

    /**
     * GET /system/menu/treeselect —— 完整菜单树，前端读 response.data。
     */
    @GetMapping("/menu/treeselect")
    public ResponseVO<List<TreeSelect>> menuTreeselect() {
        return ResponseVO.success(roleService.selectMenuTreeAll());
    }

    /**
     * GET /system/menu/roleMenuTreeselect/{roleId}
     * 前端读顶层的 response.menus 与 response.checkedKeys。
     */
    @GetMapping("/menu/roleMenuTreeselect/{roleId}")
    public RoleMenuTreeSelectVO roleMenuTreeselect(@PathVariable Long roleId) {
        return RoleMenuTreeSelectVO.success(roleService.selectMenuTreeAll(),
                roleService.selectMenuIdsByRoleId(roleId));
    }

    // ==================== 内部工具 ====================

    /** 逗号分隔 id 串 → Long[]；空格与脏数据直接忽略 */
    private Long[] parseLongArray(String csv) {
        if (csv == null || csv.isBlank()) {
            return new Long[0];
        }
        List<Long> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignored) {
                // 忽略脏数据
            }
        }
        return ids.toArray(new Long[0]);
    }

    /** 宽松地把 JSON 里的数字/字符串取成 Long，取不到返回 null */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
