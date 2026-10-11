package cn.CDPersonal.common.core.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求日志过滤器：把每一次 HTTP 请求的「谁、请求了什么、结果如何、耗时多少」打进日志。
 *
 * <p>解决的问题：以前请求 /blog/**、/auth/login 时 IDEA 控制台和文件里什么都没有，
 * 只能靠猜。挂上这个过滤器后，每个请求都会有一行进/出一对的日志。
 *
 * <p>输出样例：
 * <pre>
 * 2026-02-14 10:30:01.123 DEBUG [c.C.request.RequestLoggingFilter] : --> POST /system/auth/login from 127.0.0.1
 * 2026-02-14 10:30:01.456 DEBUG [c.C.request.RequestLoggingFilter] : <-- POST /system/auth/login 200 (333 ms)
 * </pre>
 *
 * <p>设计要点：
 * <ul>
 *   <li>继承 {@link OncePerRequestFilter}，异步/转发场景下不会重复打印。</li>
 *   <li>{@code @Order(HIGHEST_PRECEDENCE + 10)} 让它尽量靠前，能覆盖后面所有过滤器与控制器。</li>
 *   <li>用 MDC 放一个 traceId，同一次请求里的其它日志（Service/Mapper）都会带上它，
 *       排查时可以直接按 traceId 串起整条链路。</li>
 *   <li>日志 logger 名固定为 {@code cn.CDPersonal.request}，logback-spring.xml 里可以单独调它的级别。</li>
 *   <li>请求体 <b>不</b> 打印：登录请求里有明文密码，文件日志会长期留存，打出来是泄密。</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** 独立 logger 名，便于在 logback-spring.xml 里单独控制级别 */
    private static final Logger log = LoggerFactory.getLogger("cn.CDPersonal.request");

    /** MDC key：链路追踪 id */
    private static final String TRACE_ID = "traceId";

    /** 请求头里可以带这个字段来自定义 traceId，方便前端/网关透传 */
    private static final String TRACE_HEADER = "X-Trace-Id";

    /** 超过这个耗时就额外打一条 WARN，方便发现慢接口 */
    private static final long SLOW_REQUEST_MILLIS = 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = resolveTraceId(request);
        MDC.put(TRACE_ID, traceId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null ? uri : uri + "?" + query;

        long start = System.currentTimeMillis();
        log.info("--> {} {} from {} [traceId={}]", method, fullPath, clientIp(request), traceId);

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 异常必须单独记：否则只会看到"没有出日志"，看不出是抛异常了
            long cost = System.currentTimeMillis() - start;
            log.error("<!- {} {} 抛出异常 ({} ms) [traceId={}] : {}",
                    method, fullPath, cost, traceId, e.getMessage(), e);
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            int status = response.getStatus();

            // 按状态码选级别：5xx 错误、4xx 警告、2xx/3xx 正常，扫日志时一眼能看出问题
            if (status >= 500) {
                log.error("<-- {} {} {} ({} ms) [traceId={}]", method, fullPath, status, cost, traceId);
            } else if (status >= 400) {
                log.warn("<-- {} {} {} ({} ms) [traceId={}]", method, fullPath, status, cost, traceId);
            } else if (cost >= SLOW_REQUEST_MILLIS) {
                log.warn("<-- {} {} {} ({} ms, 慢请求) [traceId={}]", method, fullPath, status, cost, traceId);
            } else {
                log.info("<-- {} {} {} ({} ms) [traceId={}]", method, fullPath, status, cost, traceId);
            }

            MDC.remove(TRACE_ID);
        }
    }

    /**
     * 优先用调用方传来的 X-Trace-Id（网关转发时会带上），
     * 没有就自己生成一个，保证每条请求日志都能单独定位。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 取真实客户端 IP。
     * 前端 -> vite 代理 -> 网关 -> 本服务，request.getRemoteAddr() 只会拿到网关的地址，
     * 所以要优先看 X-Forwarded-For / X-Real-IP。
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For 可能是 "客户端IP, 代理1, 代理2"，取第一段才是真实客户端
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}