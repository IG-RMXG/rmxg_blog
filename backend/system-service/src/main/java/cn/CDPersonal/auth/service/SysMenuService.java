package cn.CDPersonal.auth.service;

import cn.CDPersonal.auth.domain.SysMenu;
import cn.CDPersonal.auth.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    public SysMenuService(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    /**
     * 取用户可见的路由树，并转换成前端要的结构。
     *
     * 说明：库里 sys_role_menu 只给「普通角色」配了菜单，admin 角色并没有把
     * 全部菜单都插一遍，所以管理员走全量分支，否则管理员会一个菜单都看不到。
     */
    public List<SysMenu> getRouters(Long userId, boolean isAdmin) {
        List<SysMenu> menus = isAdmin
                ? sysMenuMapper.selectAllMenus()
                : sysMenuMapper.selectMenusByUserId(userId);
        List<SysMenu> tree = cn.CDPersonal.auth.util.MenuTreeUtils.buildTree(menus);
        tree.forEach(this::normalize);
        return tree;
    }

    /**
     * 把数据库原始列映射成 RuoYi 前端期望的路由结构。
     *
     * 前端 SidebarItem 渲染菜单文字用的是 item.meta.title（不是 menuName），
     * 隐藏与缓存也分别读 hidden / meta.noCache。只回原始列的话，
     * 侧边栏会出现"有折叠箭头、没有文字"的空菜单。
     */
    private void normalize(SysMenu menu) {
        // visible: '0' 显示，'1' 隐藏
        menu.setHidden("1".equals(menu.getVisible()));
        // isCache: '0' 缓存，'1' 不缓存
        menu.setMeta(new SysMenu.Meta(
                menu.getMenuName(),
                menu.getIcon(),
                "1".equals(menu.getIsCache())
        ));

        List<SysMenu> children = menu.getChildren();
        boolean hasChildren = children != null && !children.isEmpty();

        if (hasChildren) {
            // 目录：顶级用 Layout，嵌套用 ParentView
            boolean external = isExternal(menu.getPath());
            if (external) {
                menu.setComponent("InnerLink");
            } else {
                // 顶级菜单由 filterAsyncRouter 传入 lastRouter=false 判断，这里统一给 Layout
                menu.setComponent(menu.getParentId() != null && menu.getParentId() == 0L
                        ? "Layout" : "ParentView");
            }
            // 只有一个子菜单时是否仍显示父级
            menu.setAlwaysShow(children.size() > 1 || isExternal(menu.getPath()));
            menu.setName(pathToName(menu.getPath()));
            children.forEach(this::normalize);
        } else {
            // 叶子节点
            if (isExternal(menu.getPath())) {
                menu.setComponent("InnerLink");
            } else if (menu.getComponent() == null || menu.getComponent().isBlank()) {
                // 目录类型但没有子菜单、也不是外链：无法渲染，直接隐藏，避免空壳菜单
                menu.setHidden(Boolean.TRUE);
            }
            menu.setAlwaysShow(Boolean.FALSE);
            menu.setName(pathToName(menu.getPath()));
        }
    }

    private boolean isExternal(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    /** 由 path 生成路由 name（keep-alive 需要），如 system/user -> SystemUser */
    private String pathToName(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("[/\\-]")) {
            if (seg.isBlank()) {
                continue;
            }
            sb.append(Character.toUpperCase(seg.charAt(0))).append(seg.substring(1));
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
