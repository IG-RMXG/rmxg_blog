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

    /** 允许上传的图片后缀（小写）；只比对扩展名，不校验文件真实内容 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    /** 单文件大小上限 5MB：恰好 5MB 放行，超过才拒绝；框架 multipart 上限是 10MB，更宽松 */
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    public BlogUploadController(BlogTokenService tokenService) {
        super(tokenService);
    }

    /**
     * 上传单张图片（已登录用户均可，不限管理员）。成功返回 {url}，可直接写进 Markdown/富文本。
     *
     * 校验顺序：登录态 → 非空 → 大小 → 后缀白名单；全部通过后才碰磁盘，
     * 非法请求不会创建 uploads/blog 目录。落盘文件名由服务端生成，不使用原始文件名，
     * 因此重复上传不会互相覆盖，也天然规避了原始文件名带路径的问题。
     */
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
            // 兜底防目录穿越：文件名已是 UUID，normalize 后再确认仍在博客目录内；
            // 将来若改回拼接原始文件名，这一层能挡住 ../ 逃逸。
            if (!target.startsWith(dir)) {
                return ResponseVO.error(500, "文件名非法");
            }
            file.transferTo(target);
            // 只返回站内相对路径：协议、域名、端口由网关/vite 代理和前端 baseURL 决定，换环境不用改库
            return ResponseVO.success(Map.of("url", BlogUploadPathUtils.BLOG_URL_PREFIX + fileName));
        } catch (IOException | IllegalStateException e) {
            return ResponseVO.error(500, "上传失败：" + e.getMessage());
        }
    }

    /**
     * 取小写扩展名并比对白名单：没有点、以点结尾（如 "a."）、后缀不在白名单都返回 null，
     * 由调用方统一按"不支持的图片类型"处理。仅凭扩展名判断，不读文件头/MIME。
     */
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
