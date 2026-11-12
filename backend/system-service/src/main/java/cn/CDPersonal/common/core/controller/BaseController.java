package cn.CDPersonal.common.core.controller;


import cn.CDPersonal.common.core.utils.PageUtils;

/**
 * 控制器基类：统一提供分页入口 startPage()，用户/角色/字典/参数/公告/岗位等
 * Controller 继承本类后直接调用，省去各自 import PageUtils。
 *
 * 调用 startPage() 后必须立刻执行数据库查询，中间插入其它查询会让 PageHelper 分页错位。
 */
public class BaseController {

    /**
     * 设置请求分页数据
     */
    protected void startPage() {
        PageUtils.startPage();
    }
}
