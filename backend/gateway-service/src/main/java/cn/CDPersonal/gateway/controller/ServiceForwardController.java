package cn.CDPersonal.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

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
 */
@RestController
public class ServiceForwardController {

    /** system-service 地址，可用 gateway.forward.system-service 覆盖 */
    @Value("${gateway.forward.system-service:http://localhost:8000}")
    private String systemServiceBase;

    /** blog-service 地址，可用 gateway.forward.blog-service 覆盖 */
    @Value("${gateway.forward.blog-service:http://localhost:8010}")
    private String blogServiceBase;

    private final RestClient restClient = RestClient.create();

    @RequestMapping(
            value = {"/system/**", "/auth/**", "/schedule/**", "/code/**", "/profile/**", "/blog/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS}
    )
    public ResponseEntity<byte[]> forward(HttpServletRequest request) throws IOException {

        String path = request.getRequestURI();
        String query = request.getQueryString();
        String target = resolveTarget(path) + (query != null ? "?" + query : "");

        // 直接读原始请求体：网关不解析 multipart（见 gateway-service 的 application.yml），
        // 这样文件上传的字节流能原样透传给下游服务。
        byte[] payload = request.getInputStream().readAllBytes();

        RestClient.RequestBodySpec spec = restClient.method(org.springframework.http.HttpMethod.valueOf(request.getMethod()))
                .uri(URI.create(target))
                .headers(headers -> {
                    Collections.list(request.getHeaderNames()).forEach(name -> {
                        // Host / Content-Length 由客户端重算，转发会导致长度不匹配
                        if (!"host".equalsIgnoreCase(name) && !"content-length".equalsIgnoreCase(name)) {
                            headers.add(name, request.getHeader(name));
                        }
                    });
                });

        if (payload.length > 0) {
            spec.contentType(resolveContentType(request));
            spec.body(payload);
        }

        return spec.exchange((req, res) -> {
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
    }

    /**
     * 按路径前缀解析真实后端地址。
     *
     * 注意顺序：/profile/blog/** 必须先于通用的 /profile/** 判定，
     * 否则博客图片会被转发到 system-service（那里已经没有 /profile/blog/** 映射）。
     */
    private String resolveTarget(String path) {
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
}
