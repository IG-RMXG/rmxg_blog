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

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 从当前请求读取 pageNum / pageSize 并开启分页；非 Web 上下文静默跳过 */
    public static void startPage() {
        Integer pageNum = null;
        Integer pageSize = null;

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            pageNum = parseInt(request.getParameter("pageNum"));
            pageSize = parseInt(request.getParameter("pageSize"));
        }

        int num = pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        PageHelper.startPage(num, size);
    }

    /** 把 startPage() 之后的查询结果包装成前端表格要的 {total, rows, code, msg} */
    public static TableDataInfo getDataTable(List<?> list) {
        TableDataInfo info = new TableDataInfo();
        info.setCode(200);
        info.setMsg("查询成功");
        if (list == null) {
            info.setRows(java.util.Collections.emptyList());
            info.setTotal(0);
            return info;
        }
        info.setRows(list);
        info.setTotal(new PageInfo<>(list).getTotal());
        return info;
    }

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
