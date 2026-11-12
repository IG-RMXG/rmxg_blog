package cn.CDPersonal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * system-service 启动类。
 *
 * 扫描根为 cn.CDPersonal，本服务内包含 auth / user / role / tool.gen / common 等包；
 * 数据源（ry-cloud 库）与 Redis（登录 token）均在 application.yml 中配置；
 * context-path 为 /system，接口对外都是 /system/xxx（如 /system/code/gen/list）。
 */
/*
 * 原来的 exclude = {DataSourceAutoConfiguration.class} 会导致
 * application.yml 里的 spring.datasource 配置完全不生效（数据源不被创建），
 * 菜单/用户查询拿不到数据库连接，因此这里必须去掉。
 */
@SpringBootApplication
public class SystemApplication {
    // 启动横幅只用于人工确认启动完成，不参与健康检查；启动失败时由 Spring Boot 抛异常退出。
    public static void main(String[] args)
    {
        SpringApplication.run(SystemApplication.class, args);
        System.out.println("╰(*°▽°*)╯  系统启动成功   ψ(｀∇´)ψ  \n");
    }
}