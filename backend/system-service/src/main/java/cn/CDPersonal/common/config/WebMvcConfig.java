package cn.CDPersonal.common.config;

import cn.CDPersonal.common.core.utils.UploadPathUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 上传文件的静态资源映射。
 *
 * 头像上传接口（POST /system/user/profile/avatar）把文件写到本地磁盘，
 * 前端拿到 imgUrl 后要能直接当图片地址用（<img :src="imgUrl"> 再经 vite/网关代理）。
 * 不注册 handler 的话这个 URL 会 404，头像上传"成功"但页面显示裂图。
 *
 * 因为 server.servlet.context-path = /system，前端最终请求的是
 * /system/profile/avatar/xxx.png，这里只写业务段 /profile/avatar/**。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        UploadPathUtils.ensureAvatarDir();
        registry.addResourceHandler(UploadPathUtils.AVATAR_URL_PREFIX + "**")
                .addResourceLocations(UploadPathUtils.avatarDirUri());
    }
}
