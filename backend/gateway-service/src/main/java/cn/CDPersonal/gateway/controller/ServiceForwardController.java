package cn.CDPersonal.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * 极简网关转发（多后端）。
 *
 * 背景：gateway-service 里并没有引入 spring-cloud-gateway，所以业务请求打到 8889
 * 时直接 404，而前端 vite 代理的目标正是 8889。这里用一个兜底 Controller
 * 按路径前缀把请求原样转发给对应后端：
 *
 *   /blog/**            -> blog-service(8010)    博客业务接口（路径原样）
 *   /profile/blog/**    -> blog-service(8010)    博客上传图片（路径原样）
 *   /system/**          -> system-service(8000)  系统业务接口（路径原样）
 *   /auth/**            -> system-service(8000)  登录/登出（补 context-path /system）
 *   /schedule/**        -> system-service(8000)  定时任务（补 context-path /system）
 *   /code/**            -> system-service(8000)  代码生成（补 context-path /system）
 *   /profile/**         -> system-service(8000)  头像等上传文件（补 context-path /system）
 *
 * 为什么要区分「补 /system」：system-service 的 context-path 是 /system，
 * 而 blog-service 没有 context-path。若一律拼接，/blog/** 会落到 blog-service
 * 的 /blog/blog/**（404）；/auth/** 这类不带 /system 的又会落到 system-service
 * context-path 之外（Tomcat 404）。
 *
 * 前端调用示例：
 *   /dev-api/blog/article/list   -> vite 去掉 /dev-api
 *     -> 8889/blog/article/list  -> 这里转发到 8010/blog/article/list
 *   /dev-api/system/menu/getRouters
 *     -> 8889/system/menu/getRouters -> 转发到 8000/system/menu/getRouters
 *
 * 说明：本类不会抢走 CaptchaController 的 /captcha。
 * 如果以后要接 spring-cloud-gateway，把本类删掉换成 routes 配置即可。
 *
 * <p>日志：前端所有请求都先经过这里，所以转发日志是排查问题的第一站。
 * 每个请求会打三条日志（logger 名 cn.CDPersonal.gateway）：
 * <pre>
 * 收到请求  POST /auth/login -> http://localhost:8000/system/auth/login
 * 下游响应  POST /auth/login 200 (52 ms) -> http://localhost:8000/system/auth/login? 等
 * </pre>
 * 另外会生成/透传 X-Trace-Id 给下游，配合 RequestLoggingFilter 的 MDC，
 * 可以用同一个 traceId 把网关和下游服务的日志串起来。
 */
@RestController
public class ServiceForwardController {

    private static final Logger log = LoggerFactory.getLogger(ServiceForwardController.class);

    /** 链路追踪头：透传给下游，下游 RequestLoggingFilter 会读它放进 MDC */
    private static final String TRACE_HEADER = "X-Trace-Id";

