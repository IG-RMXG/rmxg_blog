package cn.CDPersonal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 博客服务启动类（独立微服务）。
 *
 * 端口 8010，无 context-path：控制器直接挂在 /blog/** 下，
 * 网关把 /blog/** 与 /profile/blog/** 转发到这里。
 */
@SpringBootApplication
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
        System.out.println("╰(*°▽°*)╯  博客服务启动成功   ψ(｀∇´)ψ  \n");
    }
}
