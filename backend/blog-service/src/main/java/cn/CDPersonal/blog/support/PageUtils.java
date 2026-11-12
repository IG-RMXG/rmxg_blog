package cn.CDPersonal.blog.support;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 分页工具：从请求里读 pageNum / pageSize，交给 PageHelper 拦截下一条 SQL。
 *
 * 注意：startPage() 之后到真正执行目标查询之间，不能再触发任何数据库查询
 * （例如取当前登录用户会查 sys_user），否则 PageHelper 会把 LIMIT 拼到那条 SQL 上
 * 产生 "LIMIT 1 LIMIT ?" 语法错误。
 */
public class PageUtils extends PageHelper {

    /** 兜底页码：参数缺失、非数字或小于等于 0 时使用 */
    private static final int DEFAULT_PAGE_NUM = 1;
    /** 兜底每页条数：同样只在非法值时生效；服务端未对 pageSize 做上限校验 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 从当前请求读取 pageNum / pageSize 并开启分页；非 Web 上下文静默跳过 */
    public static void startPage() {
        Integer pageNum = null;
        Integer pageSize = null;

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 非 Web 上下文（定时任务、单元测试）没有请求对象，保持默认分页
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            pageNum = parseInt(request.getParameter("pageNum"));
            pageSize = parseInt(request.getParameter("pageSize"));
        }

        // 只采信正整数：0、负数、非数字都回退默认值，避免拼出非法 LIMIT
        int num = pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        PageHelper.startPage(num, size);
    }

    /** 把 startPage() 之后的查询结果包装成前端表格要的 {total, rows, code, msg} */
    public static TableDataInfo getDataTable(List<?> list) {
        TableDataInfo info = new TableDataInfo();
        info.setCode(200);
        info.setMsg("查询成功");
        // 未登录/无权等场景会直接传 null：这里返回空数组而不是 null，前端表格不会因 rows=null 报错
        if (list == null) {
            info.setRows(java.util.Collections.emptyList());
            info.setTotal(0);
            return info;
        }
        info.setRows(list);
        // PageInfo 依赖 PageHelper 返回的 Page 实例才能拿到真实总数；
        // 若查询前没调 startPage()，total 会退化成当前列表长度。
        info.setTotal(new PageInfo<>(list).getTotal());
        return info;
    }

    /** 宽松解析：空白或非法数字都按"未传"返回 null，由 startPage 回退默认值 */
    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
