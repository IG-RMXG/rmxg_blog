package cn.CDPersonal.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

/**
 * 极简网关转发。
 *
 * 背景：gateway-service 里并没有引入 spring-cloud-gateway，所以 /system/**、
 * /auth/** 这些请求打到 8889 时直接 404，而前端 vite 代理的目标正是 8889。
 *
 * 这里用一个兜底 Controller 把请求原样转发给 system-service(8000)，
 * 保留原始路径（system-service 的 context-path 也是 /system，所以两边路径能对上）：
 *
 *   前端 /dev-api/system/menu/getRouters
 *     -> vite 去掉 /dev-api
 *     -> 8889/system/menu/getRouters
 *     -> 这里转发到 8000/system/menu/getRouters
 *
 * 需要转发的路径分两类：
 *   1. /system/**、/auth/**        —— 大部分业务接口
 *   2. /schedule/**、/code/**      —— 定时任务、代码生成（前端 url 不带 /system）
 * 第 2 类容易漏：system-service 的 context-path 是 /system，所以它们实际会命中
 * 8000 的 /system/schedule/** 和 /system/code/**，Spring 会自动补上 context-path。
 *
 * 说明：本类不会抢走 CaptchaController 的 /captcha。
 * 如果以后要接 spring-cloud-gateway，把本类删掉换成 routes 配置即可。
 */
@RestController
public class SystemServiceForwardController {

    /** system-service 地址，可用 gateway.forward.system-service 覆盖 */
    @Value("${gateway.forward.system-service:http://localhost:8000}")
    private String systemServiceBase;

    private final RestClient restClient = RestClient.create();

    @RequestMapping(
            value = {"/system/**", "/auth/**", "/schedule/**", "/code/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS}
    )
    public ResponseEntity<byte[]> forward(HttpServletRequest request,
                                         @RequestBody(required = false) byte[] body) throws IOException {

        String path = request.getRequestURI();
        String query = request.getQueryString();

        /*
         * system-service 的 context-path 是 /system，所以转发目标必须带这个前缀。
         *
         *   /system/xxx  -> 8000/system/xxx   （本来就是 /system 开头，原样用）
         *   /auth/xxx    -> 8000/system/auth/xxx
         *   /schedule/** -> 8000/system/schedule/**
         *   /code/**     -> 8000/system/code/**
         *
         * 后三类前端 url 不带 /system，如果直接拼 systemServiceBase + path，
         * 会得到 8000/schedule/... —— 落到 context-path 之外，Tomcat 直接 404。
         */
        String mappedPath = (path.startsWith("/system/") || path.equals("/system"))
                ? path
                : "/system" + path;

        String target = systemServiceBase + mappedPath + (query != null ? "?" + query : "");

        byte[] payload = body != null ? body : request.getInputStream().readAllBytes();

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
