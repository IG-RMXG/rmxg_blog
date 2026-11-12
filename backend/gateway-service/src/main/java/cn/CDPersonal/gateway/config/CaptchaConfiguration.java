package cn.CDPersonal.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 拼图验证码的图片配置，前缀 {@code cd.captcha}，可在 application.yml 里覆盖。
 *
 * <p>{@code paths} 指向 classpath 下的图片目录：originPath 为底图，slidingBlockPath 为拼图模板，
 * 每次生成验证码时从目录里随机挑一张，不需要逐张登记。
 *
 * <p>{@code allowed-extensions} 决定扫描目录时把哪些后缀当作图片；比较前文件名会转小写，
 * 因此配置里只写小写、不带点即可（网关的 application.yml 额外放开了 jpeg/webp）。
 *
 * <p>注意：这两个目录由实现类按文件系统路径读取，打成 jar 后无法遍历，详见 BlockPuzzleCaptchaServiceImpl。
 */
@Configuration
@ConfigurationProperties(prefix = "cd.captcha")
public class CaptchaConfiguration {

    /** 图片目录配置，默认指向打包进来的 classpath 目录 */
    private Paths paths = new Paths();

    /** 允许的图片扩展名（小写、不带点），与目录内文件比较时大小写无关 */
    private List< String> allowedExtensions = List.of("jpg", "png");

    /**
     * 图片目录：originPath 放底图，slidingBlockPath 放拼图块模板（黑色部分为拼图块、其余为背景的蒙版）。
     * 取值是 Spring Resource 的写法（classpath:/...），由 ResourceLoader 解析。
     */
    public static class Paths {
        private String originPath = "classpath:/captcha/images/jigsaw";
        private String slidingBlockPath = "classpath:/captcha/images/slidingBlock";


        public String getOriginPath() {
            return originPath;
        }

        public void setOriginPath(String originPath) {
            this.originPath = originPath;
        }

        public String getSlidingBlockPath() {
            return slidingBlockPath;
        }

        public void setSlidingBlockPath(String slidingBlockPath) {
            this.slidingBlockPath = slidingBlockPath;
        }
    }

    public Paths getPaths() {
        return paths;
    }

    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
