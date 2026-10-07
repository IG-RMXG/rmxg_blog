package cn.CDPersonal.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cd.captcha")
public class CaptchaConfiguration {

    private Paths paths = new Paths();

    private List< String> allowedExtensions = List.of("jpg", "png");

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
