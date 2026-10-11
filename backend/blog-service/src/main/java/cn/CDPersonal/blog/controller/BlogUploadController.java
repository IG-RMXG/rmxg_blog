package cn.CDPersonal.blog.controller;

import cn.CDPersonal.blog.security.BlogTokenService;
import cn.CDPersonal.blog.util.BlogUploadPathUtils;
import cn.CDPersonal.common.core.ResponseVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 博客图片上传（对外前缀 /blog/upload）。
 *
 * 文件落到 uploads/blog，返回可访问 URL（/profile/blog/&lt;文件名&gt;），
 * 由 BlogWebMvcConfig 的 /profile/blog/** 静态映射 + 网关/vite 代理回源。
 */
@RestController
@RequestMapping("/blog")
public class BlogUploadController extends BlogBaseController {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    public BlogUploadController(BlogTokenService tokenService) {
        super(tokenService);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseVO<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (!isLogin()) {
            return ResponseVO.error(401, "登录状态已过期，请重新登录");
        }
        if (file == null || file.isEmpty()) {
            return ResponseVO.error(500, "上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            return ResponseVO.error(500, "图片大小不能超过 5MB");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        if (ext == null) {
            return ResponseVO.error(500, "仅支持 jpg/jpeg/png/gif/webp/bmp/svg 图片");
        }

        try {
            BlogUploadPathUtils.ensureBlogDir();
            Path dir = BlogUploadPathUtils.blogDir();
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(fileName).normalize();
            if (!target.startsWith(dir)) {
                return ResponseVO.error(500, "文件名非法");
            }
            file.transferTo(target);
            return ResponseVO.success(Map.of("url", BlogUploadPathUtils.BLOG_URL_PREFIX + fileName));
        } catch (IOException | IllegalStateException e) {
            return ResponseVO.error(500, "上传失败：" + e.getMessage());
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        String ext = originalFilename.substring(dot + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : null;
    }
}
