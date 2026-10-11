package cn.CDPersonal.blog.support;

/**
 * 博客服务自带的控制器基类，只提供分页入口。
 */
public class BaseController {

    /** 设置请求分页数据 */
    protected void startPage() {
        PageUtils.startPage();
    }
}
