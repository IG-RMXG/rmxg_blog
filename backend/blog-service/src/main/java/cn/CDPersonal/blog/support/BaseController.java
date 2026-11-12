package cn.CDPersonal.blog.support;

// 鉴权方法（isLogin / isAdmin 等）都在子类 BlogBaseController 中，本类不依赖 BlogTokenService
/**
 * 博客服务自带的控制器基类，只提供分页入口。
 */
public class BaseController {

    /** 设置请求分页数据 */
    protected void startPage() {
        // PageHelper 的分页参数存在 ThreadLocal，必须与真正执行查询的线程一致
        PageUtils.startPage();
    }
}
