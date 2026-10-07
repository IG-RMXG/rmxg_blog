package cn.CDPersonal.auth.util;

import cn.CDPersonal.auth.domain.SysMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 把数据库里的扁平分页菜单列表组装成前端需要的树。
 */
public final class MenuTreeUtils {

    private MenuTreeUtils() {
    }

    /**
     * @param menus 扁平菜单（已按 parent_id, order_num 排序）
     * @return 顶级菜单列表，children 已填充
     */
    public static List<SysMenu> buildTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        for (SysMenu menu : menus) {
            List<SysMenu> children = byParent.get(menu.getMenuId());
            if (children != null && !children.isEmpty()) {
                menu.setChildren(children);
            }
        }

        // parent_id = 0 的是顶级
        return menus.stream()
                .filter(m -> m.getParentId() != null && m.getParentId() == 0L)
                .collect(Collectors.toList());
    }
}
