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

    /** 工具类禁止实例化：全部是静态方法，不持有状态 */
    private BlogUploadPathUtils() {
    }

    /** 上传根目录（绝对路径，已 normalize） */
    public static Path uploadRoot() {
        // 系统属性优先于环境变量：便于本地用 -Dapp.upload.path 临时指到别的盘
        String configured = System.getProperty("app.upload.path");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("APP_UPLOAD_PATH");
        }
        if (configured == null || configured.isBlank()) {
            // 兜底：进程工作目录（IDE 里是项目根目录，java -jar 时是启动命令的 cwd）
            configured = System.getProperty("user.dir") + "/uploads";
        }
        // normalize 消除 "."、".." 与重复分隔符：上传接口用 startsWith(blogDir) 做目录穿越校验时才可靠
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    /** 博客图片目录 */
    public static Path blogDir() {
        // 所有博客图片固定落在 <uploadRoot>/blog 下，不与其他模块的上传目录混用
        return uploadRoot().resolve("blog");
    }

    /** 博客图片目录的 file: URI，供 Spring 静态资源映射使用（必须绝对路径） */
    public static String blogDirUri() {
        // toUri() 结尾是否带斜杠取决于目录是否真实存在，因此调用前应先 ensureBlogDir()，
        // 否则 Spring 静态资源映射会把它当成文件路径拼接（见 BlogWebMvcConfig）。
        return blogDir().toUri().toString();
    }

    /** 尽力创建目录；失败不抛，真正写文件时再报可读错误 */
    public static void ensureBlogDir() {
        // 启动期注册静态映射、每次上传前都会调用；失败时吞掉异常，真正写文件时会抛出可读错误
        try {
            Files.createDirectories(blogDir());
        } catch (IOException ignored) {
            // 交给上传接口返回业务错误
        }
    }
}
