package cn.CDPersonal.menu.service;

import cn.CDPersonal.auth.domain.SysMenu;
import cn.CDPersonal.role.mapper.SysMenuQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜单管理（CRUD）。
 *
 * 与 auth.service.SysMenuService 的分工：
 *   - 那个负责"路由树"（getRouters，按用户权限过滤、组装 children、补 meta）
 *   - 这个负责菜单管理页的增删改查（返回扁平列表，由前端 handleTree 建树）
 */
@Service
public class SysMenuManagementService {

    private final SysMenuQueryMapper menuMapper;

    public SysMenuManagementService(SysMenuQueryMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public List<SysMenu> selectMenuList(SysMenu query) {
        return menuMapper.selectMenuList(query);
    }

    public SysMenu selectMenuById(Long menuId) {
        return menuMapper.selectMenuById(menuId);
    }

    /** 同一父级下菜单名不能重复（新增 menuId 为空，修改排除自身） */
    public boolean checkMenuNameUnique(SysMenu menu) {
        Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
        return menuMapper.countMenuNameDuplicate(parentId, menu.getMenuName(), menu.getMenuId()) == 0;
    }

    @Transactional
    public int insertMenu(SysMenu menu) {
        return menuMapper.insertMenu(menu);
    }

    @Transactional
    public int updateMenu(SysMenu menu) {
        return menuMapper.updateMenu(menu);
    }

    /** 有子菜单不允许删 */
    public boolean hasChildren(Long menuId) {
        return menuMapper.countChildrenByParentId(menuId) > 0;
    }

    /** 已分配给角色不允许删 */
    public boolean isAssignedToRole(Long menuId) {
        return menuMapper.countRoleMenuByMenuId(menuId) > 0;
    }

    @Transactional
    public int deleteMenuById(Long menuId) {
        // 先清关联再删菜单，避免 sys_role_menu 留脏数据
        menuMapper.deleteRoleMenuByMenuId(menuId);
        return menuMapper.deleteMenuById(menuId);
    }
}
