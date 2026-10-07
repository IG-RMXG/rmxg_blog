package cn.CDPersonal.common.core.utils;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 分页工具类。
 *
 * 前端所有列表接口（GET + params）都带 pageNum / pageSize，
 * 分页由 PageHelper 拦截下一条 SQL 实现：调用 startPage() 之后再查数据库，
 * 返回的 List 实际是 Page 对象，用 getDataTable() 取出 total/rows。
 */
public class PageUtils extends PageHelper {

    /** 前端未传时的默认值，与 RuoYi 保持一致 */
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 从当前请求读取 pageNum / pageSize 并开启分页。
     * 非 Web 上下文（如定时任务）时静默跳过，避免抛异常。
     */
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

    /**
     * 把查询结果包装成前端表格要的 {total, rows, code, msg}。
     * 传进来的 list 必须是 startPage() 之后查出来的（即 Page 实例），
     * 否则 total 会退化成当前页条数。
     */
    public static <T> cn.CDPersonal.common.core.page.TableDataInfo getDataTable(List<T> list) {
        cn.CDPersonal.common.core.page.TableDataInfo info =
                new cn.CDPersonal.common.core.page.TableDataInfo();
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
