package cn.CDPersonal.auth.controller;

import cn.CDPersonal.auth.domain.SysMenu;
import cn.CDPersonal.auth.domain.UserInfo;
import cn.CDPersonal.auth.service.SysMenuService;
import cn.CDPersonal.auth.service.SysUserService;
import cn.CDPersonal.auth.service.TokenService;
import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.menu.service.SysMenuManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单接口，两类职责：
 *
 * 1. 路由树：getRouters —— 前端 permission.js 的 generateRoutes() 调用，
 *    按当前用户权限过滤、组装 children、补 meta（title/icon/noCache）。
 * 2. 菜单管理 CRUD：list / {menuId} / POST / PUT / DELETE —— 菜单管理页用，
 *    **返回扁平列表**（前端 handleTree 自己建树）。
 *
 * 注意 /menu/treeselect 与 /menu/roleMenuTreeselect/{roleId} 在 SysRoleController 里
 * （它们依赖角色授权，与角色编辑页一起）。
 */
@RestController
@RequestMapping("/menu")
public class SysMenuController {

    private final SysMenuService sysMenuService;
    private final SysUserService sysUserService;
    private final TokenService tokenService;
    private final SysMenuManagementService menuManagementService;

    public SysMenuController(SysMenuService sysMenuService,
                             SysUserService sysUserService,
                             TokenService tokenService,
                             SysMenuManagementService menuManagementService) {
        this.sysMenuService = sysMenuService;
        this.sysUserService = sysUserService;
        this.tokenService = tokenService;
        this.menuManagementService = menuManagementService;
    }

    @RequestMapping(value = "/getRouters", method = RequestMethod.GET)
    public ResponseVO<List<SysMenu>> getRouters(HttpServletRequest request) {
        UserInfo user = tokenService.getLoginUser(request);
        if (user == null) {
            // 返回空菜单而不是 401：前端没有全局 401 拦截，空数组的表现更可控
            return ResponseVO.success(new ArrayList<>());
        }
        boolean isAdmin = sysUserService.isAdmin(user.getUserId());
        List<SysMenu> routers = sysMenuService.getRouters(user.getUserId(), isAdmin);
        return ResponseVO.success(routers);
    }

    // ==================== 菜单管理 ====================

    /** 菜单列表：扁平数组，前端建树。参数 menuName(模糊) / visible / status */
    @GetMapping("/list")
    public ResponseVO<List<SysMenu>> list(SysMenu query) {
        return ResponseVO.success(menuManagementService.selectMenuList(query));
    }

    @GetMapping("/{menuId}")
    public ResponseVO<SysMenu> getInfo(@PathVariable Long menuId) {
        return ResponseVO.success(menuManagementService.selectMenuById(menuId));
    }

    @PostMapping
    public ResponseVO<Void> add(@RequestBody SysMenu menu) {
        if (!menuManagementService.checkMenuNameUnique(menu)) {
            return ResponseVO.error(500, "新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        menu.setCreateBy(LoginUserHolder.getUserName());
        applyDefaults(menu);
        return menuManagementService.insertMenu(menu) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@RequestBody SysMenu menu) {
        if (!menuManagementService.checkMenuNameUnique(menu)) {
            return ResponseVO.error(500, "修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        if (menu.getMenuId() != null && menu.getMenuId().equals(menu.getParentId())) {
            return ResponseVO.error(500, "修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选自己");
        }
        menu.setUpdateBy(LoginUserHolder.getUserName());
        applyDefaults(menu);
        return menuManagementService.updateMenu(menu) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    @DeleteMapping("/{menuId}")
    public ResponseVO<Void> remove(@PathVariable Long menuId) {
        if (menuManagementService.hasChildren(menuId)) {
            return ResponseVO.error(500, "存在子菜单,不允许删除");
        }
        if (menuManagementService.isAssignedToRole(menuId)) {
            return ResponseVO.error(500, "菜单已分配,不允许删除");
        }
        return menuManagementService.deleteMenuById(menuId) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }

    /** 前端有些字段可能不传，补默认值避免写库报 NOT NULL */
    private void applyDefaults(SysMenu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getOrderNum() == null || menu.getOrderNum().isBlank()) {
            menu.setOrderNum("0");
        }
        if (menu.getIsFrame() == null) {
            menu.setIsFrame("1");
        }
        if (menu.getIsCache() == null) {
            menu.setIsCache("0");
        }
        if (menu.getVisible() == null) {
            menu.setVisible("0");
        }
        if (menu.getStatus() == null) {
            menu.setStatus("0");
        }
    }
}
