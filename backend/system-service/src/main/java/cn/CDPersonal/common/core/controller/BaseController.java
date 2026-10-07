package cn.CDPersonal.common.core.controller;


import cn.CDPersonal.common.core.utils.PageUtils;

public class BaseController {

    /**
     * 设置请求分页数据
     */
    protected void startPage() {
        PageUtils.startPage();
    }
}
