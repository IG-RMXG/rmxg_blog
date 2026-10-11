package cn.CDPersonal.blog.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 博客上传目录解析（上传接口写文件 / 静态资源映射读文件，两处必须指向同一目录）。
 *
 * 解析优先级：
 *   -Dapp.upload.path   &gt;   环境变量 APP_UPLOAD_PATH   &gt;   进程工作目录/uploads
 *
 * 对外访问路径固定为 /profile/blog/&lt;文件名&gt;（本服务无 context-path，
 * 网关把 /profile/blog/** 转发到这里）。
 */
public final class BlogUploadPathUtils {

    /** 博客图片的对外 URL 前缀 */
    public static final String BLOG_URL_PREFIX = "/profile/blog/";

    private BlogUploadPathUtils() {
    }

    /** 上传根目录（绝对路径，已 normalize） */
    public static Path uploadRoot() {
        String configured = System.getProperty("app.upload.path");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("APP_UPLOAD_PATH");
        }
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("user.dir") + "/uploads";
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    /** 博客图片目录 */
    public static Path blogDir() {
        return uploadRoot().resolve("blog");
    }

    /** 博客图片目录的 file: URI，供 Spring 静态资源映射使用（必须绝对路径） */
    public static String blogDirUri() {
        return blogDir().toUri().toString();
    }

    /** 尽力创建目录；失败不抛，真正写文件时再报可读错误 */
    public static void ensureBlogDir() {
        try {
            Files.createDirectories(blogDir());
        } catch (IOException ignored) {
            // 交给上传接口返回业务错误
        }
    }
}