    /**
     * 逐跳（Hop-by-hop）请求头，按 RFC 7230 不允许被代理转发。
     *
     * Content-Length 也在里面：它由 RestClient 按实际 body 重算，
     * 原样带过去会和新 body 长度不符，导致下游解析失败。
     * Transfer-Encoding / Connection 原样转发还可能导致请求挂起。
     */
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "host", "content-length", "connection", "keep-alive",
            "transfer-encoding", "upgrade", "proxy-authenticate",
            "proxy-authorization", "te", "trailer"
    );

    /** system-service 地址，可用 gateway.forward.system-service 覆盖 */
    @Value("${gateway.forward.system-service:http://localhost:8000}")
    private String systemServiceBase;

    /** blog-service 地址，可用 gateway.forward.blog-service 覆盖 */
    @Value("${gateway.forward.blog-service:http://localhost:8010}")
    private String blogServiceBase;

    /**
     * UAT 测试服务地址。默认为空（DEV 不路由 /uat/**，直接 404）；
     * 只有 UAT 环境启动网关时设置 GATEWAY_UAT_SERVICE=http://localhost:8020 才生效。
     * 这是「测试功能不进 DEV」的第一道闸门。
     */
    @Value("${gateway.forward.uat-service:}")
    private String uatServiceBase;

    private final RestClient restClient = RestClient.create();

    @RequestMapping(
            value = {"/system/**", "/auth/**", "/schedule/**", "/code/**", "/profile/**", "/blog/**", "/uat/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS}
    )
    public ResponseEntity<byte[]> forward(HttpServletRequest request) throws IOException {

        String path = request.getRequestURI();
        String query = request.getQueryString();
        String target = resolveTarget(path);

        // /uat/** 但未配置 uat-service：DEV 环境的默认姿态，明确 404 而不是 502
        if (target == null) {
            log.warn("拒绝 {} {}: gateway.forward.uat-service 未配置（DEV 默认不路由 UAT 测试功能）",
                    request.getMethod(), path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":404,\"msg\":\"uat service not routed\"}"
                            .getBytes(StandardCharsets.UTF_8));
        }
        String fullTarget = target + (query != null ? "?" + query : "");

        // 直接读原始请求体：网关不解析 multipart（见 gateway-service 的 application.yml），
        // 这样文件上传的字节流能原样透传给下游服务。
        byte[] payload = request.getInputStream().readAllBytes();

        String method = request.getMethod();
        String traceId = resolveTraceId(request);
        String displayPath = query == null ? path : path + "?" + query;

        if (log.isDebugEnabled()) {
            // 摘要日志：Body 不打印（登录请求含明文密码，日志要长期留存，打出来是泄密）
            log.debug("转发 {} {} -> {} ({} 字节{}) [traceId={}]",
                    method, displayPath, fullTarget, payload.length,
                    payload.length > 0 ? ", " + describeContentType(request) : "",
                    traceId);
        }

        RestClient.RequestBodySpec spec = restClient.method(org.springframework.http.HttpMethod.valueOf(method))
                .uri(URI.create(fullTarget))
                .headers(headers -> {
                    Collections.list(request.getHeaderNames()).forEach(name -> {
                        // 逐跳头和长度头不能原样带过去，见 HOP_BY_HOP_HEADERS 注释
                        if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                            headers.add(name, request.getHeader(name));
                        }
                    });
                    // 把 traceId 透传给下游，两边日志能对上
                    headers.set(TRACE_HEADER, traceId);
                });

        if (payload.length > 0) {
            spec.contentType(resolveContentType(request));
            spec.body(payload);
        }

        long start = System.currentTimeMillis();
        try {
            ResponseEntity<byte[]> result = spec.exchange((req, res) -> {
                byte[] responseBody = res.bodyTo(byte[].class);
                HttpHeaders outHeaders = new HttpHeaders();
                MediaType contentType = res.getHeaders().getContentType();
                if (contentType != null) {
                    outHeaders.setContentType(contentType);
                }
                return ResponseEntity.status(res.getStatusCode())
                        .headers(outHeaders)
                        .body(responseBody);
            });

            long cost = System.currentTimeMillis() - start;
            int status = result.getStatusCode().value();
            int bodySize = result.getBody() == null ? 0 : result.getBody().length;

            if (status >= 500) {
                log.error("下游返回 {} {} ({} ms, {} 字节) -> {} [traceId={}]",
                        status, displayPath, cost, bodySize, fullTarget, traceId);
            } else if (status >= 400) {
                log.warn("下游返回 {} {} ({} ms, {} 字节) -> {} [traceId={}]",
                        status, displayPath, cost, bodySize, fullTarget, traceId);
            } else {
                log.info("下游返回 {} {} ({} ms, {} 字节) -> {} [traceId={}]",
                        status, displayPath, cost, bodySize, fullTarget, traceId);
            }
            return result;

        } catch (RestClientResponseException e) {
            // RestClient 默认把 4xx/5xx 当异常抛，这里兜住并记下状态码，避免日志里只剩一句"异常"
            long cost = System.currentTimeMillis() - start;
            int status = e.getStatusCode().value();
            log.error("下游报错 {} {} ({} ms) -> {} [traceId={}] : {}",
                    status, displayPath, cost, fullTarget, traceId, e.getMessage());
            throw e;

        } catch (Exception e) {
            // 连不上下游（服务没起 / 端口不通）：这是"登录报后端接口连接异常"最常见的原因
            long cost = System.currentTimeMillis() - start;
            log.error("转发失败 {} {} ({} ms) -> {} [traceId={}] : {}（下游服务是否已启动？）",
                    method, displayPath, cost, fullTarget, traceId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 按路径前缀解析真实后端地址；返回 null 表示该路由在当前环境未启用。
     *
     * 注意顺序：/profile/blog/** 必须先于通用的 /profile/** 判定，
     * 否则博客图片会被转发到 system-service（那里已经没有 /profile/blog/** 映射）。
     */
    private String resolveTarget(String path) {
        // UAT 测试服务：只在配置了地址（即 UAT 环境）时路由，路径原样
        if (isUatPath(path)) {
            if (uatServiceBase == null || uatServiceBase.isBlank()) {
                return null;
            }
            return uatServiceBase + path;
        }
        if (isBlogPath(path)) {
            // blog-service 无 context-path，路径原样使用
            return blogServiceBase + path;
        }
        // 其余都归 system-service，且必须带上它的 context-path /system
        String mappedPath = (path.startsWith("/system/") || path.equals("/system"))
                ? path
                : "/system" + path;
        return systemServiceBase + mappedPath;
    }

    private boolean isUatPath(String path) {
        return path.startsWith("/uat/") || path.equals("/uat");
    }

    private boolean isBlogPath(String path) {
        return path.startsWith("/blog/")
                || path.equals("/blog")
                || path.startsWith("/profile/blog/");
    }

    private MediaType resolveContentType(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return MediaType.APPLICATION_JSON;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_JSON;
        }
    }

    /** 日志里用：只描述 Content-Type，不碰真实 body */
    private String describeContentType(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType == null ? "无 Content-Type" : contentType;
    }

    /**
     * 沿用调用方带来的 X-Trace-Id；没有就生成一个。
     * RequestLoggingFilter 也会做同样的事，这里读它放进 MDC 的值能保证两边一致。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String fromMdc = MDC.get("traceId");
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        String header = request.getHeader(TRACE_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
