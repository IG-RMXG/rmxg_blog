package cn.CDPersonal.auth.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统菜单，对应 ry-cloud.sys_menu
 */
public class SysMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long menuId;
    private String menuName;
    private Long parentId;
    private String orderNum;
    private String path;
    private String component;
    private String query;
    private String isFrame;
    private String isCache;
    private String menuType;
    private String visible;
    private String status;
    private String perms;
    private String icon;
    /** 仅用于组装树，不入库 */
    private java.util.List<SysMenu> children;

    /*
     * 以下是"给前端的路由结构"字段，不是数据库列。
     *
     * 前端 layout/components/Sidebar/SidebarItem 渲染菜单名字用的是 item.meta.title，
     * 不是 menuName；隐藏/缓存也分别读 hidden / meta.noCache。
     * 如果只回数据库原始列，侧边栏会出现"有折叠箭头但没有文字"的空菜单。
     */
    /** visible='1' 时前端隐藏该菜单 */
    private Boolean hidden;
    /** 只有一个子菜单时是否仍显示父级 */
    private Boolean alwaysShow;
    /** 路由 name，供 keep-alive 使用 */
    private String name;
    /** 前端 meta 对象 */
    private Meta meta;

    /** 创建者（菜单管理页写库用；sys_menu 有该列，但不继承 BaseEntity，故单独声明） */
    private String createBy;
    /** 更新者 */
    private String updateBy;

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public static class Meta implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String icon;
        private Boolean noCache;

        public Meta() {
        }

        public Meta(String title, String icon, Boolean noCache) {
            this.title = title;
            this.icon = icon;
            this.noCache = noCache;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public Boolean getNoCache() {
            return noCache;
        }

        public void setNoCache(Boolean noCache) {
            this.noCache = noCache;
        }
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public Boolean getAlwaysShow() {
        return alwaysShow;
    }

    public void setAlwaysShow(Boolean alwaysShow) {
        this.alwaysShow = alwaysShow;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(String orderNum) {
        this.orderNum = orderNum;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getIsFrame() {
        return isFrame;
    }

    public void setIsFrame(String isFrame) {
        this.isFrame = isFrame;
    }

    public String getIsCache() {
        return isCache;
    }

    public void setIsCache(String isCache) {
        this.isCache = isCache;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public java.util.List<SysMenu> getChildren() {
        return children;
    }

    public void setChildren(java.util.List<SysMenu> children) {
        this.children = children;
    }
}
