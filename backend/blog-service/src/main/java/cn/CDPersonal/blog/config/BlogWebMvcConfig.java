package cn.CDPersonal.blog.config;

import cn.CDPersonal.blog.util.BlogUploadPathUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 博客图片的静态资源映射。
 *
 * 上传接口把文件写到 uploads/blog，前端拿到的 URL 形如 /profile/blog/xxx.png，
 * 这里注册 handler 把它映射回磁盘；网关负责把 /profile/blog/** 转发到本服务。
 */
@Configuration
public class BlogWebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册 /profile/blog/** → 磁盘 uploads/blog 的静态资源映射。
     *
     * 必须先 ensureBlogDir()：目录不存在时 Path.toUri() 不会补结尾斜杠，
     * addResourceLocations 会把该位置当成文件而非目录来拼接，请求图片会全部 404。
     *
     * 映射只读，不处理上传（上传见 BlogUploadController）；
     * 用 -Dapp.upload.path 换目录时，此处与上传接口会同时生效。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        BlogUploadPathUtils.ensureBlogDir();
        registry.addResourceHandler(BlogUploadPathUtils.BLOG_URL_PREFIX + "**")
                .addResourceLocations(BlogUploadPathUtils.blogDirUri());
    }
}
