package cn.CDPersonal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 扫描根即本类所在包 cn.CDPersonal（controller/service/mapper/config 都在其下）；
// 没有额外 @MapperScan，Mapper 接口各自标注 @Mapper，由 MyBatis 自动配置注册。
/**
 * 博客服务启动类（独立微服务）。
 *
 * 端口 8010，无 context-path：控制器直接挂在 /blog/** 下，
 * 网关把 /blog/** 与 /profile/blog/** 转发到这里。
 */
@SpringBootApplication
public class BlogApplication {

    /**
     * 进程入口：完成内嵌 Tomcat 启动与本模块 Bean 的装配。
     *
     * 端口、数据源、Redis、分页等配置全部来自 application.yml（可用 BLOG_* 环境变量覆盖）；
     * 本方法返回时 Web 容器已就绪，随后才打印启动横幅。
     */
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
        // 仅用于人工确认，不写日志文件；容器部署时看 stdout 即可
        System.out.println("╰(*°▽°*)╯  博客服务启动成功   ψ(｀∇´)ψ  \n");
    }
}
