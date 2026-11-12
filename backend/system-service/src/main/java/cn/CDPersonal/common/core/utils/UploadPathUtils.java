package cn.CDPersonal.common.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传目录解析。
 *
 * 头像上传（POST /system/user/profile/avatar）写文件，静态资源映射
 * （WebMvcConfig 的 /profile/avatar/**）读文件，两边必须指向同一个目录，
 * 所以解析逻辑抽到这里，避免两处各写一份再写歪。
 *
 * 优先级：-Dapp.upload.path > 环境变量 APP_UPLOAD_PATH > 进程工作目录/uploads。
 * 对外访问路径固定为 {context-path}/profile/avatar/<文件名>。
 */
public final class UploadPathUtils {

    /** 对外头像 URL 前缀；只写业务段（不带 context-path），WebMvcConfig 映射与上传接口共用 */
    public static final String AVATAR_URL_PREFIX = "/profile/avatar/";

    private UploadPathUtils() {
    }

    /** 上传根目录（绝对路径，已 normalize） */
    // 每次调用都重读配置（不缓存），便于测试时用 -Dapp.upload.path 切换目录；
    // normalize 后再返回，避免路径里残留多余的 ".."。
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

    /** 头像目录 */
    public static Path avatarDir() {
        return uploadRoot().resolve("avatar");
    }

    /** 头像目录的 file: URI，供 Spring 静态资源映射使用（必须是绝对路径） */
    // URI 必须以 "/" 结尾，Spring 才会当成目录前缀；否则拼接时最后一段会被当作文件名。
    public static String avatarDirUri() {
        return avatarDir().toUri().toString();
    }

    /** 尽力创建头像目录；失败不抛，真正写文件时再报可读错误 */
    // 幂等：目录已存在时 createDirectories 不报错，可在启动或每次上传前重复调用。
    public static void ensureAvatarDir() {
        try {
            Files.createDirectories(avatarDir());
        } catch (IOException ignored) {
            // 交给上传接口返回业务错误
        }
    }
}
